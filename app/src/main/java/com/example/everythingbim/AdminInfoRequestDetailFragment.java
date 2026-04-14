package com.example.everythingbim;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminInfoRequestDetailFragment extends Fragment {

    public static final String ARG_DOC_ID = "doc_id";

    private String docId = "";
    private FirebaseFirestore db;
    private FirebaseStorage   storage;

    // ─── Views ───────────────────────────────
    private LinearLayout cardWrapper;
    private TextView numberTv, statusTv, dateTv, submittedByTv;
    private TextView locationNameTv, coordinatesTv, descriptionTv, placeTypeTv;
    private LinearLayout imagesContainer, actionButtons;
    private Button acceptBtn, rejectBtn;
    private LinearLayout resolutionContainer;
    private TextView resolutionDateTv, resolutionByTv, resolutionReasonTv;
    private LinearLayout resolutionReasonRow;

    // ────────────────────────────────────────────────────────
    // FACTORY
    // ────────────────────────────────────────────────────────

    public static AdminInfoRequestDetailFragment newInstance(String docId) {
        AdminInfoRequestDetailFragment f = new AdminInfoRequestDetailFragment();
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

        View view = inflater.inflate(R.layout.fragment_admin_info_request_detail, container, false);
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (getArguments() != null) docId = getArguments().getString(ARG_DOC_ID, "");

        // Bind views
        cardWrapper         = view.findViewById(R.id.info_detail_card_wrapper);
        numberTv            = view.findViewById(R.id.info_detail_number);
        statusTv            = view.findViewById(R.id.info_detail_status);
        dateTv              = view.findViewById(R.id.info_detail_date);
        submittedByTv       = view.findViewById(R.id.info_detail_submitted_by);
        locationNameTv      = view.findViewById(R.id.info_detail_location_name);
        coordinatesTv       = view.findViewById(R.id.info_detail_coordinates);
        descriptionTv       = view.findViewById(R.id.info_detail_description);
        placeTypeTv         = view.findViewById(R.id.info_detail_place_type);
        imagesContainer     = view.findViewById(R.id.info_detail_images_container);
        actionButtons       = view.findViewById(R.id.info_detail_action_buttons);
        acceptBtn           = view.findViewById(R.id.info_detail_accept_btn);
        rejectBtn           = view.findViewById(R.id.info_detail_reject_btn);
        resolutionContainer = view.findViewById(R.id.info_detail_resolution_container);
        resolutionDateTv    = view.findViewById(R.id.info_detail_resolution_date);
        resolutionByTv      = view.findViewById(R.id.info_detail_resolution_by);
        resolutionReasonTv  = view.findViewById(R.id.info_detail_rejection_reason);
        resolutionReasonRow = view.findViewById(R.id.info_detail_rejection_reason_row);

        view.findViewById(R.id.info_detail_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        acceptBtn.setOnClickListener(v -> showAcceptDialog());
        rejectBtn.setOnClickListener(v -> showRejectDialog());

        if (!docId.isEmpty()) loadData();
        return view;
    }

    // ────────────────────────────────────────────────────────
    // LOAD DATA
    // ────────────────────────────────────────────────────────

    private void loadData() {
        db.collection("add_info_requests").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    long number = doc.contains("number") ? doc.getLong("number") : 0;
                    numberTv.setText("Request #" + number);

                    String status = doc.getString("status");
                    if (status == null) status = "In Review";
                    statusTv.setText("Status: " + status);

                    Timestamp ts = doc.getTimestamp("createdAt");
                    if (ts != null) dateTv.setText("Submitted: " + fmt(ts));

                    String sub = doc.getString("submittedByUsername");
                    submittedByTv.setText("Submitted By: User "
                            + (sub != null ? sub : ""));

                    locationNameTv.setText(nvl(doc.getString("locationName")));
                    descriptionTv.setText(nvl(doc.getString("description")));
                    placeTypeTv.setText(nvl(doc.getString("placeType")));

                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");
                    if (lat != null && lng != null)
                        coordinatesTv.setText(String.format(Locale.getDefault(),
                                "%.5f, %.5f", lat, lng));

                    List<String> urls = (List<String>) doc.get("imageUrls");
                    if (urls != null) for (String url : urls) loadImage(url);

                    if ("Completed".equals(status)) {
                        applyAcceptedState(fmt(doc.getTimestamp("resolvedAt")),
                                nvl(doc.getString("resolvedBy")));
                    } else if ("Rejected".equals(status)) {
                        applyRejectedState(fmt(doc.getTimestamp("resolvedAt")),
                                nvl(doc.getString("resolvedBy")),
                                nvl(doc.getString("rejectionReason")));
                    }

                    doc.getReference().update("read", true);
                });
    }

    // ────────────────────────────────────────────────────────
    // DIALOGS
    // ────────────────────────────────────────────────────────

    private void showAcceptDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_accept);
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialog.findViewById(R.id.dialog_accept_yes).setOnClickListener(v -> {
            dialog.dismiss(); confirmAccept();
        });
        dialog.findViewById(R.id.dialog_accept_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

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
            dialog.dismiss(); confirmReject(reason);
        });
        dialog.findViewById(R.id.dialog_reject_no).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ────────────────────────────────────────────────────────
    // CONFIRM ACCEPT / REJECT
    // ────────────────────────────────────────────────────────

    private void confirmAccept() {
        if (docId.isEmpty()) return;
        db.collection("add_info_requests").document(docId)
                .update("status", "Completed",
                        "resolvedAt", Timestamp.now(),
                        "resolvedBy", getAdminId())
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "Request Accepted", Toast.LENGTH_SHORT).show();
                    applyAcceptedState(todayStr(), "Administrator");
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmReject(String reason) {
        if (docId.isEmpty()) return;
        db.collection("add_info_requests").document(docId)
                .update("status", "Rejected",
                        "resolvedAt", Timestamp.now(),
                        "resolvedBy", getAdminId(),
                        "rejectionReason", reason)
                .addOnSuccessListener(v -> {
                    Toast.makeText(getContext(), "Request Rejected", Toast.LENGTH_SHORT).show();
                    applyRejectedState(todayStr(), "Administrator", reason);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ────────────────────────────────────────────────────────
    // BORDER STATE
    // Applied to wrapper LinearLayout — works reliably.
    // CardView.setBackground() is ignored by the framework;
    // using a plain LinearLayout wrapper with padding solves this.
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

    private void loadImage(String url) {
        try {
            storage.getReferenceFromUrl(url)
                    .getBytes(2 * 1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        ImageView iv = new ImageView(requireContext());
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(100, 100);
                        p.setMarginEnd(8);
                        iv.setLayoutParams(p);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setImageBitmap(bmp);
                        imagesContainer.addView(iv);
                    });
        } catch (Exception ignored) {}
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private String fmt(Timestamp ts) { return ts != null
            ? new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(ts.toDate())
            : todayStr(); }
    private String todayStr() { return new SimpleDateFormat("yyyy/MM/dd",
            Locale.getDefault()).format(new java.util.Date()); }
    private String getAdminId() { return FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "Admin"; }
}