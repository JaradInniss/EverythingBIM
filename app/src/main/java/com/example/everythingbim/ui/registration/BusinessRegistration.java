package com.example.everythingbim.ui.registration;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.File;
import com.example.everythingbim.databinding.ActivityBusinessRegistrationBinding;
import com.example.everythingbim.databinding.BusinessRegisForm1Binding;
import com.example.everythingbim.databinding.BusinessRegisForm2Binding;
import com.example.everythingbim.databinding.BusinessRegisForm3Binding;
import com.example.everythingbim.databinding.BusinessRegisForm4Binding;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.utils.FileAdapter;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class BusinessRegistration extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "BusinessRegistration";
    private static final String PREF_BUSINESS_REG_SCROLL_HINT_SEEN =
            KeyboardScrollHintHelper.PREF_BUSINESS_REG_SCROLL_HINT_SEEN;
    private BusinessRegViewModel viewModel;
    private ActivityBusinessRegistrationBinding binding;
    private BusinessRegisForm1Binding form1Binding;
    private BusinessRegisForm2Binding form2Binding;
    private BusinessRegisForm3Binding form3Binding;
    private BusinessRegisForm4Binding form4Binding;


    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption, imgUploadCount, fileUploadCount;
    private ViewFlipper busRegFormViewFlipper;
    private LinearLayout generalUserContainer, businessUserContainer, nextBttn1, nextBttn2, nextBttn3, prevBttn1, prevBttn2, prevBttn3, form1ErrorLayout, form3ErrorLayout;
    private RecyclerView imgIconContainer, fileIconContainer;
    private Button submitBttn;
    private ImageButton uploadImgBttn, uploadFileBttn;
    private ProgressBar progressBar;
    private TextView form1RegErrorTv, form3RegErrorTv, errorTextView;

    // Form fields (match IDs from layouts)
    private TextInputEditText businessUsername, businessEmailEt, businessNameEt, contactNumberEt, businessAddressEt, businessDescriptionEt, passwordEt, rePasswordEt;
    private Spinner businessTypeSpinner;

    // Adapters
    private FileAdapter imageAdapter, fileAdapter;

    // Constants
    private static final int MEDIA_PERMISSION_REQUEST_CODE = 100;
    private static final int FILE_PICKER_IMAGE_REQUEST_CODE = 105;
    private static final int FILE_PICKER_FILE_REQUEST_CODE = 110;

    private EditText digit1, digit2, digit3, digit4, digit5;
    private ImageView checkIcon, warningIcon;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentKeyboardExtraBottom = 0;
    private boolean hasAttemptedVerification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize binding
        binding = ActivityBusinessRegistrationBinding.inflate(getLayoutInflater());
        form1Binding = binding.businessRegisForm1;
        form2Binding = binding.businessRegisForm2;
        form3Binding = binding.businessRegisForm3;
        form4Binding = binding.businessRegisForm4;

        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(BusinessRegViewModel.class);

        initViews();
        setupObservers();
        setupKeyboardInsets();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupKeyboardInsets() {
        int initialLeft = binding.businessRegistrationScroll.getPaddingLeft();
        int initialTop = binding.businessRegistrationScroll.getPaddingTop();
        int initialRight = binding.businessRegistrationScroll.getPaddingRight();
        int initialBottom = binding.businessRegistrationScroll.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                binding.getRoot(),
                binding.businessRegistrationScroll,
                binding.businessRegistrationScroll,
                PREF_BUSINESS_REG_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> {
                    currentKeyboardExtraBottom = keyboardExtraBottom;
                    binding.businessRegistrationScroll.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + keyboardExtraBottom
                    );
                }
        );
    }

    private void initViews() {
        // TextViews
        loginOption = form1Binding.loginOpt;
        loginOption.setOnClickListener(this);
        imgUploadCount = form4Binding.imgUploadCount;
        fileUploadCount = form4Binding.fileUploadCount;
        form1RegErrorTv = form1Binding.form1RegErrorTv;
        form3RegErrorTv = form3Binding.form3RegErrorTv;

        // ViewFlipper
        busRegFormViewFlipper = binding.regFormViewflipper;

        // Linear Layouts
        generalUserContainer = binding.generalUserContainer;
        businessUserContainer = binding.businessUserContainer;
        generalUserContainer.setOnClickListener(this);
        businessUserContainer.setOnClickListener(this);

        nextBttn1 = form1Binding.nextBttn1;
        nextBttn2 = form2Binding.nextBttn2;
        nextBttn3 = form3Binding.nextBttn3;
        nextBttn1.setOnClickListener(this);
        nextBttn2.setOnClickListener(this);
        nextBttn3.setOnClickListener(this);

        prevBttn1 = form2Binding.prevBttn1;
        prevBttn2 = form3Binding.prevBttn2;
        prevBttn3 = form4Binding.prevBttn3;
        prevBttn1.setOnClickListener(this);
        prevBttn2.setOnClickListener(this);
        prevBttn3.setOnClickListener(this);

        form1ErrorLayout = form1Binding.regErrorLayout;
        form3ErrorLayout = form3Binding.regErrorLayout;

        // Buttons
        submitBttn = findViewById(R.id.submit_bttn);
        submitBttn.setOnClickListener(this);
        uploadImgBttn = findViewById(R.id.upload_img_bttn);
        uploadImgBttn.setOnClickListener(this);
        uploadFileBttn = findViewById(R.id.upload_file_bttn);
        uploadFileBttn.setOnClickListener(this);

        // Progress bar
        progressBar = findViewById(R.id.progress_bar);

        // Form fields – IDs from the included layouts
        businessUsername = form1Binding.businessRegisterUsernameEt;
        businessEmailEt = form1Binding.businessRegisterEmailEt;
        passwordEt = form1Binding.businessRegisterPasswordEt;
        rePasswordEt = form1Binding.businessRegisterRepasswordEt;
        businessNameEt = form3Binding.registerBusinessNameEt;
        contactNumberEt = form3Binding.registerContactNumberEt;
        businessAddressEt = form3Binding.registerBusinessAddressEt;
        businessDescriptionEt = form3Binding.registerBusinessDescriptionEt;
        businessTypeSpinner = form3Binding.businessTypeSpinner;

        // Setup business type dropdown
        setupBusinessTypeDropdown();

        // RecyclerViews
        imgIconContainer = form4Binding.imgIconContainer;
        imgIconContainer.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fileIconContainer = form4Binding.fileIconContainer;
        fileIconContainer.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Adapters
        imageAdapter = new FileAdapter(this, new ArrayList<>(), position -> viewModel.removeImage(position));
        fileAdapter = new FileAdapter(this, new ArrayList<>(), position -> viewModel.removeFile(position));
        imgIconContainer.setAdapter(imageAdapter);
        fileIconContainer.setAdapter(fileAdapter);

        // Digit fields
        digit1 = form2Binding.digit1;
        digit2 = form2Binding.digit2;
        digit3 = form2Binding.digit3;
        digit4 = form2Binding.digit4;
        digit5 = form2Binding.digit5;
        checkIcon = form2Binding.checkIcon;
        warningIcon = form2Binding.warningIcon;
        checkIcon.setVisibility(View.GONE);
        warningIcon.setVisibility(View.GONE);

        // Auto‑advance and backspace handling
        setDigitAutoAdvance();
        setupFocusedFieldScroll();
        updateSubmitButtonState(false);
    }

    private void updateSubmitButtonState(boolean isSubmitting) {
        if (submitBttn == null) {
            return;
        }
        submitBttn.setEnabled(!isSubmitting);
        submitBttn.setText(isSubmitting ? "Processing..." : "SUBMIT");
        submitBttn.setAlpha(isSubmitting ? 0.7f : 1f);
    }

    private void setupFocusedFieldScroll() {
        bindFocusScroll(form1Binding.businessRegisterUsernameEt, form1Binding.businessRegisterUsernameEt);
        bindFocusScroll(form1Binding.businessRegisterEmailEt, form1Binding.businessRegisterEmailEt);
        bindFocusScroll(form1Binding.businessRegisterPasswordEt, form1Binding.businessRegisterPasswordEt);
        bindFocusScroll(form1Binding.businessRegisterRepasswordEt, form1Binding.nextBttn1);

        bindFocusScroll(form2Binding.digit1, form2Binding.nextBttn2);
        bindFocusScroll(form2Binding.digit2, form2Binding.nextBttn2);
        bindFocusScroll(form2Binding.digit3, form2Binding.nextBttn2);
        bindFocusScroll(form2Binding.digit4, form2Binding.nextBttn2);
        bindFocusScroll(form2Binding.digit5, form2Binding.nextBttn2);

        bindFocusScroll(form3Binding.registerBusinessNameEt, form3Binding.registerBusinessNameEt);
        bindFocusScroll(form3Binding.registerContactNumberEt, form3Binding.registerContactNumberEt);
        bindFocusScroll(form3Binding.registerBusinessAddressEt, form3Binding.registerBusinessAddressEt);
        bindFocusScroll(form3Binding.registerBusinessDescriptionEt, form3Binding.nextBttn3);
    }

    private void bindFocusScroll(View focusedView, View anchorView) {
        focusedView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollAnchorAboveKeyboard(anchorView);
            }
        });
    }

    private void scrollAnchorAboveKeyboard(View anchorView) {
        binding.businessRegistrationScroll.post(() -> {
            if (currentKeyboardExtraBottom <= 0) {
                return;
            }
            if (!isDescendant(binding.businessRegistrationScroll, anchorView)) {
                return;
            }

            Rect rect = new Rect();
            anchorView.getDrawingRect(rect);
            binding.businessRegistrationScroll.offsetDescendantRectToMyCoords(anchorView, rect);

            int visibleHeight = binding.businessRegistrationScroll.getHeight() - currentKeyboardExtraBottom;
            int desiredBottomMargin = dpToPx(24);
            int targetBottom = visibleHeight - desiredBottomMargin;
            int delta = rect.bottom - targetBottom;
            if (delta > 0) {
                binding.businessRegistrationScroll.smoothScrollBy(0, delta);
            }
        });
    }

    private boolean isDescendant(ViewGroup parent, View child) {
        ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void setupBusinessTypeDropdown() {
        if (businessTypeSpinner == null) {
            Log.e(TAG, "setupBusinessTypeDropdown: businessTypeSpinner is null!");
            return;
        }
        final String[] BUSINESS_TYPES = {
                "Select Business Type",
                "Restaurant",
                "Retail",
                "Technology",
                "Healthcare",
                "Construction",
                "Finance"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, BUSINESS_TYPES) {

            @Override public boolean isEnabled(int position) { return position != 0; }

            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                // Make the spinner display text black
                ((android.widget.TextView) view).setTextColor(android.graphics.Color.BLACK);
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(android.graphics.Color.BLACK);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        businessTypeSpinner.setAdapter(adapter);

        // Set listener to update ViewModel when selection changes
        businessTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) { // Skip placeholder
                    String selected = BUSINESS_TYPES[position];
                    viewModel.getBusinessType().setValue(selected);
                    Log.d(TAG, "Business type selected: " + selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.getBusinessType().setValue("");
            }
        });
    }

    private void setDigitAutoAdvance() {
        // Auto‑advance to next field when a digit is entered
        digit1.addTextChangedListener(new SimpleTextWatcher(() -> digit2.requestFocus()));
        digit2.addTextChangedListener(new SimpleTextWatcher(() -> digit3.requestFocus()));
        digit3.addTextChangedListener(new SimpleTextWatcher(() -> digit4.requestFocus()));
        digit4.addTextChangedListener(new SimpleTextWatcher(() -> digit5.requestFocus()));
        attachVerificationStateWatcher(digit1);
        attachVerificationStateWatcher(digit2);
        attachVerificationStateWatcher(digit3);
        attachVerificationStateWatcher(digit4);
        attachVerificationStateWatcher(digit5);

        // Backspace: clear current field and move to previous if empty
        setBackspaceListener(digit2, digit1);
        setBackspaceListener(digit3, digit2);
        setBackspaceListener(digit4, digit3);
        setBackspaceListener(digit5, digit4);

        // On last digit, when Done is pressed, trigger verification
        digit5.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyCodeAndProceed();
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

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onLengthOne;
        SimpleTextWatcher(Runnable onLengthOne) { this.onLengthOne = onLengthOne; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { if (s.length() == 1) onLengthOne.run(); }
    }

    private String getEnteredCode() {
        return digit1.getText().toString() +
                digit2.getText().toString() +
                digit3.getText().toString() +
                digit4.getText().toString() +
                digit5.getText().toString();
    }

    private void attachVerificationStateWatcher(@NonNull EditText field) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateVerificationIndicators();
            }
        });
    }

    private void updateVerificationIndicators() {
        String enteredCode = getEnteredCode();
        if (enteredCode.isEmpty() || enteredCode.length() < 5) {
            hasAttemptedVerification = false;
            checkIcon.setVisibility(View.GONE);
            warningIcon.setVisibility(View.GONE);
            return;
        }

        boolean isValid = Boolean.TRUE.equals(viewModel.getIsCodeValid().getValue());
        if (isValid) {
            checkIcon.setVisibility(View.VISIBLE);
            warningIcon.setVisibility(View.GONE);
            return;
        }

        checkIcon.setVisibility(View.GONE);
        warningIcon.setVisibility(hasAttemptedVerification ? View.VISIBLE : View.GONE);
    }

    private void clearDigitFields() {
        digit1.setText("");
        digit2.setText("");
        digit3.setText("");
        digit4.setText("");
        digit5.setText("");
        digit1.requestFocus();
    }

    private void verifyCodeAndProceed() {
        String enteredCode = getEnteredCode();
        hasAttemptedVerification = enteredCode.length() >= 5;
        boolean success = viewModel.verifyAndProceed(enteredCode);
        if (!success) {
            clearDigitFields();
        } else {
            updateVerificationIndicators();
        }
    }

    private void setupObservers() {
        // Observe page changes
        viewModel.getCurrentPage().observe(this, page -> {
            if (page != null) busRegFormViewFlipper.setDisplayedChild(page);
        });

        // Observe file lists
        viewModel.getImageList().observe(this, images -> {
            imageAdapter.updateList(images);
            if (images.size() == 0 || images.isEmpty()) {
                imgUploadCount.setText("");
            } else {
                imgUploadCount.setText(String.format(Locale.US, "| %d", images.size()));
            }
        });

        // Observe file lists
        viewModel.getFileList().observe(this, files -> {
            fileAdapter.updateList(files);
            if (files.size() == 0 || files.isEmpty()) {
                fileUploadCount.setText("");
            } else {
                fileUploadCount.setText(String.format(Locale.US, "| %d", files.size()));
            }
        });

        // Loading and error states
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
            updateSubmitButtonState(Boolean.TRUE.equals(isLoading));
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Registration error: " + error);
            }
        });

        // Navigation
        viewModel.getNavigationEvent().observe(this, destination -> {
            if (destination != null) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BusinessRegistration.this, destination);
                String pendingAction = getIntent().getStringExtra(MainActivity.EXTRA_PENDING_ACTION);
                if (pendingAction != null && destination.equals(Login.class)) {
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, pendingAction);
                }
                startActivity(intent);
                finish();
            }
        });

        // Observe code validation result to show/hide icons
        viewModel.getIsCodeValid().observe(this, isValid -> {
            updateVerificationIndicators();
        });

        // Observe info messages (like the demo code)
        viewModel.getInfoMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        // Observe error messages
        viewModel.getErrorFields().observe(this, errors -> {
            // Get current active form bindings based on page
            handler.removeCallbacksAndMessages(null);

            int pageNum = viewModel.getCurrentPage().getValue() != null ? viewModel.getCurrentPage().getValue() : 0;

            // 1. Identify the current page's Error UI components
            LinearLayout currentErrorLayout;
            TextView currentErrorTv;

            switch (pageNum) {
                case 0:
                    currentErrorLayout = form1Binding.regErrorLayout;
                    currentErrorTv = form1Binding.form1RegErrorTv;
                    break;
                case 1:
                    currentErrorLayout = form2Binding.regErrorLayout;
                    currentErrorTv = form2Binding.form2RegErrorTv;
                    break;
                case 2:
                    currentErrorLayout = form3Binding.regErrorLayout;
                    currentErrorTv = form3Binding.form3RegErrorTv;
                    break;
                case 3:
                    currentErrorLayout = form4Binding.regErrorLayout;
                    currentErrorTv = form4Binding.form4RegErrorTv;
                    break;
                default:
                    return;
            }

            if (errors == null || errors.isEmpty()) {
                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                currentErrorLayout.setVisibility(View.GONE);
                resetAllFieldErrors(); // Helper method defined below
                return;
            }

            Map.Entry<Integer, String> firstError = errors.entrySet().iterator().next();
            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
            currentErrorLayout.setVisibility(View.VISIBLE);
            currentErrorTv.setText(firstError.getValue());

            // 3. Update icons for EVERY field currently in error
            for (Map.Entry<Integer, String> entry : errors.entrySet()) {
                TextInputLayout til = getTextInputLayoutById(entry.getKey());
                if (til != null) {
                    updateEndIcon(til, entry.getValue());
                }
            }

            // 4. Auto-hide timer
            handler.postDelayed(() -> {
                TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
                currentErrorLayout.setVisibility(View.GONE);

                // CRITICAL: Reset ALL icons on this page after the 2 seconds
                resetAllFieldErrors();
            }, 2000);
        });
    }

    private void updateEndIcon(TextInputLayout til, String error) {
        if (til == null) return;

        // Check if it's a password-style field (needs toggle)
        boolean isPasswordField = (til == form1Binding.tilBusinessRegisterPassword ||
                til == form1Binding.tilBusinessRegisterRepassword);

        if (error != null) {
            // Show Warning Icon
            til.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            til.setEndIconDrawable(ContextCompat.getDrawable(this, R.drawable.ic_warning_circle));
            til.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.dark_amaranth));
        } else {
            // Reset to Default
            if (isPasswordField) {
                til.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                til.setEndIconTintList(ContextCompat.getColorStateList(this, R.color.black));
            } else {
                til.setEndIconMode(TextInputLayout.END_ICON_NONE);
            }
        }
    }

    private TextInputLayout getTextInputLayoutById(int id) {
        if (id == R.id.business_register_username_et) return form1Binding.tilBusinessRegisterUsername;
        if (id == R.id.business_register_email_et) return form1Binding.tilBusinessRegisterEmail;
        if (id == R.id.business_register_password_et) return form1Binding.tilBusinessRegisterPassword;
        if (id == R.id.business_register_repassword_et) return form1Binding.tilBusinessRegisterRepassword;

        if (id == R.id.register_business_name_et) return form3Binding.tilRegisterBusinessNameEt;
        if (id == R.id.register_contact_number_et) return form3Binding.tilRegisterContactNumberEt;
        if (id == R.id.register_business_address_et) return form3Binding.tilRegisterBusinessAddressEt;
        if (id == R.id.register_business_description_et) return form3Binding.tilRegisterBusinessDescriptionEt;

        return null;
    }

    private void resetAllFieldErrors() {
        TextInputLayout[] allLayouts = {
                form1Binding.tilBusinessRegisterUsername, form1Binding.tilBusinessRegisterEmail,
                form1Binding.tilBusinessRegisterPassword, form1Binding.tilBusinessRegisterRepassword,
                form3Binding.tilRegisterBusinessNameEt, form3Binding.tilRegisterContactNumberEt,
                form3Binding.tilRegisterBusinessAddressEt, form3Binding.tilRegisterBusinessDescriptionEt
        };
        for (TextInputLayout til : allLayouts) {
            if (til != null) {
                til.setError(null);
                updateEndIcon(til, null);
            }
        }
    }

    // --- Permission handling ---
    private void requestFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO},
                    MEDIA_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MEDIA_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MEDIA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- File/Image selection ---
    private void handleImageUpload() {
        if (hasFilePermissions()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Images"), FILE_PICKER_IMAGE_REQUEST_CODE);
        } else {
            requestFilePermissions();
        }
    }

    private void handleFileUpload() {
        if (hasFilePermissions()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Files"), FILE_PICKER_FILE_REQUEST_CODE);
        } else {
            requestFilePermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == FILE_PICKER_IMAGE_REQUEST_CODE) {
            processSelectedFiles(data, true);
        } else if (requestCode == FILE_PICKER_FILE_REQUEST_CODE) {
            processSelectedFiles(data, false);
        }
    }

    private void processSelectedFiles(Intent data, boolean isImage) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                addFileToViewModel(uri, isImage);
            }
        } else if (data.getData() != null) {
            addFileToViewModel(data.getData(), isImage);
        }
    }

    private void addFileToViewModel(Uri uri, boolean isImage) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            cursor.close();
            String mimeType = getContentResolver().getType(uri);

            File file = new File(name, mimeType, uri, size);
            if (isImage) {
                viewModel.addImage(file);
            } else {
                viewModel.addFile(file);
            }
        }
    }

    // --- Click handling ---
    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.general_user_container) {
            viewModel.navigateTo(GeneralRegistration.class);
        } else if (id == R.id.business_user_container) {
            Toast.makeText(this, "Currently Business User", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.login_opt) {
            viewModel.navigateTo(Login.class);
        } else if (id == R.id.upload_img_bttn) {
            handleImageUpload();
        } else if (id == R.id.upload_file_bttn) {
            handleFileUpload();
        } else if (id == R.id.next_bttn1 || id == R.id.next_bttn2 || id == R.id.next_bttn3) {
            int currentPage = viewModel.getCurrentPage().getValue() != null ? viewModel.getCurrentPage().getValue() : 0;
            if (currentPage == 0) {
                // Page 1 is the verification page – verify code first
                boolean valid = viewModel.isPageValid(0,
                        businessUsername.getText().toString(),
                        businessEmailEt.getText().toString(),
                        passwordEt.getText().toString(),
                        rePasswordEt.getText().toString()
                );
                if (valid) viewModel.nextPage();

            } else if (currentPage == 1) {
                verifyCodeAndProceed();
            } else if (currentPage == 2) {
                boolean valid = viewModel.isPageValid(2,
                        businessNameEt.getText().toString(),
                        contactNumberEt.getText().toString(),
                        businessAddressEt.getText().toString(),
                        businessDescriptionEt.getText().toString()
                );
                if (valid) viewModel.nextPage();
            }
        } else if (id == R.id.prev_bttn1 || id == R.id.prev_bttn2 || id == R.id.prev_bttn3) {
            viewModel.prevPage();
        } else if (id == R.id.submit_bttn) {
            updateSubmitButtonState(true);
            String email = businessEmailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();
            String confirm = rePasswordEt.getText().toString().trim();

            if (!password.equals(confirm)) {
                updateSubmitButtonState(false);
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass values to ViewModel
            viewModel.getUsername().setValue(businessUsername.getText().toString().trim());
            viewModel.getCompanyName().setValue(businessNameEt.getText().toString().trim());
            viewModel.getBusinessEmail().setValue(email);
            viewModel.getBusinessType().setValue(
                    businessTypeSpinner != null && businessTypeSpinner.getSelectedItem() != null
                    ? businessTypeSpinner.getSelectedItem().toString().trim() : ""
            );
            viewModel.getPhone().setValue(contactNumberEt.getText().toString().trim());
            viewModel.getAddress().setValue(businessAddressEt.getText().toString().trim());
            viewModel.getDescription().setValue(businessDescriptionEt.getText().toString().trim());

            viewModel.registerBusiness(email, password);
        }
    }
}
