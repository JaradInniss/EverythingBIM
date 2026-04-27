package com.example.everythingbim;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminReportDetailFragment extends Fragment {

    // ─── Bundle keys passed from AdminReportsFragment ──
    public static final String ARG_DOC_ID = "doc_id";
    public static final String ARG_NUMBER = "number";
    public static final String ARG_TITLE = "title";
    public static final String ARG_TYPE = "type";
    public static final String ARG_SEVERITY = "severity";
    public static final String ARG_DATE = "date";
    public static final String ARG_REPORTED_USER = "reportedUser";
    public static final String ARG_CAPTION = "caption";
    public static final String ARG_IMAGE_URL = "imageUrl";
    public static final String ARG_STATUS = "status";

    // ─── Views ───────────────────────────────
    private TextView numberTv;
    private TextView severityBadge;
    private TextView statusTv;
    private TextView dateTv;
    private TextView typeTv;
    private TextView issueTv;
    private TextView userTv;
    private TextView postDateTv;
    private TextView captionTv;
    private ImageView postImage;
    private Spinner actionSpinner;
    private Button submitBtn;

    // ─── State ───────────────────────────────
    private String selectedAction = "";
    private String docId          = "";

    // ─── Cached report data ─────────────────
    private String cachedNumber     = "";
    private String cachedTitle      = "";
    private String cachedType       = "";
    private String cachedSeverity   = "";
    private String cachedDate       = "";
    private String cachedStatus     = "";
    private String cachedReportedUser = "";
    private String cachedCaption    = "";
    private String cachedImageUrl   = "";

    // ─── Firebase ────────────────────────────
    private FirebaseFirestore db;
    private FirebaseStorage   storage;

    // ─── Activity Logger ────────────────────
    private ActivityLogger activityLogger;

    // ─── Admin action options organized by severity ─────────────────
    // MAJOR: Emergency/immediate actions
    private static final String ACTION_REMOVE_IMMEDIATE = "Remove Content Immediately";
    private static final String ACTION_SUSPEND_TEMP = "Suspend Account (Temporary)";
    private static final String ACTION_SUSPEND_PERM = "Suspend Account (Permanent)";
    private static final String ACTION_LOCK_INVESTIGATION = "Lock Account Pending Investigation";
    private static final String ACTION_ESCALATE_LAW = "Escalate to Law Enforcement";
    private static final String ACTION_CRISIS_REFERRAL = "Refer to Crisis Intervention";
    private static final String ACTION_WATCHLIST = "Flag for Watch-List Monitoring";
    private static final String ACTION_NOTIFY_URGENT = "Notify Reporter (Urgent Action)";
    private static final String ACTION_CASE_NOTES_MAJOR = "Add Case Notes";

    // MODERATE: Serious but non-emergency
    private static final String ACTION_REMOVE_POST = "Remove Specific Post/Comment";
    private static final String ACTION_WARN_1ST = "Issue Formal Warning (1st)";
    private static final String ACTION_WARN_2ND = "Issue Formal Warning (2nd/Final)";
    private static final String ACTION_RESTRICT = "Restrict Account Features";
    private static final String ACTION_SHADOW_RESTRICT = "Shadow-Restrict Account";
    private static final String ACTION_PRE_APPROVAL = "Require Pre-Approval for Posts";
    private static final String ACTION_MISINFO_LABEL = "Add Misinformation Label";
    private static final String ACTION_REQUEST_EDIT = "Request Content Edit/Removal";
    private static final String ACTION_DISMISS_BORDERLINE = "Dismiss (Borderline Report)";
    private static final String ACTION_NOTIFY_OUTCOME = "Notify Reporter of Outcome";
    private static final String ACTION_CASE_NOTES_MOD = "Add Case Notes";

    // MINOR: Low-severity/automation-friendly
    private static final String ACTION_DISMISS_NO_ACTION = "Dismiss Report (No Action Needed)";
    private static final String ACTION_REMOVE_SPAM = "Remove Content (Spam/Fake Engagement)";
    private static final String ACTION_SOFT_MUTE = "Soft-Mute in Recommendations";
    private static final String ACTION_AUTO_WARN = "Issue Automated Warning";
    private static final String ACTION_MARK_BOT = "Mark as Bot/Satire Account";
    private static final String ACTION_MERGE_DUPLICATES = "Merge Duplicate Reports";
    private static final String ACTION_NOTIFY_NO_VIOLATION = "Notify Reporter (No Violation Found)";

    // CROSS-CATEGORY: Any severity
    private static final String ACTION_VIEW_HISTORY = "View Full Report History";
    private static final String ACTION_BULK_ACTION = "Bulk Action (Multiple Reports)";
    private static final String ACTION_ASSIGN_MOD = "Assign to Moderator/Escalation";
    private static final String ACTION_SET_DEADLINE = "Set Review Deadline/SLA";
    private static final String ACTION_UNDO = "Undo Previous Action";
    private static final String ACTION_EXPORT = "Export Case Data";
    private static final String ACTION_APPEAL_QUEUE = "Appeal Queue Review";

    private static final String[] ACTIONS = {
            "Select Action",
            // MAJOR
            "━━ MAJOR ━━",
            ACTION_REMOVE_IMMEDIATE,
            ACTION_SUSPEND_TEMP,
            ACTION_SUSPEND_PERM,
            ACTION_LOCK_INVESTIGATION,
            ACTION_ESCALATE_LAW,
            ACTION_CRISIS_REFERRAL,
            ACTION_WATCHLIST,
            ACTION_NOTIFY_URGENT,
            ACTION_CASE_NOTES_MAJOR,
            // MODERATE
            "━━ MODERATE ━━",
            ACTION_REMOVE_POST,
            ACTION_WARN_1ST,
            ACTION_WARN_2ND,
            ACTION_RESTRICT,
            ACTION_SHADOW_RESTRICT,
            ACTION_PRE_APPROVAL,
            ACTION_MISINFO_LABEL,
            ACTION_REQUEST_EDIT,
            ACTION_DISMISS_BORDERLINE,
            ACTION_NOTIFY_OUTCOME,
            ACTION_CASE_NOTES_MOD,
            // MINOR
            "━━ MINOR ━━",
            ACTION_DISMISS_NO_ACTION,
            ACTION_REMOVE_SPAM,
            ACTION_SOFT_MUTE,
            ACTION_AUTO_WARN,
            ACTION_MARK_BOT,
            ACTION_MERGE_DUPLICATES,
            ACTION_NOTIFY_NO_VIOLATION,
            // CROSS-CATEGORY
            "━━ ACTIONS ━━",
            ACTION_VIEW_HISTORY,
            ACTION_BULK_ACTION,
            ACTION_ASSIGN_MOD,
            ACTION_SET_DEADLINE,
            ACTION_UNDO,
            ACTION_EXPORT,
            ACTION_APPEAL_QUEUE
    };

    // ────────────────────────────────────────────────────────
    // FACTORY — pass all Report fields from the list
    // ────────────────────────────────────────────────────────

    public static AdminReportDetailFragment newInstance(
            String docId, String number, String title, String type,
            String severity, String date, String reportedUser,
            String caption, String imageUrl, String status) {

        AdminReportDetailFragment f = new AdminReportDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOC_ID, docId);
        args.putString(ARG_NUMBER, number);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TYPE, type);
        args.putString(ARG_SEVERITY, severity);
        args.putString(ARG_DATE, date);
        args.putString(ARG_REPORTED_USER, reportedUser);
        args.putString(ARG_CAPTION, caption);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_STATUS, status);
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

        View view = inflater.inflate(
                R.layout.fragment_admin_report_detail, container, false);

        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        activityLogger = new ActivityLogger();

        if (getArguments() != null) {
            docId = getArguments().getString(ARG_DOC_ID, "");
            cachedNumber = getArguments().getString(ARG_NUMBER, "");
            cachedTitle = getArguments().getString(ARG_TITLE, "");
            cachedType = getArguments().getString(ARG_TYPE, "Post");
            cachedSeverity = getArguments().getString(ARG_SEVERITY, "Minor");
            cachedDate = getArguments().getString(ARG_DATE, "");
            cachedStatus = getArguments().getString(ARG_STATUS, "In Review");
            cachedReportedUser = getArguments().getString(ARG_REPORTED_USER, "");
            cachedCaption = getArguments().getString(ARG_CAPTION, "");
            cachedImageUrl = getArguments().getString(ARG_IMAGE_URL, "");
        }

        // Bind views
        numberTv     = view.findViewById(R.id.report_detail_number);
        severityBadge = view.findViewById(R.id.report_detail_severity_badge);
        statusTv     = view.findViewById(R.id.report_detail_status);
        dateTv       = view.findViewById(R.id.report_detail_date);
        typeTv       = view.findViewById(R.id.report_detail_type);
        issueTv      = view.findViewById(R.id.report_detail_issue);
        userTv       = view.findViewById(R.id.report_detail_user);
        postDateTv   = view.findViewById(R.id.report_detail_post_date);
        captionTv    = view.findViewById(R.id.report_detail_caption);
        postImage    = view.findViewById(R.id.report_detail_image);
        actionSpinner = view.findViewById(R.id.report_detail_action_spinner);
        submitBtn    = view.findViewById(R.id.report_detail_submit_btn);

        // Back button
        view.findViewById(R.id.report_detail_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        setupActionSpinner();
        loadReportData();

        // Log activity - admin viewed this report
        if (!docId.isEmpty()) {
            activityLogger.logView(ActivityLogger.TYPE_REPORT, cachedTitle, docId);
        }

        // Submit
        submitBtn.setOnClickListener(v -> submitAction());

        return view;
    }

    // ────────────────────────────────────────────────────────
    // LOAD REPORT DATA - uses cached bundle data or fetches from Firestore
    // ────────────────────────────────────────────────────────

    private void loadReportData() {
        // If we have cached title, display it directly (came from list with full data)
        // Otherwise fetch from Firestore using docId (came from activity log)
        if (cachedTitle != null && !cachedTitle.isEmpty()) {
            displayCachedData();
        } else if (!docId.isEmpty()) {
            fetchReportFromFirestore();
        }
    }

    private void displayCachedData() {
        numberTv.setText("Report " + cachedNumber);

        if (cachedSeverity != null) {
            severityBadge.setText(cachedSeverity.toUpperCase());
            applySeverityBadgeColor(cachedSeverity);
        }

        statusTv.setText(cachedStatus != null ? cachedStatus : "In Review");
        applyStatusColor(cachedStatus);

        dateTv.setText("Submitted: " + cachedDate);

        typeTv.setText(cachedType != null ? cachedType : "Post");

        issueTv.setText(cachedTitle);

        userTv.setText(cachedReportedUser != null ? cachedReportedUser : "");

        postDateTv.setText("");

        captionTv.setText(cachedCaption != null ? cachedCaption : "");

        if (cachedImageUrl != null && !cachedImageUrl.isEmpty()) {
            loadImageFromStorage(cachedImageUrl);
        }
    }

    private void fetchReportFromFirestore() {
        db.collection("reports").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String id = doc.getId().substring(0, 3).toUpperCase();
                        String number = doc.contains("number")
                                ? "#" + doc.getLong("number")
                                : "#" + id;

                        numberTv.setText("Report " + number);
                        severityBadge.setText(doc.getString("severity") != null
                                ? doc.getString("severity").toUpperCase() : "Minor");
                        applySeverityBadgeColor(doc.getString("severity"));
                        statusTv.setText(doc.getString("status") != null
                                ? doc.getString("status") : "In Review");
                        applyStatusColor(doc.getString("status"));

                        com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null
                                ? new java.text.SimpleDateFormat("yyyy/MM/dd",
                                java.util.Locale.getDefault()).format(ts.toDate())
                                : "";
                        dateTv.setText("Submitted: " + date);

                        typeTv.setText(doc.getString("type") != null
                                ? doc.getString("type") : "Post");
                        issueTv.setText(doc.getString("title") != null
                                ? doc.getString("title") : "Report");
                        userTv.setText(doc.getString("reportedUser") != null
                                ? doc.getString("reportedUser") : "");
                        captionTv.setText(doc.getString("caption") != null
                                ? doc.getString("caption") : "");

                        String imageUrl = doc.getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            loadImageFromStorage(imageUrl);
                        }
                    } else {
                        issueTv.setText("Report not found");
                    }
                })
                .addOnFailureListener(e -> {
                    issueTv.setText("Error loading report");
                });
    }

    // ────────────────────────────────────────────────────────
    // SEVERITY BADGE COLOUR
    // Yellow = Minor, Orange = Moderate, Red = Major
    // ────────────────────────────────────────────────────────

    private void applySeverityBadgeColor(String severity) {
        switch (severity) {
            case "Major":
                severityBadge.setBackgroundResource(R.drawable.bg_severity_badge_red);
                severityBadge.setTextColor(android.graphics.Color.WHITE);
                break;
            case "Moderate":
                severityBadge.setBackgroundResource(R.drawable.bg_severity_badge_orange);
                severityBadge.setTextColor(android.graphics.Color.WHITE);
                break;
            default: // Minor
                severityBadge.setBackgroundResource(R.drawable.bg_severity_badge_yellow);
                severityBadge.setTextColor(android.graphics.Color.parseColor("#09090b"));
                break;
        }
    }

    private void applyStatusColor(String status) {
        if ("Completed".equals(status)) {
            statusTv.setTextColor(android.graphics.Color.parseColor("#28965a"));
        } else if ("Rejected".equals(status)) {
            statusTv.setTextColor(android.graphics.Color.parseColor("#c0392b"));
        } else {
            statusTv.setTextColor(android.graphics.Color.parseColor("#203088"));
        }
    }

    // ────────────────────────────────────────────────────────
    // LOAD IMAGE FROM FIREBASE STORAGE
    // ────────────────────────────────────────────────────────

    private void loadImageFromStorage(String url) {
        try {
            storage.getReferenceFromUrl(url)
                    .getBytes(2 * 1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        postImage.setImageBitmap(bmp);
                    })
                    .addOnFailureListener(e -> {
                        // Silently keep placeholder background
                    });
        } catch (Exception e) {
            // Invalid URL — keep placeholder
        }
    }

    // ────────────────────────────────────────────────────────
    // ACTION SPINNER
    // Same pattern as AddLocationRequestFragment spinners
    // ────────────────────────────────────────────────────────

    private void setupActionSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ACTIONS) {

            @Override
            public boolean isEnabled(int position) {
                // Disable header items (positions 1, 12, 25, 34)
                if (position == 0) return false; // "Select Action"
                if (position == 1 || position == 12 || position == 25 || position == 34) return false;
                return true;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v;
                if (position == 0 || position == 1 || position == 12 || position == 25 || position == 34) {
                    // Header items - gray and bold style
                    tv.setTextColor(android.graphics.Color.GRAY);
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    tv.setTextColor(android.graphics.Color.BLACK);
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(adapter);

        actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                // Header positions are disabled - reset to first valid if selected
                if (pos == 0 || pos == 1 || pos == 12 || pos == 25 || pos == 34) {
                    actionSpinner.setSelection(0);
                    selectedAction = "";
                    return;
                }
                selectedAction = ACTIONS[pos];
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) { selectedAction = ""; }
        });
    }

    // ────────────────────────────────────────────────────────
    // SUBMIT ACTION
    // Writes the admin's decision back to Firestore
    // ────────────────────────────────────────────────────────

    private void submitAction() {
        if (selectedAction.isEmpty()) {
            Toast.makeText(getContext(),
                    "Please select an action", Toast.LENGTH_SHORT).show();
            return;
        }
        if (docId.isEmpty()) return;

        // ADD THIS LOG:
        Log.d(TAG, "submitAction - docId: " + docId + ", auth: " +
                (FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getEmail() : "NOT LOGGED IN"));

        submitBtn.setEnabled(false);
        submitBtn.setText(getString(R.string.processing));

        db.collection("reports").document(docId)
                .update(
                        "status", "Completed",
                        "adminAction", selectedAction,
                        "resolvedAt", com.google.firebase.Timestamp.now()
                )
                .addOnSuccessListener(v -> {
                    // Log the completion activity
                    activityLogger.logCompletion(ActivityLogger.TYPE_REPORT, cachedTitle, docId);
                    Toast.makeText(getContext(),
                            "Action submitted: " + selectedAction,
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    Toast.makeText(getContext(),
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}