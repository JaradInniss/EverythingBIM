package com.example.everythingbim.ui.posts;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.databinding.ActivityCreatePostBinding;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;

public class CreatePostActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityCreatePostBinding binding;
    private CreatePostViewModel viewModel;

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
        observeViewModel();
    }

    private void setupViews() {
        binding.returnBttn.setOnClickListener(this);
        binding.prevBttn2.setOnClickListener(this); // Share button
        
        // Add more listeners for camera, gallery, etc. as needed
    }

    private void observeViewModel() {
        viewModel.getPostCreated().observe(this, created -> {
            if (created) {
                Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getIsSaving().observe(this, saving -> {
            binding.prevBttn2.setEnabled(!saving);
            // Show/hide progress bar if you have one
        });
    }

    @Override
    public void onClick(View view) {
        int bttnId = view.getId();

        if (bttnId == R.id.return_bttn) {
            finish();
        } else if (bttnId == R.id.prev_bttn2) {
            handleShare();
        }
    }

    private void handleShare() {
        String caption = binding.captionEt.getText().toString();
        viewModel.setCaption(caption);
        
        // Note: You'll need to set the imageUri and locationId in the viewModel 
        // based on user selection before calling createPost().
        // For now, this just triggers the viewModel logic.
        viewModel.createPost();
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
