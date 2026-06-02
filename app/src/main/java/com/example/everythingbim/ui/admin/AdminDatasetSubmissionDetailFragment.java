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
    private TextView markReviewedBtn;
    private TextView acceptBtn;
    private TextView rejectBtn;

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
        markReviewedBtn = view.findViewById(R.id.dataset_detail_mark_reviewed);
        acceptBtn = view.findViewById(R.id.dataset_detail_accept);
        rejectBtn = view.findViewById(R.id.dataset_detail_reject);

        view.findViewById(R.id.dataset_detail_back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        markReviewedBtn.setOnClickListener(v -> updateStatus("Reviewed"));
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
        String status = firstNonEmpty(document.getString("status"), "Pending Review");
        String submittedBy = firstNonEmpty(document.getString("submittedByUsername"), document.getString("email"), "User");
        String source = firstNonEmpty(document.getString("imageSource"), "Unknown");
        String result = firstNonEmpty(document.getString("analysisStatus"), document.getString("message"), "Unknown");
        String confidence = firstNonEmpty(document.getString("confidenceText"), "--");
        String note = firstNonEmpty(document.getString("userNote"), "No note provided.");
        String description = firstNonEmpty(document.getString("description"), document.getString("message"), "No additional context provided.");
        String imageUrl = document.getString("imageUrl");

        Double latitude = document.getDouble("userLatitude");
        Double longitude = document.getDouble("userLongitude");
        Boolean gpsAvailable = document.getBoolean("gpsAvailable");
        Boolean usedGps = document.getBoolean("usedGps");

        titleTv.setText(title);
        statusTv.setText(status);
        submittedByTv.setText(submittedBy);
        sourceTv.setText(source);
        resultTv.setText(result);
        confidenceTv.setText(confidence);
        noteTv.setText(note);
        detailTv.setText(description);

        Timestamp createdAt = document.getTimestamp("createdAt");
        if (createdAt != null) {
            createdAtTv.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    .format(createdAt.toDate()));
        } else {
            createdAtTv.setText("Unknown");
        }

        gpsTv.setText(Boolean.TRUE.equals(gpsAvailable)
                ? (Boolean.TRUE.equals(usedGps) ? "Available and used" : "Available")
                : "Unavailable");

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
    }

    private void updateStatus(@NonNull String status) {
        if (TextUtils.isEmpty(docId)) {
            return;
        }
        db.collection(COLLECTION_DATASET_SUBMISSIONS)
                .document(docId)
                .update("status", status, "read", true)
                .addOnSuccessListener(unused -> {
                    if (!isUiActive()) return;
                    statusTv.setText(status);
                    String action = "Reviewed".equals(status)
                            ? ActivityLogger.ACTION_VIEWED
                            : ("Accepted".equals(status)
                            ? ActivityLogger.ACTION_APPROVED
                            : ActivityLogger.ACTION_REJECTED);
                    activityLogger.logActivity(
                            ActivityLogger.TYPE_DATASET,
                            action,
                            titleTv.getText().toString(),
                            docId
                    );
                    showToast("Submission updated.");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Could not update submission.");
                });
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
}
