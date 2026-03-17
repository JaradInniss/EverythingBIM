package com.example.everythingbim.ui.registration;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.data.models.File;

import java.util.ArrayList;
import java.util.List;

public class BusinessRegViewModel extends ViewModel {

    // UI State
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final MutableLiveData<List<File>> imageList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<File>> fileList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Class<?>> navigationEvent  = new MutableLiveData<>();

    // Getters
    // NavigationEvent getter
    public LiveData<Class<?>> getNavigationEvent() { return navigationEvent; }
    public LiveData<Integer> getCurrentPage() { return currentPage; }
    public LiveData<List<File>> getImageList() { return imageList; }
    public LiveData<List<File>> getFileList() { return fileList; }


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

    // ImageList Management
    public void addImage(File file) {
        // Get current list
        List<File> currFileList = imageList.getValue();
        // Add new image to list
        currFileList.add(file);
        // Update list
        imageList.setValue(currFileList);
    }
    public void removeImage(int position) {
        // Get current list
        List<File> currFileList = imageList.getValue();
        // Remove image at position from list
        currFileList.remove(position);
        // Update list
        imageList.setValue(currFileList);
    }

    // FileList Management
    public void addFile(File file) {
        // Get current list
        List<File> currFileList = fileList.getValue();
        // Add new file to list
        currFileList.add(file);
        // Update list
        fileList.setValue(currFileList);
    }

    public void removeFile(int position) {
        // Get current list
        List<File> currFileList = fileList.getValue();
        // Remove file at position from list
        currFileList.remove(position);
        // Update list
        fileList.setValue(currFileList);
    }

    public void navigateTo(Class<?> destination) {
        navigationEvent.setValue(destination);
    }
}
