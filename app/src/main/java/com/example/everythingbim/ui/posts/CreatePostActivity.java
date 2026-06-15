package com.example.everythingbim.ui.posts;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Rect;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.SelectedImage;
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
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity that lets the user create a new post: pick a location (via Google
 * Places), attach an image (from camera or gallery), write a caption, and tag
 * other users. On submit, the image is uploaded to Firebase Storage and the
 * post document is written to Firestore.
 */
public class CreatePostActivity extends AppCompatActivity implements View.OnClickListener {

    private static final long SEARCH_DEBOUNCE_MS = 300L;
    private static final String PREF_CREATE_POST_SCROLL_HINT_SEEN =
            KeyboardScrollHintHelper.PREF_CREATE_POST_SCROLL_HINT_SEEN;

    private ActivityCreatePostBinding binding;
    private CreatePostViewModel viewModel;

    private Uri pendingCameraUri;

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private final List<AutocompletePrediction> autocompletePredictions = new ArrayList<>();

    // UI Elements
    private LinearLayout returnBttn, submitPostBttn, searchBar, cameraOptionBttn, galleryOptionBttn, clearPostContentBttn, uploadOptionsContainer;
    private EditText searchEt, userTagEt, captionEt;
    private TextView submitPostBttnText;
    private CardView locationResultsCard, imageContainer, userResultsCard;
    private ListView locationResultsList, userResultsList;
    private RecyclerView tagsRecyclerView;
    private ImageView uploadedImageView, submitPostBttnIcon, uploadMethodIcon;
    private ProgressBar searchProgress;
    private ScrollView createPostScrollView;

    private LocationSearchAdapter locationAdapter;
    private UserSearchAdapter userSearchAdapter;
    private UserTagAdapter userTagAdapter;
    private PlacesClient placesClient;
    private final List<String> locationLabels = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearchRunnable = this::performSearch;
    private boolean suppressSearchTextChange;
    private AutocompleteSessionToken autocompleteSessionToken;
    private int currentKeyboardExtraBottom;
    @Nullable
    private AlertDialog uploadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);

        ViewCompat.setOnApplyWindowInsetsListener(binding.postsMain, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom);
            return insets;
        });

        if (!isPostingAuthorized()) {
            showAuthRequiredDialog();
            return;
        }

        setupViews();
        setupAdapters();
        observeViewModel();
        registerLaunchers();
        setupListeners();
        initializePlacesClient();
    }

    @Override
    protected void onDestroy() {
        // Avoid leaking the pending search callback across configuration changes.
        searchHandler.removeCallbacks(pendingSearchRunnable);
        // Dismiss the upload dialog if it's still showing. An AlertDialog
        // holds a reference to the activity via its Window, so leaving it
        // up across a configuration change (e.g. rotation) or after the
        // activity is destroyed would cause a window leak.
        dismissUploadingDialog();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPostingAuthorized()) {
            showAuthRequiredDialog();
        }
    }

    private void setupViews() {
        returnBttn = binding.returnBttn;
        submitPostBttn = binding.submitPostBttn;
        searchBar = binding.searchBar;
        cameraOptionBttn = binding.cameraOptBttn;
        galleryOptionBttn = binding.galleryOptBttn;
        clearPostContentBttn = binding.clearPostContentBttn;
        uploadOptionsContainer = binding.uploadOptionsContainer;

        submitPostBttnIcon = binding.submitPostBttnIcon;
        submitPostBttnText = binding.submitPostBttnText;

        searchEt = binding.searchEt;
        captionEt = binding.captionEt;
        userTagEt = binding.userTagEt;

        locationResultsCard = binding.createpostSearchResultsList;
        locationResultsList = binding.postsSearchResultsList;
        userResultsCard = binding.createpostsTagUserResultsCard;
        userResultsList = binding.createpostsTagUserResultsList;
        tagsRecyclerView = binding.tagsContainer;
        imageContainer = binding.imageContainer;
        uploadedImageView = binding.createpostUploadedImage;
        uploadMethodIcon = binding.imageUploadMethodIcon;

        searchProgress = binding.createpostSearchProgress;
        createPostScrollView = binding.createPostScroll;

        // Clear button is hidden by default and toggled by field input.
        clearPostContentBttn.setVisibility(View.GONE);

        setupKeyboardInsets();
        setupFocusedFieldScroll();
    }

    private void setupAdapters() {
        locationAdapter = new LocationSearchAdapter(this, locationLabels);
        locationResultsList.setAdapter(locationAdapter);

        userSearchAdapter = new UserSearchAdapter(this, new ArrayList<>());
        userResultsList.setAdapter(userSearchAdapter);

        userTagAdapter = new UserTagAdapter(user -> viewModel.removeTaggedUser(user));
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        tagsRecyclerView.setAdapter(userTagAdapter);
    }

    private void setupListeners() {
        returnBttn.setOnClickListener(this);
        submitPostBttn.setOnClickListener(this);
        cameraOptionBttn.setOnClickListener(this);
        galleryOptionBttn.setOnClickListener(this);
        clearPostContentBttn.setOnClickListener(this);
        searchBar.setOnClickListener(this);

        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressSearchTextChange) return;
                searchHandler.removeCallbacks(pendingSearchRunnable);
                if (s == null || s.toString().trim().isEmpty()) {
                    clearPredictions();
                    return;
                }
                searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        locationResultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= autocompletePredictions.size()) return;
            AutocompletePrediction prediction = autocompletePredictions.get(position);

            searchHandler.removeCallbacks(pendingSearchRunnable);

            clearPredictions();

            suppressSearchTextChange = true;
            try {
                CharSequence primaryText = prediction.getPrimaryText(null);
                if (primaryText != null) {
                    searchEt.setText(primaryText.toString());
                    searchEt.setSelection(searchEt.getText().length());
                }
            } finally {
                suppressSearchTextChange = false;
            }

            fetchSelectedPlace(prediction);
        });

        captionEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setCaption(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        userTagEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        userResultsList.setOnItemClickListener((parent, view, position, id) -> {
            UserWithProfile selectedUser = userSearchAdapter.getItem(position);
            if (selectedUser != null) {
                viewModel.addTaggedUser(selectedUser.user);
                userTagEt.setText("");
                userResultsCard.setVisibility(View.GONE);
            }
        });
    }

    private void setupKeyboardInsets() {
        int initialLeft = createPostScrollView.getPaddingLeft();
        int initialTop = createPostScrollView.getPaddingTop();
        int initialRight = createPostScrollView.getPaddingRight();
        int initialBottom = createPostScrollView.getPaddingBottom();

        LayoutInflater.from(this).inflate(R.layout.view_scroll_hint_overlay, binding.postsMain, true);

        KeyboardScrollHintHelper.attach(
                binding.postsMain,
                createPostScrollView,
                createPostScrollView,
                PREF_CREATE_POST_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> {
                    currentKeyboardExtraBottom = keyboardExtraBottom;
                    createPostScrollView.setClipToPadding(false);
                    createPostScrollView.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + keyboardExtraBottom
                    );
                }
        );
    }

    private void setupFocusedFieldScroll() {
        bindFocusScroll(searchEt, searchEt);
        bindFocusScroll(captionEt, captionEt);
        bindFocusScroll(userTagEt, userTagEt);
    }

    private void bindFocusScroll(View focusedView, View anchorView) {
        focusedView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollAnchorAboveKeyboard(anchorView);
            } else if (focusedView == searchEt) {
                locationResultsCard.setVisibility(View.GONE);
            }
        });
    }

    private void scrollAnchorAboveKeyboard(View anchorView) {
        createPostScrollView.post(() -> {
            if (currentKeyboardExtraBottom <= 0) {
                return;
            }
            if (!isDescendant(createPostScrollView, anchorView)) {
                return;
            }

            Rect rect = new Rect();
            anchorView.getDrawingRect(rect);
            createPostScrollView.offsetDescendantRectToMyCoords(anchorView, rect);

            int visibleHeight = createPostScrollView.getHeight() - currentKeyboardExtraBottom;
            int desiredBottomMargin = dpToPx(24);
            int targetBottom = visibleHeight - desiredBottomMargin;
            int delta = rect.bottom - targetBottom;
            if (delta > 0) {
                createPostScrollView.smoothScrollBy(0, delta);
            }
        });
    }

    private boolean isDescendant(ViewGroup parent, View child) {
        ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void observeViewModel() {
        viewModel.getNavigationEvent().observe(this, selectedImage -> {
            if (selectedImage != null) {
                handleSelectedImage(selectedImage);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getImageUri().observe(this, uri -> {
            if (uri != null) {
                cameraOptionBttn.setVisibility(View.GONE);
                galleryOptionBttn.setVisibility(View.GONE);
                uploadOptionsContainer.setVisibility(View.GONE);
                imageContainer.setVisibility(View.VISIBLE);
                // Use Glide to handle content:// and file:// URIs consistently and
                // to apply the same centerCrop styling we use elsewhere in the app.
                Glide.with(this)
                        .load(uri)
                        .centerInside()
                        .placeholder(R.drawable.butterfly)
                        .into(uploadedImageView);
                updateClearButtonVisibility();
            } else {
                cameraOptionBttn.setVisibility(View.VISIBLE);
                galleryOptionBttn.setVisibility(View.VISIBLE);
                uploadOptionsContainer.setVisibility(View.VISIBLE);
                imageContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getIsPostValid().observe(this, isValid -> {
            TransitionManager.beginDelayedTransition(binding.getRoot(), new AutoTransition());
            updateShareButtonState();
        });

        viewModel.getIsSaving().observe(this, saving -> {
            updateShareButtonState();
            if (Boolean.TRUE.equals(saving)) {
                showUploadingDialog();
            } else {
                dismissUploadingDialog();
            }
        });

        viewModel.getTaggedUsers().observe(this, users -> {
            userTagAdapter.setTaggedUsers(users);
            updateClearButtonVisibility();
        });

        viewModel.getCaption().observe(this, value -> updateClearButtonVisibility());

        viewModel.getUserSearchResults().observe(this, users -> {
            userSearchAdapter.clear();
            if (users != null) {
                userSearchAdapter.addAll(users);
            }
            userSearchAdapter.notifyDataSetChanged();
            boolean hasResults = users != null && !users.isEmpty();
            boolean hasQuery = userTagEt.getText() != null
                    && !userTagEt.getText().toString().trim().isEmpty();
            userResultsCard.setVisibility(hasResults && hasQuery ? View.VISIBLE : View.GONE);
        });

        viewModel.getPostCreated().observe(this, created -> {
            if (created) {
                dismissUploadingDialog();
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onClick(View view) {
        int buttonId = view.getId();
        if (buttonId == R.id.return_bttn) {
            finish();
        } else if (buttonId == R.id.submit_post_bttn) {
            handleShare();
        } else if (buttonId == R.id.camera_opt_bttn) {
            openCamera();
        } else if (buttonId == R.id.gallery_opt_bttn) {
            openGallery();
        } else if (buttonId == R.id.search_bar) {
            searchEt.requestFocus();
        } else if (buttonId == R.id.clear_post_content_bttn) {
            clearAllFields();
            clearPostContentBttn.setVisibility(View.GONE);
        }
    }

    private void handleShare() {
        Boolean isValid = viewModel.getIsPostValid().getValue();

        if (isValid != null && isValid) {
            viewModel.createPost();
        }
        else {
            StringBuilder missingFields = new StringBuilder("Please fill out: ");
            boolean first = true;

            if (viewModel.getCaption().getValue() == null
                    || viewModel.getCaption().getValue().trim().isEmpty()) {
                missingFields.append("Caption");
                first = false;
            }

            if (viewModel.getImageUri().getValue() == null) {
                if (!first) missingFields.append(", ");
                missingFields.append("Image");
                first = false;
            }

            if (viewModel.getLocation().getValue() == null) {
                if (!first) missingFields.append(", ");
                missingFields.append("Location");
            }

            Toast.makeText(this, missingFields.toString(), Toast.LENGTH_LONG).show();
        }
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
                        // Route the camera URI through the same code path the gallery
                        // uses so the upload-method icon gets set to the camera icon.
                        viewModel.onImageSelected(new SelectedImage(
                                pendingCameraUri,
                                SelectedImage.SOURCE_CAMERA,
                                pendingCameraUri.getLastPathSegment()
                        ));
                    } else {
                        Toast.makeText(this, "Camera capture was cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initializePlacesClient() {
        String apiKey = getMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) return;
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
            if (ai.metaData != null) return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException ignored) {}
        return null;
    }

    private void clearPredictions() {
        autocompletePredictions.clear();
        locationLabels.clear();
        locationAdapter.notifyDataSetChanged();
        locationResultsCard.setVisibility(View.GONE);
        showSearchLoading(false);
    }

    private void showSearchLoading(boolean isLoading) {
        if (searchProgress != null) searchProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void performSearch() {
        if (placesClient == null || searchEt == null) return;
        String query = searchEt.getText().toString().trim();
        if (query.isEmpty()) {
            clearPredictions();
            return;
        }

        showSearchLoading(true);

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(autocompleteSessionToken)
                .setQuery(query)
                .setCountries("BB")
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            autocompletePredictions.clear();
            locationLabels.clear();
            autocompletePredictions.addAll(response.getAutocompletePredictions());

            for (AutocompletePrediction prediction : autocompletePredictions) {
                locationLabels.add(prediction.getFullText(null).toString());
            }

            locationAdapter.notifyDataSetChanged();
            locationResultsCard.setVisibility(locationLabels.isEmpty() ? View.GONE : View.VISIBLE);
            showSearchLoading(false);

        }).addOnFailureListener(error -> {
            showSearchLoading(false);
            Toast.makeText(this, "Location search failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchSelectedPlace(AutocompletePrediction prediction) {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME,
                Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.RATING, Place.Field.TYPES);

        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), fields)
                .setSessionToken(autocompleteSessionToken)
                .build();
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            viewModel.setLocationFromPlaces(place);
            locationResultsCard.setVisibility(View.GONE);
        });
    }

    private void handleSelectedImage(@NonNull SelectedImage selectedImage) {
        viewModel.setImageUri(selectedImage.getUri());
        if (SelectedImage.SOURCE_CAMERA.equals(selectedImage.getSource())) {
            uploadMethodIcon.setImageResource(R.drawable.ic_camera);
        } else {
            uploadMethodIcon.setImageResource(R.drawable.ic_images);
        }
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
        viewModel.onImageSelected(new SelectedImage(
                uri,
                SelectedImage.SOURCE_GALLERY,
                resolveDisplayName(uri)
        ));
    }

    private void updateShareButtonState() {
        boolean saving = Boolean.TRUE.equals(viewModel.getIsSaving().getValue());
        boolean canPost = !saving && Boolean.TRUE.equals(viewModel.getIsPostValid().getValue());

        if (canPost) {
            submitPostBttn.setEnabled(true);
            submitPostBttn.setBackgroundResource(R.drawable.bg_rectangle_gold);
            submitPostBttnIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.black));
            submitPostBttnText.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            submitPostBttn.setEnabled(false);
            submitPostBttn.setBackgroundResource(R.drawable.bg_rectangle_dim_grey);
            submitPostBttnIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.white));
            submitPostBttnText.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
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

    private void updateClearButtonVisibility() {
        boolean hasCaption = viewModel.getCaption().getValue() != null
                && !viewModel.getCaption().getValue().trim().isEmpty();
        boolean hasImage = viewModel.getImageUri().getValue() != null;
        boolean hasLocation = viewModel.getLocation().getValue() != null;
        boolean hasTags = viewModel.getTaggedUsers().getValue() != null
                && !viewModel.getTaggedUsers().getValue().isEmpty();
        boolean hasSearch = searchEt != null
                && searchEt.getText() != null
                && !searchEt.getText().toString().trim().isEmpty();

        boolean shouldShow = hasCaption || hasImage || hasLocation || hasTags || hasSearch;
        TransitionManager.beginDelayedTransition((ViewGroup) clearPostContentBttn.getParent(), new AutoTransition());
        clearPostContentBttn.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private void clearAllFields() {
        searchEt.setText("");
        captionEt.setText("");
        userTagEt.setText("");
        viewModel.resetDraft();
        userResultsCard.setVisibility(View.GONE);
        locationResultsCard.setVisibility(View.GONE);
        autocompleteSessionToken = AutocompleteSessionToken.newInstance();
        clearPredictions();
        Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
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

    private boolean isPostingAuthorized() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return false;
        }
        return !isGuestUser();
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
