package com.example.everythingbim.ui.posts;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// ViewModel for CreatePost Activity

public class CreatePostViewModel extends AndroidViewModel {
    private final PostDao postDao;
    private final LocationDao locationDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> caption = new MutableLiveData<>("");
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedLocationId = new MutableLiveData<>();
    private final LiveData<List<LocationEntity>> availableLocations;
    private final MutableLiveData<List<String>> taggedUsers = new MutableLiveData<>(new ArrayList<>());
    
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> postCreated = new MutableLiveData<>(false);
    private final MutableLiveData<String> validationMessage = new MutableLiveData<>();

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        postDao = database.postDao();
        locationDao = database.locationDao();
        availableLocations = locationDao.getAllLocations();
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

    public LiveData<List<LocationEntity>> getAvailableLocations() {
        return availableLocations;
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

    public LiveData<String> getValidationMessage() {
        return validationMessage;
    }

    public void createPost() {
        String currentCaption = caption.getValue();
        Uri currentUri = imageUri.getValue();
        Long locationId = selectedLocationId.getValue();

        if (locationId == null || currentUri == null) {
            validationMessage.setValue(buildValidationMessage(locationId, currentUri));
            return;
        }

        validationMessage.setValue(null);
        isSaving.setValue(true);
        executorService.execute(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            long authorId = resolveAuthorId(currentUser);
            String authorName = resolveAuthorName(currentUser);

            PostEntity newPost = new PostEntity(
                    locationId,
                    authorId,
                    authorName,
                    currentCaption,
                    currentUri.toString(),
                    System.currentTimeMillis()
            );

            postDao.insert(newPost);

            isSaving.postValue(false);
            postCreated.postValue(true);
        });
    }

    private String buildValidationMessage(Long locationId, Uri currentUri) {
        if (currentUri == null && locationId == null) {
            return "Select an image and a location before sharing.";
        }
        if (currentUri == null) {
            return "Select an image before sharing.";
        }
        return "Choose a location before sharing.";
    }

    private long resolveAuthorId(FirebaseUser currentUser) {
        if (currentUser == null || currentUser.getUid() == null) {
            return 0L;
        }
        return Integer.toUnsignedLong(currentUser.getUid().hashCode());
    }

    private String resolveAuthorName(FirebaseUser currentUser) {
        if (currentUser == null) {
            return "Current User";
        }
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
            return currentUser.getDisplayName().trim();
        }
        String email = currentUser.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                return email.substring(0, atIndex);
            }
            return email;
        }
        return "Current User";
    }
}
