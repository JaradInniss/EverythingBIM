package com.example.everythingbim.ui.home;

import android.text.TextUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
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

public class AIIdentifier extends AppCompatActivity {

    private ActivityAiidentifierBinding binding;
    private AIIdentifierViewModel viewModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        binding.serverUrlEt.setText(ModelServerSettings.getBaseUrl(this));
        binding.saveServerUrlBttn.setOnClickListener(v -> saveServerUrl());
        binding.healthCheckBttn.setOnClickListener(v -> runHealthCheck());
        
        binding.nearbyAttractionsRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void saveServerUrl() {
        String rawUrl = binding.serverUrlEt.getText() != null
                ? binding.serverUrlEt.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(rawUrl)) {
            Toast.makeText(this, "Enter a server URL first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String normalizedUrl = rawUrl.endsWith("/") ? rawUrl.substring(0, rawUrl.length() - 1) : rawUrl;
        ModelServerSettings.setBaseUrl(this, normalizedUrl);
        Toast.makeText(this, "Model server URL saved.", Toast.LENGTH_SHORT).show();
        viewModel.rerunAnalysis();
    }

    private void runHealthCheck() {
        String rawUrl = binding.serverUrlEt.getText() != null
                ? binding.serverUrlEt.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(rawUrl)) {
            Toast.makeText(this, "Enter a server URL first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String normalizedUrl = rawUrl.endsWith("/") ? rawUrl.substring(0, rawUrl.length() - 1) : rawUrl;
        ModelServerSettings.setBaseUrl(this, normalizedUrl);
        binding.healthCheckBttn.setEnabled(false);
        binding.healthCheckBttn.setText("Checking");

        viewModel.runHealthCheck((success, message) -> mainHandler.post(() -> {
            binding.healthCheckBttn.setEnabled(true);
            binding.healthCheckBttn.setText("Health Check");
            Toast.makeText(
                    this,
                    success ? "Server is reachable: " + message : "Health check failed: " + message,
                    Toast.LENGTH_LONG
            ).show();
        }));
    }

    private void handleIntent() {
        String uriString = getIntent().getStringExtra("image_uri");
        String source = getIntent().getStringExtra("image_source");
        String displayName = getIntent().getStringExtra("display_name");

        if (uriString != null && source != null) {
            Uri uri = Uri.parse(uriString);
            SelectedImage selectedImage = new SelectedImage(uri, source, displayName);
            
            updateUploadMethodUI(source);
            binding.identifierUploadedImage.setImageURI(uri);
            
            viewModel.initialize(selectedImage);
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

    private void renderState(@NonNull HomeUiState state) {
        TransitionManager.beginDelayedTransition(binding.analysisResponseContainer, new AutoTransition());

        switch (state.getStatus()) {
            case ANALYZING:
                binding.identifierResultTv.setText("Analyzing...");
                binding.identifierConfidenceScoreTv.setText("--");
                binding.identifierRelatedInfoTv.setText(state.getMessage() != null ? state.getMessage() : "");
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.GONE);
                break;
            case RESULT:
                if (state.getLandmark() != null) {
                    binding.identifierResultTv.setText(state.getLandmark().getDisplayName());
                    binding.identifierConfidenceScoreTv.setText(
                            state.getConfidenceText() != null ? state.getConfidenceText() : "--");
                    binding.identifierRelatedInfoTv.setText(state.getLandmark().getDescription());
                    binding.positiveResultContainer.setVisibility(View.VISIBLE);
                    binding.negativeResultContainer.setVisibility(View.GONE);
                }
                break;
            case UNKNOWN:
                binding.identifierResultTv.setText("Unknown Location");
                binding.identifierConfidenceScoreTv.setText("--");
                binding.identifierRelatedInfoTv.setText(state.getDetail() != null ? state.getDetail() : "");
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                String errorMessage = "Error: " + (state.getMessage() != null ? state.getMessage() : "Unknown");
                binding.identifierResultTv.setText(errorMessage);
                binding.identifierConfidenceScoreTv.setText("--");
                binding.identifierRelatedInfoTv.setText(
                        state.getDetail() != null ? state.getDetail() : "Check that the model server is reachable.");
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }
}
