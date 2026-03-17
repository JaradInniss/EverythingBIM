package com.example.everythingbim;

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

import com.google.firebase.ktx.Firebase;

public class Login extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView userTypeText, createAccountOption;
    private Button loginBttn;

    // Variables
    private String currUserType;
    private String[] userTypes = {"General", "Business"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        currUserType = userTypes[0];

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void switchUserType(String userType) {
        if (userType.equals("General")) {
            // Set background yellow
            userTypeGeneral.setBackgroundResource(R.drawable.pill_bg_yllw);
            // Remove background of Business User Icon
            userTypeBusiness.setBackgroundResource(0);
            // Change text to "General User"
            userTypeText.setText("General User");
            currUserType = userTypes[0];
        }
        else if (userType.equals("Business")) {
            // Set background yellow
            userTypeBusiness.setBackgroundResource(R.drawable.pill_bg_yllw);
            // Remove background of Business User Icon
            userTypeGeneral.setBackgroundResource(0);
            // Change text to "General User"
            userTypeText.setText("Business User");
            currUserType = userTypes[1];
        }
    }

    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.login_bttn) {
            // Login Button
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
        }
        else if (bttn_id == R.id.user_type_general) {
            switchUserType(userTypes[0]);
        }
        else if (bttn_id == R.id.user_type_business) {
            switchUserType(userTypes[1]);
        }
        else if (bttn_id == R.id.create_account_opt) {
            // Create Account option
            if (currUserType.equals("General")) {
                Intent intent = new Intent(Login.this, GeneralRegistration.class);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(Login.this, BusinessRegistration.class);
                startActivity(intent);
            }
        }
//        Firebase.analytics.logEvent("login_btn_click, null")
    }

//    private void loginUser() {
//        String email = emailEditText.getText().toString().trim();
//        String password = passwordEditText.getText().toString().trim();
//
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(this, task -> {
//                    if (task.isSuccessful()) {
//                        FirebaseUser user = mAuth.getCurrentUser();
//
//                        // Check if email is verified
//                        if (user != null && user.isEmailVerified()) {
//                            // Email verified - proceed to main app
//                            Intent intent = new Intent(Login.this, MainActivity.class);
//                            startActivity(intent);
//                            finish();
//                        } else {
//                            // Email not verified
//                            Toast.makeText(Login.this,
//                                    "Please verify your email before logging in",
//                                    Toast.LENGTH_LONG).show();
//
//                            // Option to resend verification
//                            user.sendEmailVerification();
//
//                            // Sign out
//                            mAuth.signOut();
//                        }
//                    } else {
//                        Toast.makeText(Login.this, "Login failed", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
}