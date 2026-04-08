package com.example.everythingbim;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminReportDetailFragment extends Fragment {

    // ─── Bundle key passed from AdminReportsFragment ──
    public static final String ARG_DOC_ID = "doc_id";

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

    // ─── Firebase ────────────────────────────
    private FirebaseFirestore db;
    private FirebaseStorage   storage;

    // ─── Admin action options ─────────────────
    private static final String[] ACTIONS = {
            "Select Action",
            "Dismiss Report",
            "Warn User",
            "Remove Post",
            "Suspend Account",
            "Ban Account"
    };

    // ────────────────────────────────────────────────────────
    // FACTORY — pass the Firestore document ID from the list
    // ────────────────────────────────────────────────────────

    public static AdminReportDetailFragment newInstance(String docId) {
        AdminReportDetailFragment f = new AdminReportDetailFragment();
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

        View view = inflater.inflate(
                R.layout.fragment_admin_report_detail, container, false);

        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (getArguments() != null) {
            docId = getArguments().getString(ARG_DOC_ID, "");
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
                requireActivity().getSupportFragmentManager().popBackStack());

        setupActionSpinner();
        loadReportData();

        // Submit
        submitBtn.setOnClickListener(v -> submitAction());

        return view;
    }

    // ────────────────────────────────────────────────────────
    // LOAD REPORT DATA FROM FIRESTORE
    // ────────────────────────────────────────────────────────

    private void loadReportData() {
        if (docId.isEmpty()) return;

        db.collection("reports").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // Report number
                    long number = doc.contains("number") ? doc.getLong("number") : 0;
                    numberTv.setText("Report #" + number);

                    // Severity badge — colour changes per severity
                    String severity = doc.getString("severity");
                    if (severity == null) severity = "Minor";
                    severityBadge.setText(severity.toUpperCase());
                    applySeverityBadgeColor(severity);

                    // Status
                    String status = doc.getString("status");
                    statusTv.setText(status != null ? status : "In Review");
                    applyStatusColor(status);

                    // Submitted date
                    com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                    if (ts != null) {
                        String date = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                                .format(ts.toDate());
                        dateTv.setText("Submitted: " + date);
                    }

                    // Report type + issue
                    typeTv.setText(doc.getString("type") != null
                            ? doc.getString("type") : "Post");
                    issueTv.setText(doc.getString("issue") != null
                            ? doc.getString("issue") : "");

                    // Post content
                    userTv.setText(doc.getString("reportedUser") != null
                            ? doc.getString("reportedUser") : "");
                    com.google.firebase.Timestamp postTs = doc.getTimestamp("postDate");
                    if (postTs != null) {
                        postDateTv.setText(new SimpleDateFormat("yyyy/MM/dd",
                                Locale.getDefault()).format(postTs.toDate()));
                    }
                    captionTv.setText(doc.getString("caption") != null
                            ? doc.getString("caption") : "");

                    // Load post image from Storage URL if available
                    String imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        loadImageFromStorage(imageUrl);
                    }

                    // Mark as read in Firestore
                    doc.getReference().update("read", true);
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
                return position != 0; // disable "Select Action" prompt
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v).setTextColor(position == 0
                        ? android.graphics.Color.GRAY
                        : android.graphics.Color.BLACK);
                return v;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(adapter);

        actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedAction = pos != 0 ? ACTIONS[pos] : "";
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

        submitBtn.setEnabled(false);
        submitBtn.setText(getString(R.string.processing));

        db.collection("reports").document(docId)
                .update(
                        "status", "Completed",
                        "adminAction", selectedAction,
                        "resolvedAt", com.google.firebase.Timestamp.now()
                )
                .addOnSuccessListener(v -> {
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