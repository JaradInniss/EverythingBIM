package com.example.everythingbim.ui.home;

import static com.example.everythingbim.ui.home.LandmarkRepository.PARLIAMENT_LATITUDE;
import static com.example.everythingbim.ui.home.LandmarkRepository.PARLIAMENT_LONGITUDE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.SelectedImage;
import com.example.everythingbim.databinding.ActivityAiidentifierBinding;
import com.example.everythingbim.ui.admin.AdminNotificationHelper;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AIIdentifier extends AppCompatActivity implements NearbyLocationsBottomSheet.NearbyActionsListener {
    private static final int NEARBY_PAGE_SIZE = 5;
    private static final String TAG = "AIIdentifier";
    private static final String COLLECTION_DATASET_SUBMISSIONS = "dataset_image_submissions";
    private static final String STORAGE_DATASET_SUBMISSIONS = "dataset_submissions";
    public static final String EXTRA_IMAGE_URI = "image_uri";
    public static final String EXTRA_IMAGE_SOURCE = "image_source";
    public static final String EXTRA_DISPLAY_NAME = "display_name";
    public static final String EXTRA_GPS_AVAILABLE = "gps_available";
    public static final String EXTRA_GPS_PERMISSION_GRANTED = "gps_permission_granted";
    public static final String EXTRA_USER_LATITUDE = "user_latitude";
    public static final String EXTRA_USER_LONGITUDE = "user_longitude";
    public static final String EXTRA_AUTO_OPEN_DATASET_SUBMISSION = "auto_open_dataset_submission";

    private ActivityAiidentifierBinding binding;
    private AIIdentifierViewModel viewModel;
    private NearbyLocationsAdapter nearbyLocationsAdapter;
    private final ArrayList<NearbySavedLocation> currentNearbyLocations = new ArrayList<>();
    private final LinkedHashMap<Long, NearbySavedLocation> selectedRouteLocations = new LinkedHashMap<>();
    private Landmark currentLandmark;
    private HomeUiState latestState = HomeUiState.idle();
    private SelectedImage currentSelectedImage;
    private boolean gpsPermissionGranted;
    private boolean gpsAvailable;
    private Double userLatitude;
    private Double userLongitude;
    private boolean datasetSubmissionInProgress;
    private boolean autoOpenDatasetSubmissionPending;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAiidentifierBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AIIdentifierViewModel.class);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        setupWindowInsets();
        setupViews();
        handleIntent();
        observeViewModel();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupViews() {
        binding.returnBttn.setOnClickListener(v -> finish());
        binding.reuploadBttn.setOnClickListener(v -> finish());
        binding.openNearbyBttn.setOnClickListener(v -> openNearbySheet());
        binding.viewOnMapBttn.setOnClickListener(v -> openParliamentOnMap());
        binding.addToDatasetBttn.setOnClickListener(v -> promptDatasetSubmission());

        nearbyLocationsAdapter = new NearbyLocationsAdapter(location -> openNearbySheet());
        binding.nearbyAttractionsRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.nearbyAttractionsRv.setAdapter(nearbyLocationsAdapter);
        updateNearbyPreview();
    }

    private void handleIntent() {
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        String source = getIntent().getStringExtra(EXTRA_IMAGE_SOURCE);
        String displayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        autoOpenDatasetSubmissionPending = getIntent().getBooleanExtra(EXTRA_AUTO_OPEN_DATASET_SUBMISSION, false);

        if (uriString != null && source != null) {
            Uri uri = Uri.parse(uriString);
            SelectedImage selectedImage = new SelectedImage(uri, source, displayName);
            gpsAvailable = getIntent().getBooleanExtra(EXTRA_GPS_AVAILABLE, false);
            gpsPermissionGranted = getIntent().getBooleanExtra(EXTRA_GPS_PERMISSION_GRANTED, false);
            userLatitude = getIntent().hasExtra(EXTRA_USER_LATITUDE)
                    ? getIntent().getDoubleExtra(EXTRA_USER_LATITUDE, 0d)
                    : null;
            userLongitude = getIntent().hasExtra(EXTRA_USER_LONGITUDE)
                    ? getIntent().getDoubleExtra(EXTRA_USER_LONGITUDE, 0d)
                    : null;
            currentSelectedImage = selectedImage;

            updateUploadMethodUI(source);
            binding.identifierUploadedImage.setImageURI(uri);

            viewModel.initialize(selectedImage, gpsAvailable, gpsPermissionGranted, userLatitude, userLongitude);
        } else {
            finish();
        }
    }

    private void updateUploadMethodUI(String source) {
        if (SelectedImage.SOURCE_CAMERA.equals(source)) {
            binding.imageUploadMethodTv.setText("Camera Upload");
            binding.imageUploadMethodIcon.setImageResource(R.drawable.ic_camera);
            binding.imageUploadMethodIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.space_indigo));
        } else {
            binding.imageUploadMethodTv.setText("Gallery Upload");
            binding.imageUploadMethodIcon.setImageResource(R.drawable.ic_images);
            binding.imageUploadMethodIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.space_indigo));
        }
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::renderState);
    }

    private void applyUnknownCopy() {
        binding.negativePrimaryMessageTv.setText(
                "Unfortunately, the subject of the image you uploaded could not be recognized.");
        binding.negativeRetryHintTv.setText(
                "Try uploading a different image of the subject in:\n1. At a different angle\n2. In different lighting");
        binding.negativeDatasetHintTv.setText(
                "If you have tried this already and the subject continues to be unrecognizable, it is likely that the subject is not in the Everything BIM dataset.");
        binding.negativeSendPromptTv.setText(
                "In this case would you be willing to send the image(s) to be added to our dataset so that we may improve our image recognition capabilities?");
    }

    private void applyUncertainCopy(@NonNull HomeUiState state) {
        binding.negativePrimaryMessageTv.setText(
                state.getMessage() != null ? state.getMessage() : "Possible Parliament match");
        binding.negativeRetryHintTv.setText(
                state.getDetail() != null ? state.getDetail() : "");
        binding.negativeDatasetHintTv.setText(
                "Try a clearer, front-facing photo before treating this as a confirmed match.");
        binding.negativeSendPromptTv.setText(
                "You can keep testing with more angles while we continue improving the model.");
    }

    private void renderState(@NonNull HomeUiState state) {
        latestState = state;
        TransitionManager.beginDelayedTransition(binding.analysisResponseContainer, new AutoTransition());

        switch (state.getStatus()) {
            case ANALYZING:
                binding.identifierResultTv.setText("Analyzing...");
                binding.identifierConfidenceScoreTv.setText("--");
                binding.identifierRelatedInfoTv.setText(state.getMessage() != null ? state.getMessage() : "");
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.GONE);
                binding.negativeConfidenceRow.setVisibility(View.GONE);
                break;
            case RESULT:
                if (state.getLandmark() != null) {
                    currentLandmark = state.getLandmark();
                    binding.identifierResultTv.setText(state.getLandmark().getDisplayName());
                    binding.identifierConfidenceScoreTv.setText(
                            state.getConfidenceText() != null ? state.getConfidenceText() : "--");
                    binding.identifierRelatedInfoTv.setText(
                            state.getDetail() != null ? state.getDetail() : state.getLandmark().getDescription());
                    currentNearbyLocations.clear();
                    currentNearbyLocations.addAll(state.getNearbyLocations());
                    selectedRouteLocations.clear();
                    updateNearbyPreview();
                    binding.positiveResultContainer.setVisibility(View.VISIBLE);
                    binding.negativeResultContainer.setVisibility(View.GONE);
                    binding.negativeConfidenceRow.setVisibility(View.GONE);
                }
                break;
            case UNCERTAIN:
                currentLandmark = null;
                binding.identifierResultTv.setText("Uncertain Match");
                binding.identifierConfidenceScoreTv.setText(
                        state.getConfidenceText() != null ? state.getConfidenceText() : "--");
                applyUncertainCopy(state);
                clearNearbyUi();
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                binding.negativeConfidenceRow.setVisibility(View.VISIBLE);
                binding.negativeConfidenceScoreTv.setText(
                        state.getConfidenceText() != null ? state.getConfidenceText() : "--");
                maybeAutoOpenDatasetSubmission();
                break;
            case UNKNOWN:
                currentLandmark = null;
                binding.identifierResultTv.setText("Unknown Location");
                binding.identifierConfidenceScoreTv.setText("--");
                applyUnknownCopy();
                clearNearbyUi();
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                binding.negativeConfidenceRow.setVisibility(View.GONE);
                maybeAutoOpenDatasetSubmission();
                break;
            case ERROR:
                currentLandmark = null;
                String errorMessage = "Error: " + (state.getMessage() != null ? state.getMessage() : "Unknown");
                binding.identifierResultTv.setText(errorMessage);
                binding.identifierConfidenceScoreTv.setText("--");
                binding.identifierRelatedInfoTv.setText(
                        state.getDetail() != null ? state.getDetail() : "Unable to run offline identification.");
                applyUnknownCopy();
                clearNearbyUi();
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                binding.negativeConfidenceRow.setVisibility(View.GONE);
                maybeAutoOpenDatasetSubmission();
                break;
            default:
                break;
        }
    }

    private void maybeAutoOpenDatasetSubmission() {
        if (!autoOpenDatasetSubmissionPending) {
            return;
        }
        autoOpenDatasetSubmissionPending = false;
        binding.addToDatasetBttn.post(this::promptDatasetSubmission);
    }

    private void clearNearbyUi() {
        currentLandmark = null;
        currentNearbyLocations.clear();
        selectedRouteLocations.clear();
        updateNearbyPreview();
    }

    private void updateNearbyPreview() {
        int previewCount = Math.min(currentNearbyLocations.size(), NEARBY_PAGE_SIZE);
        nearbyLocationsAdapter.submitList(new ArrayList<>(currentNearbyLocations.subList(0, previewCount)));
    }

    private void openNearbySheet() {
        if (currentNearbyLocations.isEmpty()) {
            return;
        }

        NearbyLocationsBottomSheet bottomSheet = NearbyLocationsBottomSheet.newInstance(
                new ArrayList<>(currentNearbyLocations),
                new ArrayList<>(selectedRouteLocations.keySet()),
                currentLandmark != null ? currentLandmark.getDisplayName() : null,
                currentLandmark != null ? currentLandmark.getLatitude() : 0d,
                currentLandmark != null ? currentLandmark.getLongitude() : 0d
        );
        bottomSheet.setActionsListener(this);
        bottomSheet.show(getSupportFragmentManager(), "nearby_locations_sheet");
    }

    private void openParliamentOnMap() {
        if (currentLandmark == null) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MAP_FOCUS, true);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, PARLIAMENT_LATITUDE);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, PARLIAMENT_LONGITUDE);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_NAME, "Barbados Parliament Buildings");
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, "Broad Street/Rickett Street, Bridgetown");
        startActivity(intent);
    }

    @Override
    public void onSelectionChanged(@NonNull List<NearbySavedLocation> selectedLocations) {
        selectedRouteLocations.clear();
        for (NearbySavedLocation location : selectedLocations) {
            selectedRouteLocations.put(location.getLocationId(), location);
        }
    }

    @Override
    public void onCreateRouteRequested(@NonNull List<NearbySavedLocation> selectedLocations) {
        if (selectedLocations.isEmpty()) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MAP_ROUTE, true);
        intent.putExtra(MainActivity.EXTRA_MAP_ROUTE_LOCATIONS, new ArrayList<>(selectedLocations));
        startActivity(intent);
    }

    @Override
    public void onOpenRouteExternallyRequested(@NonNull List<NearbySavedLocation> selectedLocations) {
        if (selectedLocations.isEmpty()) {
            return;
        }
        if (currentLandmark == null) {
            return;
        }

        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1")
                .append("&origin=").append(currentLandmark.getLatitude()).append(",").append(currentLandmark.getLongitude())
                .append("&travelmode=walking");

        NearbySavedLocation destination = selectedLocations.get(selectedLocations.size() - 1);
        url.append("&destination=").append(destination.getLatitude()).append(",").append(destination.getLongitude());

        if (selectedLocations.size() > 1) {
            StringBuilder waypoints = new StringBuilder();
            for (int index = 0; index < selectedLocations.size() - 1; index++) {
                NearbySavedLocation waypoint = selectedLocations.get(index);
                if (waypoints.length() > 0) {
                    waypoints.append("|");
                }
                waypoints.append(waypoint.getLatitude()).append(",").append(waypoint.getLongitude());
            }
            url.append("&waypoints=").append(Uri.encode(waypoints.toString()));
        }

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())));
    }

    @Override
    public void onLocationDetailsRequested(@NonNull NearbySavedLocation location) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MAP_FOCUS, true);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LOCATION_ID, location.getLocationId());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, location.getLatitude());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, location.getLongitude());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_NAME, location.getName());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, location.getAddress());
        startActivity(intent);
    }

    private void promptDatasetSubmission() {
        if (datasetSubmissionInProgress) {
            Toast.makeText(this, "Your submission is already being uploaded.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentSelectedImage == null || currentSelectedImage.getUri() == null) {
            Toast.makeText(this, "There is no image available to submit.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isGuestUser()) {
            showDatasetAuthRequiredDialog();
            return;
        }

        final EditText noteInput = new EditText(this);
        noteInput.setHint("Tell the team what this image shows or why it matters");
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        noteInput.setMinLines(3);
        noteInput.setMaxLines(5);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(getResources().getDisplayMetrics().density * 20f);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(noteInput);

        new AlertDialog.Builder(this)
                .setTitle("Send Image For Review")
                .setMessage("We'll send this image and the current recognition details to the admin/development team for review.")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send", (dialog, which) ->
                        submitDatasetImage(noteInput.getText() != null
                                ? noteInput.getText().toString().trim()
                                : ""))
                .show();
    }

    private void submitDatasetImage(@NonNull String userNote) {
        if (currentSelectedImage == null || currentSelectedImage.getUri() == null) {
            Toast.makeText(this, "There is no image available to submit.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            showDatasetAuthRequiredDialog();
            return;
        }

        datasetSubmissionInProgress = true;
        binding.addToDatasetBttn.setEnabled(false);
        binding.addToDatasetBttn.setText("Sending...");

        String userId = auth.getCurrentUser().getUid();
        String uploadId = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference()
                .child(STORAGE_DATASET_SUBMISSIONS)
                .child(userId)
                .child(uploadId);

        ref.putFile(currentSelectedImage.getUri())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException() != null
                                ? task.getException()
                                : new IllegalStateException("Image upload failed");
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> saveDatasetSubmission(downloadUri.toString(), userNote))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Dataset image upload failed", e);
                    finishDatasetSubmission(false, "Could not upload the image. Please try again.");
                });
    }

    private void saveDatasetSubmission(@NonNull String imageUrl, @NonNull String userNote) {
        if (auth.getCurrentUser() == null) {
            finishDatasetSubmission(false, "You need to be logged in to send an image.");
            return;
        }

        String email = auth.getCurrentUser().getEmail();
        Map<String, Object> submission = new HashMap<>();
        submission.put("userId", auth.getCurrentUser().getUid());
        submission.put("submittedByUsername", deriveSubmitterLabel(email));
        submission.put("email", email != null ? email : "");
        submission.put("status", "Pending Review");
        submission.put("read", false);
        submission.put("createdAt", Timestamp.now());
        submission.put("imageUrl", imageUrl);
        submission.put("imageSource", currentSelectedImage.getSource());
        submission.put("displayName", currentSelectedImage.getDisplayName() != null
                ? currentSelectedImage.getDisplayName()
                : "");
        submission.put("title", buildSubmissionTitle());
        submission.put("landmarkName", currentLandmark != null ? currentLandmark.getDisplayName() : "");
        submission.put("analysisStatus", latestState.getStatus().name());
        submission.put("confidenceText", latestState.getConfidenceText() != null ? latestState.getConfidenceText() : "");
        submission.put("message", latestState.getMessage() != null ? latestState.getMessage() : "");
        submission.put("description", latestState.getDetail() != null ? latestState.getDetail() : "");
        submission.put("userNote", userNote);
        submission.put("gpsAvailable", gpsAvailable);
        submission.put("gpsPermissionGranted", gpsPermissionGranted);
        submission.put("usedGps", latestState.isUsedGps());
        submission.put("gpsSupportedResult", latestState.isGpsSupportedResult());
        submission.put("imageOnlyResult", latestState.isImageOnlyResult());
        submission.put("userDistanceToLandmarkMeters", latestState.getUserDistanceToLandmarkMeters());

        if (userLatitude != null) {
            submission.put("userLatitude", userLatitude);
        }
        if (userLongitude != null) {
            submission.put("userLongitude", userLongitude);
        }

        db.collection(COLLECTION_DATASET_SUBMISSIONS)
                .add(submission)
                .addOnSuccessListener(documentReference -> {
                    AdminNotificationHelper.createNotification(
                            db,
                            AdminNotificationHelper.TYPE_DATASET_SUBMISSION,
                            "New Dataset Submission",
                            "A new dataset image submission was received.",
                            "In Review",
                            documentReference.getId(),
                            COLLECTION_DATASET_SUBMISSIONS,
                            AdminNotificationHelper.TARGET_DATASET_DETAIL,
                            AdminNotificationHelper.TYPE_DATASET_SUBMISSION,
                            buildSubmissionTitle(),
                            auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : ""
                    );
                    finishDatasetSubmission(true, "Image sent for admin review. Thank you.");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Dataset submission save failed", e);
                    finishDatasetSubmission(false, "Could not save the submission. Please try again.");
                });
    }

    private void finishDatasetSubmission(boolean success, @NonNull String message) {
        datasetSubmissionInProgress = false;
        binding.addToDatasetBttn.setEnabled(true);
        binding.addToDatasetBttn.setText("Send Image");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isGuestUser() {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String userType = preferences.getString("userType", MainActivity.USER_TYPE_GUEST);
        return MainActivity.USER_TYPE_GUEST.equals(userType) || auth.getCurrentUser() == null;
    }

    private void showDatasetAuthRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log in to send an image")
                .setMessage("We need an account before sending an image to the admin/development team. After you sign in, return here and send it again.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    Intent intent = new Intent(this, Login.class);
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_ADD_DATASET_SUBMISSION);
                    copyIdentifierContext(intent);
                    startActivity(intent);
                })
                .setNegativeButton("Create Account", (dialog, which) -> {
                    Intent intent = new Intent(this, GeneralRegistration.class);
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_ADD_DATASET_SUBMISSION);
                    copyIdentifierContext(intent);
                    startActivity(intent);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    @NonNull
    private String buildSubmissionTitle() {
        if (currentLandmark != null) {
            return currentLandmark.getDisplayName();
        }
        if (currentSelectedImage != null && currentSelectedImage.getDisplayName() != null
                && !currentSelectedImage.getDisplayName().trim().isEmpty()) {
            return currentSelectedImage.getDisplayName().trim();
        }
        return "CNN Dataset Submission";
    }

    @NonNull
    private String deriveSubmitterLabel(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Authenticated User";
        }
        return email;
    }

    private void copyIdentifierContext(@NonNull Intent intent) {
        if (currentSelectedImage == null) {
            return;
        }

        intent.putExtra(EXTRA_IMAGE_URI, currentSelectedImage.getUri().toString());
        intent.putExtra(EXTRA_IMAGE_SOURCE, currentSelectedImage.getSource());
        intent.putExtra(EXTRA_DISPLAY_NAME, currentSelectedImage.getDisplayName());
        intent.putExtra(EXTRA_GPS_AVAILABLE, gpsAvailable);
        intent.putExtra(EXTRA_GPS_PERMISSION_GRANTED, gpsPermissionGranted);
        if (userLatitude != null) {
            intent.putExtra(EXTRA_USER_LATITUDE, userLatitude);
        }
        if (userLongitude != null) {
            intent.putExtra(EXTRA_USER_LONGITUDE, userLongitude);
        }
    }
}
