package com.example.everythingbim.ui.posts;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.SelectedImage;
import com.example.everythingbim.databinding.ActivityCreatePostBinding;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;

public class CreatePostActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityCreatePostBinding binding;
    private CreatePostViewModel viewModel;
    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private AutocompleteSessionToken autocompleteSessionToken;
    private final List<AutocompletePrediction> autocompletePredictions = new ArrayList<>();

    private Uri pendingCameraUri;

    private LinearLayout returnBttn, submitPostBttn, searchBar, cameraOptionBttn, galleryOptionBttn;
    private EditText searchEt, captionEt, userTagEt;
    private CardView locationResultsCard, userResultsCard, imageContainer;
    private ListView locationResultsList, userResultsList;
    private RecyclerView tagsRecyclerView;
    private ImageView uploadedImageView, uploadMethodIcon;

    private LocationSearchAdapter locationAdapter;
    private UserSearchAdapter userSearchAdapter;
    private UserTagAdapter userTagAdapter;
    private PlacesClient placesClient;
    private final List<String> locationLabels = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearchRunnable = this::performSearch;
    private ProgressBar searchProgress;
    private boolean suppressSearchTextChange;
    private static final long SEARCH_DEBOUNCE_MS = 300L;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CreatePostViewModel.class);

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
        setupAdapters();
        observeViewModel();
        registerLaunchers();
        setupListeners();
        initializePlacesClient();
    }

    private void setupViews() {
        returnBttn = binding.returnBttn;
        submitPostBttn = binding.submitPostBttn;
        searchBar = binding.searchBar;
        cameraOptionBttn = binding.cameraOptBttn;
        galleryOptionBttn = binding.galleryOptBttn;

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
    }

    private void setupAdapters() {
        locationAdapter = new LocationSearchAdapter(this, locationLabels);
        locationResultsList.setAdapter(locationAdapter);

        userSearchAdapter = new UserSearchAdapter(this, new ArrayList<>());
        userResultsList.setAdapter(userSearchAdapter);

        userTagAdapter = new UserTagAdapter(user -> viewModel.removeTaggedUser(user));
        tagsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tagsRecyclerView.setAdapter(userTagAdapter);
    }

    private void setupListeners() {
        returnBttn.setOnClickListener(this);
        submitPostBttn.setOnClickListener(this);
        searchBar.setOnClickListener(this);
        cameraOptionBttn.setOnClickListener(this);
        galleryOptionBttn.setOnClickListener(this);

        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
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

        captionEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setCaption(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        userTagEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.searchUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        locationResultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= autocompletePredictions.size()) return;
            AutocompletePrediction prediction = autocompletePredictions.get(position);

            suppressSearchTextChange = true;
            searchEt.setText(prediction.getPrimaryText(null).toString());
            suppressSearchTextChange = false;

            fetchSelectedPlace(prediction);
            clearPredictions();
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

    private void observeViewModel() {
        viewModel.getUserSearchResults().observe(this, users -> {
            userSearchAdapter.clear();
            if (users != null && !users.isEmpty()) {
                userSearchAdapter.addAll(users);
                userResultsCard.setVisibility(View.VISIBLE);
            } else {
                userResultsCard.setVisibility(View.GONE);
            }
        });

        viewModel.getTaggedUsers().observe(this, users -> {
            userTagAdapter.setTaggedUsers(users);
        });

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

        viewModel.getIsPostValid().observe(this, isValid -> {
            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
            if (isValid) {
                binding.submitPostBttn.setBackgroundResource(R.drawable.bg_rectangle_gold);
                binding.submitPostBttnIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.black));
                binding.submitPostBttnText.setTextColor(ContextCompat.getColor(this, R.color.black));
            } else {
                binding.submitPostBttn.setBackgroundResource(R.drawable.bg_rectangle_dim_grey);
                binding.submitPostBttnIcon.setImageTintList(ContextCompat.getColorStateList(this, R.color.white));
                binding.submitPostBttnText.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
            // Button is ALWAYS enabled so it can show Toast when invalid
        });

        viewModel.getPostCreated().observe(this, created -> {
            if (created) {
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getIsSaving().observe(this, saving -> {
            binding.submitPostBttn.setEnabled(!saving);
        });

        viewModel.getSelectedImageUri().observe(this, uri -> {
            if (uri != null) {
                cameraOptionBttn.setVisibility(View.GONE);
                galleryOptionBttn.setVisibility(View.GONE);
                imageContainer.setVisibility(View.VISIBLE);
                uploadedImageView.setImageURI(uri);
            } else {
                cameraOptionBttn.setVisibility(View.VISIBLE);
                galleryOptionBttn.setVisibility(View.VISIBLE);
                imageContainer.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onClick(View view) {
        int bttnId = view.getId();

        if (bttnId == R.id.return_bttn) {
            finish();
        }
        else if (bttnId == R.id.submit_post_bttn) {
            handleShare();
        }
        else if (bttnId == R.id.search_bar) {
            searchEt.requestFocus();
        }
        else if (bttnId == R.id.camera_opt_bttn) {
            openCamera();
        }
        else if (bttnId == R.id.gallery_opt_bttn) {
            openGallery();
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
            if (viewModel.getLocation().getValue() == null) {
                missingFields.append("Location");
                first = false;
            }
            if (viewModel.getSelectedImageUri().getValue() == null) {
                if (!first) missingFields.append(", ");
                missingFields.append("Image");
                first = false;
            }
            if (viewModel.getCaption().getValue() == null || viewModel.getCaption().getValue().trim().isEmpty()) {
                if (!first) missingFields.append(", ");
                missingFields.append("Caption");
            }

            Toast.makeText(this, missingFields.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleSelectedImage(@NonNull SelectedImage selectedImage) {
        viewModel.setSelectedImageUri(selectedImage.getUri());
        if (SelectedImage.SOURCE_CAMERA.equals(selectedImage.getSource())) {
            binding.imageUploadMethodIcon.setImageResource(R.drawable.ic_camera);
        } else {
            binding.imageUploadMethodIcon.setImageResource(R.drawable.ic_images);
        }
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

        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), fields).build();
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();
            viewModel.setLocationFromPlaces(place);
            locationResultsCard.setVisibility(View.GONE);
        });
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

        cameraLauncher = registerForActivityResult(
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

    private void handleGalleryResult(@Nullable Uri uri) {
        if (uri == null) return;
        viewModel.onImageSelected(new SelectedImage(
                uri,
                SelectedImage.SOURCE_GALLERY,
                resolveDisplayName(uri)
        ));
    }

    private String resolveDisplayName(@NonNull Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex >= 0) {
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
            cursor.close();
        }
        return "image.jpg";
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
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
                    this,
                    this.getPackageName() + ".fileprovider",
                    photoFile
            );
            cameraLauncher.launch(pendingCameraUri);
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

    @NonNull
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("bim_" + timeStamp + "_", ".jpg", this.getCacheDir());
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
