package com.example.everythingbim;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminReportsFragment extends Fragment {

    // ─── Views ───────────────────────────────
    private RecyclerView recyclerView;
    private EditText searchEt;
    private View filterBtn;
    private View filterCard;
    private View filterScrim;
    private TextView filterTag;
    private TextView countTv;
    private RadioGroup typeGroup;
    private RadioGroup severityGroup;

    // ─── Adapter + data ──────────────────────
    private AdminReportsAdapter adapter;
    private List<Report> allReports    = new ArrayList<>();
    private List<Report> displayedReports = new ArrayList<>();

    // ─── Active filters ──────────────────────
    private String activeType     = null; // "Post" | "Account" | null
    private String activeSeverity = null; // "Minor" | "Moderate" | "Major" | null

    // ─── Firebase ────────────────────────────
    private FirebaseFirestore db;

    // ────────────────────────────────────────────────────────
    // LIFECYCLE
    // ────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        db = FirebaseFirestore.getInstance();

        // Bind views
        recyclerView = view.findViewById(R.id.reports_recycler);
        searchEt     = view.findViewById(R.id.reports_search_et);
        filterBtn    = view.findViewById(R.id.reports_filter_btn);
        filterCard   = view.findViewById(R.id.reports_filter_card);
        filterScrim  = view.findViewById(R.id.reports_filter_scrim);
        filterTag    = view.findViewById(R.id.reports_filter_tag);
        countTv      = view.findViewById(R.id.reports_count_tv);
        typeGroup     = view.findViewById(R.id.filter_type_group);
        severityGroup = view.findViewById(R.id.filter_severity_group);

        // Setup RecyclerView
        adapter = new AdminReportsAdapter(displayedReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(),
                        DividerItemDecoration.VERTICAL));

        // ── Filter button — shows/hides card ─
        filterBtn.setOnClickListener(v -> toggleFilterCard(true));

        // Scrim dismisses the filter card without applying
        filterScrim.setOnClickListener(v -> toggleFilterCard(false));

        // ── Filter radio listeners ────────────
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_type_post)    activeType = "Post";
            else if (checkedId == R.id.filter_type_account) activeType = "Account";
            else activeType = null;
            applyFilters();
            toggleFilterCard(false);
        });

        severityGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_severity_minor)    activeSeverity = "Minor";
            else if (checkedId == R.id.filter_severity_moderate) activeSeverity = "Moderate";
            else if (checkedId == R.id.filter_severity_major)    activeSeverity = "Major";
            else activeSeverity = null;
            applyFilters();
            toggleFilterCard(false);
        });

        // ── Search as user types ──────────────
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
        });

        loadReports();

        return view;
    }

    // ────────────────────────────────────────────────────────
    // FILTER CARD TOGGLE
    // Shows or hides the filter card and the scrim
    // ────────────────────────────────────────────────────────

    private void toggleFilterCard(boolean show) {
        filterCard.setVisibility(show ? View.VISIBLE : View.GONE);
        filterScrim.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ────────────────────────────────────────────────────────
    // APPLY FILTERS
    // Chains search + type + severity filters on allReports
    // then updates the active filter tag and count
    // ────────────────────────────────────────────────────────

    private void applyFilters() {
        String query = searchEt.getText().toString().trim().toLowerCase();

        displayedReports.clear();

        for (Report r : allReports) {
            // Search filter — matches ID or title
            if (!query.isEmpty()) {
                boolean matchesId    = r.getId().toLowerCase().contains(query);
                boolean matchesTitle = r.getTitle().toLowerCase().contains(query);
                if (!matchesId && !matchesTitle) continue;
            }

            // Type filter
            if (activeType != null && !r.getType().equalsIgnoreCase(activeType)) continue;

            // Severity filter
            if (activeSeverity != null
                    && !r.getSeverity().equalsIgnoreCase(activeSeverity)) continue;

            displayedReports.add(r);
        }

        adapter.notifyDataSetChanged();
        countTv.setText(String.valueOf(displayedReports.size()));

        // Show active filter tag — displays active severity or type
        String activeLabel = activeSeverity != null ? activeSeverity
                : activeType != null ? activeType : null;

        if (activeLabel != null) {
            filterTag.setText(activeLabel);
            filterTag.setVisibility(View.VISIBLE);
        } else {
            filterTag.setVisibility(View.GONE);
        }
    }

    // ────────────────────────────────────────────────────────
    // LOAD REPORTS FROM FIRESTORE
    // Queries the "reports" collection ordered by createdAt desc
    // ────────────────────────────────────────────────────────

    private void loadReports() {
        db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allReports.clear();

                    for (QueryDocumentSnapshot doc : snapshot) {
                        String id       = doc.getId().substring(0, 3).toUpperCase();
                        String title    = doc.getString("title");
                        String type     = doc.getString("type");     // "Post" or "Account"
                        String severity = doc.getString("severity"); // "Minor","Moderate","Major"
                        boolean read    = Boolean.TRUE.equals(doc.getBoolean("read"));

                        com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null
                                ? new java.text.SimpleDateFormat("yyyy/MM/dd",
                                java.util.Locale.getDefault()).format(ts.toDate())
                                : "";

                        String number = doc.contains("number")
                                ? "#" + doc.getLong("number")
                                : "#" + id;

                        allReports.add(new Report(
                                number,
                                title   != null ? title    : "Report",
                                type    != null ? type     : "Post",
                                severity != null ? severity : "Minor",
                                date,
                                read,
                                doc.getId()
                        ));
                    }

                    // If Firestore is empty, show placeholder data matching Figma
                    if (allReports.isEmpty()) {
                        allReports.add(new Report("#94","Post - Hate Speech",   "Post","Moderate","2026/02/11",false,""));
                        allReports.add(new Report("#93","Post - Spam",          "Post","Minor",   "2026/02/11",false,""));
                        allReports.add(new Report("#92","Account - Hacked Account","Account","Major","2026/02/11",false,""));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,""));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,""));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,""));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,""));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,""));
                    }

                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    // Show placeholder on error
                    allReports.add(new Report("#94","Post - Hate Speech",   "Post","Moderate","2026/02/11",false,""));
                    allReports.add(new Report("#93","Post - Spam",          "Post","Minor",   "2026/02/11",false,""));
                    allReports.add(new Report("#92","Account - Hacked Account","Account","Major","2026/02/11",false,""));
                    allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,""));
                    applyFilters();
                });
    }

    // ────────────────────────────────────────────────────────
    // REPORT DATA MODEL
    // ────────────────────────────────────────────────────────

    public static class Report {
        private String id, title, type, severity, date, docId;
        private boolean read;

        public Report(String id, String title, String type,
                      String severity, String date, boolean read, String docId) {
            this.id       = id;
            this.title    = title;
            this.type     = type;
            this.severity = severity;
            this.date     = date;
            this.read     = read;
            this.docId    = docId;
        }

        public String getId()       { return id; }
        public String getTitle()    { return title; }
        public String getType()     { return type; }
        public String getSeverity() { return severity; }
        public String getDate()     { return date; }
        public boolean isRead()     { return read; }
        public String getDocId()    { return docId; }
    }

    // ────────────────────────────────────────────────────────
    // ADAPTER
    // ────────────────────────────────────────────────────────

    private class AdminReportsAdapter
            extends RecyclerView.Adapter<AdminReportsAdapter.ViewHolder> {

        private List<Report> items;

        AdminReportsAdapter(List<Report> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_report, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Report r = items.get(position);

            h.number.setText(r.getId());
            h.title.setText(r.getTitle());
            h.date.setText("Submitted: " + r.getDate());

            // ── Severity colour ──────────────────
            h.severity.setText(r.getSeverity());
            switch (r.getSeverity()) {
                case "Major":
                    h.severity.setTextColor(android.graphics.Color.parseColor("#c0392b"));
                    break;
                case "Moderate":
                    h.severity.setTextColor(android.graphics.Color.parseColor("#e67e22"));
                    break;
                default: // Minor
                    h.severity.setTextColor(android.graphics.Color.parseColor("#2980b9"));
                    break;
            }

            // ── Dot — red if unread, grey if read ──
            h.dot.setBackgroundResource(r.isRead()
                    ? R.drawable.bg_dot_grey
                    : R.drawable.bg_dot_red);

            // ── View button colour — cobalt if unread, grey if read ──
            h.viewBtn.setTextColor(r.isRead()
                    ? android.graphics.Color.parseColor("#9e9e9e")
                    : android.graphics.Color.parseColor("#203088"));

            h.viewBtn.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.admin_fragment_container,
                                AdminReportDetailFragment.newInstance(r.getDocId()))
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            View dot;
            TextView number, title, severity, date, viewBtn;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                dot      = itemView.findViewById(R.id.report_dot);
                number   = itemView.findViewById(R.id.report_number);
                title    = itemView.findViewById(R.id.report_title);
                severity = itemView.findViewById(R.id.report_severity);
                date     = itemView.findViewById(R.id.report_date);
                viewBtn  = itemView.findViewById(R.id.report_view_btn);
            }
        }
    }
}