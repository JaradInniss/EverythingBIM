package com.example.everythingbim.ui.posts;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.databinding.ActivityCreatePostBinding;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CreatePostActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityCreatePostBinding binding;
    private CreatePostViewModel viewModel;
    private final List<LocationEntity> allLocations = new ArrayList<>();
    private final List<String> visibleLocationSuggestions = new ArrayList<>();
    private ArrayAdapter<String> locationSuggestionsAdapter;
    private Uri pendingCameraUri;

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);
        registerLaunchers();

        ViewCompat.setOnApplyWindowInsetsListener(binding.postsMain, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (isGuestUser()) {
            showAuthRequiredDialog();
            return;
        }

        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        binding.returnBttn.setOnClickListener(this);
        binding.prevBttn2.setOnClickListener(this);
        binding.cameraOptBttn.setOnClickListener(this);
        binding.galleryOptBttn.setOnClickListener(this);
        binding.searchBttn.setOnClickListener(this);

        locationSuggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                visibleLocationSuggestions
        );
        binding.searchResultsList.setAdapter(locationSuggestionsAdapter);

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSelectedLocationId(null);
                filterLocationSuggestions(s == null ? "" : s.toString());
                updateShareButtonState();
            }
        });

        binding.searchEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                filterLocationSuggestions(binding.searchEt.getText() == null ? "" : binding.searchEt.getText().toString());
            } else if (visibleLocationSuggestions.isEmpty()) {
                binding.searchResultsList.setVisibility(View.GONE);
            }
        });

        binding.searchResultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= visibleLocationSuggestions.size()) {
                return;
            }
            LocationEntity selected = findLocationByName(visibleLocationSuggestions.get(position));
            if (selected == null) {
                return;
            }
            applySelectedLocation(selected);
        });

        updateShareButtonState();
    }

    private void observeViewModel() {
        viewModel.getPostCreated().observe(this, created -> {
            if (created) {
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getIsSaving().observe(this, saving -> updateShareButtonState());

        viewModel.getImageUri().observe(this, uri -> {
            if (uri == null) {
                binding.selectedImageCard.setVisibility(View.GONE);
                binding.selectedImageHint.setText("Choose an image from the camera or gallery.");
            } else {
                binding.selectedImageCard.setVisibility(View.VISIBLE);
                binding.selectedImageHint.setText("Image selected and ready to share.");
                Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .placeholder(R.drawable.butterfly)
                        .into(binding.selectedImagePreview);
            }
            updateShareButtonState();
        });

        viewModel.getAvailableLocations().observe(this, locations -> {
            allLocations.clear();
            if (locations != null) {
                allLocations.addAll(locations);
            }
            filterLocationSuggestions(binding.searchEt.getText() == null ? "" : binding.searchEt.getText().toString());
        });

        viewModel.getValidationMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        int buttonId = view.getId();
        if (buttonId == R.id.return_bttn) {
            finish();
        } else if (buttonId == R.id.prev_bttn2) {
            handleShare();
        } else if (buttonId == R.id.camera_opt_bttn) {
            openCamera();
        } else if (buttonId == R.id.gallery_opt_bttn) {
            openGallery();
        } else if (buttonId == R.id.search_bttn) {
            handleLocationSearchAction();
        }
    }

    private void handleShare() {
        viewModel.setCaption(binding.captionEt.getText().toString());
        viewModel.createPost();
    }

    private void handleLocationSearchAction() {
        String query = binding.searchEt.getText() == null ? "" : binding.searchEt.getText().toString().trim();
        LocationEntity exactMatch = findLocationByName(query);
        if (exactMatch != null) {
            applySelectedLocation(exactMatch);
            return;
        }

        filterLocationSuggestions(query);
        if (!visibleLocationSuggestions.isEmpty()) {
            LocationEntity firstMatch = findLocationByName(visibleLocationSuggestions.get(0));
            if (firstMatch != null) {
                applySelectedLocation(firstMatch);
            }
        } else {
            Toast.makeText(this, "Choose a location from the app's saved locations.", Toast.LENGTH_SHORT).show();
        }
    }

    private void applySelectedLocation(LocationEntity selected) {
        binding.searchEt.setText(selected.name);
        binding.searchEt.setSelection(selected.name.length());
        binding.searchResultsList.setVisibility(View.GONE);
        viewModel.setSelectedLocationId(selected.locationId);
        updateShareButtonState();
    }

    private void registerLaunchers() {
        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleGalleryResult
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        galleryPickerLauncher.launch("image/*");
                    } else {
                        Toast.makeText(this, "Gallery permission was denied.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCameraCapture();
                    } else {
                        Toast.makeText(this, "Camera permission was denied.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingCameraUri != null) {
                        viewModel.setImageUri(pendingCameraUri);
                    } else {
                        Toast.makeText(this, "Camera capture was cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openGallery() {
        String permission = getGalleryPermission();
        if (permission == null || ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            galleryPickerLauncher.launch("image/*");
        } else {
            galleryPermissionLauncher.launch(permission);
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCameraCapture() {
        try {
            File photoFile = createImageFile();
            pendingCameraUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(pendingCameraUri);
        } catch (IOException exception) {
            Toast.makeText(this, "Unable to prepare camera capture.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGalleryResult(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "No image was selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.setImageUri(uri);
        String displayName = resolveDisplayName(uri);
        if (displayName != null && !displayName.isEmpty()) {
            binding.selectedImageHint.setText(displayName);
        }
    }

    private void filterLocationSuggestions(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.US);
        visibleLocationSuggestions.clear();

        if (!query.isEmpty()) {
            Set<String> uniqueNames = new LinkedHashSet<>();
            for (LocationEntity location : allLocations) {
                if (location == null || location.name == null) {
                    continue;
                }
                if (location.name.toLowerCase(Locale.US).contains(query)) {
                    uniqueNames.add(location.name);
                }
            }
            visibleLocationSuggestions.addAll(uniqueNames);
        }

        locationSuggestionsAdapter.notifyDataSetChanged();
        binding.searchResultsList.setVisibility(visibleLocationSuggestions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private LocationEntity findLocationByName(String name) {
        if (name == null) {
            return null;
        }
        for (LocationEntity location : allLocations) {
            if (location != null && location.name != null && location.name.equalsIgnoreCase(name.trim())) {
                return location;
            }
        }
        return null;
    }

    private void updateShareButtonState() {
        boolean saving = Boolean.TRUE.equals(viewModel.getIsSaving().getValue());
        boolean canShare = !saving
                && viewModel.getImageUri().getValue() != null
                && viewModel.getSelectedLocationId().getValue() != null;

        binding.prevBttn2.setEnabled(canShare);
        binding.prevBttn2.setBackgroundResource(
                canShare ? R.drawable.bg_rectangle_gold : R.drawable.bg_rectangle_dim_grey
        );
    }

    private String getGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("bim_post_" + timeStamp + "_", ".jpg", getCacheDir());
    }

    private String resolveDisplayName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
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

    private String fallbackFileName(Uri uri) {
        String lastSegment = uri.getLastPathSegment();
        return lastSegment != null ? lastSegment : "selected_image";
    }

    private boolean isGuestUser() {
        SharedPreferences preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String userType = preferences.getString("userType", MainActivity.USER_TYPE_GUEST);
        return MainActivity.USER_TYPE_GUEST.equals(userType);
    }

    private void showAuthRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log in to create a post")
                .setMessage("Guests can browse posts, but you'll need an account before sharing your own.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    startActivity(buildLoginIntent());
                    finish();
                })
                .setNegativeButton("Create Account", (dialog, which) -> {
                    startActivity(buildGeneralRegistrationIntent());
                    finish();
                })
                .setOnCancelListener(dialog -> finish())
                .setNeutralButton("Not now", (dialog, which) -> finish())
                .show();
    }

    private Intent buildLoginIntent() {
        Intent intent = new Intent(this, Login.class);
        intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_CREATE_POST);
        return intent;
    }

    private Intent buildGeneralRegistrationIntent() {
        Intent intent = new Intent(this, GeneralRegistration.class);
        intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_CREATE_POST);
        return intent;
    }
}
