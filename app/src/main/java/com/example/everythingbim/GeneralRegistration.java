package com.example.everythingbim;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
//import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public class GeneralRegistration extends AppCompatActivity implements View.OnClickListener {

    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption;
    private ViewFlipper genRegFormViewFlipper;
    private LinearLayout nextBttn, prevBttn;
    private Button submitBttn;

    //EditText fields from Form 1
    private EditText usernameEditText, passwordEditText, reenterPasswordEditText, emailEditText;

    // EditText fields from Form 2 (Email Verification)
    private EditText digit1, digit2, digit3, digit4, digit5;

    // Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseFunctions mFunctions;

    // Variables
    private int currPage = 1;
    private int totalPages = 2;

    private String expectedVerificationCode = ""; // This will be sent via email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_registration);

        // Initializing Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFunctions = FirebaseFunctions.getInstance();

        // ImageViews
        userTypeGeneral = findViewById(R.id.user_type_general);
        userTypeBusiness = findViewById(R.id.user_type_business);
        userTypeGeneral.setOnClickListener(this);
        userTypeBusiness.setOnClickListener(this);
        // TextViews
        loginOption = findViewById(R.id.login_opt);
        loginOption.setOnClickListener(this);
        // ViewFlipper
        genRegFormViewFlipper = findViewById(R.id.reg_form_viewflipper);
        // Linear Layouts
        nextBttn = findViewById(R.id.next_bttn);
        prevBttn = findViewById(R.id.prev_bttn);
        nextBttn.setOnClickListener(this);
        prevBttn.setOnClickListener(this);
        // Buttons
        submitBttn = findViewById(R.id.submit_bttn);
        submitBttn.setOnClickListener(this);

        // Initializing EditText fields with your specific IDs
        usernameEditText = findViewById(R.id.register_username_et);
        passwordEditText = findViewById(R.id.register_password_et);
        reenterPasswordEditText = findViewById(R.id.register_repassword_et);
        emailEditText = findViewById(R.id.register_email);

        // Initializing EditText fields for Form 2 (Email Verification)
        digit1 = findViewById(R.id.digit1);
        digit2 = findViewById(R.id.digit2);
        digit3 = findViewById(R.id.digit3);
        digit4 = findViewById(R.id.digit4);
        digit5 = findViewById(R.id.digit5);

        // Set auto-advance for digit fields
        setDigitAutoAdvance();

        // Set Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Updating button visibility based on current page
        updateNavigationButtons();
    }

    // Setup auto-advance between digit fields
    private void setDigitAutoAdvance() {
        // Digit 1 -> 2
        digit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) {
                    digit2.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        // Digit 2 -> 3
        digit2.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) {
                    digit3.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        // Digit 3 -> 4
        digit3.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) {
                    digit4.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        // Digit 4 -> 5
        digit4.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 1) {
                    digit5.requestFocus();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        // Handle backspace to go back to previous field
        setBackspaceListener(digit2, digit1);
        setBackspaceListener(digit3, digit2);
        setBackspaceListener(digit4, digit3);
        setBackspaceListener(digit5, digit4);

        // Handle "Done" action on last digit
        digit5.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Trigger verification
                verifyCode();
                return true;
            }
            return false;
        });
    }

    private void setBackspaceListener(EditText currentfield, EditText previousfield) {
        currentfield.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (currentfield.getText().toString().isEmpty()) {
                    previousfield.requestFocus();
                    previousfield.setText("");
                }
            }
            return false;
        });
    }

    // Form Previous Page Function
    private void prevPage() {
        if (currPage > 1) {
            genRegFormViewFlipper.showPrevious();
            currPage--;
            updateNavigationButtons();
        }
    }

    // Form Next Page Function
    private void nextPage() {
        // Validating form 1 before moving to form 2
        if (currPage == 1){
            if (!validatePage1()) {
                return; // Don't proceed if validation fails
            }

            // Instead of showing a demo code, register the user immediately
            // and let Firebase handle email verification
            registerUser();
        }
    }

    // Register user with Firebase
    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Show loading indicator
        submitBttn.setEnabled(false);
        submitBttn.setText("Registering...");

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Send email verification
                        sendEmailVerification(user);

                        // Save user data to Firestore
                        saveUserToFirestore(user.getUid(), username, email);
                    } else {
                        // Registration failed
                        registrationFailed(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    // Send Firebase email verification
    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(GeneralRegistration.this,
                                "Verification email sent to " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(GeneralRegistration.this,
                                "Failed to send verification email.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Save user data to Firestore
    protected void saveUserToFirestore(String userId, String username, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("userType", "general");
        userData.put("emailVerified", true); // Set to true since we verified with code
        userData.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GeneralRegistration.this,
                            "Registration successful!", Toast.LENGTH_LONG).show();
                    navigateToLogin();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GeneralRegistration.this,
                            "Account created but failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    navigateToLogin();
                });
    }

    // Update navigation buttons based on current page
    private void updateNavigationButtons() {
        // Show/hide next button
        if (currPage == totalPages) {
            nextBttn.setVisibility(View.GONE);
            submitBttn.setVisibility(View.VISIBLE);
        } else {
            nextBttn.setVisibility(View.VISIBLE);
            submitBttn.setVisibility(View.GONE);
        }

        // Show/hide previous button
        if (currPage == 1) {
            prevBttn.setVisibility(View.GONE);
        } else {
            prevBttn.setVisibility(View.VISIBLE);
        }
    }

    // Validating Form 1 fields
    private boolean validatePage1() {
        boolean isValid = true;

        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String reenterPassword = reenterPasswordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            isValid = false;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter valid email");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!password.equals(reenterPassword)) {
            reenterPasswordEditText.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    // Send verification code to email
    private void sendVerificationCode() {
        String email = emailEditText.getText().toString().trim();

        expectedVerificationCode = String.valueOf((int) (Math.random() * 90000) + 10000);

        Toast.makeText(this, "Demo mode - Verification code: " + expectedVerificationCode, Toast.LENGTH_LONG).show();

        // Show loading
        nextBttn.setEnabled(false);
        nextBttn.setAlpha(0.5f);

        // Call Cloud Function to send email
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("code", expectedVerificationCode);

        mFunctions.getHttpsCallable("sendVerificationCode")
                .call(data)
                .addOnCompleteListener(task -> {
                    nextBttn.setEnabled(true);
                    nextBttn.setAlpha(1.0f);

                    if (task.isSuccessful()) {
                        Toast.makeText(GeneralRegistration.this,
                                "Verification code sent to " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(GeneralRegistration.this,
                                "Failed to send code: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Clear all digit fields
    private void clearDigitFields() {
        digit1.setText("");
        digit2.setText("");
        digit3.setText("");
        digit4.setText("");
        digit5.setText("");
        digit1.requestFocus();
    }

    // Get entered verfication code
    private String getEnteredCode() {
        return digit1.getText().toString() +
                digit2.getText().toString() +
                digit3.getText().toString() +
                digit4.getText().toString() +
                digit5.getText().toString();
    }

    // Verify the entered code
    private boolean verifyCode() {
        String enteredCode = getEnteredCode();
        String email = emailEditText.getText().toString().trim();

        if (enteredCode.length() < 5) {
            Toast.makeText(this, "Please enter the complete 5-digit code", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Show loading
        submitBttn.setEnabled(false);
        submitBttn.setText("Verifying...");

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("code", enteredCode);

        // Call Cloud Function to verify code
        mFunctions.getHttpsCallable("verifyCode")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();

                        // Now register the user with Firebase Auth
                        registerUserAfterVerification();
                    } else {
                        submitBttn.setEnabled(true);
                        submitBttn.setText("Submit");
                        clearDigitFields();

                        Exception e = task.getException();
                        String errorMsg = e != null ? e.getMessage() : "Invalid code";

                        if (errorMsg.contains("expired")) {
                            Toast.makeText(this, "Code expired. Please request a new one.", Toast.LENGTH_LONG).show();
                            // Option to resend
                        } else {
                            Toast.makeText(this, "Invalid verification code", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        return false; // Return false immediately, actual result comes from callback
    }

    private void resendCode() {
        // Clear digit fields
        clearDigitFields();

        // Generate new code and send again
        sendVerificationCode();
    }

    private void registerUserAfterVerification() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        submitBttn.setText("Registering...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Mark email as verified in Firebase Auth
                        // Note: Firebase Auth doesn't let us manually set emailVerified
                        // So we'll track it in Firestore

                        saveUserToFirestore(user.getUid(), username, email);
                    } else {
                        registrationFailed(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    // Handle resistration failure
    private void registrationFailed(String errorMessage) {
        Toast.makeText(GeneralRegistration.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
        submitBttn.setEnabled(true);
        submitBttn.setText("Submit");
    }

    // Navigate to Login
    private void navigateToLogin() {
        Intent intent = new Intent(GeneralRegistration.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.user_type_general) {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "General User", Toast.LENGTH_SHORT).show();
            }
        }
        else if (bttn_id == R.id.user_type_business) {
            Intent intent = new Intent(GeneralRegistration.this, BusinessRegistration.class);
            startActivity(intent);
        }
        else if (bttn_id == R.id.login_opt) {
            // Return to Login
            navigateToLogin();
        }
        else if (bttn_id == R.id.next_bttn) {
            nextPage();
        }
        else if (bttn_id == R.id.prev_bttn) {
            prevPage();
        }
        else if (bttn_id == R.id.submit_bttn) {
            registerUser();
        }

    }
}