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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.home.UserNotificationHelper;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import com.example.everythingbim.ActivityLogger;

public class AdminLocationRequestDetailsFragment extends Fragment {

    private static final String TAG = "LocReqDetail";

    // ─── Bundle arg keys ─────────────────────
    public static final String ARG_DOC_ID = "doc_id";
    public static final String ARG_NUMBER = "number";
    public static final String ARG_STATUS = "status";
    public static final String ARG_DATE = "date";
    public static final String ARG_SUBMITTED_BY = "submittedBy";
    public static final String ARG_LOCATION_NAME = "locationName";
    public static final String ARG_COORDINATES = "coordinates";
    public static final String ARG_DESCRIPTION = "description";
    public static final String ARG_PLACE_TYPE = "placeType";
    public static final String ARG_REASON = "reason";
    public static final String ARG_LATITUDE = "latitude";
    public static final String ARG_LONGITUDE = "longitude";
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
    private String cachedLocationName = "";
    private String cachedCoordinates = "";
    private String cachedDescription = "";
    private String cachedPlaceType = "";
    private String cachedReason = "";
    private double cachedLatitude = 0.0;
    private double cachedLongitude = 0.0;
    private String cachedResolvedAt = "";
    private String cachedResolvedBy = "";
    private String cachedRejectionReason = "";
    private String cachedRecipientUserId = "";
    private boolean dataFromBundle = false;

    // ─── Map ─────────────────────────────────
    private MapView mapView;
    private GoogleMap googleMap;
    private double pinLat = 0.0, pinLng = 0.0;

    // ─── Views ───────────────────────────────
    private LinearLayout cardWrapper;
    private TextView numberTv, statusTv, dateTv, submittedByTv;
    private TextView locationNameTv, coordinatesTv, descriptionTv, placeTypeTv, reasonTv;
    private LinearLayout imagesContainer, actionButtons;
    private Button acceptBtn, rejectBtn;
    private LinearLayout resolutionContainer;
    private TextView resolutionDateTv, resolutionByTv, resolutionReasonTv;
    private LinearLayout resolutionReasonRow;

    // ────────────────────────────────────────────────────────
    // FACTORY — Bundle approach for instant display
    // ────────────────────────────────────────────────────────

    public static AdminLocationRequestDetailsFragment newInstance(
            String docId, String number, String status, String date,
            String submittedBy, String locationName, String coordinates,
            String description, String placeType, String reason,
            double latitude, double longitude) {

        AdminLocationRequestDetailsFragment f = new AdminLocationRequestDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOC_ID, docId);
        args.putString(ARG_NUMBER, number);
        args.putString(ARG_STATUS, status);
        args.putString(ARG_DATE, date);
        args.putString(ARG_SUBMITTED_BY, submittedBy);
        args.putString(ARG_LOCATION_NAME, locationName);
        args.putString(ARG_COORDINATES, coordinates);
        args.putString(ARG_DESCRIPTION, description);
        args.putString(ARG_PLACE_TYPE, placeType);
        args.putString(ARG_REASON, reason);
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        f.setArguments(args);
        return f;
    }

    // Legacy factory for backward compatibility with Firestore fallback
    public static AdminLocationRequestDetailsFragment newInstance(String docId) {
        AdminLocationRequestDetailsFragment f = new AdminLocationRequestDetailsFragment();
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

        View view = inflater.inflate(R.layout.fragment_admin_location_request_details,
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
            cachedLocationName = getArguments().getString(ARG_LOCATION_NAME, "");
            cachedCoordinates = getArguments().getString(ARG_COORDINATES, "");
            cachedDescription = getArguments().getString(ARG_DESCRIPTION, "");
            cachedPlaceType = getArguments().getString(ARG_PLACE_TYPE, "");
            cachedReason = getArguments().getString(ARG_REASON, "");
            cachedLatitude = getArguments().getDouble(ARG_LATITUDE, 0.0);
            cachedLongitude = getArguments().getDouble(ARG_LONGITUDE, 0.0);
            cachedResolvedAt = getArguments().getString(ARG_RESOLVED_AT, "");
            cachedResolvedBy = getArguments().getString(ARG_RESOLVED_BY, "");
            cachedRejectionReason = getArguments().getString(ARG_REJECTION_REASON, "");

            // If we have number, data was passed via Bundle
            dataFromBundle = !cachedNumber.isEmpty();
        }

        // Bind views
        cardWrapper    = view.findViewById(R.id.loc_detail_card_wrapper);
        numberTv       = view.findViewById(R.id.loc_detail_number);
        statusTv       = view.findViewById(R.id.loc_detail_status);
        dateTv         = view.findViewById(R.id.loc_detail_date);
        submittedByTv  = view.findViewById(R.id.loc_detail_submitted_by);
        locationNameTv = view.findViewById(R.id.loc_detail_location_name);
        coordinatesTv  = view.findViewById(R.id.loc_detail_coordinates);
        descriptionTv  = view.findViewById(R.id.loc_detail_description);
        placeTypeTv    = view.findViewById(R.id.loc_detail_place_type);
        reasonTv       = view.findViewById(R.id.loc_detail_reason);
        imagesContainer     = view.findViewById(R.id.loc_detail_images_container);
        actionButtons       = view.findViewById(R.id.loc_detail_action_buttons);
        acceptBtn           = view.findViewById(R.id.loc_detail_accept_btn);
        rejectBtn           = view.findViewById(R.id.loc_detail_reject_btn);
        resolutionContainer = view.findViewById(R.id.loc_detail_resolution_container);
        resolutionDateTv    = view.findViewById(R.id.loc_detail_resolution_date);
        resolutionByTv      = view.findViewById(R.id.loc_detail_resolution_by);
        resolutionReasonTv  = view.findViewById(R.id.loc_detail_rejection_reason);
        resolutionReasonRow = view.findViewById(R.id.loc_detail_rejection_reason_row);

        view.findViewById(R.id.loc_detail_back_btn).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        acceptBtn.setOnClickListener(v -> showAcceptDialog());
        rejectBtn.setOnClickListener(v -> showRejectDialog());

        // Map — read-only
        mapView = view.findViewById(R.id.loc_detail_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            googleMap = map;
            googleMap.getUiSettings().setAllGesturesEnabled(false);
            googleMap.getUiSettings().setZoomControlsEnabled(false);
            pinMap();
        });

        // Display Bundle data immediately if available
        if (dataFromBundle) {
            displayCachedData();
        }

        // Always try to fetch fresh data from Firestore as fallback/refresh
        if (!docId.isEmpty()) {
            loadData();
            // Log activity - admin viewed this location request
            activityLogger.logView(ActivityLogger.TYPE_LOCATION, cachedLocationName, docId);
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
        locationNameTv.setText(cachedLocationName);
        coordinatesTv.setText(cachedCoordinates);
        descriptionTv.setText(cachedDescription);
        placeTypeTv.setText(cachedPlaceType);
        reasonTv.setText(cachedReason);

        if (cachedLatitude != 0.0 && cachedLongitude != 0.0) {
            pinLat = cachedLatitude;
            pinLng = cachedLongitude;
            pinMap();
        }

        applyStatusVisual(cachedStatus);
    }

    private void applyStatusVisual(String status) {
        if ("Completed".equals(status)) {
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
        db.collection("add_location_requests").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isUiActive()) return;
                    if (!doc.exists()) {
                        Log.e(TAG, "Document does not exist: " + docId);
                        return;
                    }

                    Log.d(TAG, "Document found, updating fields");

                    // Use 'number' field if present, otherwise use first 6 chars of document ID
                    long number = doc.contains("number") ? doc.getLong("number") : 0;
                    if (number == 0 && !doc.getId().isEmpty()) {
                        // Fallback to doc ID when number field is missing
                        String docIdPrefix = doc.getId().substring(0, Math.min(6, doc.getId().length())).toUpperCase();
                        numberTv.setText("Request #" + docIdPrefix);
                        cachedNumber = docIdPrefix;
                    } else {
                        numberTv.setText("Request #" + number);
                        cachedNumber = String.valueOf(number);
                    }

                    String status = doc.getString("status");
                    if (status == null) status = "In Review";
                    statusTv.setText("Status: " + status);
                    cachedStatus = status;

                    Timestamp ts = doc.getTimestamp("createdAt");
                    if (ts != null) {
                        String dateStr = fmt(ts);
                        dateTv.setText("Submitted: " + dateStr);
                        cachedDate = dateStr;
                    }

                    String sub = doc.getString("submittedByUsername");
                    submittedByTv.setText("Submitted By: User "
                            + (sub != null ? sub : ""));
                    cachedSubmittedBy = sub != null ? sub : "";
                    cachedRecipientUserId = nvl(doc.getString("userId"));

                    String locationName = nvl(doc.getString("locationName"));
                    locationNameTv.setText(locationName);
                    cachedLocationName = locationName;

                    String description = nvl(doc.getString("description"));
                    descriptionTv.setText(description);
                    cachedDescription = description;

                    String placeType = nvl(doc.getString("placeType"));
                    placeTypeTv.setText(placeType);
                    cachedPlaceType = placeType;

                    String reason = nvl(doc.getString("reason"));
                    reasonTv.setText(reason);
                    cachedReason = reason;

                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");
                    if (lat != null && lng != null) {
                        String coords = String.format(Locale.getDefault(),
                                "%.5f, %.5f", lat, lng);
                        coordinatesTv.setText(coords);
                        cachedCoordinates = coords;
                        cachedLatitude = lat;
                        cachedLongitude = lng;
                        pinLat = lat; pinLng = lng;
                        pinMap();
                    }

                    List<String> urls = (List<String>) doc.get("imageUrls");
                    if (urls != null) for (String url : urls) loadImage(url);

                    if ("Completed".equals(status)) {
                        String resolvedAt = fmt(doc.getTimestamp("resolvedAt"));
                        applyAcceptedState(resolvedAt, nvl(doc.getString("resolvedBy")));
                        cachedResolvedAt = resolvedAt;
                        cachedResolvedBy = nvl(doc.getString("resolvedBy"));
                    } else if ("Rejected".equals(status)) {
                        String resolvedAt = fmt(doc.getTimestamp("resolvedAt"));
                        String rejectionReason = nvl(doc.getString("rejectionReason"));
                        applyRejectedState(resolvedAt, nvl(doc.getString("resolvedBy")), rejectionReason);
                        cachedResolvedAt = resolvedAt;
                        cachedResolvedBy = nvl(doc.getString("resolvedBy"));
                        cachedRejectionReason = rejectionReason;
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
        db.collection("add_location_requests").document(docId)
                .update("status", "Completed",
                        "resolvedAt", Timestamp.now(),
                        "resolvedBy", getAdminId())
                .addOnSuccessListener(v -> {
                    if (!isUiActive()) return;
                    // Log approval activity
                    activityLogger.logApproval(ActivityLogger.TYPE_LOCATION, cachedLocationName, docId);
                    UserNotificationHelper.createNotification(
                            db,
                            cachedRecipientUserId,
                            UserNotificationHelper.TYPE_LOCATION_REQUEST,
                            "Location Request Update",
                            "Your location request for " + firstNonEmpty(cachedLocationName, "this location") + " was accepted.",
                            "Accepted",
                            docId,
                            "add_location_requests",
                            UserNotificationHelper.TARGET_COMPLETED_LOCATION,
                            UserNotificationHelper.TYPE_LOCATION_REQUEST,
                            firstNonEmpty(cachedLocationName, "Location Request")
                    );
                    showToast("Request Accepted");
                    applyAcceptedState(todayStr(), "Administrator");
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Failed: " + e.getMessage());
                });
    }

    private void confirmReject(String reason) {
        if (docId.isEmpty()) return;
        db.collection("add_location_requests").document(docId)
                .update("status", "Rejected",
                        "resolvedAt", Timestamp.now(),
                        "resolvedBy", getAdminId(),
                        "rejectionReason", reason)
                .addOnSuccessListener(v -> {
                    if (!isUiActive()) return;
                    // Log rejection activity
                    activityLogger.logRejection(ActivityLogger.TYPE_LOCATION, cachedLocationName, docId);
                    UserNotificationHelper.createNotification(
                            db,
                            cachedRecipientUserId,
                            UserNotificationHelper.TYPE_LOCATION_REQUEST,
                            "Location Request Update",
                            "Your location request for " + firstNonEmpty(cachedLocationName, "this location") + " was rejected.",
                            "Rejected",
                            docId,
                            "add_location_requests",
                            UserNotificationHelper.TARGET_COMPLETED_LOCATION,
                            UserNotificationHelper.TYPE_LOCATION_REQUEST,
                            firstNonEmpty(cachedLocationName, "Location Request")
                    );
                    showToast("Request Rejected");
                    applyRejectedState(todayStr(), "Administrator", reason);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showToast("Failed: " + e.getMessage());
                });
    }

    // ────────────────────────────────────────────────────────
    // BORDER STATE — applied to wrapper LinearLayout, not CardView
    // This is the correct approach: setBackgroundResource on a plain
    // LinearLayout works reliably; CardView ignores setBackground().
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
    // MAP + HELPERS
    // ────────────────────────────────────────────────────────

    private void pinMap() {
        if (googleMap == null || pinLat == 0.0) return;
        LatLng loc = new LatLng(pinLat, pinLng);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(loc));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));
    }

    private void loadImage(String url) {
        try {
            storage.getReferenceFromUrl(url)
                    .getBytes(2 * 1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        if (!isUiActive()) return;
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        ImageView iv = new ImageView(requireContext());
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(90, 70);
                        p.setMarginEnd(8);
                        iv.setLayoutParams(p);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setImageBitmap(bmp);
                        imagesContainer.addView(iv);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image: " + e.getMessage());
        }
    }

    private boolean isUiActive() {
        return isAdded() && getView() != null;
    }

    private void showToast(String message) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
    private String fmt(Timestamp ts) { return ts != null
            ? new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(ts.toDate())
            : todayStr(); }
    private String todayStr() { return new SimpleDateFormat("yyyy/MM/dd",
            Locale.getDefault()).format(new java.util.Date()); }
    private String getAdminId() { return FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "Admin"; }

    @Override public void onResume()    { super.onResume();    if (mapView != null) mapView.onResume(); }
    @Override public void onPause()     { super.onPause();     if (mapView != null) mapView.onPause(); }
    @Override public void onStart()     { super.onStart();     if (mapView != null) mapView.onStart(); }
    @Override public void onStop()      { super.onStop();      if (mapView != null) mapView.onStop(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (mapView != null) mapView.onSaveInstanceState(out);
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        googleMap = null;
        super.onDestroyView();
    }
}
