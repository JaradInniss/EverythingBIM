package com.example.everythingbim.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.ActivityLogger;
import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.ui.home.UserNotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminDatasetSubmissionDetailFragment extends Fragment {
    private static final String ARG_DOC_ID = "doc_id";
    private static final String COLLECTION_DATASET_SUBMISSIONS = "dataset_image_submissions";

    private FirebaseFirestore db;
    private String docId;
    private ActivityLogger activityLogger;

    private TextView numberTv;
    private TextView titleTv;
    private TextView statusTv;
    private ImageView imageView;
    private TextView submittedByTv;
    private TextView createdAtTv;
    private TextView sourceTv;
    private TextView resultTv;
    private TextView confidenceTv;
    private TextView gpsTv;
    private TextView coordinatesTv;
    private TextView noteTv;
    private TextView detailTv;
    private TextView acceptBtn;
    private TextView rejectBtn;
    private View actionButtons;
    private View resolutionContainer;
    private View rejectionReasonRow;
    private TextView resolutionDateTv;
    private TextView resolutionByTv;
    private TextView rejectionReasonTv;
    private View cardWrapper;
    private String recipientUserId = "";

    public static AdminDatasetSubmissionDetailFragment newInstance(@NonNull String docId) {
        AdminDatasetSubmissionDetailFragment fragment = new AdminDatasetSubmissionDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOC_ID, docId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dataset_submission_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        activityLogger = new ActivityLogger(requireContext());
        docId = getArguments() != null ? getArguments().getString(ARG_DOC_ID) : null;

        numberTv = view.findViewById(R.id.dataset_detail_number);
        titleTv = view.findViewById(R.id.dataset_detail_title);
        statusTv = view.findViewById(R.id.dataset_detail_status);
        imageView = view.findViewById(R.id.dataset_detail_image);
        submittedByTv = view.findViewById(R.id.dataset_detail_submitted_by);
        createdAtTv = view.findViewById(R.id.dataset_detail_created_at);
        sourceTv = view.findViewById(R.id.dataset_detail_source);
        resultTv = view.findViewById(R.id.dataset_detail_result);
        confidenceTv = view.findViewById(R.id.dataset_detail_confidence);
        gpsTv = view.findViewById(R.id.dataset_detail_gps);
        coordinatesTv = view.findViewById(R.id.dataset_detail_coordinates);
        noteTv = view.findViewById(R.id.dataset_detail_note);
        detailTv = view.findViewById(R.id.dataset_detail_description);
        acceptBtn = view.findViewById(R.id.dataset_detail_accept);
        rejectBtn = view.findViewById(R.id.dataset_detail_reject);
        actionButtons = view.findViewById(R.id.dataset_detail_action_buttons);
        resolutionContainer = view.findViewById(R.id.dataset_detail_resolution_container);
        rejectionReasonRow = view.findViewById(R.id.dataset_detail_rejection_reason_row);
        resolutionDateTv = view.findViewById(R.id.dataset_detail_resolution_date);
        resolutionByTv = view.findViewById(R.id.dataset_detail_resolution_by);
        rejectionReasonTv = view.findViewById(R.id.dataset_detail_rejection_reason);
        cardWrapper = view.findViewById(R.id.dataset_detail_card_wrapper);

        view.findViewById(R.id.dataset_detail_back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        acceptBtn.setOnClickListener(v -> updateStatus("Accepted"));
        rejectBtn.setOnClickListener(v -> updateStatus("Rejected"));

        if (TextUtils.isEmpty(docId)) {
            showToast("Submission not found.");
            if (isAdded()) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
            return;
        }

        loadSubmission();
    }

    private void loadSubmission() {
        db.collection(COLLECTION_DATASET_SUBMISSIONS)
                .document(docId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!isUiActive()) return;
                    markSubmissionReadOnOpen(document);
                    bindSubmission(document);
                    if (document.exists()) {
                        String title = firstNonEmpty(
                                document.getString("title"),
                                document.getString("landmarkName"),
                                document.getString("displayName"),
                                "Dataset Image Submission"
                        );
                        activityLogger.logView(ActivityLogger.TYPE_DATASET, title, docId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Could not load submission.");
                });
    }

    private void markSubmissionReadOnOpen(@NonNull DocumentSnapshot document) {
        if (!document.exists()) {
            return;
        }

        Boolean read = document.getBoolean("read");
        if (Boolean.TRUE.equals(read)) {
            return;
        }

        db.collection(COLLECTION_DATASET_SUBMISSIONS)
                .document(docId)
                .update("read", true);
    }

    private void bindSubmission(@NonNull DocumentSnapshot document) {
        if (!isUiActive()) return;
        if (!document.exists()) {
            showToast("Submission not found.");
            return;
        }

        String title = firstNonEmpty(
                document.getString("title"),
                document.getString("landmarkName"),
                document.getString("displayName"),
                "Dataset Image Submission"
        );
        String number = document.contains("number")
                ? "#" + document.getLong("number")
                : "#" + document.getId().substring(0, Math.min(6, document.getId().length())).toUpperCase();
        String rawStatus = firstNonEmpty(document.getString("status"), "Pending Review");
        String status = normalizeStatusLabel(rawStatus);
        String submittedBy = firstNonEmpty(document.getString("submittedByUsername"), document.getString("email"), "User");
        String source = firstNonEmpty(document.getString("imageSource"), "Unknown");
        String result = firstNonEmpty(document.getString("analysisStatus"), document.getString("message"), "Unknown");
        String confidence = firstNonEmpty(document.getString("confidenceText"), "--");
        String note = firstNonEmpty(document.getString("userNote"), "No note provided.");
        String description = firstNonEmpty(document.getString("description"), document.getString("message"), "No additional context provided.");
        String imageUrl = document.getString("imageUrl");
        recipientUserId = firstNonEmpty(document.getString("userId"));

        Double latitude = document.getDouble("userLatitude");
        Double longitude = document.getDouble("userLongitude");
        Boolean gpsAvailable = document.getBoolean("gpsAvailable");
        Boolean usedGps = document.getBoolean("usedGps");
        String reviewedBy = firstNonEmpty(document.getString("reviewedBy"), document.getString("resolvedBy"));
        Timestamp reviewedAt = firstNonEmpty(document.getString("reviewedBy")).isEmpty()
                ? document.getTimestamp("resolvedAt")
                : document.getTimestamp("reviewedAt");
        String rejectionReason = firstNonEmpty(document.getString("rejectionReason"), document.getString("reviewNotes"));

        numberTv.setText("Request " + number);
        titleTv.setText(title);
        statusTv.setText("Status: " + status);
        submittedByTv.setText("Submitted By: " + submittedBy);
        sourceTv.setText("Image Source: " + source);
        resultTv.setText("Result State: " + result);
        confidenceTv.setText("Confidence: " + confidence);
        noteTv.setText(note);
        detailTv.setText(description);

        Timestamp createdAt = document.getTimestamp("createdAt");
        if (createdAt != null) {
            createdAtTv.setText("Submitted: " + new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    .format(createdAt.toDate()));
        } else {
            createdAtTv.setText("Submitted: Unknown");
        }

        gpsTv.setText("GPS Support: " + (Boolean.TRUE.equals(gpsAvailable)
                ? (Boolean.TRUE.equals(usedGps) ? "Available and used" : "Available")
                : "Unavailable"));

        if (latitude != null && longitude != null) {
            coordinatesTv.setText(String.format(Locale.getDefault(), "%.5f, %.5f", latitude, longitude));
        } else {
            coordinatesTv.setText("Not provided");
        }

        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(imageView)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_images)
                    .error(R.drawable.ic_images)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_images);
        }

        applyStatusState(status, reviewedAt, reviewedBy, rejectionReason);
    }

    private void updateStatus(@NonNull String status) {
        if (TextUtils.isEmpty(docId)) {
            return;
        }
        long now = System.currentTimeMillis();
        db.collection(COLLECTION_DATASET_SUBMISSIONS)
                .document(docId)
                .update(
                        "status", status,
                        "read", true,
                        "resolvedAt", new Timestamp(new java.util.Date(now)),
                        "resolvedBy", "Administrator"
                )
                .addOnSuccessListener(unused -> {
                    if (!isUiActive()) return;
                    statusTv.setText("Status: " + normalizeStatusLabel(status));
                    String action = "Accepted".equals(status)
                            ? ActivityLogger.ACTION_APPROVED
                            : ActivityLogger.ACTION_REJECTED;
                    activityLogger.logActivity(
                            ActivityLogger.TYPE_DATASET,
                            action,
                            titleTv.getText().toString(),
                            docId
                    );
                    UserNotificationHelper.createNotification(
                            db,
                            recipientUserId,
                            UserNotificationHelper.TYPE_DATASET_SUBMISSION,
                            "Dataset Submission Update",
                            "Your image submission was " + ("Accepted".equals(status) ? "accepted." : "rejected."),
                            status,
                            docId,
                            COLLECTION_DATASET_SUBMISSIONS,
                            UserNotificationHelper.TARGET_DATASET_DIALOG,
                            UserNotificationHelper.TYPE_DATASET_SUBMISSION,
                            titleTv.getText().toString()
                    );
                    applyStatusState(
                            normalizeStatusLabel(status),
                            new Timestamp(new java.util.Date(now)),
                            "Administrator",
                            ""
                    );
                    showToast("Submission updated.");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Could not update submission.");
                });
    }

    private void applyStatusState(@NonNull String status,
                                  @Nullable Timestamp resolvedAt,
                                  @NonNull String resolvedBy,
                                  @NonNull String rejectionReason) {
        if ("Accepted".equalsIgnoreCase(status)) {
            applyAcceptedState(formatTimestamp(resolvedAt), resolvedBy);
        } else if ("Rejected".equalsIgnoreCase(status)) {
            applyRejectedState(formatTimestamp(resolvedAt), resolvedBy, rejectionReason);
        } else {
            if (cardWrapper != null) {
                cardWrapper.setBackgroundResource(R.drawable.bg_card_default);
            }
            if (actionButtons != null) {
                actionButtons.setVisibility(View.VISIBLE);
            }
            if (resolutionContainer != null) {
                resolutionContainer.setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    private String normalizeStatusLabel(@Nullable String status) {
        if (status == null
                || status.trim().isEmpty()
                || "Pending Review".equalsIgnoreCase(status)
                || "Reviewed".equalsIgnoreCase(status)) {
            return "In Review";
        }
        return status.trim();
    }

    private void applyAcceptedState(@NonNull String date, @NonNull String by) {
        if (cardWrapper != null) {
            cardWrapper.setBackgroundResource(R.drawable.bg_card_accepted);
        }
        if (statusTv != null) {
            statusTv.setText("Status: Accepted");
            statusTv.setTextColor(android.graphics.Color.parseColor("#28965a"));
        }
        if (actionButtons != null) {
            actionButtons.setVisibility(View.GONE);
        }
        if (resolutionContainer != null) {
            resolutionContainer.setVisibility(View.VISIBLE);
        }
        if (rejectionReasonRow != null) {
            rejectionReasonRow.setVisibility(View.GONE);
        }
        if (resolutionDateTv != null) {
            resolutionDateTv.setText("Accepted: " + date);
        }
        if (resolutionByTv != null) {
            resolutionByTv.setText("Accepted By: " + firstNonEmpty(by, "Administrator"));
        }
    }

    private void applyRejectedState(@NonNull String date, @NonNull String by, @NonNull String reason) {
        if (cardWrapper != null) {
            cardWrapper.setBackgroundResource(R.drawable.bg_card_rejected);
        }
        if (statusTv != null) {
            statusTv.setText("Status: Rejected");
            statusTv.setTextColor(android.graphics.Color.parseColor("#c0392b"));
        }
        if (actionButtons != null) {
            actionButtons.setVisibility(View.GONE);
        }
        if (resolutionContainer != null) {
            resolutionContainer.setVisibility(View.VISIBLE);
        }
        if (rejectionReasonRow != null) {
            rejectionReasonRow.setVisibility(View.VISIBLE);
        }
        if (resolutionDateTv != null) {
            resolutionDateTv.setText("Rejected: " + date);
        }
        if (resolutionByTv != null) {
            resolutionByTv.setText("Rejected By: " + firstNonEmpty(by, "Administrator"));
        }
        if (rejectionReasonTv != null) {
            rejectionReasonTv.setText("Reason For Rejection: " + firstNonEmpty(reason, "No reason provided."));
        }
    }

    private boolean isUiActive() {
        return isAdded() && getView() != null;
    }

    private void showToast(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @NonNull
    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    @NonNull
    private String formatTimestamp(@Nullable Timestamp timestamp) {
        if (timestamp == null) {
            return "Unknown";
        }
        return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(timestamp.toDate());
    }
}
