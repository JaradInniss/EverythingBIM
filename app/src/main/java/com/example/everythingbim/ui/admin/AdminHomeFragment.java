package com.example.everythingbim.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import com.example.everythingbim.R;
import com.example.everythingbim.ActivityLogger;
import com.example.everythingbim.ui.home.NotificationsActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin Home Fragment displaying dashboard with request statistics and weekly chart.
 */
public class AdminHomeFragment extends Fragment {

    private static final int MAX_RECENT_ACTIVITIES = 3;
    private static final float MAX_BAR_HEIGHT_DP = 120f;

    // Firestore instance
    private FirebaseFirestore db;

    // View references for bar chart
    private View barMon, barTue, barWed, barThu, barFri, barSat, barSun;
    private TextView tvMonValue, tvTueValue, tvWedValue, tvThuValue, tvFriValue, tvSatValue, tvSunValue;
    private TextView tvRequestVolumeTotal, tvRequestVolumeIncremental;
    private TextView tvNotificationBadge;

    // View references for request type cards
    private TextView tvBusinessCount, tvBusinessIncremental;
    private TextView tvInfoCount, tvInfoIncremental;
    private TextView tvLocationCount, tvLocationIncremental;
    private TextView tvReportsCount, tvReportsIncremental;

    // View references for recent activity items
    private LinearLayout activityItem1, activityItem2, activityItem3;
    private ImageView iconActivity1, iconActivity2, iconActivity3;
    private TextView tvActivityTitle1, tvActivityTitle2, tvActivityTitle3;
    private TextView tvActivitySubtitle1, tvActivitySubtitle2, tvActivitySubtitle3;
    private TextView tvActivityTime1, tvActivityTime2, tvActivityTime3;
    private TextView tvActivityView1, tvActivityView2, tvActivityView3;

    // Activity data storage
    private List<ActivityItem> recentActivities = new ArrayList<>();

    // Guard flag to prevent double updateActivityUI() calls during back navigation
    private boolean activityUIAlreadyBound = false;

    // View dot indicators for request cards
    private View dotBusiness, dotInfo, dotLocation, dotReports;

    // View dot indicators for recent activity items
    private View dotActivity1, dotActivity2, dotActivity3;
    private int unreadBusinessCount;
    private int unreadInfoCount;
    private int unreadLocationCount;
    private int unreadDatasetCount;
    private int unreadReportsCount;
    private int unreadAdminNotificationCount;

    // Collection names
    private static final String COLLECTION_BUSINESS = "add_business_requests";
    private static final String COLLECTION_INFO = "add_information_requests";
    private static final String COLLECTION_LOCATION = "add_location_requests";
    private static final String COLLECTION_DATASET = "dataset_image_submissions";
    private static final String COLLECTION_REPORTS = "add_reports";

    public AdminHomeFragment() {
        // Required empty public constructor
    }

    public static AdminHomeFragment newInstance(String param1, String param2) {
        AdminHomeFragment fragment = new AdminHomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize bar chart views
        barMon = view.findViewById(R.id.bar_mon);
        barTue = view.findViewById(R.id.bar_tue);
        barWed = view.findViewById(R.id.bar_wed);
        barThu = view.findViewById(R.id.bar_thu);
        barFri = view.findViewById(R.id.bar_fri);
        barSat = view.findViewById(R.id.bar_sat);
        barSun = view.findViewById(R.id.bar_sun);

        // Initialize value text views
        tvMonValue = view.findViewById(R.id.tv_mon_value);
        tvTueValue = view.findViewById(R.id.tv_tue_value);
        tvWedValue = view.findViewById(R.id.tv_wed_value);
        tvThuValue = view.findViewById(R.id.tv_thu_value);
        tvFriValue = view.findViewById(R.id.tv_fri_value);
        tvSatValue = view.findViewById(R.id.tv_sat_value);
        tvSunValue = view.findViewById(R.id.tv_sun_value);

        // Initialize total and incremental text views
        tvRequestVolumeTotal = view.findViewById(R.id.tv_request_volume_total);
        tvRequestVolumeIncremental = view.findViewById(R.id.tv_request_volume_incremental);
        tvNotificationBadge = view.findViewById(R.id.tv_notification_badge);

        // Initialize request type card views
        tvBusinessCount = view.findViewById(R.id.tv_business_count);
        tvBusinessIncremental = view.findViewById(R.id.tv_business_count_incremental);
        tvInfoCount = view.findViewById(R.id.tv_info_count);
        tvInfoIncremental = view.findViewById(R.id.tv_info_count_incremental);
        tvLocationCount = view.findViewById(R.id.tv_location_count);
        tvLocationIncremental = view.findViewById(R.id.tv_location_count_incremental);
        tvReportsCount = view.findViewById(R.id.tv_reports_count);
        tvReportsIncremental = view.findViewById(R.id.tv_reports_count_incremental);

        // Initialize recent activity views
        activityItem1 = view.findViewById(R.id.activity_item_1);
        activityItem2 = view.findViewById(R.id.activity_item_2);
        activityItem3 = view.findViewById(R.id.activity_item_3);
        iconActivity1 = view.findViewById(R.id.icon_activity_1);
        iconActivity2 = view.findViewById(R.id.icon_activity_2);
        iconActivity3 = view.findViewById(R.id.icon_activity_3);
        tvActivityTitle1 = view.findViewById(R.id.tv_activity_title_1);
        tvActivityTitle2 = view.findViewById(R.id.tv_activity_title_2);
        tvActivityTitle3 = view.findViewById(R.id.tv_activity_title_3);
        tvActivitySubtitle1 = view.findViewById(R.id.tv_activity_subtitle_1);
        tvActivitySubtitle2 = view.findViewById(R.id.tv_activity_subtitle_2);
        tvActivitySubtitle3 = view.findViewById(R.id.tv_activity_subtitle_3);
        tvActivityTime1 = view.findViewById(R.id.tv_activity_time_1);
        tvActivityTime2 = view.findViewById(R.id.tv_activity_time_2);
        tvActivityTime3 = view.findViewById(R.id.tv_activity_time_3);
        tvActivityView1 = view.findViewById(R.id.tv_activity_view_1);
        tvActivityView2 = view.findViewById(R.id.tv_activity_view_2);
        tvActivityView3 = view.findViewById(R.id.tv_activity_view_3);

        // Bind card dot indicators
        dotBusiness = view.findViewById(R.id.dot_business);
        dotInfo = view.findViewById(R.id.dot_info);
        dotLocation = view.findViewById(R.id.dot_location);
        dotReports = view.findViewById(R.id.dot_reports);

        // Bind recent activity dot indicators
        dotActivity1 = view.findViewById(R.id.dot_activity_1);
        dotActivity2 = view.findViewById(R.id.dot_activity_2);
        dotActivity3 = view.findViewById(R.id.dot_activity_3);

        // Setup card click listeners (for timestamp tracking)
        setupCardClickListeners();

        // Setup activity item click listeners
        setupActivityClickListeners();

        // Load data from Firestore
        loadDashboardData();
        listenForAdminBellNotifications();

        // Load recent activities from local store.
        // Called here (not just in loadDashboardData) so that on back-navigation
        // from detail screens, the activity list is repopulated when views are rebound.
        activityUIAlreadyBound = false;
        loadRecentActivities();

        // Start listening for new activities
        listenForNewActivities();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only refresh dashboard data on resume — onViewCreated already loaded
        // recent activities and bound all views. This refreshes Firestore listeners
        // without reloading recent activities (which would call updateActivityUI again
        // after the view is already stable).
        loadUnreadCounts();
        loadWeeklyVolumeData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activityListenerRegistration != null) {
            activityListenerRegistration.remove();
            activityListenerRegistration = null;
        }
        if (adminNotificationListenerRegistration != null) {
            adminNotificationListenerRegistration.remove();
            adminNotificationListenerRegistration = null;
        }
        // Reset guard flag so updateActivityUI runs fresh on next view creation
        activityUIAlreadyBound = false;
    }

    /**
     * Sets up click listeners for activity items to navigate to detail screens.
     */
    private void setupActivityClickListeners() {
        View.OnClickListener clickListener = v -> {
            int position = -1;
            if (v == activityItem1 || v == tvActivityView1) position = 0;
            else if (v == activityItem2 || v == tvActivityView2) position = 1;
            else if (v == activityItem3 || v == tvActivityView3) position = 2;

            if (position >= 0 && position < recentActivities.size()) {
                ActivityItem activity = recentActivities.get(position);
                // Mark as read in SharedPreferences when viewed
                if (activity.requestId != null) {
                    ReadStateManager.markActivityRead(requireContext(), activity.requestId);
                }
                navigateToActivityDetail(activity);
            }
        };

        if (activityItem1 != null) activityItem1.setOnClickListener(clickListener);
        if (activityItem2 != null) activityItem2.setOnClickListener(clickListener);
        if (activityItem3 != null) activityItem3.setOnClickListener(clickListener);
        if (tvActivityView1 != null) tvActivityView1.setOnClickListener(clickListener);
        if (tvActivityView2 != null) tvActivityView2.setOnClickListener(clickListener);
        if (tvActivityView3 != null) tvActivityView3.setOnClickListener(clickListener);
    }

    /**
     * Sets up click listeners for the 4 request type cards.
     * Clicking a card marks that section as read (sets lastReadTimestamp to now).
     */
    private void setupCardClickListeners() {
        View cardBusiness = requireView().findViewById(R.id.card_business);
        View cardInfo = requireView().findViewById(R.id.card_info);
        View cardLocation = requireView().findViewById(R.id.card_location);
        View cardReports = requireView().findViewById(R.id.card_reports);
        View bellButton = requireView().findViewById(R.id.btn_notification);

        if (cardBusiness != null) {
            cardBusiness.setOnClickListener(v -> {
                // Don't mark as read here - only when admin clicks "View" on individual requests
                navigateToRequestSection("business");
            });
        }
        if (cardInfo != null) {
            cardInfo.setOnClickListener(v -> {
                // Don't mark as read here - only when admin clicks "View" on individual requests
                Fragment fragment = AdminUserFragment.newInstance(AdminUserFragment.TAB_GENERAL);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.admin_fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
        if (cardLocation != null) {
            cardLocation.setOnClickListener(v -> {
                // Don't mark as read here - only when admin clicks "View" on individual requests
                navigateToRequestSection("location");
            });
        }
        if (cardReports != null) {
            cardReports.setOnClickListener(v -> {
                // Don't mark as read here - only when admin clicks "View" on individual requests
                navigateToReportsSection();
            });
        }

        if (bellButton != null) {
            bellButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NotificationsActivity.class);
                intent.putExtra(NotificationsActivity.EXTRA_NOTIFICATION_MODE, NotificationsActivity.MODE_ADMIN);
                startActivity(intent);
            });
        }
    }

    /**
     * Navigates to the appropriate request list fragment based on type.
     */
    private void navigateToRequestSection(String type) {
        Fragment fragment;
        if ("business".equals(type)) {
            // Business verification requests are shown in AdminUserFragment on BUSINESS tab
            fragment = AdminUserFragment.newInstance(AdminUserFragment.TAB_BUSINESS);
        } else {
            // Location and Info requests use AdminUserRequestsFragment
            fragment = AdminUserRequestsFragment.newInstance(type);
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Navigates to the reports section.
     */
    private void navigateToReportsSection() {
        Fragment fragment = new AdminReportsFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Navigates to the appropriate detail fragment based on activity type.
     * Passes all available activity data to the detail fragment.
     */
    private void navigateToActivityDetail(ActivityItem activity) {
        Fragment fragment = null;
        Bundle args = new Bundle();

        // Pass all activity data to the detail fragment
        args.putString("doc_id", activity.requestId);
        args.putString("requestTitle", activity.requestTitle != null ? activity.requestTitle : "");
        args.putString("requestType", activity.requestType != null ? activity.requestType : "");

        switch (activity.requestType) {
            case ActivityLogger.TYPE_REPORT:
                fragment = new AdminReportDetailFragment();
                break;
            case ActivityLogger.TYPE_BUSINESS:
                fragment = new AdminBizVerificationDetailFragment();
                break;
            case ActivityLogger.TYPE_LOCATION:
                fragment = new AdminLocationRequestDetailsFragment();
                break;
            case ActivityLogger.TYPE_INFO:
                fragment = new AdminInfoRequestDetailFragment();
                break;
            case ActivityLogger.TYPE_DATASET:
                fragment = AdminDatasetSubmissionDetailFragment.newInstance(activity.requestId);
                break;
        }

        if (fragment != null) {
            fragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.admin_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    // Listener registration for cleanup
    private com.google.firebase.firestore.ListenerRegistration activityListenerRegistration;
    private com.google.firebase.firestore.ListenerRegistration adminNotificationListenerRegistration;

    /**
     * Listens for new activities in real-time.
     */
    private void listenForNewActivities() {
        // Remove previous listener before adding new one to prevent duplicates
        if (activityListenerRegistration != null) {
            activityListenerRegistration.remove();
            activityListenerRegistration = null;
        }

        activityListenerRegistration = db.collection("admin_activities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;
                        if (value != null && !value.getDocumentChanges().isEmpty()) {
                            // Check if this is actually a new document (not just re-reading same data)
                            String newDocId = value.getDocumentChanges().get(0).getDocument().getId();
                            if (!newDocId.equals(lastProcessedActivityId)) {
                                lastProcessedActivityId = newDocId;
                                loadRecentActivities();
                            }
                        }
                    }
                });
    }

    private void listenForAdminBellNotifications() {
        if (adminNotificationListenerRegistration != null) {
            adminNotificationListenerRegistration.remove();
            adminNotificationListenerRegistration = null;
        }

        adminNotificationListenerRegistration = db.collection(AdminNotificationHelper.COLLECTION_ADMIN_NOTIFICATIONS)
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    unreadAdminNotificationCount = (error != null || value == null) ? 0 : value.size();
                    updateNotificationBadge();
                });
    }

    private String lastProcessedActivityId = null;

    /**
     * Loads all dashboard data from Firestore.
     */
    private void loadDashboardData() {
        loadUnreadCounts();
        loadWeeklyVolumeData();
        loadRecentActivities();
    }

    /**
     * Loads recent activities from local SharedPreferences store.
     * Falls back to empty list if no activities logged yet.
     */
    private void loadRecentActivities() {
        List<Map<String, Object>> localActivities = ActivityLogger.loadLocalActivities(requireContext());
        recentActivities.clear();
        for (Map<String, Object> act : localActivities) {
            ActivityItem item = new ActivityItem();
            item.requestType = (String) act.get("requestType");
            item.action = (String) act.get("action");
            item.requestTitle = (String) act.get("requestTitle");
            item.requestId = (String) act.get("requestId");
            item.adminUsername = (String) act.get("adminUsername");
            item.timestamp = (com.google.firebase.Timestamp) act.get("timestamp");
            item.displayTime = (String) act.get("displayTime");
            recentActivities.add(item);
            if (recentActivities.size() >= MAX_RECENT_ACTIVITIES) break;
        }
        updateActivityUI();
    }

    /**
     * Updates the UI with recent activities.
     */
    private void updateActivityUI() {
        if (getView() == null) return;

        // Guard: prevent double execution during back navigation
        if (activityUIAlreadyBound) return;

        // Reset all to invisible first
        if (activityItem1 != null) activityItem1.setVisibility(View.GONE);
        if (activityItem2 != null) activityItem2.setVisibility(View.GONE);
        if (activityItem3 != null) activityItem3.setVisibility(View.GONE);

        for (int i = 0; i < recentActivities.size() && i < MAX_RECENT_ACTIVITIES; i++) {
            ActivityItem activity = recentActivities.get(i);

            switch (i) {
                case 0:
                    updateActivityItem(activityItem1, iconActivity1, tvActivityTitle1,
                            tvActivitySubtitle1, tvActivityTime1, dotActivity1, tvActivityView1, activity);
                    if (activityItem1 != null) activityItem1.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    updateActivityItem(activityItem2, iconActivity2, tvActivityTitle2,
                            tvActivitySubtitle2, tvActivityTime2, dotActivity2, tvActivityView2, activity);
                    if (activityItem2 != null) activityItem2.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    updateActivityItem(activityItem3, iconActivity3, tvActivityTitle3,
                            tvActivitySubtitle3, tvActivityTime3, dotActivity3, tvActivityView3, activity);
                    if (activityItem3 != null) activityItem3.setVisibility(View.VISIBLE);
                    break;
            }
        }

        // Mark that views have been bound to prevent double execution
        activityUIAlreadyBound = true;
    }

    /**
     * Updates a single activity item view.
     */
    private void updateActivityItem(LinearLayout itemView, ImageView iconView,
                                    TextView titleView, TextView subtitleView,
                                    TextView timeView, View dotView,
                                    TextView viewBtn, ActivityItem activity) {
        if (itemView == null) return;

        // Set icon based on request type
        if (iconView != null) {
            switch (activity.requestType) {
                case ActivityLogger.TYPE_REPORT:
                    iconView.setImageResource(R.drawable.ic_flag_banner);
                    iconView.setColorFilter(requireContext().getResources().getColor(R.color.bright_gold, null));
                    break;
                case ActivityLogger.TYPE_BUSINESS:
                    iconView.setImageResource(R.drawable.ic_storefront);
                    iconView.setColorFilter(requireContext().getResources().getColor(R.color.prussian_blue, null));
                    break;
                case ActivityLogger.TYPE_LOCATION:
                    iconView.setImageResource(R.drawable.ic_pin_area);
                    iconView.setColorFilter(requireContext().getResources().getColor(R.color.prussian_blue, null));
                    break;
                case ActivityLogger.TYPE_INFO:
                    iconView.setImageResource(R.drawable.ic_info);
                    iconView.setColorFilter(requireContext().getResources().getColor(R.color.prussian_blue, null));
                    break;
                case ActivityLogger.TYPE_DATASET:
                    iconView.setImageResource(R.drawable.ic_images);
                    iconView.setColorFilter(requireContext().getResources().getColor(R.color.prussian_blue, null));
                    break;
            }
        }

        // Set title: "[Action] [Type]" e.g., "Viewed Report", "Approved Business"
        if (titleView != null) {
            String title = activity.action + " " + activity.requestType;
            titleView.setText(title);
        }

        // Set subtitle: "[RequestTitle]" e.g., "Post-Spam", "Jordan's Supermarket"
        if (subtitleView != null) {
            String subtitle = activity.requestTitle != null ? activity.requestTitle : "";
            subtitleView.setText(subtitle);
        }

        // Set time: time ago format
        if (timeView != null && activity.timestamp != null) {
            String timeAgo = ActivityLogger.getTimeAgo(activity.timestamp);
            timeView.setText(" • " + timeAgo);
        }

        // Dot and View button color based on effective read state
        boolean isRead = activity.requestId != null && ReadStateManager.isActivityRead(requireContext(), activity.requestId);
        if (dotView != null) {
            dotView.setBackgroundResource(isRead ? R.drawable.bg_dot_grey : R.drawable.bg_dot_red);
        }
        if (viewBtn != null) {
            viewBtn.setTextColor(isRead
                    ? android.graphics.Color.parseColor("#9e9e9e")
                    : android.graphics.Color.parseColor("#203088"));
            // "Viewed" action → button says "View"; further actions (Approve/Reject/Complete) → "Done"
            if (ActivityLogger.ACTION_VIEWED.equals(activity.action)) {
                viewBtn.setText("View");
            } else {
                viewBtn.setText("Done");
            }
        }
    }

    /**
     * Loads unread request counts from each collection.
     * Uses lastReadTimestamp per collection: items created after the timestamp are "new" (unread).
     * Falls back to Firestore read=false field if no timestamp is set (first app open).
     * Also updates the dot indicators for each card.
     */
    private void loadUnreadCounts() {
        Context ctx = requireContext();
        long businessTs = ReadStateManager.getBusinessLastRead(ctx);
        long infoTs = ReadStateManager.getInfoLastRead(ctx);
        long locationTs = ReadStateManager.getLocationLastRead(ctx);
        long datasetTs = ReadStateManager.getDatasetLastRead(ctx);
        long reportsTs = ReadStateManager.getReportsLastRead(ctx);

        // Business requests
        db.collection(COLLECTION_BUSINESS)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = countUnreadSince(snap, businessTs, ReadStateManager.KEY_LAST_READ_BUSINESS);
                    unreadBusinessCount = count;
                    updateBusinessCount(count, 0);
                    updateDotVisibility(dotBusiness, count > 0);
                    updateNotificationBadge();
                })
                .addOnFailureListener(e -> {
                    unreadBusinessCount = 0;
                    updateBusinessCount(0, 0);
                    updateDotVisibility(dotBusiness, false);
                    updateNotificationBadge();
                });

        // Info requests
        db.collection(COLLECTION_INFO)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = countUnreadSince(snap, infoTs, ReadStateManager.KEY_LAST_READ_INFO);
                    unreadInfoCount = count;
                    updateInfoCount(unreadInfoCount + unreadDatasetCount, 0);
                    updateDotVisibility(dotInfo, (unreadInfoCount + unreadDatasetCount) > 0);
                    updateNotificationBadge();
                })
                .addOnFailureListener(e -> {
                    unreadInfoCount = 0;
                    updateInfoCount(unreadInfoCount + unreadDatasetCount, 0);
                    updateDotVisibility(dotInfo, (unreadInfoCount + unreadDatasetCount) > 0);
                    updateNotificationBadge();
                });

        // Location requests
        db.collection(COLLECTION_LOCATION)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = countUnreadSince(snap, locationTs, ReadStateManager.KEY_LAST_READ_LOCATION);
                    unreadLocationCount = count;
                    updateLocationCount(count, 0);
                    updateDotVisibility(dotLocation, count > 0);
                    updateNotificationBadge();
                })
                .addOnFailureListener(e -> {
                    unreadLocationCount = 0;
                    updateLocationCount(0, 0);
                    updateDotVisibility(dotLocation, false);
                    updateNotificationBadge();
                });

        db.collection(COLLECTION_DATASET)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = countUnreadSince(snap, datasetTs, ReadStateManager.KEY_LAST_READ_DATASET);
                    unreadDatasetCount = count;
                    updateInfoCount(unreadInfoCount + unreadDatasetCount, 0);
                    updateDotVisibility(dotInfo, (unreadInfoCount + unreadDatasetCount) > 0);
                    updateNotificationBadge();
                })
                .addOnFailureListener(e -> {
                    unreadDatasetCount = 0;
                    updateInfoCount(unreadInfoCount + unreadDatasetCount, 0);
                    updateDotVisibility(dotInfo, (unreadInfoCount + unreadDatasetCount) > 0);
                    updateNotificationBadge();
                });

        // Reports
        db.collection(COLLECTION_REPORTS)
                .get()
                .addOnSuccessListener(snap -> {
                    int count = countUnreadSince(snap, reportsTs, ReadStateManager.KEY_LAST_READ_REPORTS);
                    unreadReportsCount = count;
                    updateReportsCount(count, 0);
                    updateDotVisibility(dotReports, count > 0);
                    updateNotificationBadge();
                })
                .addOnFailureListener(e -> {
                    unreadReportsCount = 0;
                    updateReportsCount(0, 0);
                    updateDotVisibility(dotReports, false);
                    updateNotificationBadge();
                });
    }

    /**
     * Counts items with read=false that were created at or after the given timestamp.
     * If timestamp is 0 (first open), counts all items with read=false.
     * Also checks local SharedPreferences for individually read request IDs.
     */
    private int countUnreadSince(QuerySnapshot snap, long lastReadTs, String sectionKey) {
        int count = 0;
        if (snap == null) return 0;
        Context ctx = getContext();
        if (ctx == null) return 0; // Fragment detached, don't update count
        Set<String> localReadIds = ReadStateManager.getReadRequestIds(ctx);
        
        for (DocumentSnapshot doc : snap.getDocuments()) {
            // Skip if individually marked as read in SharedPreferences
            if (localReadIds.contains(doc.getId())) continue;
            
            // Skip if already read in Firestore
            Boolean read = doc.getBoolean("read");
            if (Boolean.TRUE.equals(read)) continue;
            
            // Only count items created at or after lastReadTimestamp (if timestamp > 0)
            if (lastReadTs > 0) {
                com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                if (createdAt != null && createdAt.toDate().getTime() < lastReadTs) continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Updates dot visibility based on whether there are unread items.
     */
    private void updateDotVisibility(View dot, boolean hasUnread) {
        if (dot == null) return;
        dot.setVisibility(hasUnread ? View.VISIBLE : View.INVISIBLE);
        dot.setBackgroundResource(hasUnread ? R.drawable.bg_dot_red : R.drawable.bg_dot_grey);
    }

    private void updateNotificationBadge() {
        if (tvNotificationBadge == null) {
            return;
        }

        if (unreadAdminNotificationCount <= 0) {
            tvNotificationBadge.setVisibility(View.GONE);
            return;
        }

        tvNotificationBadge.setVisibility(View.VISIBLE);
        tvNotificationBadge.setText(unreadAdminNotificationCount > 99
                ? "99+"
                : String.valueOf(unreadAdminNotificationCount));
    }

    /**
     * Loads weekly volume data for the bar chart.
     */
    private void loadWeeklyVolumeData() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Task<QuerySnapshot> businessTask = db.collection("add_business_requests")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> infoTask = db.collection(COLLECTION_INFO)
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> locationTask = db.collection("add_location_requests")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> datasetTask = db.collection(COLLECTION_DATASET)
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Task<QuerySnapshot> reportsTask = db.collection("add_reports")
                .whereGreaterThanOrEqualTo("createdAt", new com.google.firebase.Timestamp(calendar.getTime()))
                .get();

        Tasks.whenAllSuccess(businessTask, infoTask, locationTask, datasetTask, reportsTask)
                .addOnSuccessListener(results -> {
                    Map<String, Integer> weeklyData = new LinkedHashMap<>();
                    int totalThisWeek = 0;

                    weeklyData.put("mon", 0);
                    weeklyData.put("tue", 0);
                    weeklyData.put("wed", 0);
                    weeklyData.put("thu", 0);
                    weeklyData.put("fri", 0);
                    weeklyData.put("sat", 0);
                    weeklyData.put("sun", 0);

                    for (Object result : results) {
                        if (result instanceof QuerySnapshot) {
                            QuerySnapshot snapshot = (QuerySnapshot) result;
                            totalThisWeek += snapshot.size();

                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                Calendar docCal = Calendar.getInstance();
                                if (doc.getTimestamp("createdAt") != null) {
                                    docCal.setTime(doc.getTimestamp("createdAt").toDate());
                                    int dayOfWeek = docCal.get(Calendar.DAY_OF_WEEK);

                                    String dayKey;
                                    switch (dayOfWeek) {
                                        case Calendar.MONDAY: dayKey = "mon"; break;
                                        case Calendar.TUESDAY: dayKey = "tue"; break;
                                        case Calendar.WEDNESDAY: dayKey = "wed"; break;
                                        case Calendar.THURSDAY: dayKey = "thu"; break;
                                        case Calendar.FRIDAY: dayKey = "fri"; break;
                                        case Calendar.SATURDAY: dayKey = "sat"; break;
                                        case Calendar.SUNDAY: dayKey = "sun"; break;
                                        default: continue;
                                    }

                                    int current = weeklyData.get(dayKey);
                                    weeklyData.put(dayKey, current + 1);
                                }
                            }
                        }
                    }

                    int previousWeekTotal = (int) (totalThisWeek * 0.9);
                    int incremental = totalThisWeek - previousWeekTotal;
                    updateRequestVolumeChart(weeklyData, totalThisWeek, incremental);
                })
                .addOnFailureListener(e -> {
                    Map<String, Integer> emptyData = new LinkedHashMap<>();
                    emptyData.put("mon", 0);
                    emptyData.put("tue", 0);
                    emptyData.put("wed", 0);
                    emptyData.put("thu", 0);
                    emptyData.put("fri", 0);
                    emptyData.put("sat", 0);
                    emptyData.put("sun", 0);
                    updateRequestVolumeChart(emptyData, 0, 0);
                });
    }

    private void updateBusinessCount(int count, int incremental) {
        if (tvBusinessCount != null) tvBusinessCount.setText(String.format("%02d", count));
        if (tvBusinessIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvBusinessIncremental.setText(text);
        }
    }

    private void updateInfoCount(int count, int incremental) {
        if (tvInfoCount != null) tvInfoCount.setText(String.format("%02d", count));
        if (tvInfoIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvInfoIncremental.setText(text);
        }
    }

    private void updateLocationCount(int count, int incremental) {
        if (tvLocationCount != null) tvLocationCount.setText(String.format("%02d", count));
        if (tvLocationIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvLocationIncremental.setText(text);
        }
    }

    private void updateReportsCount(int count, int incremental) {
        if (tvReportsCount != null) tvReportsCount.setText(String.format("%02d", count));
        if (tvReportsIncremental != null) {
            String text = incremental >= 0 ? String.format("+%d", incremental) : String.format("%d", incremental);
            tvReportsIncremental.setText(text);
        }
    }

    /**
     * Updates the Request Volume bar chart.
     */
    public void updateRequestVolumeChart(@NonNull Map<String, Integer> weeklyData,
                                          int total, int incremental) {
        if (getContext() == null || getView() == null) return;

        int maxValue = 1;
        for (Integer value : weeklyData.values()) {
            if (value != null && value > maxValue) maxValue = value;
        }

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float maxBarHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, MAX_BAR_HEIGHT_DP, dm);

        updateBar(barMon, tvMonValue, weeklyData.get("mon"), maxValue, maxBarHeightPx);
        updateBar(barTue, tvTueValue, weeklyData.get("tue"), maxValue, maxBarHeightPx);
        updateBar(barWed, tvWedValue, weeklyData.get("wed"), maxValue, maxBarHeightPx);
        updateBar(barThu, tvThuValue, weeklyData.get("thu"), maxValue, maxBarHeightPx);
        updateBar(barFri, tvFriValue, weeklyData.get("fri"), maxValue, maxBarHeightPx);
        updateBar(barSat, tvSatValue, weeklyData.get("sat"), maxValue, maxBarHeightPx);
        updateBar(barSun, tvSunValue, weeklyData.get("sun"), maxValue, maxBarHeightPx);

        if (tvRequestVolumeTotal != null) {
            tvRequestVolumeTotal.setText(String.format(" - %d", total));
        }

        if (tvRequestVolumeIncremental != null) {
            String incrementalText = incremental >= 0
                    ? String.format(" +%d", incremental)
                    : String.format(" %d", incremental);
            tvRequestVolumeIncremental.setText(incrementalText);
        }
    }

    private void updateBar(View bar, TextView valueText, Integer value,
                          int maxValue, float maxBarHeightPx) {
        if (bar == null || valueText == null || value == null) return;

        valueText.setText(String.valueOf(value));
        float proportionalHeight = (value / (float) maxValue) * maxBarHeightPx;
        int minHeightPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4f, getResources().getDisplayMetrics());
        int barHeightPx = Math.max((int) proportionalHeight, minHeightPx);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
        params.height = barHeightPx;
        bar.setLayoutParams(params);
    }

    /**
     * Data class for activity items.
     */
    private static class ActivityItem {
        String requestType;
        String action;
        String requestTitle;
        String requestId;
        String adminUsername;
        com.google.firebase.Timestamp timestamp;
        String displayTime;
    }
}
