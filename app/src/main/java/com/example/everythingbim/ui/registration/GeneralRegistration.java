package com.example.everythingbim.ui.registration;
import com.example.everythingbim.ui.login.Login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import com.example.everythingbim.R;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

//import com.example.everythingbim.ui.registration.GeneralRegViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GeneralRegistration extends AppCompatActivity implements View.OnClickListener {

    private GeneralRegViewModel viewModel;

    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption;
    private ViewFlipper genRegFormViewFlipper;
    private LinearLayout nextBttn, prevBttn;
    private Button submitBttn;

    // EditText fields from Form 1
    private EditText usernameEditText, passwordEditText, reenterPasswordEditText, emailEditText;

    // EditText fields from Form 2 (Email Verification)
    private EditText digit1, digit2, digit3, digit4, digit5;

    // Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Verification code (temporary, ideally should be in ViewModel)
    private String expectedVerificationCode = "";

    // Maximum pages
    private static final int TOTAL_PAGES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_general_registration);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(GeneralRegViewModel.class);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupObservers();

        // Set Window Insets
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

        // Form fields
        usernameEditText = findViewById(R.id.register_username_et);
        passwordEditText = findViewById(R.id.register_password_et);
        reenterPasswordEditText = findViewById(R.id.register_repassword_et);
        emailEditText = findViewById(R.id.register_email);

        // Digit fields for verification
        digit1 = findViewById(R.id.digit1);
        digit2 = findViewById(R.id.digit2);
        digit3 = findViewById(R.id.digit3);
        digit4 = findViewById(R.id.digit4);
        digit5 = findViewById(R.id.digit5);

        // Auto‑advance and backspace logic
        setDigitAutoAdvance();
    }

    private void setupObservers() {
        // Observe current page and update UI
        viewModel.getCurrentPage().observe(this, page -> {
            if (page != null) {
                genRegFormViewFlipper.setDisplayedChild(page);
                updateNavigationButtons(page);
            }
        });

        // Observe navigation events (one‑time)
        viewModel.getNavigationEvent().observe(this, destination -> {
            if (destination != null) {
                // For login, clear the back stack
                if (destination.equals(Login.class)) {
                    Intent intent = new Intent(GeneralRegistration.this, destination);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    startActivity(new Intent(GeneralRegistration.this, destination));
                }
            }
        });
    }

    private void updateNavigationButtons(int currentPage) {
        // Show/hide next and submit buttons
        if (currentPage == TOTAL_PAGES - 1) { // zero‑based index, last page is TOTAL_PAGES-1
            nextBttn.setVisibility(View.GONE);
            submitBttn.setVisibility(View.VISIBLE);
        } else {
            nextBttn.setVisibility(View.VISIBLE);
            submitBttn.setVisibility(View.GONE);
        }

        // Show/hide previous button
        prevBttn.setVisibility(currentPage == 0 ? View.GONE : View.VISIBLE);
    }

    // --- Form Validation & Navigation Logic ---

    // Called when Next button is clicked
    private void onNextClicked() {
        int currentPage = viewModel.getCurrentPage().getValue() != null ? viewModel.getCurrentPage().getValue() : 0;
        if (currentPage == 0) {
            // Validate page 1 before moving to page 2
            if (validatePage1()) {
                // Generate and send verification code (demo only)
                sendVerificationCode();
                clearDigitFields();
                viewModel.nextPage();
            }
        } else {
            viewModel.nextPage();
        }
    }

    // Called when Previous button is clicked
    private void onPrevClicked() {
        viewModel.prevPage();
    }

    // Called when Submit button is clicked
    private void onSubmitClicked() {
        registerUser();
    }

    // Validation for form page 1 (username, email, password)
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

    // Demo verification code (in real app, this should be sent via email)
    private void sendVerificationCode() {
//        String email = emailEditText.getText().toString().trim();
        expectedVerificationCode = String.valueOf((int) (Math.random() * 90000) + 10000);
        Toast.makeText(this, "Demo: Verification code is " + expectedVerificationCode, Toast.LENGTH_LONG).show();
    }

    private void clearDigitFields() {
        digit1.setText("");
        digit2.setText("");
        digit3.setText("");
        digit4.setText("");
        digit5.setText("");
        digit1.requestFocus();
    }

    private String getEnteredCode() {
        return digit1.getText().toString() +
                digit2.getText().toString() +
                digit3.getText().toString() +
                digit4.getText().toString() +
                digit5.getText().toString();
    }

    private boolean verifyCode() {
        String enteredCode = getEnteredCode();
        if (enteredCode.length() < 5) {
            Toast.makeText(this, "Please enter the complete 5-digit code", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (enteredCode.equals(expectedVerificationCode)) {
            Toast.makeText(this, "Email verification successful", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(this, "Invalid verification code", Toast.LENGTH_SHORT).show();
            clearDigitFields();
            return false;
        }
    }

    private void registerUser() {
        if (!verifyCode()) {
            return;
        }

        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        submitBttn.setEnabled(false);
        submitBttn.setText("Processing...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), username, email);
                            sendFirebaseEmailVerification(user);
                        }
                    } else {
                        registrationFailed(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    private void saveUserToFirestore(String userId, String username, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        userData.put("userType", "general");
        userData.put("emailVerified", true);  // you may want to set this after email verification
        userData.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GeneralRegistration.this, "Registration successful", Toast.LENGTH_LONG).show();
                    // Navigate to login (via ViewModel to keep event handling consistent)
                    viewModel.navigateTo(Login.class);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GeneralRegistration.this, "Account created but failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    viewModel.navigateTo(Login.class);
                });
    }

    private void sendFirebaseEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email verification sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to send email verification", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registrationFailed(String errorMessage) {
        Toast.makeText(GeneralRegistration.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
        submitBttn.setEnabled(true);
        submitBttn.setText("Submit");
    }

    // --- Digit Field Auto-Advance ---
    private void setDigitAutoAdvance() {
        // Forward auto‑advance
        digit1.addTextChangedListener(new SimpleTextWatcher(() -> digit2.requestFocus()));
        digit2.addTextChangedListener(new SimpleTextWatcher(() -> digit3.requestFocus()));
        digit3.addTextChangedListener(new SimpleTextWatcher(() -> digit4.requestFocus()));
        digit4.addTextChangedListener(new SimpleTextWatcher(() -> digit5.requestFocus()));

        // Backspace handling
        setBackspaceListener(digit2, digit1);
        setBackspaceListener(digit3, digit2);
        setBackspaceListener(digit4, digit3);
        setBackspaceListener(digit5, digit4);

        // Done action on last digit
        digit5.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSubmitClicked(); // or verifyCode() – depends on your flow
                return true;
            }
            return false;
        });
    }

    private void setBackspaceListener(EditText current, EditText previous) {
        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (current.getText().toString().isEmpty()) {
                    previous.requestFocus();
                    previous.setText("");
                }
            }
            return false;
        });
    }

    // Helper for text changes
    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onLengthOne;
        SimpleTextWatcher(Runnable onLengthOne) { this.onLengthOne = onLengthOne; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { if (s.length() == 1) onLengthOne.run(); }
    }

    // --- OnClick Handling ---
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.user_type_general) {
            Toast.makeText(this, "General User", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.user_type_business) {
            viewModel.navigateTo(BusinessRegistration.class);
        } else if (id == R.id.login_opt) {
            viewModel.navigateTo(Login.class);
        } else if (id == R.id.next_bttn) {
            onNextClicked();
        } else if (id == R.id.prev_bttn) {
            onPrevClicked();
        } else if (id == R.id.submit_bttn) {
            onSubmitClicked();
        }
    }
}