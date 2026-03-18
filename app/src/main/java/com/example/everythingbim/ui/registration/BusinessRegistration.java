package com.example.everythingbim.ui.registration;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.data.models.File;
import com.example.everythingbim.ui.utils.FileAdapter;
import com.example.everythingbim.ui.utils.FilePicker;
import com.example.everythingbim.R;
import com.example.everythingbim.ui.login.Login;

import java.util.ArrayList;
import java.util.List;

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

    // Util Classes
    FilePicker filePicker = new FilePicker();
    private FileAdapter imageAdapter, fileAdapter;

    // Variables
    private final int MEDIA_PERMISSION_REQUEST_CODE = 100;
    private static final int FILE_PICKER_IMAGE_REQUEST_CODE = 105;
    private static final int FILE_PICKER_FILE_REQUEST_CODE = 110;
    private List<File> fileList;
    private List<File> imageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_business_registration);

        viewModel = new ViewModelProvider(this).get(BusinessRegViewModel.class);

        // Lists
        fileList = new ArrayList<>();
        imageList = new ArrayList<>();

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

        // RecyclerViews
        imgIconContainer = findViewById(R.id.img_icon_container);
        imgIconContainer.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fileIconContainer = findViewById(R.id.file_icon_container);
        fileIconContainer.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Adapters
        imageAdapter = new FileAdapter(this, imageList, position -> {
            imageList.remove(position);
            imageAdapter.notifyItemRemoved(position);
            imageAdapter.notifyItemRangeChanged(position, imageList.size());
            imgUploadCount.setText(String.format("(%d)", imageList.size()));
        });
        fileAdapter = new FileAdapter(this, fileList, position -> {
            fileList.remove(position);
            fileAdapter.notifyItemRemoved(position);
            fileAdapter.notifyItemRangeChanged(position, fileList.size());
            fileUploadCount.setText(String.format("(%d)", fileList.size()));
        });
        imgIconContainer.setAdapter(imageAdapter);
        fileIconContainer.setAdapter(fileAdapter);
    }

    private void setupObservers() {
        // Observe Page Changes
        viewModel.getCurrentPage().observe(this, page -> {
            busRegFormViewFlipper.setDisplayedChild(page);
        });

        // Observe Image List Changes
        viewModel.getImageList().observe(this, imageList -> {
            // Update Adapter
            imageAdapter.notifyDataSetChanged();
            imgUploadCount.setText(String.format("(%d)", imageList.size()));
        });

        // Observe File List Changes
        viewModel.getFileList().observe(this, fileList -> {
            // Update Adapter
            fileAdapter.notifyDataSetChanged();
            fileUploadCount.setText(String.format("(%d)", fileList.size()));
        });

        // Observe Navigation
        viewModel.getNavigationEvent().observe(this, destination -> {
            if (destination != null) {
                Intent intent = new Intent(BusinessRegistration.this, destination);
                startActivity(intent);
            }
        });
    }

    private void requestFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
            }, MEDIA_PERMISSION_REQUEST_CODE);
        }
        else {
            // Android 12
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, MEDIA_PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        }
        else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MEDIA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // OnClick Listener for Buttons
    @Override
    public void onClick(View view) {
        int bttn_id = view.getId();

        if (bttn_id == R.id.user_type_general) {
            viewModel.navigateTo(GeneralRegistration.class);
        }
        else if (bttn_id == R.id.user_type_business) {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(this, "Currently Business User", Toast.LENGTH_SHORT).show();
            }
        }
        else if (bttn_id == R.id.login_opt) {
            // Return to Log in
            viewModel.navigateTo(Login.class);
        }
        if (bttn_id == R.id.upload_img_bttn) {
            handleImageUpload();
        }
        else if (bttn_id == R.id.upload_file_bttn) {
            handleFileUpload();
        }
        else if (bttn_id == R.id.next_bttn1 || bttn_id == R.id.next_bttn2 || bttn_id == R.id.next_bttn3) {
            viewModel.nextPage();
        }
        else if (bttn_id == R.id.prev_bttn1 || bttn_id == R.id.prev_bttn2 || bttn_id == R.id.prev_bttn3) {
            viewModel.prevPage();
        }
        else if (bttn_id == R.id.submit_bttn) {
            Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
            viewModel.navigateTo(Login.class);
        }
    }

    // Image Upload Function
    private void handleImageUpload() {
        // Check if Permission Granted
        if (hasFilePermissions()) {
            // Open File Picker
            filePicker.openFilePicker(this, FILE_PICKER_IMAGE_REQUEST_CODE);
        }
        else {
            // Request File Permissions
            requestFilePermissions();
        }
    }

    // File Upload Function
    private void handleFileUpload() {
        // Check if Permission Granted
        if (hasFilePermissions()) {
            // Open File Picker
            filePicker.openFilePicker(this, FILE_PICKER_FILE_REQUEST_CODE);
        }
        else {
            // Request File Permissions
            requestFilePermissions();
        }
    }

    private void addFileToList(Uri uri, List<File> targetList, FileAdapter targetAdapter) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            long size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            cursor.close();
            String mimeType = getContentResolver().getType(uri);

            File file = new File(name, mimeType, uri, size);
            targetList.add(file);
            targetAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) { return; }

        // Determine which list and adapter to update
        TextView targetCounter;
        List<File> targetList;
        FileAdapter targetAdapter;
        if (requestCode == FILE_PICKER_IMAGE_REQUEST_CODE) {
            targetCounter = imgUploadCount;
            targetList = imageList;
            targetAdapter = imageAdapter;
        }
        else if (requestCode == FILE_PICKER_FILE_REQUEST_CODE) {
            targetCounter = fileUploadCount;
            targetList = fileList;
            targetAdapter = fileAdapter;
        }
        else { return; }

        if (data.getClipData() != null) {
            // Multiple Files Selected
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                addFileToList(uri, targetList, targetAdapter);
            }
            targetCounter.setText(String.format("(%d)", targetList.size()));
        }
        else if (data.getData() != null) {
            // Single File Selected
            Uri uri = data.getData();
            addFileToList(uri, targetList, targetAdapter);
            targetCounter.setText(String.format("(%d)", targetList.size()));
        }
    }
}