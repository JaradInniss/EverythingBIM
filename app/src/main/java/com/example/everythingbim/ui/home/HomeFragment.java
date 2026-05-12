package com.example.everythingbim.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.data.models.SelectedImage;
import com.example.everythingbim.databinding.FragmentHomeBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private FragmentHomeBinding binding;

    private Uri pendingCameraUri;

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean gpsAvailable;
    private boolean locationPermissionGranted;
    private Double lastKnownLatitude;
    private Double lastKnownLongitude;
    private SelectedImage pendingLocationVerificationImage;

    public HomeFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        registerLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupActions();
        observeViewModel();
        return binding.getRoot();
    }

    private void registerLaunchers() {
        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleGalleryResult
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        galleryPickerLauncher.launch("image/*");
                    } else {
                        viewModel.onSelectionError("Gallery permission was denied.");
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCameraCapture();
                    } else {
                        viewModel.onSelectionError("Camera permission was denied.");
                    }
                }
        );

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    locationPermissionGranted = isGranted;
                    if (isGranted) {
                        refreshLocationContext();
                    }

                    if (pendingLocationVerificationImage != null) {
                        launchIdentifier(pendingLocationVerificationImage);
                        pendingLocationVerificationImage = null;
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingCameraUri != null) {
                        viewModel.onImageSelected(new SelectedImage(
                                pendingCameraUri,
                                SelectedImage.SOURCE_CAMERA,
                                "camera_capture.jpg"
                        ));
                    } else {
                        viewModel.onSelectionError("Camera capture was cancelled.");
                    }
                }
        );
    }

    private void setupActions() {
        binding.galleryOptBttn.setOnClickListener(v -> openGallery());
        binding.cameraOptBttn.setOnClickListener(v -> openCamera());
    }

    private void observeViewModel() {
        viewModel.getNavigationEvent().observe(getViewLifecycleOwner(), selectedImage -> {
            if (selectedImage != null) {
                handleSelectedImage(selectedImage);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSelectedImage(@NonNull SelectedImage selectedImage) {
        if (hasLocationPermission()) {
            locationPermissionGranted = true;
            refreshLocationContext();
            launchIdentifier(selectedImage);
            return;
        }

        pendingLocationVerificationImage = selectedImage;
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void launchIdentifier(@NonNull SelectedImage selectedImage) {
        Intent intent = new Intent(requireContext(), AIIdentifier.class);
        intent.putExtra("image_uri", selectedImage.getUri().toString());
        intent.putExtra("image_source", selectedImage.getSource());
        intent.putExtra("display_name", selectedImage.getDisplayName());
        intent.putExtra("gps_available", gpsAvailable);
        intent.putExtra("gps_permission_granted", locationPermissionGranted);
        if (lastKnownLatitude != null && lastKnownLongitude != null) {
            intent.putExtra("user_latitude", lastKnownLatitude);
            intent.putExtra("user_longitude", lastKnownLongitude);
        }
        startActivity(intent);
    }

    private void openGallery() {
        refreshLocationContext();
        String permission = getGalleryPermission();
        if (permission == null || ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            galleryPickerLauncher.launch("image/*");
        } else {
            galleryPermissionLauncher.launch(permission);
        }
    }

    private void openCamera() {
        refreshLocationContext();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraCapture() {
        try {
            File photoFile = createImageFile();
            pendingCameraUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(pendingCameraUri);
        } catch (IOException exception) {
            viewModel.onSelectionError("Unable to create a temporary image for camera capture.");
        }
    }

    @Nullable
    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void handleGalleryResult(@Nullable Uri uri) {
        if (uri == null) {
            viewModel.onSelectionError("No image was selected.");
            return;
        }

        viewModel.onImageSelected(new SelectedImage(
                uri,
                SelectedImage.SOURCE_GALLERY,
                resolveDisplayName(uri)
        ));
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshLocationContext() {
        locationPermissionGranted = hasLocationPermission();
        if (!locationPermissionGranted) {
            gpsAvailable = false;
            lastKnownLatitude = null;
            lastKnownLongitude = null;
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                gpsAvailable = true;
                lastKnownLatitude = location.getLatitude();
                lastKnownLongitude = location.getLongitude();
            } else {
                gpsAvailable = false;
                lastKnownLatitude = null;
                lastKnownLongitude = null;
            }
        }).addOnFailureListener(error -> {
            gpsAvailable = false;
            lastKnownLatitude = null;
            lastKnownLongitude = null;
        });
    }

    @NonNull
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("bim_capture_" + timeStamp + "_", ".jpg", requireContext().getCacheDir());
    }

    @Nullable
    private String resolveDisplayName(@NonNull Uri uri) {
        Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            return fallbackFileName(uri);
        }

        try {
            if (cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } finally {
            cursor.close();
        }
        return fallbackFileName(uri);
    }

    @NonNull
    private String fallbackFileName(@NonNull Uri uri) {
        String path = uri.getPath();
        if (path == null) return "image.jpg";
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            return path.substring(cut + 1);
        }
        return path;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
