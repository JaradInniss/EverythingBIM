package com.example.everythingbim.ui.posts;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.repository.PostRepository;
import com.example.everythingbim.data.repository.UserRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.Place;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreatePostViewModel extends AndroidViewModel {
    private static final String TAG = "CreatePostViewModel";

    private final LocationDao locationDao;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<String> caption = new MutableLiveData<>("");
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<Long> selectedLocationId = new MutableLiveData<>();
    private final LiveData<List<LocationEntity>> availableLocations;
    private final MutableLiveData<List<UserEntity>> taggedUsers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> userQuery = new MutableLiveData<>("");
    private final LiveData<List<UserWithProfile>> userSearchResults;

    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> postCreated = new MutableLiveData<>(false);
    private final MutableLiveData<String> validationMessage = new MutableLiveData<>();

    private static final class UploadResult {
        @Nullable
        final String downloadUrl;
        @Nullable
        final String errorMessage;

        UploadResult(@Nullable String downloadUrl, @Nullable String errorMessage) {
            this.downloadUrl = downloadUrl;
            this.errorMessage = errorMessage;
        }
    }

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        locationDao = database.locationDao();
        postRepository = new PostRepository(application);
        userRepository = new UserRepository(application);
        availableLocations = locationDao.getAllLocations();
        userSearchResults = Transformations.switchMap(
                userQuery,
                query -> userRepository.searchUsersInFirestore(query)
        );
    }

    public LiveData<String> getCaption() {
        return caption;
    }

    public void setCaption(@Nullable String text) {
        caption.setValue(text == null ? "" : text);
    }

    public LiveData<Uri> getImageUri() {
        return imageUri;
    }

    public void setImageUri(@Nullable Uri uri) {
        imageUri.setValue(uri);
    }

    public LiveData<Long> getSelectedLocationId() {
        return selectedLocationId;
    }

    public void setSelectedLocationId(@Nullable Long locationId) {
        selectedLocationId.setValue(locationId);
    }

    public void clearDraft() {
        caption.setValue("");
        imageUri.setValue(null);
        selectedLocationId.setValue(null);
        taggedUsers.setValue(new ArrayList<>());
        userQuery.setValue("");
        validationMessage.setValue(null);
        postCreated.setValue(false);
    }

    public LiveData<List<LocationEntity>> getAvailableLocations() {
        return availableLocations;
    }

    public LiveData<List<UserEntity>> getTaggedUsers() {
        return taggedUsers;
    }

    public LiveData<List<UserWithProfile>> getUserSearchResults() {
        return userSearchResults;
    }

    public void searchUsers(@Nullable String query) {
        userQuery.setValue(query == null ? "" : query.trim());
    }

    public void addTaggedUser(@Nullable UserEntity user) {
        if (user == null) {
            return;
        }
        List<UserEntity> currentTags = taggedUsers.getValue();
        if (currentTags == null) {
            currentTags = new ArrayList<>();
        } else {
            currentTags = new ArrayList<>(currentTags);
        }
        if (!currentTags.contains(user)) {
            currentTags.add(user);
            taggedUsers.setValue(currentTags);
        }
    }

    public void removeTaggedUser(@Nullable UserEntity user) {
        if (user == null) {
            return;
        }
        List<UserEntity> currentTags = taggedUsers.getValue();
        if (currentTags == null) {
            return;
        }
        List<UserEntity> updatedTags = new ArrayList<>(currentTags);
        updatedTags.remove(user);
        taggedUsers.setValue(updatedTags);
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

    public void setLocationFromPlaces(@NonNull Place place) {
        executorService.execute(() -> {
            LocationEntity newLocation = new LocationEntity(
                    place.getName(),
                    place.getLatLng() != null ? place.getLatLng().latitude : 0.0,
                    place.getLatLng() != null ? place.getLatLng().longitude : 0.0,
                    place.getRating() != null ? place.getRating().floatValue() : 0.0f,
                    false,
                    "GooglePlaces",
                    "",
                    place.getTypes() != null && !place.getTypes().isEmpty() ? place.getTypes().get(0).name() : "General",
                    "",
                    place.getAddress()
            );
            long id = locationDao.insert(newLocation);
            selectedLocationId.postValue(id);
        });
    }

    public void createPost() {
        String currentCaption = caption.getValue();
        Uri currentUri = imageUri.getValue();
        Long locationId = selectedLocationId.getValue();

        if (locationId == null || currentUri == null) {
            validationMessage.setValue(buildValidationMessage(locationId, currentUri));
            return;
        }

        if (currentCaption == null || currentCaption.trim().isEmpty()) {
            validationMessage.setValue("Add a caption before sharing.");
            return;
        }

        validationMessage.setValue(null);
        isSaving.setValue(true);
        executorService.execute(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getUid() == null) {
                validationMessage.postValue("Sign in to create a post.");
                isSaving.postValue(false);
                return;
            }

            refreshAuthSessionIfPossible(currentUser);

            UploadResult uploadResult = uploadImageToStorage(currentUri, currentUser.getUid());
            if (uploadResult.downloadUrl == null) {
                validationMessage.postValue(uploadResult.errorMessage != null
                        ? uploadResult.errorMessage
                        : "Failed to upload image. Please try again.");
                isSaving.postValue(false);
                return;
            }

            long authorId = resolveAuthorId(currentUser);
            String authorName = resolveAuthorName(currentUser);
            List<String> taggedUids = buildTaggedUserUids();
            String locationName = resolveLocationName(locationId);

            PostEntity newPost = new PostEntity(
                    locationId,
                    locationName,
                    authorId,
                    authorName,
                    currentUser.getUid(),
                    currentCaption,
                    uploadResult.downloadUrl,
                    System.currentTimeMillis(),
                    taggedUids
            );
            newPost.likeCount = 0;

            postRepository.createPostInFirestoreAsync(
                    newPost,
                    currentUser.getUid(),
                    locationName,
                    (persisted, error) -> {
                        if (error != null || persisted == null) {
                            validationMessage.postValue("Failed to save post details. Please try again.");
                            isSaving.postValue(false);
                            return;
                        }
                        isSaving.postValue(false);
                        postCreated.postValue(true);
                    }
            );
        });
    }

    private List<String> buildTaggedUserUids() {
        List<UserEntity> currentTags = taggedUsers.getValue();
        List<String> taggedUids = new ArrayList<>();
        if (currentTags == null) {
            return taggedUids;
        }
        for (UserEntity user : currentTags) {
            if (user != null && user.firebaseUid != null && !user.firebaseUid.trim().isEmpty()) {
                taggedUids.add(user.firebaseUid);
            }
        }
        return taggedUids;
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

    @Nullable
    private UploadResult uploadImageToStorage(@NonNull Uri uri, @Nullable String uid) {
        try {
            String safeUid = (uid == null || uid.isEmpty()) ? "anonymous" : uid;
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("post_images/" + safeUid + "/" + System.currentTimeMillis() + ".jpg");

            UploadTask.TaskSnapshot snapshot = Tasks.await(storageRef.putFile(uri));
            if (snapshot == null) {
                Log.w(TAG, "Upload snapshot was null");
                return new UploadResult(null, "Upload failed before Firebase returned a result.");
            }
            return new UploadResult(Tasks.await(snapshot.getStorage().getDownloadUrl()).toString(), null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to upload post image", e);
            return new UploadResult(null, buildUploadErrorMessage(e));
        }
    }

    @NonNull
    private String buildUploadErrorMessage(@NonNull Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof StorageException) {
            int errorCode = ((StorageException) cause).getErrorCode();
            if (errorCode == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "You need to sign in again before uploading a post image.";
            }
            if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                return "Firebase Storage denied this upload. Check Storage rules or App Check for this app.";
            }
            if (errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED
                    || errorCode == StorageException.ERROR_QUOTA_EXCEEDED) {
                return "Image upload could not finish right now. Please try again in a moment.";
            }
            if (errorCode == StorageException.ERROR_CANCELED) {
                return "Image upload was canceled before it finished.";
            }
        }

        String message = cause.getMessage();
        if (message != null) {
            String normalizedMessage = message.toLowerCase(java.util.Locale.US);
            if (normalizedMessage.contains("permission denied")) {
                return "Firebase Storage denied this upload. Check Storage rules or App Check for this app.";
            }
            if (normalizedMessage.contains("network")) {
                return "Image upload failed because the network connection was interrupted.";
            }
        }

        return "Failed to upload image: " + exception.getClass().getSimpleName();
    }

    private void refreshAuthSessionIfPossible(@NonNull FirebaseUser currentUser) {
        try {
            GetTokenResult tokenResult = Tasks.await(currentUser.getIdToken(true));
            if (tokenResult == null || tokenResult.getToken() == null || tokenResult.getToken().trim().isEmpty()) {
                Log.w(TAG, "Firebase auth token refresh returned an empty token before post upload");
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to refresh Firebase auth token before post upload; proceeding with current session", e);
        }
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

    @Nullable
    private String resolveLocationName(long locationId) {
        List<LocationEntity> locations = availableLocations.getValue();
        if (locations == null) {
            return null;
        }
        for (LocationEntity location : locations) {
            if (location != null && location.locationId == locationId) {
                return location.name;
            }
        }
        return null;
    }
}
