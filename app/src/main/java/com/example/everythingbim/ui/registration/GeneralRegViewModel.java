package com.example.everythingbim.ui.registration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GeneralRegViewModel extends ViewModel {
    // UI State
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final MutableLiveData<Class<?>> navigationEvent  = new MutableLiveData<>();

    // Getters
    public LiveData<Class<?>> getNavigationEvent() { return navigationEvent; }
    public LiveData<Integer> getCurrentPage() { return currentPage; }

    // Logic Functions
    // Form Page Navigation
    public void nextPage() {
        Integer curr = currentPage.getValue();
        if (curr != null) { currentPage.setValue(curr + 1); }
    }
    public void prevPage() {
        Integer curr = currentPage.getValue();
        if (curr != null) { currentPage.setValue(curr - 1); }
    }

    public void navigateTo(Class<?> destination) {
        navigationEvent.setValue(destination);
    }
}
