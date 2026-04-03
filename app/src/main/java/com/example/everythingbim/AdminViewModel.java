package com.example.everythingbim;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AdminViewModel extends ViewModel {

    private final MutableLiveData<Integer> navbarItemId = new MutableLiveData<>(R.id.admin_navbar_home);

    public LiveData<Integer> getNavbarItemId() {
        return navbarItemId;
    }

    public void setNavbarItemId(int id) {
        if (navbarItemId.getValue() == null || navbarItemId.getValue() != id) {
            navbarItemId.setValue(id);
        }
    }
}
