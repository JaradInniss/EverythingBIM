package com.example.everythingbim.ui.admin;

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
import android.widget.EditText;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.example.everythingbim.ActivityLogger;
import com.example.everythingbim.ui.home.UserNotificationHelper;

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
    private TextView contentHeaderTv;
    private TextView captionLabelTv;
    private TextView imageLabelTv;
    private TextView userTypeTv;
    private TextView contactTv;
    private TextView descriptionTv;
    private ImageView postImage;
    private Spinner actionSpinner;
    private Button submitBtn;
    private EditText actionReasonEt;
    private View postDateLayout;
    private View accountInfoLayout;
    private View descriptionLayout;

    // ─── State ───────────────────────────────
    private String selectedAction = "";
    private String docId          = "";
    private String actionReason   = "";

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
    private static final String ACTION_CASE_NOTES_MOD = "Add Case Notes";

    // MINOR: Low-severity/automation-friendly
    private static final String ACTION_DISMISS_NO_ACTION = "Dismiss Report (No Action Needed)";
    private static final String ACTION_REMOVE_SPAM = "Remove Content (Spam/Fake Engagement)";
    private static final String ACTION_SOFT_MUTE = "Soft-Mute in Recommendations";
    private static final String ACTION_AUTO_WARN = "Issue Automated Warning";
    private static final String ACTION_MARK_BOT = "Mark as Bot/Satire Account";
    private static final String ACTION_MERGE_DUPLICATES = "Merge Duplicate Reports";

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
            ACTION_CASE_NOTES_MOD,
            // MINOR
            "━━ MINOR ━━",
            ACTION_DISMISS_NO_ACTION,
            ACTION_REMOVE_SPAM,
            ACTION_SOFT_MUTE,
            ACTION_AUTO_WARN,
            ACTION_MARK_BOT,
            ACTION_MERGE_DUPLICATES,
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
        activityLogger = new ActivityLogger(requireContext());

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
        actionReasonEt = view.findViewById(R.id.report_detail_action_reason_et);

        // New views for content switching
        contentHeaderTv = view.findViewById(R.id.report_detail_content_header);
        captionLabelTv = view.findViewById(R.id.report_detail_caption_label);
        imageLabelTv = view.findViewById(R.id.report_detail_image_label);
        postDateLayout = view.findViewById(R.id.report_detail_post_date_layout);
        accountInfoLayout = view.findViewById(R.id.report_detail_account_info_layout);
        userTypeTv = view.findViewById(R.id.report_detail_user_type);
        contactTv = view.findViewById(R.id.report_detail_contact);
        descriptionTv = view.findViewById(R.id.report_detail_description);
        descriptionLayout = view.findViewById(R.id.report_detail_description_layout);

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
        // Always fetch from Firestore to get complete/accurate data including postDate
        if (!docId.isEmpty()) {
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

        descriptionTv.setText("");
        descriptionLayout.setVisibility(View.GONE);
    }

    private void fetchReportFromFirestore() {
        db.collection("reports").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String id = doc.getId().substring(0, 3).toUpperCase();
                        String number = doc.contains("number")
                                ? "#" + doc.getLong("number")
                                : "#" + id;

                        numberTv.setText("Report " + number);

                        // Derive severity from reason (ViewPost doesn't save severity explicitly)
                        String reason = doc.getString("reason");
                        String severity = deriveSeverity(reason);
                        severityBadge.setText(severity.toUpperCase());
                        applySeverityBadgeColor(severity);
                        statusTv.setText(doc.getString("status") != null
                                ? doc.getString("status") : "In Review");
                        applyStatusColor(doc.getString("status"));

                        com.google.firebase.Timestamp ts = doc.getTimestamp("submittedAt");
                        String date = ts != null
                                ? new java.text.SimpleDateFormat("yyyy/MM/dd",
                                java.util.Locale.getDefault()).format(ts.toDate())
                                : "";
                        dateTv.setText("Submitted: " + date);

                        // Build title from reportType and reason
                        String reportType = doc.getString("reportType");
                        boolean isPostReport = "Post".equalsIgnoreCase(reportType);
                        String title = (reportType != null ? reportType : "Report") + " - " + (reason != null ? reason : "Unknown");
                        typeTv.setText(reportType != null ? reportType : "Post");
                        issueTv.setText(title);
                        userTv.setText(doc.getString("reportedUser") != null
                                ? doc.getString("reportedUser") : "");

                        // Description (optional field)
                        String description = doc.getString("description");
                        if (description != null && !description.isEmpty()) {
                            descriptionTv.setText(description);
                            descriptionLayout.setVisibility(View.VISIBLE);
                        } else {
                            descriptionLayout.setVisibility(View.GONE);
                        }

                        // Switch UI based on report type
                        if (isPostReport) {
                            // Show Post-specific fields
                            contentHeaderTv.setText("Post Content:");
                            postDateLayout.setVisibility(View.VISIBLE);
                            captionLabelTv.setVisibility(View.VISIBLE);
                            captionTv.setVisibility(View.VISIBLE);
                            imageLabelTv.setVisibility(View.VISIBLE);
                            postImage.setVisibility(View.VISIBLE);
                            accountInfoLayout.setVisibility(View.GONE);

                            // Populate post fields
                            captionTv.setText(doc.getString("postCaption") != null
                                    ? doc.getString("postCaption") : "");

                            // Load post date if available (stored as long timestamp millis)
                            Long postDateMillis = doc.getLong("postDate");
                            if (postDateMillis != null && postDateMillis > 0) {
                                String postDateStr = new java.text.SimpleDateFormat("yyyy/MM/dd",
                                        java.util.Locale.getDefault()).format(new Date(postDateMillis));
                                postDateTv.setText(postDateStr);
                            } else {
                                postDateTv.setText("N/A");
                            }

                            String imageUrl = doc.getString("postImageUrl");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                loadImageFromStorage(imageUrl);
                            } else {
                                postImage.setImageDrawable(null);
                            }
                        } else {
                            // Show Account-specific fields
                            contentHeaderTv.setText("Account Info:");
                            postDateLayout.setVisibility(View.GONE);
                            captionLabelTv.setVisibility(View.GONE);
                            captionTv.setVisibility(View.GONE);
                            imageLabelTv.setVisibility(View.GONE);
                            postImage.setVisibility(View.GONE);
                            accountInfoLayout.setVisibility(View.VISIBLE);

                            // Populate account fields
                            String userType = doc.getString("userType");
                            userTypeTv.setText(userType != null ? capitalizeFirst(userType) : "General");

                            String contactInfo = doc.getString("contactInfo");
                            contactTv.setText(contactInfo != null ? contactInfo : "Not available");
                        }
                    } else {
                        issueTv.setText("Report not found");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    issueTv.setText("Error loading report");
                });
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
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
                        if (!isUiActive()) return;
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
                // Disable header items: Select Action, -- MAJOR --, -- MODERATE --, -- MINOR --, -- ACTIONS --
                if (position == 0) return false; // "Select Action"
                if (position == 1) return false;  // "━━ MAJOR ━━"
                if (position == 10) return false; // "━━ MODERATE ━━"
                if (position == 21) return false; // "━━ MINOR ━━"
                if (position == 28) return false; // "━━ ACTIONS ━━"
                return true;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) v;
                if (position == 0 || position == 1 || position == 10 || position == 21 || position == 28) {
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
                if (pos == 0 || pos == 1 || pos == 10 || pos == 21 || pos == 28) {
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
            showToast("Please select an action");
            return;
        }
        if (docId.isEmpty()) return;

        // Capture the action reason
        actionReason = actionReasonEt != null ? actionReasonEt.getText().toString().trim() : "";

        submitBtn.setEnabled(false);
        submitBtn.setText(getString(R.string.processing));

        // Handle special actions that require additional processing
        if (ACTION_REMOVE_IMMEDIATE.equals(selectedAction)) {
            handleRemoveContentImmediately();
            return;
        } else if (ACTION_WATCHLIST.equals(selectedAction)) {
            handleFlagForWatchList();
            return;
        } else if (ACTION_WARN_1ST.equals(selectedAction)) {
            handleIssueWarning();
            return;
        } else if (ACTION_REQUEST_EDIT.equals(selectedAction)) {
            handleRequestEditRemoval();
            return;
        } else if (ACTION_DISMISS_NO_ACTION.equals(selectedAction)) {
            handleDismissReport();
            return;
        } else if (ACTION_REMOVE_SPAM.equals(selectedAction)) {
            handleRemoveSpamContent();
            return;
        }

        // Standard action - just update report status
        db.collection("reports").document(docId)
                .update(
                        "status", "Completed",
                        "adminAction", selectedAction,
                        "actionReason", actionReason,
                        "resolvedAt", com.google.firebase.Timestamp.now()
                )
                .addOnSuccessListener(v -> {
                    if (!isUiActive()) return;
                    activityLogger.logCompletion(ActivityLogger.TYPE_REPORT, cachedTitle, docId);
                    showToast("Action submitted: " + selectedAction);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed: " + e.getMessage());
                });
    }

    // Handle Remove Content Immediately action - deletes post and notifies both parties
    private void handleRemoveContentImmediately() {
        // First fetch the report to get post details
        db.collection("reports").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String reportType = doc.getString("reportType");
                        // postId is stored as a String (Firestore ID) but might be saved differently
                        String postId = doc.getString("postId");
                        // reportedUserUid is the Firebase Auth UID of the post creator
                        String authorUid = doc.getString("reportedUserUid");
                        String reporterId = doc.getString("reporterId");
                        String reporterUid = doc.getString("reporterUid");  // Firebase Auth UID for reporter

                        // For post reports, delete the post from Firestore
                        if ("Post".equals(reportType) && postId != null && !postId.isEmpty()) {
                            db.collection("posts").document(postId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Post deleted: " + postId);
                                        // Notify post creator
                                        notifyContentRemoved(authorUid, reporterUid, reporterId, postId, true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to delete post", e);
                                        // Still mark report as complete and notify
                                        notifyContentRemoved(authorUid, reporterUid, reporterId, postId, false);
                                    });
                        } else {
                            // For account reports, just notify
                            notifyContentRemoved(authorUid, reporterUid, reporterId, null, false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed to process action: " + e.getMessage());
                });
    }

    // Notify both the content creator and the reporter about content removal
    private void notifyContentRemoved(String authorUid, String reporterUid, String reporterId, String postId, boolean postDeleted) {
        String notificationType = "content_removed";

        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        // Notify the content creator - authorUid is the Firebase Auth UID (reportedUserUid)
        if (authorUid != null && !authorUid.isEmpty() && !"anonymous".equals(authorUid)) {
            final String creatorMessage;
            if (postDeleted) {
                creatorMessage = "Your post has been removed for violating community guidelines." + reasonSuffix;
            } else {
                creatorMessage = "Your account has been flagged for review. Please contact support if you believe this is an error." + reasonSuffix;
            }

            // Use authorUid directly as Firebase Auth UID (reportedUserUid)
            UserNotificationHelper.createNotification(
                    db, authorUid, notificationType,
                    "Content Removed",
                    creatorMessage,
                    postId,
                    "posts"
            );
        }
        // Also notify reporter regardless of outcome
        notifyReporter(reporterUid, reporterId, postId, postDeleted);
    }

    // Notify the reporter that their report was actioned
    private void notifyReporter(String reporterUid, String reporterId, String postId, boolean wasRemoved) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        String message;
        if (wasRemoved) {
            message = "Thank you for your report. The content you reported has been removed for violating community guidelines." + reasonSuffix;
        } else {
            message = "Thank you for your report. We've reviewed the content and taken appropriate action." + reasonSuffix;
        }

        // Use reporterUid directly if available (new reports), otherwise use reporterId lookup (legacy)
        if (reporterUid != null && !reporterUid.isEmpty() && !"anonymous".equals(reporterUid)) {
            UserNotificationHelper.createNotification(
                    db, reporterUid, "report_actioned",
                    "Report Update",
                    message,
                    postId,
                    "posts"
            );
            completeReportAction(ACTION_REMOVE_IMMEDIATE);
        } else {
            // Fallback: look up reporter by userId field using reporterId (local Room user ID)
            if (reporterId == null || reporterId.isEmpty()) {
                completeReportAction(ACTION_REMOVE_IMMEDIATE);
                return;
            }
            try {
                Long reporterIdLong = Long.parseLong(reporterId);
                db.collection("users").whereEqualTo("userId", reporterIdLong).get()
                        .addOnSuccessListener(userDocs -> {
                            if (!userDocs.isEmpty()) {
                                String uid = userDocs.getDocuments().get(0).getId();
                                UserNotificationHelper.createNotification(
                                        db, uid, "report_actioned",
                                        "Report Update",
                                        message,
                                        postId,
                                        "posts"
                                );
                            }
                            completeReportAction(ACTION_REMOVE_IMMEDIATE);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to notify reporter", e);
                            completeReportAction(ACTION_REMOVE_IMMEDIATE);
                        });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reporterId format: " + reporterId, e);
                completeReportAction(ACTION_REMOVE_IMMEDIATE);
            }
        }
    }

    // Handle Flag for Watch-List Monitoring action
    private void handleFlagForWatchList() {
        // First fetch the report to get account details
        db.collection("reports").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String reportType = doc.getString("reportType");
                        // accountId and reportedUserId are stored as numbers in Firestore
                        // Use get("fieldName") and convert to handle both String and Number types
                        Object accountIdObj = doc.get("accountId");
                        Object reportedUserIdObj = doc.get("reportedUserId");
                        String reporterId = doc.getString("reporterId");
                        String reporterUid = doc.getString("reporterUid");  // Firebase Auth UID for reporter
                        String userType = doc.getString("userType");
                        // reportedUserUid is the Firebase Auth UID of the account being reported
                        String reportedUserUid = doc.getString("reportedUserUid");

                        // Convert accountId - handle both String and Number
                        String accountId = accountIdObj != null ? String.valueOf(accountIdObj) : null;
                        // Convert reportedUserId (this is the authorId for the reported user)
                        String reportedUserId = reportedUserIdObj != null ? String.valueOf(reportedUserIdObj) : null;

                        // Add to watch list
                        addToWatchList(accountId, reportedUserId, userType, docId);

                        // Notify both parties - use Firebase Auth UIDs directly
                        notifyWatchListFlagged(reportedUserUid, reporterUid, reporterId, accountId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed to process action: " + e.getMessage());
                });
    }

    // Handle Issue Formal Warning (1st) action - sends warning notification to creator and notifies reporter
    private void handleIssueWarning() {
        db.collection("reports").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String reportType = doc.getString("reportType");
                        String postId = doc.getString("postId");
                        String authorUid = doc.getString("reportedUserUid");
                        String reporterId = doc.getString("reporterId");
                        String reporterUid = doc.getString("reporterUid");

                        // Notify the content creator about the warning
                        notifyFormalWarning(authorUid, reporterUid, reporterId, postId, reportType);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed to process action: " + e.getMessage());
                });
    }

    // Notify content creator about formal warning
    private void notifyFormalWarning(String authorUid, String reporterUid, String reporterId, String postId, String reportType) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        if (authorUid != null && !authorUid.isEmpty() && !"anonymous".equals(authorUid)) {
            String message = "You have received a formal warning for violating our community guidelines. Please review our policies to avoid further action." + reasonSuffix;
            UserNotificationHelper.createNotification(
                    db, authorUid, "formal_warning",
                    "Formal Warning Issued",
                    message,
                    postId,
                    "posts"
            );
        }
        notifyReporterWarning(reporterUid, reporterId, postId);
    }

    // Notify reporter that their report resulted in a warning
    private void notifyReporterWarning(String reporterUid, String reporterId, String postId) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        String message = "Thank you for your report. The account has been issued a formal warning." + reasonSuffix;

        if (reporterUid != null && !reporterUid.isEmpty() && !"anonymous".equals(reporterUid)) {
            UserNotificationHelper.createNotification(
                    db, reporterUid, "report_actioned",
                    "Report Update",
                    message,
                    postId,
                    "posts"
            );
            completeReportAction(ACTION_WARN_1ST);
        } else {
            // Fallback: look up reporter by userId field using reporterId (local Room user ID)
            if (reporterId == null || reporterId.isEmpty()) {
                completeReportAction(ACTION_WARN_1ST);
                return;
            }
            try {
                Long reporterIdLong = Long.parseLong(reporterId);
                db.collection("users").whereEqualTo("userId", reporterIdLong).get()
                        .addOnSuccessListener(userDocs -> {
                            if (!userDocs.isEmpty()) {
                                String uid = userDocs.getDocuments().get(0).getId();
                                UserNotificationHelper.createNotification(
                                        db, uid, "report_actioned",
                                        "Report Update",
                                        message,
                                        postId,
                                        "posts"
                                );
                            }
                            completeReportAction(ACTION_WARN_1ST);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to notify reporter for warning", e);
                            completeReportAction(ACTION_WARN_1ST);
                        });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reporterId format: " + reporterId, e);
                completeReportAction(ACTION_WARN_1ST);
            }
        }
    }

    // Handle Request Content Edit/Removal action - sends edit/removal request to creator and notifies reporter
    private void handleRequestEditRemoval() {
        db.collection("reports").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String reportType = doc.getString("reportType");
                        String postId = doc.getString("postId");
                        String authorUid = doc.getString("reportedUserUid");
                        String reporterId = doc.getString("reporterId");
                        String reporterUid = doc.getString("reporterUid");

                        // Notify the content creator about the edit/removal request
                        notifyEditRemovalRequest(authorUid, reporterUid, reporterId, postId, reportType);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed to process action: " + e.getMessage());
                });
    }

    // Notify content creator about edit/removal request
    private void notifyEditRemovalRequest(String authorUid, String reporterUid, String reporterId, String postId, String reportType) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        if (authorUid != null && !authorUid.isEmpty() && !"anonymous".equals(authorUid)) {
            String message = "Your content has been flagged for review. Please edit or remove the content that violates our community guidelines." + reasonSuffix;
            UserNotificationHelper.createNotification(
                    db, authorUid, "content_edit_request",
                    "Action Required: Edit or Remove Content",
                    message,
                    postId,
                    "posts"
            );
        }
        notifyReporterEditRequest(reporterUid, reporterId, postId);
    }

    // Notify reporter that their report resulted in an edit/removal request
    private void notifyReporterEditRequest(String reporterUid, String reporterId, String postId) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        String message = "Thank you for your report. The content owner has been requested to edit or remove the content." + reasonSuffix;

        if (reporterUid != null && !reporterUid.isEmpty() && !"anonymous".equals(reporterUid)) {
            UserNotificationHelper.createNotification(
                    db, reporterUid, "report_actioned",
                    "Report Update",
                    message,
                    postId,
                    "posts"
            );
            completeReportAction(ACTION_REQUEST_EDIT);
        } else {
            // Fallback: look up reporter by userId field using reporterId (local Room user ID)
            if (reporterId == null || reporterId.isEmpty()) {
                completeReportAction(ACTION_REQUEST_EDIT);
                return;
            }
            try {
                Long reporterIdLong = Long.parseLong(reporterId);
                db.collection("users").whereEqualTo("userId", reporterIdLong).get()
                        .addOnSuccessListener(userDocs -> {
                            if (!userDocs.isEmpty()) {
                                String uid = userDocs.getDocuments().get(0).getId();
                                UserNotificationHelper.createNotification(
                                        db, uid, "report_actioned",
                                        "Report Update",
                                        message,
                                        postId,
                                        "posts"
                                );
                            }
                            completeReportAction(ACTION_REQUEST_EDIT);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to notify reporter for edit request", e);
                            completeReportAction(ACTION_REQUEST_EDIT);
                        });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reporterId format: " + reporterId, e);
                completeReportAction(ACTION_REQUEST_EDIT);
            }
        }
    }

    // Handle Dismiss Report (No Action Needed) - notifies reporter that report was dismissed
    private void handleDismissReport() {
        db.collection("reports").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String reporterUid = doc.getString("reporterUid");
                        String reporterId = doc.getString("reporterId");
                        String postId = doc.getString("postId");

                        // Notify reporter about dismissal
                        notifyReportDismissed(reporterUid, reporterId, postId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed to process action: " + e.getMessage());
                });
    }

    // Notify reporter that their report was dismissed
    private void notifyReportDismissed(String reporterUid, String reporterId, String postId) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        String message = "Thank you for your report. After review, we found that no action is needed at this time." + reasonSuffix;

        if (reporterUid != null && !reporterUid.isEmpty() && !"anonymous".equals(reporterUid)) {
            UserNotificationHelper.createNotification(
                    db, reporterUid, "report_actioned",
                    "Report Update",
                    message,
                    postId,
                    "posts"
            );
            completeReportAction(ACTION_DISMISS_NO_ACTION);
        } else {
            // Fallback: look up reporter by userId field using reporterId (local Room user ID)
            if (reporterId == null || reporterId.isEmpty()) {
                completeReportAction(ACTION_DISMISS_NO_ACTION);
                return;
            }
            try {
                Long reporterIdLong = Long.parseLong(reporterId);
                db.collection("users").whereEqualTo("userId", reporterIdLong).get()
                        .addOnSuccessListener(userDocs -> {
                            if (!userDocs.isEmpty()) {
                                String uid = userDocs.getDocuments().get(0).getId();
                                UserNotificationHelper.createNotification(
                                        db, uid, "report_actioned",
                                        "Report Update",
                                        message,
                                        postId,
                                        "posts"
                                );
                            }
                            completeReportAction(ACTION_DISMISS_NO_ACTION);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to notify reporter for dismissal", e);
                            completeReportAction(ACTION_DISMISS_NO_ACTION);
                        });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reporterId format: " + reporterId, e);
                completeReportAction(ACTION_DISMISS_NO_ACTION);
            }
        }
    }

    // Handle Remove Content (Spam/Fake Engagement) - deletes post and notifies both parties
    private void handleRemoveSpamContent() {
        db.collection("reports").document(docId).get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (doc.exists()) {
                        String reportType = doc.getString("reportType");
                        String postId = doc.getString("postId");
                        String authorUid = doc.getString("reportedUserUid");
                        String reporterId = doc.getString("reporterId");
                        String reporterUid = doc.getString("reporterUid");

                        // For post reports, delete the post from Firestore
                        if ("Post".equals(reportType) && postId != null && !postId.isEmpty()) {
                            db.collection("posts").document(postId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Spam post deleted: " + postId);
                                        notifySpamContentRemoved(authorUid, reporterUid, reporterId, postId, true);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to delete spam post", e);
                                        notifySpamContentRemoved(authorUid, reporterUid, reporterId, postId, false);
                                    });
                        } else {
                            // For account reports, just notify
                            notifySpamContentRemoved(authorUid, reporterUid, reporterId, null, false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed to process action: " + e.getMessage());
                });
    }

    // Notify both the content creator and the reporter about spam content removal
    private void notifySpamContentRemoved(String authorUid, String reporterUid, String reporterId, String postId, boolean postDeleted) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        // Notify the content creator
        if (authorUid != null && !authorUid.isEmpty() && !"anonymous".equals(authorUid)) {
            final String creatorMessage;
            if (postDeleted) {
                creatorMessage = "Your post has been removed for violating our spam and fake engagement policies." + reasonSuffix;
            } else {
                creatorMessage = "Your account has been flagged for spam/fake engagement. Please review our policies." + reasonSuffix;
            }

            UserNotificationHelper.createNotification(
                    db, authorUid, "spam_content_removed",
                    "Content Removed",
                    creatorMessage,
                    postId,
                    "posts"
            );
        }
        notifyReporterSpamRemoval(reporterUid, reporterId, postId, postDeleted);
    }

    // Notify the reporter that their report resulted in spam content removal
    private void notifyReporterSpamRemoval(String reporterUid, String reporterId, String postId, boolean wasRemoved) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        String message;
        if (wasRemoved) {
            message = "Thank you for your report. The spam content you reported has been removed." + reasonSuffix;
        } else {
            message = "Thank you for your report. We've reviewed the content and taken appropriate action." + reasonSuffix;
        }

        if (reporterUid != null && !reporterUid.isEmpty() && !"anonymous".equals(reporterUid)) {
            UserNotificationHelper.createNotification(
                    db, reporterUid, "report_actioned",
                    "Report Update",
                    message,
                    postId,
                    "posts"
            );
            completeReportAction(ACTION_REMOVE_SPAM);
        } else {
            // Fallback: look up reporter by userId field using reporterId (local Room user ID)
            if (reporterId == null || reporterId.isEmpty()) {
                completeReportAction(ACTION_REMOVE_SPAM);
                return;
            }
            try {
                Long reporterIdLong = Long.parseLong(reporterId);
                db.collection("users").whereEqualTo("userId", reporterIdLong).get()
                        .addOnSuccessListener(userDocs -> {
                            if (!userDocs.isEmpty()) {
                                String uid = userDocs.getDocuments().get(0).getId();
                                UserNotificationHelper.createNotification(
                                        db, uid, "report_actioned",
                                        "Report Update",
                                        message,
                                        postId,
                                        "posts"
                                );
                            }
                            completeReportAction(ACTION_REMOVE_SPAM);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to notify reporter for spam removal", e);
                            completeReportAction(ACTION_REMOVE_SPAM);
                        });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reporterId format: " + reporterId, e);
                completeReportAction(ACTION_REMOVE_SPAM);
            }
        }
    }

    // Add account to watch-list collection
    private void addToWatchList(String accountId, String userId, String userType, String relatedReportId) {
        Map<String, Object> watchListEntry = new HashMap<>();
        watchListEntry.put("userId", userId);  // This is authorId (long)
        watchListEntry.put("accountId", accountId);
        watchListEntry.put("userType", userType != null ? userType : "general");
        watchListEntry.put("relatedReportId", relatedReportId);
        watchListEntry.put("addedAt", com.google.firebase.Timestamp.now());
        watchListEntry.put("addedBy", FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "admin");

        db.collection("watch_list").add(watchListEntry)
                .addOnSuccessListener(ref -> Log.d(TAG, "Added to watch list: " + ref.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add to watch list", e));
    }

    // Notify both the account owner and the reporter about watch-list flagging
    private void notifyWatchListFlagged(String authorUid, String reporterUid, String reporterId, String accountId) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        // Notify the account owner - authorUid is the Firebase Auth UID (reportedUserUid)
        if (authorUid != null && !authorUid.isEmpty() && !"anonymous".equals(authorUid)) {
            UserNotificationHelper.createNotification(
                    db, authorUid, "account_watchlisted",
                    "Account Under Review",
                    "Your account has been flagged for monitoring. Please review our community guidelines." + reasonSuffix,
                    accountId,
                    "users"
            );
        }
        notifyReporterWatchList(reporterUid, reporterId, accountId);
    }

    // Notify the reporter that their report resulted in watch-list flagging
    private void notifyReporterWatchList(String reporterUid, String reporterId, String accountId) {
        // Build reason suffix if provided
        String reasonSuffix = "";
        if (actionReason != null && !actionReason.isEmpty()) {
            reasonSuffix = " Reason: " + actionReason;
        }

        String message = "Thank you for your report. The account you reported has been flagged for monitoring." + reasonSuffix;

        // Use reporterUid directly if available (new reports), otherwise use reporterId lookup (legacy)
        if (reporterUid != null && !reporterUid.isEmpty() && !"anonymous".equals(reporterUid)) {
            UserNotificationHelper.createNotification(
                    db, reporterUid, "report_actioned",
                    "Report Update",
                    message,
                    accountId,
                    "users"
            );
            completeReportAction(ACTION_WATCHLIST);
        } else {
            // Fallback: look up reporter by userId field using reporterId (local Room user ID)
            if (reporterId == null || reporterId.isEmpty()) {
                completeReportAction(ACTION_WATCHLIST);
                return;
            }
            try {
                Long reporterIdLong = Long.parseLong(reporterId);
                db.collection("users").whereEqualTo("userId", reporterIdLong).get()
                        .addOnSuccessListener(userDocs -> {
                            if (!userDocs.isEmpty()) {
                                String uid = userDocs.getDocuments().get(0).getId();
                                UserNotificationHelper.createNotification(
                                        db, uid, "report_actioned",
                                        "Report Update",
                                        message,
                                        accountId,
                                        "users"
                                );
                            }
                            completeReportAction(ACTION_WATCHLIST);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to notify reporter for watch list", e);
                            completeReportAction(ACTION_WATCHLIST);
                        });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid reporterId format: " + reporterId, e);
                completeReportAction(ACTION_WATCHLIST);
            }
        }
    }

    // Complete the report action and go back
    private void completeReportAction(String action) {
        db.collection("reports").document(docId)
                .update(
                        "status", "Completed",
                        "adminAction", action,
                        "actionReason", actionReason,
                        "resolvedAt", com.google.firebase.Timestamp.now()
                )
                .addOnSuccessListener(v -> {
                    if (!isUiActive()) return;
                    activityLogger.logCompletion(ActivityLogger.TYPE_REPORT, cachedTitle, docId);
                    showToast("Action completed: " + action);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isUiActive()) return;
                    submitBtn.setEnabled(true);
                    submitBtn.setText("SUBMIT");
                    showToast("Failed: " + e.getMessage());
                });
    }

    private boolean isUiActive() {
        return isAdded() && getView() != null;
    }

    private void showToast(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}