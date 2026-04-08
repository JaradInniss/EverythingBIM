package com.example.everythingbim.ui.login;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.BusinessRegistration;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.NavigationCommand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoginViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private SharedPreferences sharedPreferences; // may be null
    private final MutableLiveData<UserType> selectedUserType = new MutableLiveData<>(UserType.ADMIN);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<HashMap<Integer, String>> errorFields = new MutableLiveData<>();
    private final SingleLiveEvent<NavigationCommand> navigationEvent = new SingleLiveEvent<>();

    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();

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

    public void onLoginClicked(String email, String password) {
        Log.d("LoginViewModel", "isFormValid: "+isFormValid(email, password));
        if (!isFormValid(email, password)) {
            return;
        }
        navigationEvent.setValue(new NavigationCommand(MainActivity.class));

//        isLoading.setValue(true);
//        errorFields.setValue(null);
//
//        auth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                    isLoading.setValue(false);
//                    if (task.isSuccessful()) {
//                        FirebaseUser user = auth.getCurrentUser();
//                        if (user != null) {
//                            fetchUserTypeAndNavigate(user.getUid());
//                        } else {
//                            setErrorField(R.id.login_error, "Authentication Error");
//                        }
//                    } else {
//                        String error = task.getException() != null ?
//                                task.getException().getMessage() : "Authentication failed";
//                        setErrorField(R.id.login_error, error);
//                    }
//                });
    }

    private void fetchUserTypeAndNavigate(String userId) {
        isLoading.setValue(true);
        // First, try to fetch from 'users' collection (general users)
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // General user found
                            String userType = document.getString("userType");
                            if (userType != null) {
                                saveAndNavigate(userType, userId);
                            } else {
                                setErrorField(R.id.login_error, "User type not found");
                            }
                            return; // success, no need to check businesses
                        }
                    }
                    // If not found in 'users', try 'businesses'
                    db.collection("businesses").document(userId).get()
                            .addOnCompleteListener(task2 -> {
                                isLoading.setValue(false);
                                if (task2.isSuccessful()) {
                                    DocumentSnapshot doc = task2.getResult();
                                    if (doc.exists()) {
                                        String userType = doc.getString("userType");
                                        if (userType != null) {
                                            saveAndNavigate(userType, userId);
                                        } else {
                                            setErrorField(R.id.login_error, "User type not found");
                                        }
                                    } else {
                                        setErrorField(R.id.login_error, "User document not found");
                                    }
                                } else {
                                    String error = task2.getException() != null ?
                                            task2.getException().getMessage() : "Failed to fetch user data";
                                    setErrorField(R.id.login_error, error);
                                }
                            });
                });
    }

    private void saveAndNavigate(String userType, String userId) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putString("userType", userType).apply();
        }
        Bundle extras = new Bundle();
        extras.putString("userType", userType);
        navigationEvent.setValue(new NavigationCommand(MainActivity.class, extras));
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