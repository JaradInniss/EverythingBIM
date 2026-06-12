package com.example.everythingbim.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminWatchListFragment extends Fragment {

    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private RecyclerView recyclerView;
    private WatchListAdapter adapter;
    private TextView countTv;
    private View emptyState;
    private List<WatchListEntry> watchList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_watchlist, container, false);

        db = FirebaseFirestore.getInstance();

        // Bind views
        view.findViewById(R.id.watchlist_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        recyclerView = view.findViewById(R.id.watchlist_recycler);
        countTv = view.findViewById(R.id.watchlist_count_tv);
        emptyState = view.findViewById(R.id.watchlist_empty_state);

        // Setup RecyclerView
        adapter = new WatchListAdapter(watchList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(),
                        DividerItemDecoration.VERTICAL));

        loadWatchList();

        return view;
    }

    private void loadWatchList() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("watch_list")
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        showToast("Error loading watch list");
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        watchList.clear();
                        adapter.notifyDataSetChanged();
                        countTv.setText("0 accounts");
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        watchList.clear();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String docId = doc.getId();
                            String userId = doc.getString("userId");
                            String userType = doc.getString("userType");
                            String relatedReportId = doc.getString("relatedReportId");

                            Timestamp ts = doc.getTimestamp("addedAt");
                            String date = ts != null
                                    ? new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(ts.toDate())
                                    : "";

                            watchList.add(new WatchListEntry(docId, userId, userType, date, relatedReportId));
                        }
                        adapter.notifyDataSetChanged();
                        countTv.setText(watchList.size() + " account" + (watchList.size() != 1 ? "s" : ""));
                        emptyState.setVisibility(watchList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void showToast(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Watch list entry data model
    public static class WatchListEntry {
        private String docId;
        private String userId;  // This is authorId (long)
        private String userType;
        private String dateAdded;
        private String relatedReportId;

        public WatchListEntry(String docId, String userId, String userType, String dateAdded, String relatedReportId) {
            this.docId = docId;
            this.userId = userId;
            this.userType = userType;
            this.dateAdded = dateAdded;
            this.relatedReportId = relatedReportId;
        }

        public String getDocId() { return docId; }
        public String getUserId() { return userId; }
        public String getUserType() { return userType != null ? userType : "general"; }
        public String getDateAdded() { return dateAdded; }
        public String getRelatedReportId() { return relatedReportId != null ? relatedReportId : ""; }
    }

    // Adapter
    private class WatchListAdapter extends RecyclerView.Adapter<WatchListAdapter.ViewHolder> {

        private List<WatchListEntry> items;

        WatchListAdapter(List<WatchListEntry> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_watchlist, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            WatchListEntry entry = items.get(position);

            h.userId.setText("User ID: " + entry.getUserId());
            h.userType.setText(capitalizeFirst(entry.getUserType()));
            h.dateAdded.setText("Added: " + entry.getDateAdded());
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView userId, userType, dateAdded;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                userId = itemView.findViewById(R.id.watchlist_item_user_id);
                userType = itemView.findViewById(R.id.watchlist_item_user_type);
                dateAdded = itemView.findViewById(R.id.watchlist_item_date);
            }
        }
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}