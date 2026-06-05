package com.example.everythingbim.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.R;
import com.example.everythingbim.data.FirebaseProvider;
import com.example.everythingbim.data.RealFirebaseProvider;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.ui.admin.AdminActivity;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.BusinessRegistration;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.NavigationCommand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginViewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseProvider firebaseProvider;

    private SharedPreferences sharedPreferences; // may be null
    private final MutableLiveData<UserType> selectedUserType = new MutableLiveData<>(UserType.GENERAL);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<HashMap<Integer, String>> errorFields = new MutableLiveData<>();
    private final SingleLiveEvent<NavigationCommand> navigationEvent = new SingleLiveEvent<>();

    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();

    /**
     * Default constructor for production use.
     * Uses RealFirebaseProvider to get actual Firebase instances.
     */
    public LoginViewModel() {
        this(new RealFirebaseProvider());
    }

    /**
     * Constructor for testing - allows injection of mock FirebaseProvider.
     */
    public LoginViewModel(FirebaseProvider provider) {
        this.firebaseProvider = provider;
        this.auth = provider.getAuth();
        this.db = provider.getFirestore();
    }

    // Getters
    public LiveData<UserType> getSelectedUserType() { return selectedUserType; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<HashMap<Integer, String>> getErrorFields() { return errorFields; }
    public SingleLiveEvent<NavigationCommand> getNavigationEvent() { return navigationEvent; }
    public SingleLiveEvent<String> getToastMessage() { return toastMessage; }


    // Setters
    public void setSelectedUserType(UserType userType) { selectedUserType.setValue(userType); }
    public void setSharedPreferences(SharedPreferences prefs) {
        this.sharedPreferences = prefs;
    }

    // Logic Functions
    public void setErrorField(int fieldId, String errorMessage) {
        HashMap<Integer, String> currError = errorFields.getValue();
        if (currError == null) { currError = new HashMap<>(); }

        if (errorMessage == null) {
            currError.remove(fieldId);
        }
        else {
            currError.put(fieldId, errorMessage);
        }

        if (currError.isEmpty()) {
            errorFields.setValue(null);
        }
        else {
            errorFields.setValue(currError);
        }
    }

    // Validate fields and set error message if invalid
    public void validateEmail(String email) {
        if (email.isEmpty()) {
            setErrorField(R.id.login_email_et, "Email Field Cannot Be Empty");
            return;
        }
        setErrorField(R.id.login_email_et, null);
    }

    public void validatePassword(String password) {
        if (password.isEmpty()) {
            setErrorField(R.id.login_password_et, "Password Field Cannot Be Empty");
            return;
        }
        setErrorField(R.id.login_password_et, null);
    }

    // Checks if form fields are valid
    public boolean isFormValid(String email, String password) {
        validateEmail(email);
        validatePassword(password);

        return errorFields.getValue() == null;
    }

    /*
     * ORIGINAL BROKEN CODE - Commented out because Firebase Auth was never called
     * Users were never authenticated, causing "permission denied" on all Firestore writes
     * because request.auth was always null.
     *
    public void onLoginClicked(String email, String password) {
        Log.d("LoginViewModel", "isFormValid: "+isFormValid(email, password));
        if (!isFormValid(email, password)) {
            return;
        }

        // BYPASS Firebase Auth for testing - persist the selected role and open the matching shell.
        UserType selected = selectedUserType.getValue() != null
                ? selectedUserType.getValue()
                : UserType.GENERAL;

        if (selected == UserType.ADMIN) {
            saveAndNavigate("admin", "");
        } else if (selected == UserType.BUSINESS) {
            saveAndNavigate(MainActivity.USER_TYPE_BUSINESS, "");
        } else {
            saveAndNavigate(MainActivity.USER_TYPE_GENERAL, "");
        }
    }
    */

    public void onLoginClicked(String email, String password) {
        Log.d("LoginViewModel", "isFormValid: "+isFormValid(email, password));
        if (!isFormValid(email, password)) {
            return;
        }

        isLoading.setValue(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            fetchUserTypeAndNavigate(firebaseUser.getUid());
                        } else {
                            setErrorField(R.id.login_error, "User not found");
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        setErrorField(R.id.login_error, errorMessage);
                    }
                });
    }

    private void fetchUserTypeAndNavigate(String userId) {
        isLoading.setValue(true);
        UserType selected = selectedUserType.getValue() != null
                ? selectedUserType.getValue()
                : UserType.GENERAL;

        String[] collectionsToCheck = selected == UserType.ADMIN
                ? new String[]{"admins", "users", "businesses"}
                : new String[]{"users", "businesses", "admins"};

        fetchUserTypeFromCollections(userId, collectionsToCheck, 0);
    }

    private void fetchUserTypeFromCollections(@NonNull String userId,
                                              @NonNull String[] collectionsToCheck,
                                              int index) {
        if (index >= collectionsToCheck.length) {
            isLoading.setValue(false);
            setErrorField(R.id.login_error, "User document not found");
            return;
        }

        String collectionName = collectionsToCheck[index];
        db.collection(collectionName).document(userId).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        isLoading.setValue(false);
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Failed to fetch user data";
                        setErrorField(R.id.login_error, error);
                        return;
                    }

                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        String resolvedUserType = normalizeResolvedUserType(
                                document.getString("userType"),
                                collectionName
                        );
                        if (resolvedUserType != null) {
                            isLoading.setValue(false);
                            saveAndNavigate(resolvedUserType, userId);
                        } else {
                            isLoading.setValue(false);
                            setErrorField(R.id.login_error, "User type not found");
                        }
                        return;
                    }

                    fetchUserTypeFromCollections(userId, collectionsToCheck, index + 1);
                });
    }

    private String normalizeResolvedUserType(String rawUserType, String sourceCollection) {
        if ("admins".equals(sourceCollection)) {
            return "admin";
        }
        if (rawUserType == null || rawUserType.trim().isEmpty()) {
            return null;
        }

        String normalized = rawUserType.trim().toLowerCase(Locale.US);
        if ("admin".equals(normalized)
                || MainActivity.USER_TYPE_GENERAL.equals(normalized)
                || MainActivity.USER_TYPE_BUSINESS.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private void saveAndNavigate(String userType, String userId) {
        if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userType", userType);
            editor.putString("userId", userId);
            editor.apply();
        }
        Bundle extras = new Bundle();
        extras.putString("userType", userType);
        extras.putString("userId", userId);
        if ("admin".equals(userType)) {
            navigationEvent.setValue(new NavigationCommand(AdminActivity.class, extras));
        } else {
            navigationEvent.setValue(new NavigationCommand(MainActivity.class, extras));
        }
    }

    public void onCreateAccountClicked() {
        Log.d("LoginViewModel", "User Type: " + selectedUserType.getValue());
        if (selectedUserType.getValue() == UserType.GENERAL) {
            navigationEvent.setValue(new NavigationCommand(GeneralRegistration.class));
        } else if (selectedUserType.getValue() == UserType.BUSINESS) {
            navigationEvent.setValue(new NavigationCommand(BusinessRegistration.class));
        }
        else if (selectedUserType.getValue() == UserType.ADMIN) {
            Log.d("LoginViewModel", "Checked for Admin");
            toastMessage.setValue("Please select a user type");
        }
    }

    /**
     * SingleLiveEvent - ensures the event is delivered only once.
     */
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
