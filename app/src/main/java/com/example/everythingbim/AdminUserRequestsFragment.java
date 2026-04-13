package com.example.everythingbim;

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

public class AdminUserRequestsFragment extends Fragment {

    // ─── Bundle args ─────────────────────────
    // Pass REQUEST_TYPE = "location" or "info"
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
    private String requestType = "info"; // "info" or "location"

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
        sectionTitle.setText(requestType.equals("location")
                ? "Add Location Requests"
                : "Add Information Requests");

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isLoadingRequests = false;
        currentLoadId++;
    }

    // ────────────────────────────────────────────────────────
    // LOAD DATA FROM FIRESTORE
    // ────────────────────────────────────────────────────────

    private void loadData() {
        if (isLoadingRequests) return;
        isLoadingRequests = true;

        final int thisLoadId = ++currentLoadId;

        // Clear items at the START to prevent any duplication
        allItems.clear();

        String collection = requestType.equals("location")
                ? "add_location_requests"
                : "add_info_requests";

        db.collection(collection)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (thisLoadId != currentLoadId) {
                        isLoadingRequests = false;
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snap) {
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
                        allItems.add(new RequestItem(number, name, submittedBy, date, read, doc.getId()));
                    }
                    if (allItems.isEmpty()) addPlaceholders();
                    applyFilters();
                    countTv.setText(String.valueOf(allItems.size()));
                    isLoadingRequests = false;
                })
                .addOnFailureListener(e -> {
                    if (thisLoadId != currentLoadId) {
                        isLoadingRequests = false;
                        return;
                    }
                    addPlaceholders();
                    applyFilters();
                    isLoadingRequests = false;
                });
    }

    private void addPlaceholders() {
        if (requestType.equals("location")) {
            allItems.add(new RequestItem("#201","Hackerton's Pub","User","2026/02/11",false,""));
            allItems.add(new RequestItem("#200","Marton Gardens","User","2026/02/01",false,""));
            allItems.add(new RequestItem("#199","Larton's Cemetery","User","2026/01/28",true,""));
            allItems.add(new RequestItem("#198","St. Michael's Church","User","2026/01/25",true,""));
            allItems.add(new RequestItem("#197","Bathsheba Rock","User","2026/01/20",true,""));
        } else {
            allItems.add(new RequestItem("#88","The Emancipation Statue","User","2026/02/11",false,""));
            allItems.add(new RequestItem("#87","Marton Gardens","User","2026/02/01",true,""));
            allItems.add(new RequestItem("#86","St. George Parish Church","User","2026/01/28",true,""));
        }
        countTv.setText(String.valueOf(allItems.size()));
    }

    // ────────────────────────────────────────────────────────
    // APPLY FILTERS
    // ────────────────────────────────────────────────────────

    private void applyFilters() {
        String query = searchEt.getText().toString().trim().toLowerCase();
        displayedItems.clear();

        for (RequestItem item : allItems) {
            // Read filter
            if (readFilter == ReadFilter.UNREAD && item.read) continue;
            if (readFilter == ReadFilter.READ && !item.read) continue;

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

            h.dot.setBackgroundResource(item.read
                    ? R.drawable.bg_dot_grey
                    : R.drawable.bg_dot_red);

            h.viewBtn.setTextColor(item.read
                    ? android.graphics.Color.parseColor("#9e9e9e")
                    : android.graphics.Color.parseColor("#203088"));

            h.viewBtn.setOnClickListener(v -> {
                Fragment parentFrag = getParentFragment();
                if (parentFrag instanceof AdminFragment) {
                    ((AdminFragment) parentFrag).navigateToRequestDetail(requestType, item.docId);
                } else {
                    Fragment detail;
                    if ("location".equals(requestType)) {
                        detail = AdminLocationRequestDetailsFragment.newInstance(item.docId);
                    } else if ("info".equals(requestType)) {
                        detail = AdminInfoRequestDetailFragment.newInstance(item.docId);
                    } else {
                        detail = AdminBizVerificationDetailFragment.newInstance(item.docId);
                    }
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.admin_fragment_container, detail)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            View dot;
            TextView number, title, submittedBy, date, viewBtn;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                dot         = itemView.findViewById(R.id.user_req_dot);
                number      = itemView.findViewById(R.id.user_req_number);
                title       = itemView.findViewById(R.id.user_req_title);
                submittedBy = itemView.findViewById(R.id.user_req_submitted_by);
                date        = itemView.findViewById(R.id.user_req_date);
                viewBtn     = itemView.findViewById(R.id.user_req_view_btn);
            }
        }
    }

    // ─── Data model ──────────────────────────
    private static class RequestItem {
        String number, title, submittedBy, date, docId;
        boolean read;

        RequestItem(String number, String title, String submittedBy,
                    String date, boolean read, String docId) {
            this.number = number; this.title = title;
            this.submittedBy = submittedBy; this.date = date;
            this.read = read; this.docId = docId;
        }
    }
}