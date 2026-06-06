package com.example.everythingbim.ui.admin;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.everythingbim.R;

public class AdminUserRequestsFragment extends Fragment {

    // ─── Bundle args ─────────────────────────
    // Pass REQUEST_TYPE = "location", "info", "dataset", "submissions", or "business_location"
    public static final String ARG_TYPE = "request_type";

    // ─── Read filter ─────────────────────────
    private enum ReadFilter { ALL, UNREAD, READ }
    private ReadFilter readFilter = ReadFilter.ALL;

    // ─── Views ───────────────────────────────
    private EditText searchEt;
    private TextView filterAll, filterUnread, filterRead;
    private TextView countTv;
    private RecyclerView recyclerView;

    // ─── Adapter + data ──────────────────────
    private RequestListAdapter adapter;
    private List<RequestItem> allItems       = new ArrayList<>();
    private List<RequestItem> displayedItems = new ArrayList<>();

    // ─── Firebase ────────────────────────────
    private FirebaseFirestore db;
    private boolean isLoadingRequests = false;
    private int currentLoadId = 0;

    // ─── Request type ─────────────────────────
    private String requestType = "info"; // "info", "location", "dataset", "submissions", or "business_location"

    // ────────────────────────────────────────────────────────
    // FACTORY
    // ────────────────────────────────────────────────────────

    public static AdminUserRequestsFragment newInstance(String type) {
        AdminUserRequestsFragment f = new AdminUserRequestsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        f.setArguments(args);
        return f;
    }

    // ────────────────────────────────────────────────────────
    // LIFECYCLE
    // ────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_user_requests, container, false);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            requestType = getArguments().getString(ARG_TYPE, "info");
        }

        // Bind views
        searchEt     = view.findViewById(R.id.req_list_search_et);
        filterAll    = view.findViewById(R.id.req_filter_all);
        filterUnread = view.findViewById(R.id.req_filter_unread);
        filterRead   = view.findViewById(R.id.req_filter_read);
        countTv      = view.findViewById(R.id.req_list_count_tv);

        // Update header labels based on type
        TextView sectionTitle = view.findViewById(R.id.req_list_section_title);
        if (requestType.equals("location")) {
            sectionTitle.setText("Add Location Requests");
        } else if (requestType.equals("business_location")) {
            sectionTitle.setText("Business Location Requests");
        } else if (requestType.equals("submissions")) {
            sectionTitle.setText("Users' Submissions");
        } else if (requestType.equals("dataset")) {
            sectionTitle.setText("Dataset Image Submissions");
        } else {
            sectionTitle.setText("Information Requests");
        }

        // Back button
        view.findViewById(R.id.req_list_back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // RecyclerView
        recyclerView = view.findViewById(R.id.req_list_recycler);
        adapter = new RequestListAdapter(displayedItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        // Filter pills
        filterAll.setOnClickListener(v -> { readFilter = ReadFilter.ALL; updatePills(); applyFilters(); });
        filterUnread.setOnClickListener(v -> { readFilter = ReadFilter.UNREAD; updatePills(); applyFilters(); });
        filterRead.setOnClickListener(v -> { readFilter = ReadFilter.READ; updatePills(); applyFilters(); });

        // Search
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
        });

        loadData();
        return view;
    }

    // ────────────────────────────────────────────────────────
    // LOAD DATA FROM FIRESTORE - uses snapshot listener for real-time updates
    // ────────────────────────────────────────────────────────

    private void loadData() {
        // Remove previous listener before adding new one to prevent duplicates
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if ("submissions".equals(requestType)) {
            loadCombinedSubmissions();
            return;
        }

        String collection = getCollectionName();

        listenerRegistration = db.collection(collection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        addPlaceholders();
                        applyFilters();
                        return;
                    }

                    if (snap == null || snap.isEmpty()) {
                        addPlaceholders();
                    } else {
                        allItems.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            allItems.add(buildRequestItem(doc, requestType));
                        }
                    }
                    applyFilters();
                    countTv.setText(String.valueOf(allItems.size()));
                });
    }

    private void loadCombinedSubmissions() {
        final int loadId = ++currentLoadId;
        allItems.clear();
        final List<RequestItem> combinedItems = new ArrayList<>();
        final int[] remainingLoads = {2};
        final boolean[] hadSuccess = {false};

        com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener =
                (snap, error) -> {
                    if (!isAdded() || getView() == null || loadId != currentLoadId) {
                        return;
                    }

                    if (error == null && snap != null) {
                        hadSuccess[0] = true;
                        for (QueryDocumentSnapshot doc : snap) {
                            String type = "dataset_image_submissions".equals(doc.getReference().getParent().getId())
                                    ? "dataset"
                                    : "info";
                            combinedItems.add(buildRequestItem(doc, type));
                        }
                    }

                    remainingLoads[0]--;
                    if (remainingLoads[0] > 0) {
                        return;
                    }

                    allItems.clear();
                    if (!hadSuccess[0] || combinedItems.isEmpty()) {
                        addPlaceholders();
                    } else {
                        combinedItems.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));
                        allItems.addAll(combinedItems);
                    }

                    applyFilters();
                    countTv.setText(String.valueOf(allItems.size()));
                };

        com.google.firebase.firestore.ListenerRegistration infoRegistration = db.collection("add_information_requests")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
        com.google.firebase.firestore.ListenerRegistration datasetRegistration = db.collection("dataset_image_submissions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);

        listenerRegistration = () -> {
            infoRegistration.remove();
            datasetRegistration.remove();
        };
    }

    // Listener registration for cleanup
    private com.google.firebase.firestore.ListenerRegistration listenerRegistration;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        currentLoadId++;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload read state from SharedPreferences in case it changed
        applyFilters();
    }

    // Check if a request is locally marked as read
    public boolean isRequestRead(String docId) {
        return ReadStateManager.isRequestRead(requireContext(), docId);
    }

    // Mark a request as read and persist to SharedPreferences
    public void markAsRead(String docId) {
        ReadStateManager.markRequestRead(requireContext(), docId);
        applyFilters();
    }

    // Effective read state = Firestore read OR locally tracked read
    private boolean isEffectivelyRead(RequestItem item) {
        return item.read || ReadStateManager.isRequestRead(requireContext(), item.docId);
    }

    private void addPlaceholders() {
        allItems.clear();
        if (requestType.equals("location")) {
            RequestItem item1 = new RequestItem("#201","Hackerton's Pub","User","2026/02/11",false,"ph_loc_201");
            item1.locationName = "Hackerton's Pub";
            item1.description = "Historic bar located in Bridgetown";
            item1.placeType = "Entertainment";
            item1.reason = "Popular landmark not on map";
            item1.coordinates = "13.11332, -59.59877";
            item1.latitude = 13.11332;
            item1.longitude = -59.59877;
            allItems.add(item1);

            RequestItem item2 = new RequestItem("#200","Marton Gardens","User","2026/02/01",false,"ph_loc_200");
            item2.locationName = "Marton Gardens";
            item2.description = "Beautiful garden estate";
            item2.placeType = "Nature";
            item2.reason = "Residential area missing from map";
            item2.coordinates = "13.18965, -59.54321";
            item2.latitude = 13.18965;
            item2.longitude = -59.54321;
            allItems.add(item2);

            RequestItem item3 = new RequestItem("#199","Larton's Cemetery","User","2026/01/28",true,"ph_loc_199");
            item3.locationName = "Larton's Cemetery";
            item3.description = "Historic burial ground";
            item3.placeType = "Historical";
            item3.reason = "Heritage site needs marking";
            item3.coordinates = "13.15678, -59.61234";
            item3.latitude = 13.15678;
            item3.longitude = -59.61234;
            allItems.add(item3);
        } else if (requestType.equals("dataset")) {
            RequestItem item1 = new RequestItem("#311","Unknown Waterfront Landmark","admin01@test.com","2026/05/31",false,"ph_dataset_311");
            item1.status = "In Review";
            item1.requestType = "dataset";
            item1.description = "Low-confidence CNN result submitted for dataset review.";
            item1.reason = "User believes this landmark is not yet represented in the model.";
            allItems.add(item1);

            RequestItem item2 = new RequestItem("#310","Cricket Venue Detail Shot","admin01@test.com","2026/05/30",true,"ph_dataset_310");
            item2.status = "In Review";
            item2.requestType = "dataset";
            item2.description = "Close-up venue photo shared to improve false-negative handling.";
            item2.reason = "Helpful candidate for future training data expansion.";
            allItems.add(item2);
        } else if (requestType.equals("submissions")) {
            RequestItem item1 = new RequestItem("#311","Unknown Waterfront Landmark","admin01@test.com","2026/05/31",false,"ph_dataset_311");
            item1.status = "In Review";
            item1.requestType = "dataset";
            item1.createdAtMillis = 1748649600000L;
            item1.description = "Low-confidence CNN result submitted for dataset review.";
            item1.reason = "User believes this landmark is not yet represented in the model.";
            allItems.add(item1);

            RequestItem item2 = new RequestItem("#88","The Emancipation Statue","User","2026/02/11",false,"ph_info_88");
            item2.requestType = "info";
            item2.createdAtMillis = 1739232000000L;
            item2.locationName = "The Emancipation Statue";
            item2.description = "Statue commemorating emancipation";
            item2.placeType = "Monument";
            item2.coordinates = "13.11332, -59.59877";
            item2.latitude = 13.11332;
            item2.longitude = -59.59877;
            allItems.add(item2);

            allItems.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));
        } else if (requestType.equals("business_location")) {
            RequestItem item1 = new RequestItem("#145","Harbour Lights","Business Owner","2026/03/04",false,"ph_biz_145");
            item1.requestType = "business_location";
            item1.title = "Harbour Lights";
            item1.phone = "(246) 555-0145";
            item1.email = "owner@harbourlights.test";
            item1.address = "Bay Street, Bridgetown";
            item1.description = "Business requested to be added as a nightlife venue.";
            item1.businessType = "Entertainment";
            allItems.add(item1);

            RequestItem item2 = new RequestItem("#144","Bath Hut Cafe","Business Owner","2026/02/27",true,"ph_biz_144");
            item2.requestType = "business_location";
            item2.title = "Bath Hut Cafe";
            item2.phone = "(246) 555-0144";
            item2.email = "hello@bathhutcafe.test";
            item2.address = "Bathsheba, St. Joseph";
            item2.description = "Cafe owner submitted a business location request.";
            item2.businessType = "Food & Drink";
            allItems.add(item2);
        } else {
            RequestItem item1 = new RequestItem("#88","The Emancipation Statue","User","2026/02/11",false,"ph_info_88");
            item1.requestType = "info";
            item1.locationName = "The Emancipation Statue";
            item1.description = "Statue commemorating emancipation";
            item1.placeType = "Monument";
            item1.coordinates = "13.11332, -59.59877";
            item1.latitude = 13.11332;
            item1.longitude = -59.59877;
            allItems.add(item1);

            RequestItem item2 = new RequestItem("#87","Marton Gardens","User","2026/02/01",true,"ph_info_87");
            item2.requestType = "info";
            item2.locationName = "Marton Gardens";
            item2.description = "Public garden and park";
            item2.placeType = "Park";
            item2.coordinates = "13.18965, -59.54321";
            item2.latitude = 13.18965;
            item2.longitude = -59.54321;
            allItems.add(item2);

            RequestItem item3 = new RequestItem("#86","St. George Parish Church","User","2026/01/28",true,"ph_info_86");
            item3.requestType = "info";
            item3.locationName = "St. George Parish Church";
            item3.description = "Historic church building";
            item3.placeType = "Religious";
            item3.coordinates = "13.20123, -59.55123";
            item3.latitude = 13.20123;
            item3.longitude = -59.55123;
            allItems.add(item3);
        }
        countTv.setText(String.valueOf(allItems.size()));
    }

    @NonNull
    private String getCollectionName() {
        if ("location".equals(requestType)) {
            return "add_location_requests";
        }
        if ("business_location".equals(requestType)) {
            return "add_business_location_requests";
        }
        if ("dataset".equals(requestType)) {
            return "dataset_image_submissions";
        }
        return "add_information_requests";
    }

    private RequestItem buildRequestItem(QueryDocumentSnapshot doc, String type) {
        String number = doc.contains("number")
                ? "#" + doc.getLong("number")
                : "#" + doc.getId().substring(0, 3).toUpperCase();
        String name = doc.getString("locationName") != null
                ? doc.getString("locationName")
                : doc.getString("title") != null
                ? doc.getString("title") : "Request";
        String submittedBy = doc.getString("submittedByUsername") != null
                ? doc.getString("submittedByUsername") : "User";
        boolean read = Boolean.TRUE.equals(doc.getBoolean("read"));
        Timestamp ts = doc.getTimestamp("createdAt");
        String date = ts != null ? new SimpleDateFormat("yyyy/MM/dd",
                Locale.getDefault()).format(ts.toDate()) : "";

        RequestItem item = new RequestItem(number, name, submittedBy, date, read, doc.getId());
        item.requestType = type;
        item.createdAtMillis = ts != null ? ts.toDate().getTime() : 0L;

        item.status = doc.getString("status") != null ? doc.getString("status") : "In Review";
        item.locationName = doc.getString("locationName") != null ? doc.getString("locationName") : "";
        item.description = doc.getString("description") != null ? doc.getString("description") : "";
        item.placeType = doc.getString("placeType") != null ? doc.getString("placeType") : "";
        item.reason = doc.getString("reason") != null ? doc.getString("reason") : "";
        item.imageUrl = doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "";
        item.userNote = doc.getString("userNote") != null ? doc.getString("userNote") : "";

        Double lat = doc.getDouble("latitude");
        Double lng = doc.getDouble("longitude");
        if (lat != null && lng != null) {
            item.latitude = lat;
            item.longitude = lng;
            item.coordinates = String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng);
        }

        item.phone = doc.getString("phone") != null ? doc.getString("phone") : "";
        item.email = doc.getString("email") != null ? doc.getString("email") : "";
        item.address = doc.getString("address") != null ? doc.getString("address") : "";
        item.businessType = doc.getString("businessType") != null ? doc.getString("businessType") : "";
        if (item.businessType.isEmpty()) {
            item.businessType = doc.getString("type") != null ? doc.getString("type") : "";
        }
        if (item.businessType.isEmpty()) {
            item.businessType = doc.getString("businessCategory") != null ? doc.getString("businessCategory") : "";
        }
        if (item.businessType.isEmpty()) {
            item.businessType = doc.getString("BusinessName") != null ? doc.getString("BusinessName") : "";
        }

        return item;
    }

    // ────────────────────────────────────────────────────────
    // APPLY FILTERS
    // ────────────────────────────────────────────────────────

    private void applyFilters() {
        String query = searchEt.getText().toString().trim().toLowerCase();
        displayedItems.clear();

        for (RequestItem item : allItems) {
            // Read filter - use effective read state (Firestore + local SharedPreferences)
            boolean effectivelyRead = isEffectivelyRead(item);
            if (readFilter == ReadFilter.UNREAD && effectivelyRead) continue;
            if (readFilter == ReadFilter.READ && !effectivelyRead) continue;

            // Search filter
            if (!query.isEmpty()) {
                boolean matches = item.number.toLowerCase().contains(query)
                        || item.title.toLowerCase().contains(query)
                        || item.submittedBy.toLowerCase().contains(query);
                if (!matches) continue;
            }

            displayedItems.add(item);
        }

        adapter.notifyDataSetChanged();
        countTv.setText(String.valueOf(displayedItems.size()));
    }

    // ────────────────────────────────────────────────────────
    // UPDATE PILLS VISUAL STATE
    // ────────────────────────────────────────────────────────

    private void updatePills() {
        // Reset
        filterAll.setBackgroundResource(R.drawable.bg_search_filter_inactive);
        filterUnread.setBackgroundResource(R.drawable.bg_biz_unread_pill);
        filterRead.setBackgroundResource(R.drawable.bg_biz_read_pill);
        int dark = getResources().getColor(R.color.black, null);
        filterAll.setTextColor(dark);
        filterUnread.setTextColor(dark);
        filterRead.setTextColor(dark);

        // Set active
        switch (readFilter) {
            case ALL:
                filterAll.setBackgroundResource(R.drawable.bg_search_filter_active);
                filterAll.setTextColor(android.graphics.Color.WHITE);
                break;
            case UNREAD:
                filterUnread.setBackgroundResource(R.drawable.bg_search_filter_active);
                filterUnread.setTextColor(android.graphics.Color.WHITE);
                break;
            case READ:
                filterRead.setBackgroundResource(R.drawable.bg_search_filter_active);
                filterRead.setTextColor(android.graphics.Color.WHITE);
                break;
        }
    }

    // ────────────────────────────────────────────────────────
    // ADAPTER
    // ────────────────────────────────────────────────────────

    private class RequestListAdapter
            extends RecyclerView.Adapter<RequestListAdapter.ViewHolder> {

        private List<RequestItem> items;

        RequestListAdapter(List<RequestItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user_request, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            RequestItem item = items.get(position);

            h.number.setText(item.number);
            h.title.setText(item.title);
            h.submittedBy.setText("Submitted By: " + item.submittedBy);
            h.date.setText(item.date);

            String badgeType = "submissions".equals(requestType) ? item.requestType : requestType;
            if ("info".equals(badgeType)) {
                h.typeBadge.setVisibility(View.VISIBLE);
                h.typeBadge.setText("Info Request");
                h.typeBadge.setBackgroundResource(R.drawable.bg_submission_badge_info);
                h.typeBadge.setTextColor(android.graphics.Color.WHITE);
            } else if ("dataset".equals(badgeType)) {
                h.typeBadge.setVisibility(View.VISIBLE);
                h.typeBadge.setText("Dataset Image");
                h.typeBadge.setBackgroundResource(R.drawable.bg_submission_badge_dataset);
                h.typeBadge.setTextColor(android.graphics.Color.parseColor("#8B0000"));
            } else {
                h.typeBadge.setVisibility(View.GONE);
            }

            // Dot - use effective read state (Firestore + local SharedPreferences)
            boolean effectivelyRead = isEffectivelyRead(item);
            h.dot.setBackgroundResource(effectivelyRead
                    ? R.drawable.bg_dot_grey
                    : R.drawable.bg_dot_red);

            h.viewBtn.setTextColor(effectivelyRead
                    ? android.graphics.Color.parseColor("#9e9e9e")
                    : android.graphics.Color.parseColor("#203088"));

            h.viewBtn.setOnClickListener(v -> {
                // Mark as read in SharedPreferences when viewed
                markAsRead(item.docId);

                Fragment parentFrag = getParentFragment();
                String detailType = "submissions".equals(requestType) ? item.requestType : requestType;

                if (parentFrag instanceof AdminFragment) {
                    ((AdminFragment) parentFrag).navigateToRequestDetail(detailType, item.docId);
                } else {
                    Fragment detail;
                    if ("location".equals(detailType)) {
                        detail = AdminLocationRequestDetailsFragment.newInstance(
                                item.docId,
                                item.number,
                                item.status,
                                item.date,
                                item.submittedBy,
                                item.locationName,
                                item.coordinates,
                                item.description,
                                item.placeType,
                                item.reason,
                                item.latitude,
                                item.longitude
                        );
                    } else if ("info".equals(detailType)) {
                        detail = AdminInfoRequestDetailFragment.newInstance(
                                item.docId,
                                item.number,
                                item.status,
                                item.date,
                                item.submittedBy,
                                item.locationName,
                                item.coordinates,
                                item.description,
                                item.placeType,
                                item.latitude,
                                item.longitude
                        );
                    } else if ("dataset".equals(detailType)) {
                        detail = AdminDatasetSubmissionDetailFragment.newInstance(item.docId);
                    } else if ("business_location".equals(requestType)) {
                        detail = AdminBizLocationDetailFragment.newInstance(
                                item.docId,
                                item.number,
                                item.status,
                                item.date,
                                item.submittedBy,
                                item.locationName,
                                item.placeType,
                                item.coordinates,
                                item.latitude,
                                item.longitude
                        );
                    } else {
                        detail = AdminBizVerificationDetailFragment.newInstance(
                                item.docId,
                                item.number,
                                item.status,
                                item.date,
                                item.submittedBy,
                                item.title,
                                item.phone,
                                item.email,
                                item.address,
                                item.description,
                                item.businessType
                        );
                    }

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .add(R.id.admin_fragment_container, detail)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            View dot;
            TextView number, title, submittedBy, date, viewBtn, typeBadge;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                dot         = itemView.findViewById(R.id.user_req_dot);
                number      = itemView.findViewById(R.id.user_req_number);
                title       = itemView.findViewById(R.id.user_req_title);
                typeBadge   = itemView.findViewById(R.id.user_req_type_badge);
                submittedBy = itemView.findViewById(R.id.user_req_submitted_by);
                date        = itemView.findViewById(R.id.user_req_date);
                viewBtn     = itemView.findViewById(R.id.user_req_view_btn);
            }
        }
    }

    // ─── Data model ──────────────────────────
    private static class RequestItem {
        // Common fields
        String number, title, submittedBy, date, docId, status;
        boolean read;
        String requestType;
        long createdAtMillis;

        // Location & Info request fields
        String locationName, description, placeType, reason, coordinates;
        double latitude, longitude;
        String imageUrl, userNote;

        // Business verification fields
        String phone, email, address, businessType;

        RequestItem(String number, String title, String submittedBy,
                    String date, boolean read, String docId) {
            this.number = number; this.title = title;
            this.submittedBy = submittedBy; this.date = date;
            this.read = read; this.docId = docId;
            this.status = "In Review";
            this.requestType = "info";
            this.createdAtMillis = 0L;
            this.locationName = "";
            this.description = "";
            this.placeType = "";
            this.reason = "";
            this.coordinates = "";
            this.latitude = 0.0;
            this.longitude = 0.0;
            this.imageUrl = "";
            this.userNote = "";
            this.phone = "";
            this.email = "";
            this.address = "";
            this.businessType = "";
        }
    }
}
