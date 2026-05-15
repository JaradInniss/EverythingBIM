package com.example.everythingbim.ui.registration;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.R;
import com.example.everythingbim.data.FirebaseProvider;
import com.example.everythingbim.data.RealFirebaseProvider;
import com.example.everythingbim.data.models.BusinessProfile;
import com.example.everythingbim.data.models.File;
import com.example.everythingbim.ui.login.Login;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BusinessRegViewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseProvider firebaseProvider;

    // Form fields
    private final MutableLiveData<String> companyName = new MutableLiveData<>();
    private final MutableLiveData<String> businessEmail = new MutableLiveData<>();
    private final MutableLiveData<String> phone = new MutableLiveData<>();
    private final MutableLiveData<String> address = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();

    // UI State
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final MutableLiveData<List<File>> imageList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<File>> fileList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final SingleLiveEvent<Class<?>> navigationEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> infoMessage = new SingleLiveEvent<>();
    private final MutableLiveData<HashMap<Integer, String>> errorFields = new MutableLiveData<>();
    private String currPassword = "";

    // Verification code
    private String demoVerificationCode;
    private final MutableLiveData<Boolean> isCodeValid = new MutableLiveData<>(false);

    /**
     * Default constructor for production use.
     * Uses RealFirebaseProvider to get actual Firebase instances.
     */
    public BusinessRegViewModel() {
        this(new RealFirebaseProvider());
    }

    /**
     * Constructor for testing - allows injection of mock FirebaseProvider.
     */
    public BusinessRegViewModel(FirebaseProvider provider) {
        this.firebaseProvider = provider;
        this.auth = provider.getAuth();
        this.db = provider.getFirestore();
        this.storage = provider.getStorage();
    }

    // Getters
    public LiveData<Integer> getCurrentPage() { return currentPage; }
    public LiveData<List<File>> getImageList() { return imageList; }
    public LiveData<List<File>> getFileList() { return fileList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public SingleLiveEvent<Class<?>> getNavigationEvent() { return navigationEvent; }
    public SingleLiveEvent<String> getInfoMessage() { return infoMessage; }
    public LiveData<Boolean> getIsCodeValid() { return isCodeValid; }
    public LiveData<HashMap<Integer, String>> getErrorFields() { return errorFields; }

    // Form field getters/setters
    public MutableLiveData<String> getCompanyName() { return companyName; }
    public MutableLiveData<String> getBusinessEmail() { return businessEmail; }
    public MutableLiveData<String> getPhone() { return phone; }
    public MutableLiveData<String> getAddress() { return address; }
    public MutableLiveData<String> getDescription() { return description; }

    // Navigation
    public void nextPage() {
        Integer curr = currentPage.getValue();
        if (curr == null) return;

        // If moving from page 0 (account credentials) to page 1 (verification), generate code
        if (curr == 0) {
            generateDemoCode();
        }

        // If moving from page 1 (verification) to page 2, verify code first
        if (curr == 1) {
            // Verification must be done by the activity before calling nextPage()
            return;
        }

        currentPage.setValue(curr + 1);
    }

    public void prevPage() {
        Integer curr = currentPage.getValue();
        if (curr != null && curr > 0) {
            currentPage.setValue(curr - 1);
        }
    }

    public void navigateTo(Class<?> destination) {
        navigationEvent.setValue(destination);
    }

    // File list management
    public void addImage(File file) { List<File> curr = imageList.getValue(); if (curr != null) { curr.add(file); imageList.setValue(curr); } }
    public void removeImage(int position) { List<File> curr = imageList.getValue(); if (curr != null && position < curr.size()) { curr.remove(position); imageList.setValue(curr); } }
    public void addFile(File file) { List<File> curr = fileList.getValue(); if (curr != null) { curr.add(file); fileList.setValue(curr); } }
    public void removeFile(int position) { List<File> curr = fileList.getValue(); if (curr != null && position < curr.size()) { curr.remove(position); fileList.setValue(curr); } }

    public void setErrorField(int fieldId, String errorMessage) {
        HashMap<Integer, String> curr = errorFields.getValue();
        // Use LinkedHashMap to preserve the order (Username -> Email -> Password)
        if (curr == null) curr = new java.util.LinkedHashMap<>();

        if (errorMessage == null) {
            curr.remove(fieldId);
        } else {
            curr.put(fieldId, errorMessage);
        }
        errorFields.setValue(curr.isEmpty() ? null : curr);
    }

    // Validate fields and set error message if invalid
    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            setErrorField(R.id.business_register_username_et, "Username Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.business_register_username_et, null);
        }
    }
    public void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            setErrorField(R.id.business_register_email_et, "Email Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.business_register_email_et, null);
        }
    }
    public void validatePassword(String password) {
        currPassword = password;
        if (password == null || password.isEmpty()) {
            setErrorField(R.id.business_register_password_et, "Password Field Cannot Be Empty");
        }
        else if (password.length() < 8) {
            setErrorField(R.id.business_register_password_et, "Password Must Be At Least 8 Characters");
        }
        else {
            setErrorField(R.id.business_register_password_et, null);
        }
    }
    public void validateRePassword(String repassword) {
        if (repassword == null || repassword.isEmpty()) {
            setErrorField(R.id.business_register_repassword_et, "Confirm Password Field Cannot Be Empty");
        }
        else if (!repassword.equals(currPassword)) {
            setErrorField(R.id.business_register_repassword_et, "Passwords Must Match");
        }
        else {
            setErrorField(R.id.business_register_repassword_et, null);
        }
    }
    public void validateBusinessName(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            setErrorField(R.id.register_business_name_et, "Business Name Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.register_business_name_et, null);
        }
    }
    public void validatePhoneNumber(String phoneNum) {
        if (phoneNum == null || phoneNum.trim().isEmpty()) {
            setErrorField(R.id.register_contact_number_et, "Phone Number Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.register_contact_number_et, null);
        }
    }
    public void validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            setErrorField(R.id.register_business_address_et, "Address Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.register_business_address_et, null);
        }
    }
    public void validateBusinessDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            setErrorField(R.id.register_business_description_et, "Business Description Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.register_business_description_et, null);
        }
    }

    public boolean isPageValid(int pageNum, String... params) {
        errorMessage.setValue(null);
        errorFields.setValue(null); // Clear previous errors

        if (pageNum == 0) { // Form 1: Credentials
            validateUsername(params[0]);
            validateEmail(params[1]);
            validatePassword(params[2]);
            validateRePassword(params[3]);
        }
        else if (pageNum == 2) { // Form 3: Business Details
            validateBusinessName(params[0]);
            validatePhoneNumber(params[1]);
            validateAddress(params[2]);
            validateBusinessDescription(params[3]);
        }

        return errorFields.getValue() == null;
    }

    // --- Demo verification ---
    private void generateDemoCode() {
        demoVerificationCode = String.valueOf((int) (Math.random() * 90000) + 10000);
        infoMessage.setValue("Demo verification code: " + demoVerificationCode);
        isCodeValid.setValue(false);
        Log.d("BusinessRegViewModel", "Generated demo verification code: " + demoVerificationCode);
    }

    public boolean verifyAndProceed(String enteredCode) {
        if (enteredCode.length() < 5) {
            errorMessage.setValue("Please enter the complete 5-digit code");
            isCodeValid.setValue(false);
            return false;
        }
        if (enteredCode.equals(demoVerificationCode)) {
            isCodeValid.setValue(true);
            // Move to next page
            currentPage.setValue(2);
            return true;
        } else {
            errorMessage.setValue("Invalid verification code");
            isCodeValid.setValue(false);
            return false;
        }
    }

    // --- Business registration ---
    public void registerBusiness(String email, String password) {
        if (email.isEmpty() || password.isEmpty() || companyName.getValue() == null || companyName.getValue().isEmpty()) {
            errorMessage.setValue("Please fill all required fields");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            uploadAllFilesAndSaveProfile(user.getUid(), email);
                        } else {
                            registrationFailed("Authentication error");
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        registrationFailed(error);
                    }
                });
    }

    private void uploadAllFilesAndSaveProfile(String userId, String email) {
        List<String> imageUrls = new ArrayList<>();
        List<String> fileUrls = new ArrayList<>();

        List<com.google.android.gms.tasks.Task<Uri>> uploadTasks = new ArrayList<>();

        List<File> images = imageList.getValue();
        if (images != null) {
            for (File file : images) {
                StorageReference ref = storage.getReference()
                        .child("businesses")
                        .child(userId)
                        .child("images")
                        .child(UUID.randomUUID().toString());
                UploadTask uploadTask = ref.putFile(file.getUri());
                com.google.android.gms.tasks.Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                });
                uploadTasks.add(urlTask);
                urlTask.addOnSuccessListener(uri -> imageUrls.add(uri.toString()));
            }
        }

        List<File> files = fileList.getValue();
        if (files != null) {
            for (File file : files) {
                StorageReference ref = storage.getReference()
                        .child("businesses")
                        .child(userId)
                        .child("files")
                        .child(UUID.randomUUID().toString());
                UploadTask uploadTask = ref.putFile(file.getUri());
                com.google.android.gms.tasks.Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                });
                uploadTasks.add(urlTask);
                urlTask.addOnSuccessListener(uri -> fileUrls.add(uri.toString()));
            }
        }

        Tasks.whenAllSuccess(uploadTasks)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        BusinessProfile profile = new BusinessProfile(
                                userId,
                                companyName.getValue(),
                                email,
                                phone.getValue(),
                                address.getValue(),
                                description.getValue(),
                                imageUrls,
                                fileUrls,
                                com.google.firebase.Timestamp.now()
                        );

                        db.collection("businesses").document(userId).set(profile)
                                .addOnSuccessListener(aVoid -> {
                                    isLoading.setValue(false);
                                    sendEmailVerification();
                                    navigationEvent.setValue(Login.class);
                                })
                                .addOnFailureListener(e -> {
                                    registrationFailed("Failed to save profile: " + e.getMessage());
                                });
                    } else {
                        registrationFailed("File upload failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            infoMessage.setValue("Verification email sent. Please check your inbox.");
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to send verification email";
                            errorMessage.setValue(error);
                        }
                    });
        } else {
            errorMessage.setValue("User not found, cannot send verification email");
        }
    }

    private void registrationFailed(String message) {
        isLoading.setValue(false);
        errorMessage.setValue(message);
    }

    public static class SingleLiveEvent<T> extends MutableLiveData<T> {
        private final AtomicBoolean pending = new AtomicBoolean(false);

        @Override
        public void setValue(T value) {
            pending.set(true);
            super.setValue(value);
        }

        @Override
        public void observe(@NonNull androidx.lifecycle.LifecycleOwner owner,
                            @NonNull androidx.lifecycle.Observer<? super T> observer) {
            super.observe(owner, t -> {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            });
        }
    }
}