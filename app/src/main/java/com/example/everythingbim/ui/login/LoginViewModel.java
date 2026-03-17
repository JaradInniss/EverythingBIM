package com.example.everythingbim.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.ui.registration.BusinessRegistration;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.data.models.UserType;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<UserType> selectedUserType = new MutableLiveData<>(UserType.GENERAL);
    private final MutableLiveData<Class<?>> navigationEvent  = new MutableLiveData<>();

    // SelectedUserType getter and setter
    public LiveData<UserType> getSelectedUserType() { return selectedUserType; }

    public void setSelectedUserType(UserType userType) { selectedUserType.setValue(userType); }

    public LiveData<Class<?>> getNavigationEvent() { return navigationEvent; }

    // Login button click handler
    public void onLoginClicked() {
        navigationEvent.setValue(MainActivity.class);
    }

    // Create Account Option click handler
    public void onCreateAccountClicked() {
        if (selectedUserType.getValue() == UserType.GENERAL) {
            navigationEvent.setValue(GeneralRegistration.class);
        }
        else
        {
            navigationEvent.setValue(BusinessRegistration.class);
        }
    }
}
