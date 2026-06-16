package com.example.everythingbim.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;
    private FragmentHomeBinding binding;

    private Uri pendingCameraUri;

    private ActivityResultLauncher<String[]> galleryPickerLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean gpsAvailable;
    private boolean locationPermissionGranted;
    private Double lastKnownLatitude;
    private Double lastKnownLongitude;
    private SelectedImage pendingLocationVerificationImage;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListenerRegistration;

    public HomeFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        registerLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupActions();
        observeViewModel();

        // Check login state and set username immediately
        android.content.SharedPreferences prefs = getActivity()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        if (userId.isEmpty()) {
            // Not logged in - show "User" using binding
            binding.homeWelcomeUsernameTv.setText("User");
        } else {
            // Logged in - load username from Firestore
            loadWelcomeUsername();
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reset username to "User" if not logged in
        if (getActivity() == null) return;

        android.content.SharedPreferences prefs = getActivity()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        if (userId.isEmpty()) {
            if (binding.homeWelcomeUsernameTv != null) {
                binding.homeWelcomeUsernameTv.setText("User");
            }
        } else {
            // Reload username in case it changed
            loadWelcomeUsername();
        }
    }

    private void loadWelcomeUsername() {
        if (getActivity() == null) return;

        android.content.SharedPreferences prefs = getActivity()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        String userType = prefs.getString("userType", "");

        if (userId.isEmpty()) return;

        // Use Firebase Auth UID to query Firestore for username
        String collection = "business".equals(userType) ? "businesses" : "users";
        db.collection(collection).document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String username;
                        if ("business".equals(userType)) {
                            username = doc.getString("companyName");
                        } else {
                            username = doc.getString("username");
                        }
                        if (username != null && !username.isEmpty()) {
                            if (binding.homeWelcomeUsernameTv != null) {
                                binding.homeWelcomeUsernameTv.setText(username);
                            }
                        }
                    }
                });
    }

    private void registerLaunchers() {
        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handleGalleryResult
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

        binding.notificationBttn.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Log in to view notifications.", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(requireContext(), NotificationsActivity.class));
        });
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
        intent.putExtra(AIIdentifier.EXTRA_IMAGE_URI, selectedImage.getUri().toString());
        intent.putExtra(AIIdentifier.EXTRA_IMAGE_SOURCE, selectedImage.getSource());
        intent.putExtra(AIIdentifier.EXTRA_DISPLAY_NAME, selectedImage.getDisplayName());
        intent.putExtra(AIIdentifier.EXTRA_GPS_AVAILABLE, gpsAvailable);
        intent.putExtra(AIIdentifier.EXTRA_GPS_PERMISSION_GRANTED, locationPermissionGranted);
        if (lastKnownLatitude != null && lastKnownLongitude != null) {
            intent.putExtra(AIIdentifier.EXTRA_USER_LATITUDE, lastKnownLatitude);
            intent.putExtra(AIIdentifier.EXTRA_USER_LONGITUDE, lastKnownLongitude);
        }
        startActivity(intent);
    }

    private void openGallery() {
        refreshLocationContext();
        launchGalleryPicker();
    }

    private void launchGalleryPicker() {
        galleryPickerLauncher.launch(new String[]{"image/*"});
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

    private void handleGalleryResult(@Nullable Uri uri) {
        if (uri == null) {
            viewModel.onSelectionError("No image was selected.");
            return;
        }
        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException ignored) {
            // Some providers do not support persistable permissions.
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
    public void onStart() {
        super.onStart();
        bindNotificationBadge();
    }

    @Override
    public void onStop() {
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            notificationListenerRegistration = null;
        }
        super.onStop();
    }

    private void bindNotificationBadge() {
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            notificationListenerRegistration = null;
        }

        if (binding == null || auth.getCurrentUser() == null) {
            updateNotificationBadge(0);
            return;
        }

        notificationListenerRegistration = db.collection(UserNotificationHelper.COLLECTION_USER_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", auth.getCurrentUser().getUid())
                .whereEqualTo("read", false)
                .addSnapshotListener((snap, error) -> {
                    if (binding == null) {
                        return;
                    }
                    if (error != null || snap == null) {
                        updateNotificationBadge(0);
                        return;
                    }
                    updateNotificationBadge(snap.size());
                });
    }

    private void updateNotificationBadge(int count) {
        if (binding == null) {
            return;
        }
        if (count <= 0) {
            binding.notificationBadgeTv.setVisibility(View.GONE);
            return;
        }

        binding.notificationBadgeTv.setVisibility(View.VISIBLE);
        binding.notificationBadgeTv.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
