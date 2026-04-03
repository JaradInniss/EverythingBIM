package com.example.everythingbim.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private HomeViewModel viewModel;

    private ImageButton cameraButton;
    private ImageButton galleryButton;
    private LinearLayout actionContainer;
    private LinearLayout previewCard;
    private LinearLayout analyzingCard;
    private LinearLayout resultCard;
    private LinearLayout unknownCard;
    private LinearLayout errorCard;
    private TextView sourceLabel;
    private TextView fileNameLabel;
    private ImageView previewImage;
    private ProgressBar analyzingProgress;
    private TextView analyzingLabel;
    private TextView resultTitle;
    private TextView resultDescription;
    private TextView resultDetail;
    private TextView unknownDetail;
    private TextView errorMessage;
    private TextView retryButton;

    private Uri pendingCameraUri;

    private ActivityResultLauncher<String> galleryPickerLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    public HomeFragment() {
    }

    public static HomeFragment newInstance(String param1, String param2) {
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        registerLaunchers();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        bindViews(root);
        setupActions();
        observeUiState();
        return root;
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

    private void bindViews(@NonNull View root) {
        cameraButton = root.findViewById(R.id.camera_opt_bttn);
        galleryButton = root.findViewById(R.id.gallery_opt_bttn);
        actionContainer = root.findViewById(R.id.image_upload_opts_container);
        previewCard = root.findViewById(R.id.home_preview_card);
        analyzingCard = root.findViewById(R.id.home_analyzing_card);
        resultCard = root.findViewById(R.id.home_result_card);
        unknownCard = root.findViewById(R.id.home_unknown_card);
        errorCard = root.findViewById(R.id.home_error_card);
        sourceLabel = root.findViewById(R.id.home_source_label);
        fileNameLabel = root.findViewById(R.id.home_file_name_label);
        previewImage = root.findViewById(R.id.home_preview_image);
        analyzingProgress = root.findViewById(R.id.home_analyzing_progress);
        analyzingLabel = root.findViewById(R.id.home_analyzing_label);
        resultTitle = root.findViewById(R.id.home_result_title);
        resultDescription = root.findViewById(R.id.home_result_description);
        resultDetail = root.findViewById(R.id.home_result_detail);
        unknownDetail = root.findViewById(R.id.home_unknown_detail);
        errorMessage = root.findViewById(R.id.home_error_message);
        retryButton = root.findViewById(R.id.home_retry_bttn);
    }

    private void setupActions() {
        galleryButton.setOnClickListener(v -> openGallery());
        cameraButton.setOnClickListener(v -> openCamera());
        retryButton.setOnClickListener(v -> viewModel.reset());
    }

    private void observeUiState() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(@NonNull HomeUiState state) {
        setSectionVisibility(state.getStatus());

        SelectedImage selectedImage = state.getSelectedImage();
        if (selectedImage != null) {
            previewImage.setImageURI(selectedImage.getUri());
            sourceLabel.setText(selectedImage.getSource());

            String displayName = selectedImage.getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                fileNameLabel.setText(displayName);
                fileNameLabel.setVisibility(View.VISIBLE);
            } else {
                fileNameLabel.setVisibility(View.GONE);
            }
        } else {
            previewImage.setImageDrawable(null);
            sourceLabel.setText("");
            fileNameLabel.setVisibility(View.GONE);
        }

        switch (state.getStatus()) {
            case ANALYZING:
                analyzingProgress.setVisibility(View.VISIBLE);
                analyzingLabel.setText(state.getMessage() != null
                        ? state.getMessage()
                        : "Analyzing image in demo mode...");
                break;
            case RESULT:
                if (state.getLandmark() != null) {
                    resultTitle.setText(state.getLandmark().getDisplayName());
                    resultDescription.setText(state.getLandmark().getDescription());
                    resultDetail.setText(state.getDetail() != null ? state.getDetail() : "Demo result");
                }
                break;
            case UNKNOWN:
                unknownDetail.setText(state.getDetail() != null ? state.getDetail() : "");
                break;
            case ERROR:
                errorMessage.setText(state.getMessage() != null ? state.getMessage() : "Something went wrong.");
                break;
            case IDLE:
            case PREVIEW_READY:
            default:
                break;
        }
    }

    private void setSectionVisibility(@NonNull HomeUiState.Status status) {
        boolean hasImage = status != HomeUiState.Status.IDLE && status != HomeUiState.Status.ERROR;
        actionContainer.setVisibility(status == HomeUiState.Status.IDLE ? View.VISIBLE : View.GONE);
        previewCard.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        analyzingCard.setVisibility(status == HomeUiState.Status.ANALYZING ? View.VISIBLE : View.GONE);
        resultCard.setVisibility(status == HomeUiState.Status.RESULT ? View.VISIBLE : View.GONE);
        unknownCard.setVisibility(status == HomeUiState.Status.UNKNOWN ? View.VISIBLE : View.GONE);
        errorCard.setVisibility(status == HomeUiState.Status.ERROR ? View.VISIBLE : View.GONE);
        retryButton.setVisibility(status == HomeUiState.Status.IDLE ? View.GONE : View.VISIBLE);
    }

    private void openGallery() {
        String permission = getGalleryPermission();
        if (permission == null || ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            galleryPickerLauncher.launch("image/*");
        } else {
            galleryPermissionLauncher.launch(permission);
        }
    }

    private void openCamera() {
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

    @NonNull
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return File.createTempFile("demo_capture_" + timeStamp + "_", ".jpg", requireContext().getCacheDir());
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

    @Nullable
    private String fallbackFileName(@NonNull Uri uri) {
        String path = uri.getLastPathSegment();
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        int separatorIndex = path.lastIndexOf('/');
        if (separatorIndex >= 0 && separatorIndex < path.length() - 1) {
            return path.substring(separatorIndex + 1);
        }
        return path.replace("image:", "");
    }
}
