package com.example.everythingbim.ui.admin;

import android.content.Context;
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
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminReportsFragment extends Fragment {
    private static final String PREF_ADMIN_REPORTS_SCROLL_HINT_SEEN =
            "admin_reports_scroll_hint_seen";

    // Views
    private RecyclerView recyclerView;
    private EditText searchEt;
    private View filterBtn;
    private View filterCard;
    private View filterScrim;
    private TextView filterTag;
    private TextView countTv;
    private RadioGroup typeGroup;
    private RadioGroup severityGroup;
    private RadioGroup readStatusGroup;  // Read/Unread filter
    private RadioGroup statusGroup;       // Report status filter (In Review/Completed/Rejected)

    // Adapter + data
    private AdminReportsAdapter adapter;
    private List<Report> allReports    = new ArrayList<>();
    private List<Report> displayedReports = new ArrayList<>();

    // Active filters
    private String activeType     = null; // "Post" | "Account" | null
    private String activeSeverity = null; // "Minor" | "Moderate" | "Major" | null
    private String activeReadStatus = null; // "Read" | "Unread" | null
    private String activeStatus   = null; // "In Review" | "Completed" | "Rejected" | null

    // Firebase
    private FirebaseFirestore db;
    // Local read state tracking (SharedPreferences-backed via ReadStateManager)
    private boolean isLoadingReports = false;
    private int currentLoadId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        db = FirebaseFirestore.getInstance();
        isLoadingReports = false;

        // Bind views
        recyclerView = view.findViewById(R.id.reports_recycler);
        searchEt     = view.findViewById(R.id.reports_search_et);
        filterBtn    = view.findViewById(R.id.reports_filter_btn);
        View watchlistBtn = view.findViewById(R.id.reports_watchlist_btn);
        filterCard   = view.findViewById(R.id.reports_filter_card);
        filterScrim  = view.findViewById(R.id.reports_filter_scrim);
        filterTag    = view.findViewById(R.id.reports_filter_tag);
        countTv      = view.findViewById(R.id.reports_count_tv);
        typeGroup     = view.findViewById(R.id.filter_type_group);
        severityGroup = view.findViewById(R.id.filter_severity_group);
        readStatusGroup = view.findViewById(R.id.filter_read_status_group);
        statusGroup   = view.findViewById(R.id.filter_status_group);

        // Setup RecyclerView
        adapter = new AdminReportsAdapter(displayedReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(),
                        DividerItemDecoration.VERTICAL));
        setupKeyboardHints(view);

        // Filter button
        filterBtn.setOnClickListener(v -> toggleFilterCard(true));

        // Watchlist button - navigate to watch list screen
        watchlistBtn.setOnClickListener(v -> {
            if (getParentFragment() instanceof AdminFragment) {
                ((AdminFragment) getParentFragment()).navigateToWatchList();
            }
        });

        // Scrim dismisses the filter card without applying
        filterScrim.setOnClickListener(v -> toggleFilterCard(false));

        // Filter radio listeners
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_type_all)     activeType = null;
            else if (checkedId == R.id.filter_type_post)    activeType = "Post";
            else if (checkedId == R.id.filter_type_account) activeType = "Account";
            else activeType = null;
            applyFilters();
            toggleFilterCard(false);
        });

        severityGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_severity_all)    activeSeverity = null;
            else if (checkedId == R.id.filter_severity_minor)    activeSeverity = "Minor";
            else if (checkedId == R.id.filter_severity_moderate) activeSeverity = "Moderate";
            else if (checkedId == R.id.filter_severity_major)    activeSeverity = "Major";
            else activeSeverity = null;
            applyFilters();
            toggleFilterCard(false);
        });

        statusGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_status_all)    activeStatus = null;
            else if (checkedId == R.id.filter_status_in_review) activeStatus = "In Review";
            else if (checkedId == R.id.filter_status_completed) activeStatus = "Completed";
            else if (checkedId == R.id.filter_status_rejected) activeStatus = "Rejected";
            else activeStatus = null;
            applyFilters();
            toggleFilterCard(false);
        });

        // Read Status filter (Read/Unread)
        readStatusGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filter_read_all)    activeReadStatus = null;
            else if (checkedId == R.id.filter_read_unread) activeReadStatus = "Unread";
            else if (checkedId == R.id.filter_read_read)   activeReadStatus = "Read";
            else activeReadStatus = null;
            applyFilters();
            toggleFilterCard(false);
        });

        // Search as user types
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

    private void setupKeyboardHints(View root) {
        KeyboardScrollHintHelper.attach(
                root,
                root,
                recyclerView,
                PREF_ADMIN_REPORTS_SCROLL_HINT_SEEN,
                extraBottom -> {
                    if (recyclerView == null) {
                        return;
                    }
                    recyclerView.setPadding(
                            recyclerView.getPaddingLeft(),
                            recyclerView.getPaddingTop(),
                            recyclerView.getPaddingRight(),
                            extraBottom);
                    recyclerView.setClipToPadding(false);
                });
    }

    // Filter card toggle
    private void toggleFilterCard(boolean show) {
        filterCard.setVisibility(show ? View.VISIBLE : View.GONE);
        filterScrim.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Apply filters
    private void applyFilters() {
        String query = searchEt.getText().toString().trim().toLowerCase();

        displayedReports.clear();

        for (Report r : allReports) {
            // Search filter
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

            // Read Status filter (Read/Unread)
            if (activeReadStatus != null) {
                boolean isRead = isEffectivelyRead(r);
                if (activeReadStatus.equals("Read") && !isRead) continue;
                if (activeReadStatus.equals("Unread") && isRead) continue;
            }

            // Report Status filter (In Review/Completed/Rejected)
            if (activeStatus != null && !r.getStatus().equalsIgnoreCase(activeStatus)) continue;

            displayedReports.add(r);
        }

        adapter.notifyDataSetChanged();
        countTv.setText(String.valueOf(displayedReports.size()));

        // Show active filter tag (format: "Severity, Type, Read Status, Report Status")
        String severityLabel = activeSeverity != null ? activeSeverity : "All";
        String typeLabel = activeType != null ? activeType : "All";
        String readLabel = activeReadStatus != null ? activeReadStatus : "All";
        String reportLabel = activeStatus != null ? activeStatus : "All";
        String activeLabel = severityLabel + ", " + typeLabel + ", " + readLabel + ", " + reportLabel;

        if (activeType != null || activeSeverity != null || activeReadStatus != null || activeStatus != null) {
            filterTag.setText(activeLabel);
            filterTag.setVisibility(View.VISIBLE);
        } else {
            filterTag.setVisibility(View.GONE);
        }
    }

    // Load reports from Firestore - uses snapshot listener for real-time updates
    private void loadReports() {
        // Remove previous listener before adding new one to prevent duplicates
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("reports")
                .orderBy("submittedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        // Show placeholder on error
                        allReports.clear();
                        allReports.add(new Report("#94","Post - Hate Speech",   "Post","Moderate","2026/02/11",false,"ph_report_94","User12342","This is spam content","","In Review"));
                        allReports.add(new Report("#93","Post - Spam",          "Post","Minor",   "2026/02/11",false,"ph_report_93","User56789","Fake engagement post","","In Review"));
                        allReports.add(new Report("#92","Account - Hacked Account","Account","Major","2026/02/11",false,"ph_report_92","HackedUser","Account was compromised","","In Review"));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,"ph_report_91","User11111","Another spam post","","Completed"));
                        applyFilters();
                        return;
                    }

                    if (snapshot == null || snapshot.isEmpty()) {
                        // If Firestore is empty, show placeholder data
                        allReports.clear();
                        allReports.add(new Report("#94","Post - Hate Speech",   "Post","Moderate","2026/02/11",false,"ph_report_94","User12342","This is spam content","","In Review"));
                        allReports.add(new Report("#93","Post - Spam",          "Post","Minor",   "2026/02/11",false,"ph_report_93","User56789","Fake engagement post","","In Review"));
                        allReports.add(new Report("#92","Account - Hacked Account","Account","Major","2026/02/11",false,"ph_report_92","HackedUser","Account was compromised","","In Review"));
                        allReports.add(new Report("#91","Post - Spam",          "Post","Minor",   "2026/02/09",true,"ph_report_91","User11111","Another spam post","","Completed"));
                        allReports.add(new Report("#90","Post - Spam",          "Post","Minor",   "2026/02/09",true,"ph_report_90","User22222","More spam content","","Completed"));
                        allReports.add(new Report("#89","Post - Spam",          "Post","Minor",   "2026/02/09",true,"ph_report_89","User33333","Even more spam","","Completed"));
                        allReports.add(new Report("#88","Post - Spam",          "Post","Minor",   "2026/02/09",true,"ph_report_88","User44444","Spam again","","Completed"));
                        allReports.add(new Report("#87","Post - Spam",          "Post","Minor",   "2026/02/09",true,"ph_report_87","User55555","Yet another spam","","Completed"));
                    } else {
                        allReports.clear();
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String docId = doc.getId();
                            String id = docId.substring(0, Math.min(3, docId.length())).toUpperCase();

                            // Read fields saved by ViewPost.submitReport()
                            String reportType = doc.getString("reportType");  // "Post" or "Account"
                            String reason = doc.getString("reason");         // "Spam", "Hacked account", etc.

                            // Build title and derive severity
                            String title = (reportType != null ? reportType : "Report") + " - " + (reason != null ? reason : "Unknown");
                            String severity = deriveSeverity(reason);
                            String type = reportType != null ? reportType : "Post";

                            // Read date from submittedAt (Timestamp)
                            com.google.firebase.Timestamp ts = doc.getTimestamp("submittedAt");
                            String date = ts != null
                                    ? new java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(ts.toDate())
                                    : "";

                            // Read additional fields for detail view
                            String reportedUser = doc.getString("reportedUser") != null ? doc.getString("reportedUser") : "";
                            String caption = doc.getString("postCaption") != null ? doc.getString("postCaption") : "";
                            String imageUrl = doc.getString("postImageUrl") != null ? doc.getString("postImageUrl") : "";
                            String status = doc.getString("status") != null ? doc.getString("status") : "In Review";

                            boolean read = Boolean.TRUE.equals(doc.getBoolean("read"));

                            allReports.add(new Report(
                                    "#" + id,
                                    title,
                                    type,
                                    severity,
                                    date,
                                    read,
                                    docId,
                                    reportedUser,
                                    caption,
                                    imageUrl,
                                    status
                            ));
                        }
                    }

                    applyFilters();
                });
    }

    // Listener registration for cleanup
    private com.google.firebase.firestore.ListenerRegistration listenerRegistration;

    // Derive severity from report reason
    private String deriveSeverity(String reason) {
        if (reason == null) return "Minor";
        switch (reason) {
            case "Hacked account":
                return "Major";
            case "Dangerous activities":
            case "Hate speech":
            case "Sexual content":
                return "Major";
            case "Offensive behaviour":
                return "Moderate";
            case "Spam":
            default:
                return "Minor";
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        currentLoadId++; // Invalidate any pending callbacks
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload read state from SharedPreferences in case it changed
        applyFilters();
    }

    // Check if a report is locally marked as read
    public boolean isReportRead(String docId) {
        return ReadStateManager.isReportRead(requireContext(), docId);
    }

    // Mark a report as read and persist to SharedPreferences
    public void markAsRead(String docId) {
        ReadStateManager.markReportRead(requireContext(), docId);
        applyFilters();
    }

    // Effective read state = Firestore read OR locally tracked read
    private boolean isEffectivelyRead(Report r) {
        return r.isRead() || ReadStateManager.isReportRead(requireContext(), r.getDocId());
    }

    // Report data model
    public static class Report {
        private String id, title, type, severity, date, docId;
        private boolean read;
        private String reportedUser;
        private String caption;
        private String imageUrl;
        private String status;

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

        public Report(String id, String title, String type,
                      String severity, String date, boolean read, String docId,
                      String reportedUser, String caption, String imageUrl, String status) {
            this.id            = id;
            this.title         = title;
            this.type          = type;
            this.severity      = severity;
            this.date          = date;
            this.read          = read;
            this.docId         = docId;
            this.reportedUser  = reportedUser;
            this.caption       = caption;
            this.imageUrl      = imageUrl;
            this.status        = status;
        }

        public String getId()          { return id; }
        public String getTitle()       { return title; }
        public String getType()        { return type; }
        public String getSeverity()     { return severity; }
        public String getDate()        { return date; }
        public boolean isRead()        { return read; }
        public String getDocId()       { return docId; }
        public String getReportedUser(){ return reportedUser != null ? reportedUser : ""; }
        public String getCaption()     { return caption != null ? caption : ""; }
        public String getImageUrl()    { return imageUrl != null ? imageUrl : ""; }
        public String getStatus()      { return status != null ? status : "In Review"; }
    }

    // Adapter
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

            // Severity colour
            h.severity.setText(r.getSeverity());
            switch (r.getSeverity()) {
                case "Major":
                    h.severity.setTextColor(android.graphics.Color.parseColor("#c0392b"));
                    break;
                case "Moderate":
                    h.severity.setTextColor(android.graphics.Color.parseColor("#e67e22"));
                    break;
                default:
                    h.severity.setTextColor(android.graphics.Color.parseColor("#2980b9"));
                    break;
            }

            // Dot - use effective read state (Firestore + local SharedPreferences)
            boolean effectivelyRead = isEffectivelyRead(r);
            h.dot.setBackgroundResource(effectivelyRead
                    ? R.drawable.bg_dot_grey
                    : R.drawable.bg_dot_red);

            // Status dot
            String status = r.getStatus();
            if ("In Review".equals(status)) {
                h.statusDot.setBackgroundResource(R.drawable.bg_dot_light_blue);
            } else if ("Completed".equals(status)) {
                h.statusDot.setBackgroundResource(R.drawable.bg_dot_green);
            } else if ("Rejected".equals(status)) {
                h.statusDot.setBackgroundResource(R.drawable.bg_dot_orange);
            } else {
                h.statusDot.setBackgroundResource(R.drawable.bg_dot_grey);
            }

            // View button colour - use effective read state
            h.viewBtn.setTextColor(effectivelyRead
                    ? android.graphics.Color.parseColor("#9e9e9e")
                    : android.graphics.Color.parseColor("#203088"));

            h.viewBtn.setOnClickListener(v -> {
                // Mark as read in SharedPreferences when viewed
                markAsRead(r.getDocId());

                if (getParentFragment() instanceof AdminFragment) {
                    ((AdminFragment) getParentFragment()).navigateToReportDetail(
                            r.getDocId(),
                            r.getId(),
                            r.getTitle(),
                            r.getType(),
                            r.getSeverity(),
                            r.getDate(),
                            r.getReportedUser(),
                            r.getCaption(),
                            r.getImageUrl(),
                            r.getStatus());
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            View dot;
            View statusDot;
            TextView number, title, severity, date, viewBtn;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                dot        = itemView.findViewById(R.id.report_dot);
                statusDot  = itemView.findViewById(R.id.status_dot);
                number     = itemView.findViewById(R.id.report_number);
                title      = itemView.findViewById(R.id.report_title);
                severity   = itemView.findViewById(R.id.report_severity);
                date       = itemView.findViewById(R.id.report_date);
                viewBtn    = itemView.findViewById(R.id.report_view_btn);
            }
        }
    }
}
