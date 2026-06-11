package com.example.everythingbim.ui.posts;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.view.Gravity;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.databinding.ActivityCreatePostBinding;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CreatePostActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String PREF_CREATE_POST_SCROLL_HINT_SEEN = "create_post_scroll_hint_seen";
    private static final int IMAGE_SOURCE_UNKNOWN = 0;
    private static final int IMAGE_SOURCE_CAMERA = 1;
    private static final int IMAGE_SOURCE_GALLERY = 2;

    private ActivityCreatePostBinding binding;
    private CreatePostViewModel viewModel;
    private final List<AutocompletePrediction> autocompletePredictions = new ArrayList<>();
    private final List<String> locationSuggestions = new ArrayList<>();
    private ArrayAdapter<String> locationSuggestionsAdapter;
    private UserSearchAdapter userSearchAdapter;
    private UserTagAdapter userTagAdapter;
    private Uri pendingCameraUri;
    private PlacesClient placesClient;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearchRunnable = this::performSearch;
    private boolean suppressSearchTextChange;
    private AutocompleteSessionToken autocompleteSessionToken;
    private int selectedImageSource = IMAGE_SOURCE_UNKNOWN;
    @Nullable
    private AlertDialog uploadingDialog;

    private ActivityResultLauncher<String[]> galleryPickerLauncher;
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
        setupKeyboardInsets();

        if (isGuestUser()) {
            showAuthRequiredDialog();
            return;
        }

        setupViews();
        observeViewModel();
        initializePlacesClient();
    }

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacks(pendingSearchRunnable);
        dismissUploadingDialog();
        super.onDestroy();
    }

    private void setupKeyboardInsets() {
        int initialLeft = binding.createPostScroll.getPaddingLeft();
        int initialTop = binding.createPostScroll.getPaddingTop();
        int initialRight = binding.createPostScroll.getPaddingRight();
        int initialBottom = binding.createPostScroll.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                binding.getRoot(),
                binding.createPostScroll,
                binding.createPostScroll,
                PREF_CREATE_POST_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> binding.createPostScroll.setPadding(
                        initialLeft,
                        initialTop,
                        initialRight,
                        initialBottom + keyboardExtraBottom
                )
        );
    }

    private void setupViews() {
        binding.returnBttn.setOnClickListener(this);
        binding.clearBttn.setOnClickListener(this);
        binding.prevBttn2.setOnClickListener(this);
        binding.cameraOptBttn.setOnClickListener(this);
        binding.galleryOptBttn.setOnClickListener(this);
        binding.searchBttn.setOnClickListener(this);

        locationSuggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                locationSuggestions
        );
        binding.searchResultsList.setAdapter(locationSuggestionsAdapter);
        userSearchAdapter = new UserSearchAdapter(this, new ArrayList<>());
        binding.createpostsTagUserResultsList.setAdapter(userSearchAdapter);
        userTagAdapter = new UserTagAdapter(viewModel::removeTaggedUser);
        binding.tagsContainer.setLayoutManager(new LinearLayoutManager(this));
        binding.tagsContainer.setAdapter(userTagAdapter);

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressSearchTextChange) {
                    return;
                }
                viewModel.setSelectedLocationId(null);
                searchHandler.removeCallbacks(pendingSearchRunnable);
                if (s == null || s.toString().trim().isEmpty()) {
                    clearPredictions();
                } else {
                    searchHandler.postDelayed(pendingSearchRunnable, 300L);
                }
                updateShareButtonState();
                updateClearButtonVisibility();
            }
        });
        binding.captionEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setCaption(s != null ? s.toString() : "");
                updateShareButtonState();
                updateClearButtonVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateClearButtonVisibility();
            }
        });
        binding.userTagEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchUsers(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.searchEt.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (binding.searchEt.getText() != null && !binding.searchEt.getText().toString().trim().isEmpty()) {
                    performSearch();
                }
            } else if (locationSuggestions.isEmpty()) {
                setLocationResultsVisible(false);
            }
        });

        binding.searchResultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= autocompletePredictions.size()) {
                return;
            }
            AutocompletePrediction prediction = autocompletePredictions.get(position);
            searchHandler.removeCallbacks(pendingSearchRunnable);
            clearPredictions();

            suppressSearchTextChange = true;
            try {
                CharSequence primaryText = prediction.getPrimaryText(null);
                if (primaryText != null) {
                    binding.searchEt.setText(primaryText.toString());
                    binding.searchEt.setSelection(binding.searchEt.getText().length());
                }
            } finally {
                suppressSearchTextChange = false;
            }

            fetchSelectedPlace(prediction);
        });
        binding.createpostsTagUserResultsList.setOnItemClickListener((parent, view, position, id) -> {
            UserWithProfile selectedUser = userSearchAdapter.getItem(position);
            if (selectedUser == null || selectedUser.user == null) {
                return;
            }
            viewModel.addTaggedUser(selectedUser.user);
            binding.userTagEt.setText("");
            binding.createpostsTagUserResultsCard.setVisibility(View.GONE);
        });

        updateShareButtonState();
        updateClearButtonVisibility();
    }

    private void observeViewModel() {
        viewModel.getPostCreated().observe(this, created -> {
            if (created) {
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getIsSaving().observe(this, saving -> updateShareButtonState());
        viewModel.getIsSaving().observe(this, saving -> {
            if (Boolean.TRUE.equals(saving)) {
                showUploadingDialog();
            } else {
                dismissUploadingDialog();
            }
        });

        viewModel.getImageUri().observe(this, uri -> {
            if (uri == null) {
                binding.uploadOptionsContainer.setVisibility(View.VISIBLE);
                binding.imageContainer.setVisibility(View.GONE);
                selectedImageSource = IMAGE_SOURCE_UNKNOWN;
            } else {
                binding.uploadOptionsContainer.setVisibility(View.GONE);
                binding.imageContainer.setVisibility(View.VISIBLE);
                binding.imageUploadMethodIcon.setImageResource(
                        selectedImageSource == IMAGE_SOURCE_CAMERA ? R.drawable.ic_camera : R.drawable.ic_images
                );
                Glide.with(this)
                        .load(uri)
                        .centerCrop()
                        .placeholder(R.drawable.butterfly)
                        .into(binding.createpostUploadedImage);
            }
            updateShareButtonState();
            updateClearButtonVisibility();
        });

        viewModel.getAvailableLocations().observe(this, locations -> {
            updateShareButtonState();
        });

        viewModel.getSelectedLocationId().observe(this, locationId -> {
            updateShareButtonState();
            updateClearButtonVisibility();
        });
        viewModel.getTaggedUsers().observe(this, users -> {
            userTagAdapter.setTaggedUsers(users);
            updateClearButtonVisibility();
        });
        viewModel.getUserSearchResults().observe(this, users -> {
            userSearchAdapter.clear();
            if (users != null) {
                userSearchAdapter.addAll(users);
            }
            userSearchAdapter.notifyDataSetChanged();

            boolean hasResults = users != null && !users.isEmpty();
            boolean hasQuery = binding.userTagEt.getText() != null
                    && !binding.userTagEt.getText().toString().trim().isEmpty();
            binding.createpostsTagUserResultsCard.setVisibility(hasResults && hasQuery ? View.VISIBLE : View.GONE);
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
        } else if (buttonId == R.id.clear_bttn) {
            clearDraft();
        } else if (buttonId == R.id.prev_bttn2) {
            handleShare();
        } else if (buttonId == R.id.camera_opt_bttn) {
            openCamera();
        } else if (buttonId == R.id.gallery_opt_bttn) {
            openGallery();
        } else if (buttonId == R.id.search_bttn) {
            searchHandler.removeCallbacks(pendingSearchRunnable);
            performSearch();
        }
    }

    private void handleShare() {
        viewModel.createPost();
    }

    private void clearDraft() {
        searchHandler.removeCallbacks(pendingSearchRunnable);
        pendingCameraUri = null;
        selectedImageSource = IMAGE_SOURCE_UNKNOWN;
        suppressSearchTextChange = true;
        try {
            binding.searchEt.setText("");
        } finally {
            suppressSearchTextChange = false;
        }
        binding.captionEt.setText("");
        binding.userTagEt.setText("");
        clearPredictions();
        userSearchAdapter.clear();
        userSearchAdapter.notifyDataSetChanged();
        binding.createpostsTagUserResultsCard.setVisibility(View.GONE);
        viewModel.clearDraft();
        updateShareButtonState();
        updateClearButtonVisibility();
        Toast.makeText(this, "Post draft cleared.", Toast.LENGTH_SHORT).show();
    }

    private void applySelectedLocationById(long locationId, String locationName) {
        binding.searchEt.setText(locationName);
        binding.searchEt.setSelection(locationName.length());
        setLocationResultsVisible(false);
        viewModel.setSelectedLocationId(locationId);
        updateShareButtonState();
        updateClearButtonVisibility();
    }

    private void initializePlacesClient() {
        String apiKey = getMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this.getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);
        autocompleteSessionToken = AutocompleteSessionToken.newInstance();
    }

    @Nullable
    private String getMapsApiKey() {
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                return ai.metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return null;
    }

    private void clearPredictions() {
        autocompletePredictions.clear();
        locationSuggestions.clear();
        locationSuggestionsAdapter.notifyDataSetChanged();
        setLocationResultsVisible(false);
        if (binding.createpostSearchProgress != null) {
            binding.createpostSearchProgress.setVisibility(View.GONE);
        }
    }

    private void performSearch() {
        if (placesClient == null || binding.searchEt == null) {
            return;
        }
        String query = binding.searchEt.getText() == null ? "" : binding.searchEt.getText().toString().trim();
        if (query.isEmpty()) {
            clearPredictions();
            return;
        }

        if (binding.createpostSearchProgress != null) {
            binding.createpostSearchProgress.setVisibility(View.VISIBLE);
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(autocompleteSessionToken)
                .setQuery(query)
                .setCountries("BB")
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    autocompletePredictions.clear();
                    locationSuggestions.clear();
                    autocompletePredictions.addAll(response.getAutocompletePredictions());

                    for (AutocompletePrediction prediction : autocompletePredictions) {
                        locationSuggestions.add(prediction.getFullText(null).toString());
                    }

                    locationSuggestionsAdapter.notifyDataSetChanged();
                    setLocationResultsVisible(!locationSuggestions.isEmpty());
                    if (binding.createpostSearchProgress != null) {
                        binding.createpostSearchProgress.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(error -> {
                    if (binding.createpostSearchProgress != null) {
                        binding.createpostSearchProgress.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Location search failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchSelectedPlace(AutocompletePrediction prediction) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.RATING,
                Place.Field.TYPES
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), fields)
                .setSessionToken(autocompleteSessionToken)
                .build();
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            viewModel.setLocationFromPlaces(place);
            setLocationResultsVisible(false);
            updateClearButtonVisibility();
        }).addOnFailureListener(error ->
                Toast.makeText(this, "Failed to select location", Toast.LENGTH_SHORT).show());
    }

    private void registerLaunchers() {
        galleryPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handleGalleryResult
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
                        selectedImageSource = IMAGE_SOURCE_CAMERA;
                        viewModel.setImageUri(pendingCameraUri);
                    } else {
                        Toast.makeText(this, "Camera capture was cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void openGallery() {
        launchGalleryPicker();
    }

    private void launchGalleryPicker() {
        galleryPickerLauncher.launch(new String[]{"image/*"});
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

        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (SecurityException ignored) {
        }

        selectedImageSource = IMAGE_SOURCE_GALLERY;
        viewModel.setImageUri(uri);
        updateClearButtonVisibility();
    }

    private void updateShareButtonState() {
        boolean saving = Boolean.TRUE.equals(viewModel.getIsSaving().getValue());
        boolean canShare = !saving
                && viewModel.getImageUri().getValue() != null
                && viewModel.getSelectedLocationId().getValue() != null
                && binding.captionEt.getText() != null
                && !binding.captionEt.getText().toString().trim().isEmpty();

        binding.prevBttn2.setEnabled(canShare);
        binding.prevBttn2.setBackgroundResource(
                canShare ? R.drawable.bg_rectangle_gold : R.drawable.bg_rectangle_dim_grey
        );
    }

    private void updateClearButtonVisibility() {
        boolean hasCaption = binding.captionEt.getText() != null
                && !binding.captionEt.getText().toString().trim().isEmpty();
        boolean hasImage = viewModel.getImageUri().getValue() != null;
        boolean hasLocation = viewModel.getSelectedLocationId().getValue() != null;
        boolean hasTags = viewModel.getTaggedUsers().getValue() != null
                && !viewModel.getTaggedUsers().getValue().isEmpty();
        boolean hasSearch = binding.searchEt.getText() != null
                && !binding.searchEt.getText().toString().trim().isEmpty();

        boolean shouldShow = hasCaption || hasImage || hasLocation || hasTags || hasSearch;
        TransitionManager.beginDelayedTransition((ViewGroup) binding.clearBttn.getParent(), new AutoTransition());
        binding.clearBttn.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void setLocationResultsVisible(boolean visible) {
        binding.searchResultsCard.setVisibility(visible ? View.VISIBLE : View.GONE);
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

    private void showUploadingDialog() {
        if (uploadingDialog == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_uploading_post, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(view);
            uploadingDialog = builder.create();
            uploadingDialog.setCancelable(false);
            uploadingDialog.setCanceledOnTouchOutside(false);
        }
        if (!isFinishing() && !isDestroyed() && !uploadingDialog.isShowing()) {
            uploadingDialog.show();
            if (uploadingDialog.getWindow() != null) {
                uploadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                uploadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                uploadingDialog.getWindow().setGravity(Gravity.CENTER);
            }
        }
    }

    private void dismissUploadingDialog() {
        if (uploadingDialog != null && uploadingDialog.isShowing()) {
            uploadingDialog.dismiss();
        }
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
