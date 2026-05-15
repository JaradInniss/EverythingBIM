package com.example.everythingbim.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.R;

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
