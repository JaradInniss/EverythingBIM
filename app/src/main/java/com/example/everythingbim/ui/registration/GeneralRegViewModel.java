package com.example.everythingbim.ui.registration;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.R;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GeneralRegViewModel extends ViewModel {

    private String currPassword = "";

    // UI State
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final SingleLiveEvent<Class<?>> navigationEvent = new SingleLiveEvent<>();
    private final MutableLiveData<HashMap<Integer, String>> errorFields = new MutableLiveData<>();

    // Getters
    public MutableLiveData<Integer> getCurrentPage() { return currentPage; }
    public SingleLiveEvent<Class<?>> getNavigationEvent() { return navigationEvent; }
    public LiveData<HashMap<Integer, String>> getErrorFields() { return errorFields; }

    // Logic Functions

    public void setErrorField(int fieldId, String errorMessage) {
        HashMap<Integer, String> currErrors = errorFields.getValue();
        if (currErrors == null) currErrors = new HashMap<>();

        if (errorMessage == null) {
            currErrors.remove(fieldId); // Remove the field from the error list
        } else {
            currErrors.put(fieldId, errorMessage);
        }

        // Set to null if empty so isFormValid() (Map == null) works
        if (currErrors.isEmpty()) {
            errorFields.setValue(null);
        } else {
            errorFields.setValue(currErrors);
        }
    }

    // Validate fields and set error message if invalid
    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            setErrorField(R.id.register_username_et, "Username Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.register_username_et, null);
        }
    }
    public void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            setErrorField(R.id.register_email_et, "Email Field Cannot Be Empty");
        }
        else {
            setErrorField(R.id.register_email_et, null);
        }
    }
    public void validatePassword(String password) {
        currPassword = password;
        if (password == null || password.isEmpty()) {
            setErrorField(R.id.register_password_et, "Password Field Cannot Be Empty");
        }
        else if (password.length() < 8) {
            setErrorField(R.id.register_password_et, "Password Must Be At Least 8 Characters");
        }
        else {
            setErrorField(R.id.register_password_et, null);
        }
    }
    public void validateRePassword(String repassword) {
        if (repassword == null || repassword.isEmpty()) {
            setErrorField(R.id.register_repassword_et, "Password Field Cannot Be Empty");
        }
        else if (!repassword.equals(currPassword)) {
            setErrorField(R.id.register_repassword_et, "Passwords Must Match");
        }
        else {
            setErrorField(R.id.register_repassword_et, null);
        }
    }

    // Checks if form fields are valid
    public boolean isFormValid(String username, String email, String password, String repassword) {
        validateUsername(username);
        validateEmail(email);
        validatePassword(password);
        validateRePassword(repassword);

        return errorFields.getValue() == null;
    }

    // Navigation methods
    public void nextPage() {
        Integer curr = currentPage.getValue();
        if (curr != null) {
            currentPage.setValue(curr + 1);
        }
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

    /**
     * A LiveData class that only delivers the value once, preventing accidental
     * re‑delivery after configuration changes.
     */
    public static class SingleLiveEvent<T> extends MutableLiveData<T> {
        private final AtomicBoolean pending = new AtomicBoolean(false);

        @Override
        public void setValue(T value) {
            pending.set(true);
            super.setValue(value);
        }

        @Override
        public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
            super.observe(owner, t -> {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            });
        }
    }
}