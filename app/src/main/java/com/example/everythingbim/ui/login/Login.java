package com.example.everythingbim.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.everythingbim.R;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.databinding.ActivityLoginBinding;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private LoginViewModel viewModel;
    private ActivityLoginBinding binding;
    private SharedPreferences sharedPreferences;

    // UI elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView userTypeText, createAccountOption;
    private Button loginBttn;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;
    private TextView errorTextView;

    // Variables
    private final Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.setSharedPreferences(sharedPreferences);

        initViews();
        setupObservers();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        userTypeText = findViewById(R.id.user_type_txt);
        createAccountOption = findViewById(R.id.create_account_opt);
        loginBttn = findViewById(R.id.login_bttn);
        emailEditText = findViewById(R.id.login_email_et);
        passwordEditText = findViewById(R.id.login_password_et);
        progressBar = findViewById(R.id.login_progress_bar);
        errorTextView = findViewById(R.id.login_error);

        userTypeGeneral.setOnClickListener(this);
        userTypeBusiness.setOnClickListener(this);
        createAccountOption.setOnClickListener(this);
        loginBttn.setOnClickListener(this);
    }

    private void setupObservers() {
        // Observe user type selection (icons and text)
        viewModel.getSelectedUserType().observe(this, userType -> {
            if (userType == UserType.GENERAL) {
                userTypeGeneral.setBackgroundResource(R.drawable.pill_bg_yllw);
                userTypeBusiness.setBackgroundResource(0);
                userTypeText.setText(R.string.general_user);
            } else {
                userTypeBusiness.setBackgroundResource(R.drawable.pill_bg_yllw);
                userTypeGeneral.setBackgroundResource(0);
                userTypeText.setText(R.string.business_user);
            }
        });

        // Loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                loginBttn.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                loginBttn.setEnabled(true);
            }
        });

        // Error messages
        viewModel.getErrorFields().observe(this, errors -> {
            // Safety check: If the ViewModel sends a null map, stop execution
            handler.removeCallbacksAndMessages(null);
            if (errors == null) {
                // If errors are null, manually hide everything immediately
                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                errorTextView.setVisibility(View.GONE);
                binding.tilLoginEmail.setError(null);
                binding.tilLoginPassword.setError(null);
                return;
            }

            // Map IDs to Layouts: We create a temporary map to link the EditText IDs
            HashMap<Integer, TextInputLayout> fieldMap = new HashMap<>();
            fieldMap.put(R.id.login_email_et, binding.tilLoginEmail);
            fieldMap.put(R.id.login_password_et, binding.tilLoginPassword);

            // Iterate through errors: The ViewModel might return multiple errors at once
            for (Map.Entry<Integer, String> entry : errors.entrySet()) {
                int fieldId = entry.getKey();
                String error = entry.getValue();

                // Find the corresponding layout for the field that has the error
                TextInputLayout fieldLayout = fieldMap.get(fieldId);
                if (fieldLayout == null) continue;

                // Animate layout changes
                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                // Set the Error UI: Update the Material layout and our custom TextView
                fieldLayout.setError(error);        // Shows red text under the entry field
                fieldLayout.setErrorEnabled(true);
                errorTextView.setVisibility(View.VISIBLE); // Makes error text appear
                errorTextView.setText(error);

                // Delayed Disappearance: Create a timer to hide the error after 2 seconds
                handler.postDelayed(() -> {

                    // Animate the views sliding back into their original places
                    TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                    // Reset UI: Hide the error box and clear the red outlines/text from the field
                    errorTextView.setVisibility(View.GONE);
                    fieldLayout.setError(null);
                    fieldLayout.setErrorEnabled(false);
                }, 2000);
            }
        });

        // Navigation commands
        viewModel.getNavigationEvent().observe(this, command -> {
            if (command != null) {
                Intent intent = new Intent(Login.this, command.getDestination());
                if (command.getExtras() != null) {
                    intent.putExtras(command.getExtras());
                }
                startActivity(intent);
                finish(); // optional: remove login from back stack
            }
        });
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.login_bttn) {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            viewModel.onLoginClicked(email, password);
        } else if (id == R.id.user_type_general) {
            viewModel.setSelectedUserType(UserType.GENERAL);
        } else if (id == R.id.user_type_business) {
            viewModel.setSelectedUserType(UserType.BUSINESS);
        } else if (id == R.id.create_account_opt) {
            viewModel.onCreateAccountClicked();
        }
    }
}