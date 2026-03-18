package com.example.everythingbim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView userTypeText, createAccountOption, forgotPasswordBttn;
    private Button loginBttn;
    private EditText emailEditText, passwordEditText;
    private CheckBox rememberMeCheck;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Shared Preferences for Remember Me
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_USER_TYPE = "userType";

    // Variables
    private String currUserType;
    private String[] userTypes = {"General", "Business"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Shared Preferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Initialize UI elements
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

        // Forgot Password Button
        forgotPasswordBttn = findViewById(R.id.forgot_password_bttn);
        forgotPasswordBttn.setOnClickListener(this);

        // Button
        loginBttn = findViewById(R.id.login_bttn);
        loginBttn.setOnClickListener(this);

        // EditTexts
        emailEditText = findViewById(R.id.login_username_et);
        passwordEditText = findViewById(R.id.login_password_et);

        // CheckBox
        rememberMeCheck = findViewById(R.id.remember_me_check);
        rememberMeCheck.setOnClickListener(this);

        // Load saved credentials if Remember Me was checked
        loadSavedCredentials();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadSavedCredentials() {
        boolean remember = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (remember) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            String savedUserType = sharedPreferences.getString(KEY_USER_TYPE, "General");

            emailEditText.setText(savedEmail);
            passwordEditText.setText(savedPassword);
            rememberMeCheck.setChecked(true);

            // Set the correct user type
            if (savedUserType.equals("Business")) {
                switchUserType(userTypes[1]);
            } else {
                switchUserType(userTypes[0]);
            }
        }
    }

    private void saveCredentials(String email, String password, boolean remember) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (remember) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_USER_TYPE, currUserType);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void switchUserType(String userType) {
        if (userType.equals("General")) {
            userTypeGeneral.setBackgroundResource(R.drawable.pill_bg_yllw);
            userTypeBusiness.setBackgroundResource(0);
            userTypeText.setText("General User");
            currUserType = userTypes[0];
        }
        else if (userType.equals("Business")) {
            userTypeBusiness.setBackgroundResource(R.drawable.pill_bg_yllw);
            userTypeGeneral.setBackgroundResource(0);
            userTypeText.setText("Business User");
            currUserType = userTypes[1];
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Save credentials if Remember Me is checked
        saveCredentials(email, password, rememberMeCheck.isChecked());

        // Authenticate with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            checkUserTypeAndProceed(user);
                        } else {
                            setLoadingState(false);
                            Toast.makeText(Login.this,
                                    "Login failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoadingState(false);
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";

                        Toast.makeText(Login.this,
                                "Login failed: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserTypeAndProceed(FirebaseUser user) {
        String userId = user.getUid();

        // Get user data from Firestore to check user type
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoadingState(false);

                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("userType");
                        String username = documentSnapshot.getString("username");

                        // Verify that the selected user type matches the registered type
                        if (userType != null && userType.equalsIgnoreCase(currUserType)) {
                            Toast.makeText(Login.this,
                                    "Welcome, " + (username != null ? username : "User") + "!",
                                    Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            mAuth.signOut();
                            String message = userType != null ?
                                    "This account is registered as a " + userType + " user" :
                                    "User type mismatch";
                            Toast.makeText(Login.this, message, Toast.LENGTH_LONG).show();

                            // Clear saved credentials if user type mismatch
                            if (rememberMeCheck.isChecked()) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.clear();
                                editor.apply();
                                rememberMeCheck.setChecked(false);
                                emailEditText.setText("");
                                passwordEditText.setText("");
                            }
                        }
                    } else {
                        mAuth.signOut();
                        Toast.makeText(Login.this,
                                "User data not found",
                                Toast.LENGTH_LONG).show();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    mAuth.signOut();
                    Toast.makeText(Login.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            loginBttn.setEnabled(false);
            loginBttn.setText("Logging in...");
            emailEditText.setEnabled(false);
            passwordEditText.setEnabled(false);
            userTypeGeneral.setEnabled(false);
            userTypeBusiness.setEnabled(false);
        } else {
            loginBttn.setEnabled(true);
            loginBttn.setText("LOGIN");
            emailEditText.setEnabled(true);
            passwordEditText.setEnabled(true);
            userTypeGeneral.setEnabled(true);
            userTypeBusiness.setEnabled(true);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(Login.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void forgotPassword() {
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        forgotPasswordBttn.setEnabled(false);
        forgotPasswordBttn.setText("Sending...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    forgotPasswordBttn.setEnabled(true);
                    forgotPasswordBttn.setText("Forgot Password?");

                    if (task.isSuccessful()) {
                        Toast.makeText(Login.this,
                                "Password reset email sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to send reset email";
                        Toast.makeText(Login.this,
                                "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.login_bttn) {
            loginUser();
        }
        else if (bttn_id == R.id.user_type_general) {
            switchUserType(userTypes[0]);
        }
        else if (bttn_id == R.id.user_type_business) {
            switchUserType(userTypes[1]);
        }
        else if (bttn_id == R.id.create_account_opt) {
            if (currUserType.equals("General")) {
                Intent intent = new Intent(Login.this, GeneralRegistration.class);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(Login.this, BusinessRegistration.class);
                startActivity(intent);
            }
        }
        else if (bttn_id == R.id.forgot_password_bttn) {
            forgotPassword();
        }
    }
}