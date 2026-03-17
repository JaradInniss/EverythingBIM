package com.example.everythingbim.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.UserType;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private LoginViewModel viewModel;
    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView userTypeText, createAccountOption;
    private Button loginBttn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        initViews();
        setupObservers();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        // ImageViews
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        userTypeGeneral.setOnClickListener(this);
        userTypeBusiness.setOnClickListener(this);

        // TextViews
        userTypeText = findViewById(R.id.user_type_txt);
        createAccountOption = findViewById(R.id.create_account_opt);
        createAccountOption.setOnClickListener(this);

        // Button
        loginBttn = findViewById(R.id.login_bttn);
        loginBttn.setOnClickListener(this);
    }

    private void setupObservers() {
        // Observe the selected user type changes
        viewModel.getSelectedUserType().observe(this, userType -> {
            updateUI(userType);
        });

        // Observe the navigation event
        viewModel.getNavigationEvent().observe(this, destination -> {
            if (destination != null) {
                Intent intent = new Intent(this, destination);
                if (destination == Login.class){
                    intent.putExtra("userType", viewModel.getSelectedUserType().getValue());
                    startActivity(intent);
                }
                else {
                    startActivity(intent);
                }

            }
        });
    }

    private void updateUI(UserType userType) {
        if (userType == UserType.GENERAL) {
            // Set background yellow
            userTypeGeneral.setBackgroundResource(R.drawable.pill_bg_yllw);
            // Remove background of Business User Icon
            userTypeBusiness.setBackgroundResource(0);
            // Change text to "General User"
            userTypeText.setText("General User");
        }
        else if (userType == UserType.BUSINESS) {
            // Set background yellow
            userTypeBusiness.setBackgroundResource(R.drawable.pill_bg_yllw);
            // Remove background of Business User Icon
            userTypeGeneral.setBackgroundResource(0);
            // Change text to "General User"
            userTypeText.setText("Business User");
        }
    }

    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.login_bttn) {
            // Login Button
            viewModel.onLoginClicked();
        }
        else if (bttn_id == R.id.user_type_general) {
            viewModel.setSelectedUserType(UserType.GENERAL);
        }
        else if (bttn_id == R.id.user_type_business) {
            viewModel.setSelectedUserType(UserType.BUSINESS);
        }
        else if (bttn_id == R.id.create_account_opt) {
            // Create Account option
            viewModel.onCreateAccountClicked();
        }
    }
}