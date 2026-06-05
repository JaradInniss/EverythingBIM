package com.example.everythingbim.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.admin.AdminActivity;
import com.example.everythingbim.ui.admin.AdminDatasetSubmissionDetailFragment;
import com.example.everythingbim.ui.admin.AdminNotificationHelper;
import com.example.everythingbim.ui.main.MainActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class NotificationsActivity extends AppCompatActivity {
    public static final String EXTRA_NOTIFICATION_MODE = "notification_mode";
    public static final String MODE_USER = "user";
    public static final String MODE_ADMIN = "admin";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    private RecyclerView notificationsRecycler;
    private TextView emptyStateTv;
    private NotificationGroupAdapter adapter;
    private TextView titleTv;
    private String notificationMode = MODE_USER;
    private final Set<String> expandedNotificationIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        notificationMode = resolveMode(getIntent());

        notificationsRecycler = findViewById(R.id.notifications_recycler);
        emptyStateTv = findViewById(R.id.notifications_empty_state);
        titleTv = findViewById(R.id.notifcation_title_txt);
        notificationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationGroupAdapter();
        notificationsRecycler.setAdapter(adapter);
        titleTv.setText(MODE_ADMIN.equals(notificationMode) ? "Admin Notifications" : "Notifications");

        findViewById(R.id.return_bttn).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindNotifications();
    }

    @Override
    protected void onStop() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        super.onStop();
    }

    private void bindNotifications() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (MODE_ADMIN.equals(notificationMode)) {
            listenerRegistration = db.collection(AdminNotificationHelper.COLLECTION_ADMIN_NOTIFICATIONS)
                    .addSnapshotListener((snap, error) -> {
                        if (error != null) {
                            showEmptyState("Could not load notifications.");
                            return;
                        }
                        renderNotificationSnapshot(snap, true);
                    });
            return;
        }

        if (auth.getCurrentUser() == null) {
            showEmptyState("Log in to view notifications.");
            return;
        }

        listenerRegistration = db.collection(UserNotificationHelper.COLLECTION_USER_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", auth.getCurrentUser().getUid())
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        showEmptyState("Could not load notifications.");
                        return;
                    }
                    renderNotificationSnapshot(snap, false);
                });
    }

    private void renderNotificationSnapshot(@Nullable com.google.firebase.firestore.QuerySnapshot snap,
                                            boolean adminMode) {
        if (snap == null || snap.isEmpty()) {
            showEmptyState("No notifications yet.");
            return;
        }

        List<NotificationItem> items = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            NotificationItem item = NotificationItem.fromDocument(doc, adminMode);
            items.add(item);
        }

        Collections.sort(items, Comparator.comparing(
                (NotificationItem item) -> item.createdAt != null ? item.createdAt.toDate() : null,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        adapter.submitGroups(groupNotifications(items));
        notificationsRecycler.setVisibility(View.VISIBLE);
        emptyStateTv.setVisibility(View.GONE);
    }

    private void showEmptyState(@NonNull String message) {
        notificationsRecycler.setVisibility(View.GONE);
        emptyStateTv.setVisibility(View.VISIBLE);
        emptyStateTv.setText(message);
        adapter.submitGroups(new ArrayList<>());
    }

    @NonNull
    private List<NotificationGroup> groupNotifications(@NonNull List<NotificationItem> items) {
        Map<String, List<NotificationItem>> grouped = new LinkedHashMap<>();
        for (NotificationItem item : items) {
            String label = formatGroupLabel(item.createdAt);
            List<NotificationItem> group = grouped.get(label);
            if (group == null) {
                group = new ArrayList<>();
                grouped.put(label, group);
            }
            group.add(item);
        }

        List<NotificationGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<NotificationItem>> entry : grouped.entrySet()) {
            groups.add(new NotificationGroup(entry.getKey(), entry.getValue()));
        }
        return groups;
    }

    @NonNull
    private String formatGroupLabel(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "Earlier";
        }

        Calendar itemCal = Calendar.getInstance();
        itemCal.setTime(timestamp.toDate());

        Calendar today = Calendar.getInstance();
        if (isSameDay(itemCal, today)) {
            return "Today";
        }

        today.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(itemCal, today)) {
            return "Yesterday";
        }

        return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(timestamp.toDate());
    }

    private boolean isSameDay(@NonNull Calendar one, @NonNull Calendar two) {
        return one.get(Calendar.YEAR) == two.get(Calendar.YEAR)
                && one.get(Calendar.DAY_OF_YEAR) == two.get(Calendar.DAY_OF_YEAR);
    }

    private void toggleNotification(@NonNull NotificationItem item, @NonNull ListView listView) {
        boolean expand = !expandedNotificationIds.contains(item.id);
        if (expand) {
            expandedNotificationIds.add(item.id);
            if (!item.read) {
                markNotificationRead(item);
            }
        } else {
            expandedNotificationIds.remove(item.id);
        }

        adapter.notifyDataSetChanged();
        setListViewHeightBasedOnChildren(listView);
    }

    private void markNotificationRead(@NonNull NotificationItem item) {
        db.collection(item.collectionName)
                .document(item.id)
                .update("read", true);

        if (MODE_ADMIN.equals(notificationMode)
                && item.relatedCollection != null
                && !item.relatedCollection.trim().isEmpty()
                && item.relatedDocId != null
                && !item.relatedDocId.trim().isEmpty()) {
            db.collection(item.relatedCollection)
                    .document(item.relatedDocId)
                    .update("read", true);
        }
    }

    private void openNotificationTarget(@NonNull NotificationItem item) {
        if (MODE_ADMIN.equals(notificationMode)) {
            openAdminTarget(item);
            return;
        }
        openUserTarget(item);
    }

    private void openAdminTarget(@NonNull NotificationItem item) {
        if (AdminNotificationHelper.TARGET_INFO_DETAIL.equals(item.targetScreen)
                || AdminNotificationHelper.TARGET_LOCATION_DETAIL.equals(item.targetScreen)
                || AdminNotificationHelper.TARGET_DATASET_DETAIL.equals(item.targetScreen)) {
            Intent intent = new Intent(this, AdminActivity.class);
            intent.putExtra(AdminActivity.EXTRA_NOTIFICATION_TARGET, item.targetScreen);
            intent.putExtra(AdminActivity.EXTRA_NOTIFICATION_DOC_ID, item.relatedDocId);
            startActivity(intent);
            return;
        }

        showNotificationDialog(item.title, item.message);
    }

    private void openUserTarget(@NonNull NotificationItem item) {
        if (UserNotificationHelper.TARGET_COMPLETED_INFO.equals(item.targetScreen)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_VIEW_COMPLETED_INFORMATION_REQUESTS);
            startActivity(intent);
            return;
        }

        if (UserNotificationHelper.TARGET_COMPLETED_LOCATION.equals(item.targetScreen)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_VIEW_COMPLETED_LOCATION_REQUESTS);
            startActivity(intent);
            return;
        }

        if (UserNotificationHelper.TARGET_COMPLETED_BUSINESS_VERIFICATION.equals(item.targetScreen)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_VIEW_COMPLETED_ACCOUNT_VERIFICATION_REQUESTS);
            startActivity(intent);
            return;
        }

        showNotificationDialog(item.title, item.message);
    }

    private void showNotificationDialog(@NonNull String title, @NonNull String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @NonNull
    private String resolveMode(@Nullable Intent intent) {
        if (intent == null) {
            return MODE_USER;
        }
        String rawMode = intent.getStringExtra(EXTRA_NOTIFICATION_MODE);
        if (MODE_ADMIN.equalsIgnoreCase(rawMode)) {
            return MODE_ADMIN;
        }
        return MODE_USER;
    }

    private static class NotificationGroup {
        final String label;
        final List<NotificationItem> items;

        NotificationGroup(String label, List<NotificationItem> items) {
            this.label = label;
            this.items = items;
        }
    }

    private static class NotificationItem {
        final String id;
        final String collectionName;
        final String title;
        final String message;
        final String type;
        final boolean read;
        final Timestamp createdAt;
        final String status;
        final String relatedDocId;
        final String relatedCollection;
        final String targetScreen;
        final String targetType;
        final String entityName;

        NotificationItem(String id,
                         String collectionName,
                         String title,
                         String message,
                         String type,
                         boolean read,
                         @Nullable Timestamp createdAt,
                         String status,
                         String relatedDocId,
                         String relatedCollection,
                         String targetScreen,
                         String targetType,
                         String entityName) {
            this.id = id;
            this.collectionName = collectionName;
            this.title = title;
            this.message = message;
            this.type = type;
            this.read = read;
            this.createdAt = createdAt;
            this.status = status;
            this.relatedDocId = relatedDocId;
            this.relatedCollection = relatedCollection;
            this.targetScreen = targetScreen;
            this.targetType = targetType;
            this.entityName = entityName;
        }

        static NotificationItem fromDocument(@NonNull DocumentSnapshot doc, boolean adminMode) {
            String title = firstNonEmpty(doc.getString("title"), "Notification");
            String message = firstNonEmpty(doc.getString("message"), "You have an update.");
            String type = firstNonEmpty(doc.getString("type"), UserNotificationHelper.TYPE_INFO_REQUEST);
            boolean read = Boolean.TRUE.equals(doc.getBoolean("read"));
            String status = firstNonEmpty(doc.getString("status"), "In Review");
            String relatedDocId = firstNonEmpty(doc.getString("relatedDocId"), "");
            String relatedCollection = firstNonEmpty(doc.getString("relatedCollection"), "");
            String targetScreen = firstNonEmpty(doc.getString("targetScreen"), "");
            String targetType = firstNonEmpty(doc.getString("targetType"), type);
            String entityName = firstNonEmpty(doc.getString("entityName"), "");
            String collection = adminMode
                    ? AdminNotificationHelper.COLLECTION_ADMIN_NOTIFICATIONS
                    : UserNotificationHelper.COLLECTION_USER_NOTIFICATIONS;

            return new NotificationItem(
                    doc.getId(),
                    collection,
                    title,
                    message,
                    type,
                    read,
                    doc.getTimestamp("createdAt"),
                    status,
                    relatedDocId,
                    relatedCollection,
                    targetScreen,
                    targetType,
                    entityName
            );
        }

        @NonNull
        private static String firstNonEmpty(String... values) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
            return "";
        }
    }

    private class NotificationGroupAdapter extends RecyclerView.Adapter<NotificationGroupAdapter.GroupViewHolder> {
        private final List<NotificationGroup> groups = new ArrayList<>();

        void submitGroups(@NonNull List<NotificationGroup> newGroups) {
            groups.clear();
            groups.addAll(newGroups);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification_group, parent, false);
            return new GroupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
            NotificationGroup group = groups.get(position);
            holder.dateTv.setText(group.label);
            NotificationItemAdapter itemAdapter = new NotificationItemAdapter(group.items, holder.listView);
            holder.listView.setAdapter(itemAdapter);
            setListViewHeightBasedOnChildren(holder.listView);
        }

        @Override
        public int getItemCount() {
            return groups.size();
        }

        class GroupViewHolder extends RecyclerView.ViewHolder {
            final TextView dateTv;
            final ListView listView;

            GroupViewHolder(@NonNull View itemView) {
                super(itemView);
                dateTv = itemView.findViewById(R.id.notifications_list_date);
                listView = itemView.findViewById(R.id.notifications_group_list);
            }
        }
    }

    private class NotificationItemAdapter extends BaseAdapter {
        private final List<NotificationItem> items;
        private final ListView listView;

        NotificationItemAdapter(List<NotificationItem> items, @NonNull ListView listView) {
            this.items = items;
            this.listView = listView;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null
                    ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);

            NotificationItem item = items.get(position);
            boolean expanded = expandedNotificationIds.contains(item.id);

            View card = view.findViewById(R.id.notification_card);
            View header = view.findViewById(R.id.notification_header);
            View unreadDot = view.findViewById(R.id.notification_unread_dot);
            TextView titleView = view.findViewById(R.id.notification_title);
            TextView messageView = view.findViewById(R.id.notification_message);
            TextView statusView = view.findViewById(R.id.notification_status);
            TextView detailView = view.findViewById(R.id.notification_detail);
            TextView actionView = view.findViewById(R.id.notification_action_cta);
            ImageView iconView = view.findViewById(R.id.notification_icon);
            ImageView arrowView = view.findViewById(R.id.navigate_to_content_bttn);
            View expandedContainer = view.findViewById(R.id.notification_expanded_container);

            titleView.setText(item.title);
            messageView.setText(item.message);
            statusView.setText("Status: " + firstNonEmpty(item.status, "In Review"));
            detailView.setText(buildExpandedDetail(item));
            actionView.setText(resolveActionLabel(item));
            iconView.setImageResource(resolveNotificationIcon(item.type));

            unreadDot.setVisibility(item.read ? View.GONE : View.VISIBLE);
            titleView.setTypeface(null, item.read ? Typeface.NORMAL : Typeface.BOLD);
            messageView.setAlpha(item.read ? 0.75f : 1f);
            card.setAlpha(item.read ? 0.92f : 1f);

            expandedContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            arrowView.setRotation(expanded ? 90f : 0f);

            header.setOnClickListener(v -> toggleNotification(item, listView));
            actionView.setOnClickListener(v -> openNotificationTarget(item));
            return view;
        }

        @NonNull
        private String buildExpandedDetail(@NonNull NotificationItem item) {
            String entity = firstNonEmpty(item.entityName, "this item");
            if (MODE_ADMIN.equals(notificationMode)) {
                if (AdminNotificationHelper.TYPE_INFO_REQUEST.equals(item.type)) {
                    return "A user submission for " + entity + " is ready for review.";
                }
                if (AdminNotificationHelper.TYPE_LOCATION_REQUEST.equals(item.type)) {
                    return "A location request for " + entity + " is waiting for your review.";
                }
                if (AdminNotificationHelper.TYPE_DATASET_SUBMISSION.equals(item.type)) {
                    return "A dataset image submission related to " + entity + " is available to review.";
                }
                return item.message;
            }

            if (UserNotificationHelper.TYPE_INFO_REQUEST.equals(item.type)) {
                return "Your information request for " + entity + " has a new status update.";
            }
            if (UserNotificationHelper.TYPE_LOCATION_REQUEST.equals(item.type)) {
                return "Your location request for " + entity + " has a new status update.";
            }
            if (UserNotificationHelper.TYPE_DATASET_SUBMISSION.equals(item.type)) {
                return "Your dataset image submission has a new status update.";
            }
            if (UserNotificationHelper.TYPE_BUSINESS_VERIFICATION.equals(item.type)) {
                return "Your business verification has a new status update.";
            }
            return item.message;
        }

        @NonNull
        private String resolveActionLabel(@NonNull NotificationItem item) {
            if (UserNotificationHelper.TARGET_DATASET_DIALOG.equals(item.targetScreen)) {
                return "View update";
            }
            return "Open details";
        }
    }

    private int resolveNotificationIcon(@NonNull String type) {
        switch (type) {
            case UserNotificationHelper.TYPE_LOCATION_REQUEST:
                return R.drawable.ic_pin_area;
            case UserNotificationHelper.TYPE_DATASET_SUBMISSION:
                return R.drawable.ic_images;
            case UserNotificationHelper.TYPE_BUSINESS_VERIFICATION:
                return R.drawable.ic_storefront;
            case UserNotificationHelper.TYPE_INFO_REQUEST:
            default:
                return R.drawable.ic_info;
        }
    }

    private void setListViewHeightBasedOnChildren(@NonNull ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        int availableWidth = listView.getWidth() > 0
                ? listView.getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * Math.max(listAdapter.getCount() - 1, 0));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @NonNull
    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
