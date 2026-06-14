package com.example.everythingbim.ui.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UserViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isAdminAccessVisible = new MutableLiveData<>(false);

    public LiveData<Boolean> isAdminAccessVisible() {
        return _isAdminAccessVisible;
    }

    public void onAdminSecretTriggered() {
        _isAdminAccessVisible.setValue(true);
    }
}
