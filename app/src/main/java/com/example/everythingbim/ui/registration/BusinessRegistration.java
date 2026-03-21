package com.example.everythingbim.ui.registration;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
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
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.utils.FileAdapter;

import java.util.ArrayList;
import java.util.Locale;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

public class BusinessRegistration extends AppCompatActivity implements View.OnClickListener {

    private BusinessRegViewModel viewModel;

    // UI Elements
    private ImageView userTypeGeneral, userTypeBusiness;
    private TextView loginOption, imgUploadCount, fileUploadCount;
    private ViewFlipper busRegFormViewFlipper;
    private LinearLayout nextBttn1, nextBttn2, nextBttn3, prevBttn1, prevBttn2, prevBttn3;
    private RecyclerView imgIconContainer, fileIconContainer;
    private Button submitBttn;
    private ImageButton uploadImgBttn, uploadFileBttn;
    private ProgressBar progressBar;
    private TextView errorTextView;

    // Form fields (match IDs from layouts)
    private EditText businessNameEt, businessEmailEt, contactNumberEt, businessAddressEt, businessDescriptionEt;
    private EditText passwordEt, confirmPasswordEt;

    // Adapters
    private FileAdapter imageAdapter, fileAdapter;

    // Constants
    private static final int MEDIA_PERMISSION_REQUEST_CODE = 100;
    private static final int FILE_PICKER_IMAGE_REQUEST_CODE = 105;
    private static final int FILE_PICKER_FILE_REQUEST_CODE = 110;

    private EditText digit1, digit2, digit3, digit4, digit5;
    private ImageView checkIcon, warningIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_business_registration);

        viewModel = new ViewModelProvider(this).get(BusinessRegViewModel.class);

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
        userTypeBusiness.setOnClickListener(this);
        userTypeGeneral.setOnClickListener(this);
        // TextViews
        loginOption = findViewById(R.id.login_opt);
        loginOption.setOnClickListener(this);
        imgUploadCount = findViewById(R.id.img_upload_count);
        fileUploadCount = findViewById(R.id.file_upload_count);
        // ViewFlipper
        busRegFormViewFlipper = findViewById(R.id.reg_form_viewflipper);

        // Linear Layouts
        nextBttn1 = findViewById(R.id.next_bttn1);
        nextBttn2 = findViewById(R.id.next_bttn2);
        nextBttn3 = findViewById(R.id.next_bttn3);
        nextBttn1.setOnClickListener(this);
        nextBttn2.setOnClickListener(this);
        nextBttn3.setOnClickListener(this);

        prevBttn1 = findViewById(R.id.prev_bttn1);
        prevBttn2 = findViewById(R.id.prev_bttn2);
        prevBttn3 = findViewById(R.id.prev_bttn3);
        prevBttn1.setOnClickListener(this);
        prevBttn2.setOnClickListener(this);
        prevBttn3.setOnClickListener(this);

        // Buttons
        submitBttn = findViewById(R.id.submit_bttn);
        submitBttn.setOnClickListener(this);
        uploadImgBttn = findViewById(R.id.upload_img_bttn);
        uploadImgBttn.setOnClickListener(this);
        uploadFileBttn = findViewById(R.id.upload_file_bttn);
        uploadFileBttn.setOnClickListener(this);

        // Progress bar and error text
        progressBar = findViewById(R.id.progress_bar);
        errorTextView = findViewById(R.id.error_text);

        // Form fields – IDs from the included layouts
        businessNameEt = findViewById(R.id.business_name_et);
        businessEmailEt = findViewById(R.id.business_email_et);
        contactNumberEt = findViewById(R.id.contact_number_et);
        businessAddressEt = findViewById(R.id.business_address_et);
        businessDescriptionEt = findViewById(R.id.business_description_et);
        passwordEt = findViewById(R.id.password_et);
        confirmPasswordEt = findViewById(R.id.confirm_password_et);

        // RecyclerViews
        imgIconContainer = findViewById(R.id.img_icon_container);
        imgIconContainer.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fileIconContainer = findViewById(R.id.file_icon_container);
        fileIconContainer.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Adapters
        imageAdapter = new FileAdapter(this, new ArrayList<>(), position -> viewModel.removeImage(position));
        fileAdapter = new FileAdapter(this, new ArrayList<>(), position -> viewModel.removeFile(position));
        imgIconContainer.setAdapter(imageAdapter);
        fileIconContainer.setAdapter(fileAdapter);

        // Digit fields
        digit1 = findViewById(R.id.digit1);
        digit2 = findViewById(R.id.digit2);
        digit3 = findViewById(R.id.digit3);
        digit4 = findViewById(R.id.digit4);
        digit5 = findViewById(R.id.digit5);
        checkIcon = findViewById(R.id.check_icon);
        warningIcon = findViewById(R.id.warning_icon);

        // Auto‑advance and backspace handling
        setDigitAutoAdvance();
    }

    private void setDigitAutoAdvance() {
        // Auto‑advance to next field when a digit is entered
        digit1.addTextChangedListener(new SimpleTextWatcher(() -> digit2.requestFocus()));
        digit2.addTextChangedListener(new SimpleTextWatcher(() -> digit3.requestFocus()));
        digit3.addTextChangedListener(new SimpleTextWatcher(() -> digit4.requestFocus()));
        digit4.addTextChangedListener(new SimpleTextWatcher(() -> digit5.requestFocus()));

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
        boolean success = viewModel.verifyAndProceed(enteredCode);
        if (!success) {
            clearDigitFields();
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
            imgUploadCount.setText(String.format(Locale.US, "(%d)", images.size()));
        });
        viewModel.getFileList().observe(this, files -> {
            fileAdapter.updateList(files);
            fileUploadCount.setText(String.format(Locale.US, "(%d)", files.size()));
        });

        // Loading and error states
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                submitBttn.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                submitBttn.setEnabled(true);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                errorTextView.setText(error);
                errorTextView.setVisibility(View.VISIBLE);
            } else {
                errorTextView.setVisibility(View.GONE);
            }
        });

        // Navigation
        viewModel.getNavigationEvent().observe(this, destination -> {
            if (destination != null) {
                startActivity(new Intent(BusinessRegistration.this, destination));
                finish();
            }
        });

        // Observe code validation result to show/hide icons
        viewModel.getIsCodeValid().observe(this, isValid -> {
            if (isValid) {
                checkIcon.setVisibility(View.VISIBLE);
                warningIcon.setVisibility(View.GONE);
            } else {
                checkIcon.setVisibility(View.GONE);
                warningIcon.setVisibility(View.VISIBLE);
            }
        });

        // Observe info messages (like the demo code)
        viewModel.getInfoMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- Permission handling ---
    private void requestFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO},
                    MEDIA_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    MEDIA_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
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

        if (id == R.id.user_type_general) {
            viewModel.navigateTo(GeneralRegistration.class);
        } else if (id == R.id.user_type_business) {
            Toast.makeText(this, "Currently Business User", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.login_opt) {
            viewModel.navigateTo(Login.class);
        } else if (id == R.id.upload_img_bttn) {
            handleImageUpload();
        } else if (id == R.id.upload_file_bttn) {
            handleFileUpload();
        } else if (id == R.id.next_bttn1 || id == R.id.next_bttn2 || id == R.id.next_bttn3) {
            int currentPage = viewModel.getCurrentPage().getValue() != null ? viewModel.getCurrentPage().getValue() : 0;
            if (currentPage == 1) {
                // Page 1 is the verification page – verify code first
                verifyCodeAndProceed();
            } else {
                viewModel.nextPage();
            }
        } else if (id == R.id.prev_bttn1 || id == R.id.prev_bttn2 || id == R.id.prev_bttn3) {
            viewModel.prevPage();
        } else if (id == R.id.submit_bttn) {
            String email = businessEmailEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();
            String confirm = confirmPasswordEt.getText().toString().trim();

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pass values to ViewModel
            viewModel.getCompanyName().setValue(businessNameEt.getText().toString().trim());
            viewModel.getBusinessEmail().setValue(email);
            viewModel.getPhone().setValue(contactNumberEt.getText().toString().trim());
            viewModel.getAddress().setValue(businessAddressEt.getText().toString().trim());
            viewModel.getDescription().setValue(businessDescriptionEt.getText().toString().trim());

            viewModel.registerBusiness(email, password);
        }
    }
}