package com.example.everythingbim.ui.posts;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.entities.PostEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreatePostViewModel extends AndroidViewModel {

    private final PostDao postDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> caption = new MutableLiveData<>("");
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedLocationId = new MutableLiveData<>();
    private final MutableLiveData<List<String>> taggedUsers = new MutableLiveData<>(new ArrayList<>());
    
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> postCreated = new MutableLiveData<>(false);

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        postDao = AppDatabase.getInstance(application).postDao();
    }

    public LiveData<String> getCaption() {
        return caption;
    }

    public void setCaption(String text) {
        caption.setValue(text);
    }

    public LiveData<Uri> getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri uri) {
        imageUri.setValue(uri);
    }

    public LiveData<Long> getSelectedLocationId() {
        return selectedLocationId;
    }

    public void setSelectedLocationId(Long locationId) {
        selectedLocationId.setValue(locationId);
    }

    public LiveData<List<String>> getTaggedUsers() {
        return taggedUsers;
    }

    public void addTaggedUser(String user) {
        List<String> currentTags = taggedUsers.getValue();
        if (currentTags != null && !currentTags.contains(user)) {
            currentTags.add(user);
            taggedUsers.setValue(currentTags);
        }
    }

    public void removeTaggedUser(String user) {
        List<String> currentTags = taggedUsers.getValue();
        if (currentTags != null) {
            currentTags.remove(user);
            taggedUsers.setValue(currentTags);
        }
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getPostCreated() {
        return postCreated;
    }

    public void createPost() {
        String currentCaption = caption.getValue();
        Uri currentUri = imageUri.getValue();
        Long locationId = selectedLocationId.getValue();

        if (locationId == null || currentUri == null) {
            // Validation logic could go here or in the Activity
            return;
        }

        isSaving.setValue(true);
        executorService.execute(() -> {
            // In a real app, you'd upload the image to Firebase Storage first
            // and then save the resulting URL to the local database.
            // For now, we'll just simulate saving the local entity.
            
            PostEntity newPost = new PostEntity(
                    locationId,
                    0, // authorId (should come from Auth)
                    currentCaption,
                    currentUri.toString(),
                    System.currentTimeMillis()
            );
            
            postDao.insert(newPost);
            
            isSaving.postValue(false);
            postCreated.postValue(true);
        });
    }
}
