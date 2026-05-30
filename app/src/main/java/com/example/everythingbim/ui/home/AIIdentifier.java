package com.example.everythingbim.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;

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
import com.example.everythingbim.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AIIdentifier extends AppCompatActivity implements NearbyLocationsBottomSheet.NearbyActionsListener {
    private ActivityAiidentifierBinding binding;
    private AIIdentifierViewModel viewModel;
    private NearbyLocationsAdapter nearbyLocationsAdapter;
    private final ArrayList<NearbySavedLocation> currentNearbyLocations = new ArrayList<>();
    private final LinkedHashMap<Long, NearbySavedLocation> selectedRouteLocations = new LinkedHashMap<>();
    private DemoLandmark currentLandmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAiidentifierBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AIIdentifierViewModel.class);

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

        nearbyLocationsAdapter = new NearbyLocationsAdapter(location -> openNearbySheet());
        binding.nearbyAttractionsRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.nearbyAttractionsRv.setAdapter(nearbyLocationsAdapter);
    }

    private void handleIntent() {
        String uriString = getIntent().getStringExtra("image_uri");
        String source = getIntent().getStringExtra("image_source");
        String displayName = getIntent().getStringExtra("display_name");

        if (uriString != null && source != null) {
            Uri uri = Uri.parse(uriString);
            SelectedImage selectedImage = new SelectedImage(uri, source, displayName);
            boolean gpsAvailable = getIntent().getBooleanExtra("gps_available", false);
            boolean gpsPermissionGranted = getIntent().getBooleanExtra("gps_permission_granted", false);
            Double userLatitude = getIntent().hasExtra("user_latitude")
                    ? getIntent().getDoubleExtra("user_latitude", 0d)
                    : null;
            Double userLongitude = getIntent().hasExtra("user_longitude")
                    ? getIntent().getDoubleExtra("user_longitude", 0d)
                    : null;

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
                    nearbyLocationsAdapter.submitList(state.getNearbyLocations());
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
                break;
            default:
                break;
        }
    }

    private void clearNearbyUi() {
        currentLandmark = null;
        currentNearbyLocations.clear();
        selectedRouteLocations.clear();
        nearbyLocationsAdapter.submitList(java.util.Collections.emptyList());
    }

    private void openNearbySheet() {
        if (currentNearbyLocations.isEmpty()) {
            return;
        }

        NearbyLocationsBottomSheet bottomSheet = NearbyLocationsBottomSheet.newInstance(
                new ArrayList<>(currentNearbyLocations),
                new ArrayList<>(selectedRouteLocations.keySet())
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
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, currentLandmark.getLatitude());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, currentLandmark.getLongitude());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_TITLE, currentLandmark.getDisplayName());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, currentLandmark.getDescription());
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
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_TITLE, location.getName());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, location.getAddress());
        startActivity(intent);
    }
}
