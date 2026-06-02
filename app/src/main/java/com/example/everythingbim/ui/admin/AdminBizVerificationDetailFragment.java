package com.example.everythingbim.ui.admin;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import com.example.everythingbim.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import com.example.everythingbim.ActivityLogger;

public class AdminBizVerificationDetailFragment extends Fragment {

    private static final String TAG = "BizVerDetail";

    // ─── Bundle arg keys ─────────────────────
    public static final String ARG_DOC_ID = "doc_id";
    public static final String ARG_NUMBER = "number";
    public static final String ARG_STATUS = "status";
    public static final String ARG_DATE = "date";
    public static final String ARG_SUBMITTED_BY = "submittedBy";
    public static final String ARG_NAME = "name";
    public static final String ARG_PHONE = "phone";
    public static final String ARG_EMAIL = "email";
    public static final String ARG_ADDRESS = "address";
    public static final String ARG_DESCRIPTION = "description";
    public static final String ARG_TYPE = "type";
    public static final String ARG_RESOLVED_AT = "resolvedAt";
    public static final String ARG_RESOLVED_BY = "resolvedBy";
    public static final String ARG_REJECTION_REASON = "rejectionReason";

    // ─── State ───────────────────────────────
    private String docId = "";
    private FirebaseFirestore db;
    private FirebaseStorage   storage;
    private ActivityLogger activityLogger;

    // ─── Cached data from Bundle ─────────────
    private String cachedNumber = "";
    private String cachedStatus = "";
    private String cachedDate = "";
    private String cachedSubmittedBy = "";
    private String cachedName = "";
    private String cachedPhone = "";
    private String cachedEmail = "";
    private String cachedAddress = "";
    private String cachedDescription = "";
    private String cachedType = "";
    private String cachedResolvedAt = "";
    private String cachedResolvedBy = "";
    private String cachedRejectionReason = "";
    private boolean dataFromBundle = false;

    // ─── Views ───────────────────────────────
    private LinearLayout cardWrapper;
    private TextView numberTv, statusTv, dateTv, submittedByTv;
    private TextView nameTv, phoneTv, emailTv, addressTv, descriptionTv, typeTv;
    private LinearLayout imagesContainer, docsContainer;
    private LinearLayout actionButtons;
    private Button acceptBtn, rejectBtn;
    private LinearLayout resolutionContainer;
    private TextView resolutionDateTv, resolutionByTv, resolutionReasonTv;
    private LinearLayout resolutionReasonRow;

    // ────────────────────────────────────────────────────────
    // FACTORY — Bundle approach for instant display
    // ────────────────────────────────────────────────────────

    public static AdminBizVerificationDetailFragment newInstance(
            String docId, String number, String status, String date,
            String submittedBy, String name, String phone, String email,
            String address, String description, String type) {

        AdminBizVerificationDetailFragment f = new AdminBizVerificationDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOC_ID, docId);
        args.putString(ARG_NUMBER, number);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_DATE, date);
        args.putString(ARG_SUBMITTED_BY, submittedBy);
        args.putString(ARG_NAME, name);
        args.putString(ARG_PHONE, phone);
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_ADDRESS, address);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_TYPE, type);
        f.setArguments(args);
        return f;
    }

    // Legacy factory for backward compatibility
    public static AdminBizVerificationDetailFragment newInstance(String docId) {
        AdminBizVerificationDetailFragment f = new AdminBizVerificationDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOC_ID, docId);
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

        View view = inflater.inflate(R.layout.fragment_admin_biz_verification_detail,
                container, false);
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        activityLogger = new ActivityLogger(requireContext());

        // Extract Bundle data first
        if (getArguments() != null) {
            docId = getArguments().getString(ARG_DOC_ID, "");
            cachedNumber = getArguments().getString(ARG_NUMBER, "");
            cachedStatus = getArguments().getString(ARG_STATUS, "In Review");
            cachedDate = getArguments().getString(ARG_DATE, "");
            cachedSubmittedBy = getArguments().getString(ARG_SUBMITTED_BY, "");
            cachedName = getArguments().getString(ARG_NAME, "");
            cachedPhone = getArguments().getString(ARG_PHONE, "");
            cachedEmail = getArguments().getString(ARG_EMAIL, "");
            cachedAddress = getArguments().getString(ARG_ADDRESS, "");
            cachedDescription = getArguments().getString(ARG_DESCRIPTION, "");
            cachedType = getArguments().getString(ARG_TYPE, "");
            cachedResolvedAt = getArguments().getString(ARG_RESOLVED_AT, "");
            cachedResolvedBy = getArguments().getString(ARG_RESOLVED_BY, "");
            cachedRejectionReason = getArguments().getString(ARG_REJECTION_REASON, "");

            dataFromBundle = !cachedNumber.isEmpty();
        }

        // Bind views
        cardWrapper         = view.findViewById(R.id.biz_detail_card_wrapper);
        numberTv            = view.findViewById(R.id.biz_detail_number);
        statusTv            = view.findViewById(R.id.biz_detail_status);
        dateTv              = view.findViewById(R.id.biz_detail_date);
        submittedByTv       = view.findViewById(R.id.biz_detail_submitted_by);
        nameTv              = view.findViewById(R.id.biz_detail_name);
        phoneTv             = view.findViewById(R.id.biz_detail_phone);
        emailTv             = view.findViewById(R.id.biz_detail_email);
        addressTv           = view.findViewById(R.id.biz_detail_address);
        descriptionTv       = view.findViewById(R.id.biz_detail_description);
        typeTv              = view.findViewById(R.id.biz_detail_type);
        imagesContainer     = view.findViewById(R.id.biz_detail_images_container);
        docsContainer       = view.findViewById(R.id.biz_detail_docs_container);
        actionButtons       = view.findViewById(R.id.biz_detail_action_buttons);
        acceptBtn           = view.findViewById(R.id.biz_detail_accept_btn);
        rejectBtn           = view.findViewById(R.id.biz_detail_reject_btn);
        resolutionContainer = view.findViewById(R.id.biz_detail_resolution_container);
        resolutionDateTv    = view.findViewById(R.id.biz_detail_resolution_date);
        resolutionByTv      = view.findViewById(R.id.biz_detail_resolution_by);
        resolutionReasonTv  = view.findViewById(R.id.biz_detail_rejection_reason);
        resolutionReasonRow = view.findViewById(R.id.biz_detail_rejection_reason_row);

        view.findViewById(R.id.biz_detail_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        acceptBtn.setOnClickListener(v -> showAcceptDialog());
        rejectBtn.setOnClickListener(v -> showRejectDialog());

        // Display Bundle data immediately if available
        if (dataFromBundle) {
            displayCachedData();
        }

        // Always try to fetch fresh data from Firestore as fallback
        if (!docId.isEmpty()) {
            loadData();
            // Log activity - admin viewed this business request
            activityLogger.logView(ActivityLogger.TYPE_BUSINESS, cachedName, docId);
        }

        return view;
    }

    // ────────────────────────────────────────────────────────
    // DISPLAY DATA FROM BUNDLE (instant display)
    // ────────────────────────────────────────────────────────

    private void displayCachedData() {
        Log.d(TAG, "Displaying cached data from Bundle");
        numberTv.setText("Request " + cachedNumber);
        statusTv.setText("Status: " + cachedStatus);
        dateTv.setText("Submitted: " + cachedDate);
        submittedByTv.setText("Submitted By: " + cachedSubmittedBy);
        nameTv.setText(cachedName);
        phoneTv.setText(cachedPhone);
        emailTv.setText(cachedEmail);
        addressTv.setText(cachedAddress);
        descriptionTv.setText(cachedDescription);
        typeTv.setText(cachedType);

        applyStatusVisual(cachedStatus);
    }

    private void applyStatusVisual(String status) {
        if ("Completed".equals(status) || "Approved".equals(status)) {
            applyAcceptedState(cachedResolvedAt, cachedResolvedBy);
        } else if ("Rejected".equals(status)) {
            applyRejectedState(cachedResolvedAt, cachedResolvedBy, cachedRejectionReason);
        }
    }

    // ────────────────────────────────────────────────────────
    // LOAD DATA FROM FIRESTORE (fallback/refresh)
    // ────────────────────────────────────────────────────────

    private void loadData() {
        Log.d(TAG, "Loading data from Firestore for docId: " + docId);
        db.collection("businesses").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (!doc.exists()) {
                        Log.e(TAG, "Document does not exist: " + docId);
                        return;
                    }

                    Log.d(TAG, "Document found, updating fields");

                    long number = doc.contains("requestNumber")
                            ? doc.getLong("requestNumber") : 0;
                    numberTv.setText("Request #" + number);
                    cachedNumber = String.valueOf(number);

                    String status = doc.getString("verificationStatus");
                    if (status == null) status = "In Review";
                    statusTv.setText("Status: " + status);
                    cachedStatus = status;

                    Timestamp ts = doc.getTimestamp("createdAt");
                    if (ts != null) {
                        dateTv.setText("Submitted: " + fmt(ts));
                        cachedDate = fmt(ts);
                    }

                    String submittedBy = doc.getString("username");
                    submittedByTv.setText("Submitted By: BusinessUser "
                            + (submittedBy != null ? submittedBy : ""));
                    cachedSubmittedBy = submittedBy != null ? submittedBy : "";

                    String businessName = nvl(doc.getString("BusinessName"));
                    nameTv.setText(businessName);
                    cachedName = businessName;

                    cachedPhone = nvl(doc.getString("phone"));
                    phoneTv.setText(cachedPhone);

                    cachedEmail = nvl(doc.getString("email"));
                    emailTv.setText(cachedEmail);

                    cachedAddress = nvl(doc.getString("address"));
                    addressTv.setText(cachedAddress);

                    cachedDescription = nvl(doc.getString("description"));
                    descriptionTv.setText(cachedDescription);

                    cachedType = nvl(doc.getString("businessType"));
                    typeTv.setText(cachedType);

                    // Business images
                    List<String> imgUrls = (List<String>) doc.get("imageUrls");
                    if (imgUrls != null) for (String url : imgUrls) loadImage(url, imagesContainer);

                    // Business documents/certificates
                    List<String> docUrls = (List<String>) doc.get("fileUrls");
                    if (docUrls != null) for (String url : docUrls) loadImage(url, docsContainer);

                    // Restore resolved state
                    if ("Completed".equals(status) || "Approved".equals(status)) {
                        applyAcceptedState(fmt(doc.getTimestamp("resolvedAt")),
                                nvl(doc.getString("resolvedBy")));
                    } else if ("Rejected".equals(status)) {
                        applyRejectedState(fmt(doc.getTimestamp("resolvedAt")),
                                nvl(doc.getString("resolvedBy")),
                                nvl(doc.getString("rejectionReason")));
                    }

                    doc.getReference().update("read", true);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.e(TAG, "Failed to load document: " + e.getMessage(), e);
                    showToast("Failed to load request details");
                });
    }

    // ────────────────────────────────────────────────────────
    // ACCEPT DIALOG
    // ────────────────────────────────────────────────────────

    private void showAcceptDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_accept);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.dialog_accept_yes).setOnClickListener(v -> {
            dialog.dismiss();
            confirmAccept();
        });
        dialog.findViewById(R.id.dialog_accept_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ────────────────────────────────────────────────────────
    // REJECT DIALOG
    // ────────────────────────────────────────────────────────

    private void showRejectDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_reject);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText reasonEt = dialog.findViewById(R.id.dialog_reject_reason_et);

        dialog.findViewById(R.id.dialog_reject_yes).setOnClickListener(v -> {
            String reason = reasonEt.getText().toString().trim();
            if (reason.isEmpty()) { reasonEt.setError("Please enter a reason"); return; }
            dialog.dismiss();
            confirmReject(reason);
        });
        dialog.findViewById(R.id.dialog_reject_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ────────────────────────────────────────────────────────
    // CONFIRM ACCEPT
    // ────────────────────────────────────────────────────────

    private void confirmAccept() {
        if (docId.isEmpty()) return;
        String adminId = getAdminId();
        String today   = todayStr();

        db.collection("businesses").document(docId)
                .update("verificationStatus", "Completed",
                        "verified",           true,
                        "resolvedAt",         Timestamp.now(),
                        "resolvedBy",         adminId)
                .addOnSuccessListener(v -> {
                    if (!isUiActive()) return;
                    // Log approval activity
                    activityLogger.logApproval(ActivityLogger.TYPE_BUSINESS, cachedName, docId);
                    showToast("Request Accepted");
                    applyAcceptedState(today, "Administrator");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Failed: " + e.getMessage());
                });
    }

    // ────────────────────────────────────────────────────────
    // CONFIRM REJECT
    // ────────────────────────────────────────────────────────

    private void confirmReject(String reason) {
        if (docId.isEmpty()) return;
        String adminId = getAdminId();
        String today   = todayStr();

        db.collection("businesses").document(docId)
                .update("verificationStatus", "Rejected",
                        "verified",           false,
                        "resolvedAt",         Timestamp.now(),
                        "resolvedBy",         adminId,
                        "rejectionReason",    reason)
                .addOnSuccessListener(v -> {
                    if (!isUiActive()) return;
                    // Log rejection activity
                    activityLogger.logRejection(ActivityLogger.TYPE_BUSINESS, cachedName, docId);
                    showToast("Request Rejected");
                    applyRejectedState(today, "Administrator", reason);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Failed: " + e.getMessage());
                });
    }

    // ────────────────────────────────────────────────────────
    // APPLY ACCEPTED STATE
    // Sets green background on the WRAPPER (not CardView) — works reliably
    // ────────────────────────────────────────────────────────

    private void applyAcceptedState(String date, String by) {
        cardWrapper.setBackgroundResource(R.drawable.bg_card_accepted);

        statusTv.setText("Status: Accepted");
        statusTv.setTextColor(android.graphics.Color.parseColor("#28965a"));

        actionButtons.setVisibility(View.GONE);

        resolutionContainer.setVisibility(View.VISIBLE);
        resolutionReasonRow.setVisibility(View.GONE);
        resolutionDateTv.setText("Accepted: " + date);
        resolutionByTv.setText("Accepted By: " + by);
    }

    // ────────────────────────────────────────────────────────
    // APPLY REJECTED STATE
    // Sets red background on the WRAPPER — works reliably
    // ────────────────────────────────────────────────────────

    private void applyRejectedState(String date, String by, String reason) {
        cardWrapper.setBackgroundResource(R.drawable.bg_card_rejected);

        statusTv.setText("Status: Rejected");
        statusTv.setTextColor(android.graphics.Color.parseColor("#c0392b"));

        actionButtons.setVisibility(View.GONE);

        resolutionContainer.setVisibility(View.VISIBLE);
        resolutionReasonRow.setVisibility(View.VISIBLE);
        resolutionDateTv.setText("Rejected: " + date);
        resolutionByTv.setText("Rejected By: " + by);
        resolutionReasonTv.setText("Reason For Rejection: " + reason);
    }

    // ────────────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────────────

    private void loadImage(String url, LinearLayout container) {
        try {
            storage.getReferenceFromUrl(url)
                    .getBytes(2 * 1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        if (!isUiActive()) return;
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        ImageView iv = new ImageView(requireContext());
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(80, 60);
                        p.setMarginEnd(8);
                        iv.setLayoutParams(p);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setImageBitmap(bmp);
                        container.addView(iv);
                    });
        } catch (Exception ignored) {}
    }

    private boolean isUiActive() {
        return isAdded() && getView() != null;
    }

    private void showToast(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String fmt(Timestamp ts) {
        return ts != null
                ? new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(ts.toDate())
                : todayStr();
    }

    private String todayStr() {
        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(new java.util.Date());
    }

    private String getAdminId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "Admin";
    }
}
