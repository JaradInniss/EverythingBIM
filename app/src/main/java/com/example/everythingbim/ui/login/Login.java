package com.example.everythingbim.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
import com.example.everythingbim.ui.utils.NavigationCommand;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private LoginViewModel viewModel;
    private SharedPreferences sharedPreferences;

    // UI elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView userTypeText, createAccountOption;
    private Button loginBttn;
    private EditText emailEditText, passwordEditText;
    private ProgressBar progressBar;
    private TextView errorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE);

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
        emailEditText = findViewById(R.id.login_username_et);
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
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                errorTextView.setText(error);
                errorTextView.setVisibility(View.VISIBLE);
            } else {
                errorTextView.setVisibility(View.GONE);
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