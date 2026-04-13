package com.example.everythingbim.ui.home;

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

public class AIIdentifier extends AppCompatActivity {

    private ActivityAiidentifierBinding binding;
    private AIIdentifierViewModel viewModel;

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
        
        binding.nearbyAttractionsRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
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
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.GONE);
                break;
            case RESULT:
                if (state.getLandmark() != null) {
                    binding.identifierResultTv.setText(state.getLandmark().getDisplayName());
                    binding.identifierConfidenceScoreTv.setText("95%"); // Demo value
                    binding.identifierRelatedInfoTv.setText(state.getLandmark().getDescription());
                    binding.positiveResultContainer.setVisibility(View.VISIBLE);
                    binding.negativeResultContainer.setVisibility(View.GONE);
                }
                break;
            case UNKNOWN:
                binding.identifierResultTv.setText("Unknown Location");
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                String errorMessage = "Error: " + (state.getMessage() != null ? state.getMessage() : "Unknown");
                binding.identifierResultTv.setText(errorMessage);
                binding.positiveResultContainer.setVisibility(View.GONE);
                binding.negativeResultContainer.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }
}
