package com.example.everythingbim.ui.login;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.BusinessRegistration;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.NavigationCommand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoginViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private SharedPreferences sharedPreferences; // may be null

    private final MutableLiveData<UserType> selectedUserType = new MutableLiveData<>(UserType.GENERAL);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final SingleLiveEvent<NavigationCommand> navigationEvent = new SingleLiveEvent<>();

    public void setSharedPreferences(SharedPreferences prefs) {
        this.sharedPreferences = prefs;
    }

    public LiveData<UserType> getSelectedUserType() { return selectedUserType; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public SingleLiveEvent<NavigationCommand> getNavigationEvent() { return navigationEvent; }

    public void setSelectedUserType(UserType userType) { selectedUserType.setValue(userType); }

    public void onLoginClicked(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
//            errorMessage.setValue("Please enter both email and password");
            return;
        }
        navigationEvent.setValue(new NavigationCommand(MainActivity.class));


//        isLoading.setValue(true);
//        errorMessage.setValue(null);


//        auth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                    isLoading.setValue(false);
//                    if (task.isSuccessful()) {
//                        FirebaseUser user = auth.getCurrentUser();
//                        if (user != null) {
//                            fetchUserTypeAndNavigate(user.getUid());
//                        } else {
//                            errorMessage.setValue("Authentication error");
//                        }
//                    } else {
//                        String error = task.getException() != null ?
//                                task.getException().getMessage() : "Authentication failed";
//                        errorMessage.setValue(error);
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
                                errorMessage.setValue("User type not found");
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
                                            errorMessage.setValue("User type not found");
                                        }
                                    } else {
                                        errorMessage.setValue("User document not found");
                                    }
                                } else {
                                    String error = task2.getException() != null ?
                                            task2.getException().getMessage() : "Failed to fetch user data";
                                    errorMessage.setValue(error);
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
        if (selectedUserType.getValue() == UserType.GENERAL) {
            navigationEvent.setValue(new NavigationCommand(GeneralRegistration.class));
        } else {
            navigationEvent.setValue(new NavigationCommand(BusinessRegistration.class));
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