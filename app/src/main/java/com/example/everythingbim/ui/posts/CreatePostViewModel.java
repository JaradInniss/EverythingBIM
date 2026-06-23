package com.example.everythingbim.ui.posts;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.dao.UserDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.SelectedImage;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ViewModel for the {@link CreatePostActivity}. Manages the draft state of a
 * post (caption, image, location, tagged users) and persists the result to
 * both Firebase Storage (for the image) and Firestore (for the document).
 */
public class CreatePostViewModel extends AndroidViewModel {

    private static final String TAG = "CreatePostViewModel";

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

    private final PostDao postDao;
    private final LocationDao locationDao;
    private final UserDao userDao;
    private PostRepository postRepository;
    private UserRepository userRepository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final SingleLiveEvent<SelectedImage> navigationEvent = new SingleLiveEvent<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<LocationEntity> location = new MutableLiveData<>();
    private final MutableLiveData<String> caption = new MutableLiveData<>("");
    private final MutableLiveData<Uri> imageUri = new MutableLiveData<>();
    private final MutableLiveData<List<UserEntity>> taggedUsers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> selectedLocationId = new MutableLiveData<>();
    private final LiveData<List<LocationEntity>> availableLocations;

    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> postCreated = new MutableLiveData<>(false);
    private final MediatorLiveData<Boolean> isPostValid = new MediatorLiveData<>();

    private final MutableLiveData<String> userQuery = new MutableLiveData<>("");
    private final LiveData<List<UserWithProfile>> userSearchResults;

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        postDao = database.postDao();
        locationDao = database.locationDao();
        userDao = database.userDao();
        availableLocations = locationDao.getAllLocations();

        // Recompute validity whenever any required field changes.
        isPostValid.addSource(caption, text -> validatePost());
        isPostValid.addSource(imageUri, uri -> validatePost());
        isPostValid.addSource(location, value -> validatePost());

        // The previous implementation searched the local Room database, which
        // only knows about the two seed users. Searching Firestore instead
        // finds any user that has registered through the app, regardless of
        // whether they've been mirrored to the local cache.
        userSearchResults = Transformations.switchMap(userQuery,
                query -> getOrCreateUserRepository().searchUsersInFirestore(query));
    }

    private PostRepository getOrCreatePostRepository() {
        if (postRepository == null) {
            postRepository = new PostRepository(getApplication());
        }
        return postRepository;
    }

    private UserRepository getOrCreateUserRepository() {
        if (userRepository == null) {
            userRepository = new UserRepository(getApplication());
        }
        return userRepository;
    }

    // ---- Getters / Setters -------------------------------------------------

    @NonNull public LiveData<SelectedImage> getNavigationEvent() { return navigationEvent; }
    @NonNull public LiveData<String> getErrorMessage() { return errorMessage; }

    public LiveData<LocationEntity> getLocation() { return location; }

    public void setLocation(LocationEntity newLocation) {
        location.setValue(newLocation);
        validatePost();
    }

    public LiveData<String> getCaption() { return caption; }

    public void setCaption(@Nullable String text) {
        // Treat null as empty so observers can rely on a non-null value.
        caption.setValue(text == null ? "" : text);
    }

    public LiveData<Uri> getImageUri() { return imageUri; }

    public void setImageUri(@Nullable Uri uri) { imageUri.setValue(uri); }

    public LiveData<Long> getSelectedLocationId() { return selectedLocationId; }

    public void setSelectedLocationId(@Nullable Long locationId) { selectedLocationId.setValue(locationId); }

    public LiveData<List<LocationEntity>> getAvailableLocations() { return availableLocations; }

    public LiveData<List<UserEntity>> getTaggedUsers() { return taggedUsers; }

    public LiveData<Boolean> getIsSaving() { return isSaving; }

    public LiveData<Boolean> getPostCreated() { return postCreated; }

    public LiveData<List<UserWithProfile>> getUserSearchResults() { return userSearchResults; }

    public LiveData<Boolean> getIsPostValid() { return isPostValid; }

    // ---- Image selection ---------------------------------------------------

    public void onImageSelected(@NonNull SelectedImage selectedImage) {
        navigationEvent.setValue(selectedImage);
    }

    public void onSelectionError(@NonNull String message) {
        errorMessage.setValue(message);
    }

    // ---- Tagged user management -------------------------------------------

    public void addTaggedUser(@Nullable UserEntity user) {
        if (user == null) return; // Guard against null (see equals contract).
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
        if (user == null) return;
        List<UserEntity> currentTags = taggedUsers.getValue();
        if (currentTags != null) {
            List<UserEntity> updatedTags = new ArrayList<>(currentTags);
            updatedTags.remove(user);
            taggedUsers.setValue(updatedTags);
        }
    }

    /** Replaces the tagged users list with an empty one. */
    public void clearTaggedUsers() {
        taggedUsers.setValue(new ArrayList<>());
    }

    public void searchUsers(@Nullable String query) {
        userQuery.setValue(query == null ? "" : query);
    }

    // ---- Location management ---------------------------------------------

    public void setLocationFromPlaces(@NonNull Place place) {
        executorService.execute(() -> {
            double latitude = place.getLatLng() != null ? place.getLatLng().latitude : 0.0;
            double longitude = place.getLatLng() != null ? place.getLatLng().longitude : 0.0;
            LocationEntity newLocation = new LocationEntity(
                    place.getName(),
                    latitude,
                    longitude,
                    (place.getRating() != null) ? place.getRating().floatValue() : 0.0f,
                    false,
                    "GooglePlaces",
                    "",
                    (place.getTypes() != null && !place.getTypes().isEmpty()) ? place.getTypes().get(0).name() : "General",
                    "",
                    place.getAddress()
            );

            long id = locationDao.insert(newLocation);
            newLocation.setLocationId(id);
            location.postValue(newLocation);
            // Keep the legacy selectedLocationId in sync for backwards compatibility
            // with code that still reads it.
            selectedLocationId.postValue(id);
        });
    }

    // ---- Validation -------------------------------------------------------

    public void validatePost() {
        String currentCaption = caption.getValue();
        Uri currentUri = imageUri.getValue();
        LocationEntity currentLocation = location.getValue();

        boolean isValid = (currentCaption != null && !currentCaption.trim().isEmpty())
                && (currentUri != null)
                && (currentLocation != null);
        // Use postValue instead of setValue because this method can be called
        // from a background thread (e.g., loadLocationForEdit via executorService)
        isPostValid.postValue(isValid);
    }

    // ---- Reset all draft state -------------------------------------------

    /**
     * Wipes every draft field on this ViewModel. Called when the user taps the
     * "Clear" button in the activity. The activity is responsible for also
     * clearing its own UI text fields.
     */
    public void resetDraft() {
        caption.setValue("");
        imageUri.setValue(null);
        location.setValue(null);
        selectedLocationId.setValue(null);
        taggedUsers.setValue(new ArrayList<>());
        userQuery.setValue("");
        validatePost();
    }

    // ---- Setters for Edit Mode --------------------------------------------

    public void setImageUrl(@Nullable String url) {
        // For edit mode: image is already uploaded, just store the URL
        if (url != null && !url.isEmpty()) {
            imageUri.setValue(Uri.parse(url));
        }
    }

    public void setLocationName(@Nullable String name) {
        // For edit mode: pre-fill location name without creating a new location
        // The actual location entity will be fetched when saving
    }

    public void setTaggedUsers(@Nullable List<String> uids) {
        // For edit mode: pre-fill tagged users
        // In edit mode, we store UIDs and resolve them to UserEntity later
        taggedUsers.setValue(new ArrayList<>());
    }

    public void loadLocationForEdit(long locationId, @Nullable String locationName) {
        executorService.execute(() -> {
            LocationEntity loc = locationDao.getLocationByIdSync(locationId);
            if (loc != null) {
                location.postValue(loc);
                selectedLocationId.postValue(locationId);
                validatePost();
            } else if (locationName != null && !locationName.isEmpty()) {
                // Create a minimal location entity from the name
                LocationEntity fallback = new LocationEntity(
                        locationName, 0.0, 0.0, 0f, false, "", "", "", "", ""
                );
                fallback.setLocationId(locationId);
                location.postValue(fallback);
                selectedLocationId.postValue(locationId);
                validatePost();
            }
        });
    }

    /**
     * Sets a temporary location for immediate validation.
     * Used in edit mode before the actual location is loaded.
     */
    public void setLocationForValidation(@NonNull LocationEntity tempLocation) {
        location.setValue(tempLocation);
        validatePost();
    }

    // ---- Update existing post -------------------------------------------

    /**
     * Updates an existing post. Called from edit mode.
     * @param postId The local Room post ID
     * @param firestoreId The Firestore document ID
     */
    public void updatePost(long postId, @Nullable String firestoreId) {
        String currentCaption = caption.getValue();
        Uri currentUri = imageUri.getValue();
        LocationEntity currentLocation = location.getValue();
        List<UserEntity> currentTags = taggedUsers.getValue();

        if (currentLocation == null
                || currentCaption == null
                || currentCaption.trim().isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            errorMessage.setValue("Sign in to update a post.");
            return;
        }

        isSaving.setValue(true);
        executorService.execute(() -> {
            String uid = currentUser.getUid();
            String authorName = resolveAuthorName(currentUser);
            long authorLocalId = resolveAuthorId(currentUser);

            List<String> taggedUids = new ArrayList<>();
            if (currentTags != null) {
                for (UserEntity user : currentTags) {
                    if (user.firebaseUid != null) {
                        taggedUids.add(user.firebaseUid);
                    }
                }
            }

            // Determine image URL - if URI is a local file, upload it
            String imageUrl = null;
            if (currentUri != null) {
                if (currentUri.toString().startsWith("http")) {
                    // Already a remote URL, use it directly
                    imageUrl = currentUri.toString();
                } else {
                    // Local file, need to upload
                    UploadResult uploadResult = uploadImageToStorage(currentUri, uid);
                    if (uploadResult.downloadUrl == null) {
                        errorMessage.postValue("Failed to upload image. Please try again.");
                        isSaving.postValue(false);
                        return;
                    }
                    imageUrl = uploadResult.downloadUrl;
                }
            }

            // Fetch existing post to preserve fields that shouldn't change
            PostEntity existingPost = postDao.getPostByIdSync(postId);
            if (existingPost == null) {
                errorMessage.postValue("Post not found.");
                isSaving.postValue(false);
                return;
            }

            // Update the post
            existingPost.caption = currentCaption;
            existingPost.locationId = currentLocation.getLocationId();
            existingPost.locationName = currentLocation.getName();
            existingPost.locationLatitude = currentLocation.getLatitude();
            existingPost.locationLongitude = currentLocation.getLongitude();
            existingPost.locationAddress = currentLocation.getAddress();
            if (imageUrl != null) {
                existingPost.imageUrl = imageUrl;
            }
            existingPost.taggedUserUids = taggedUids;

            // If we have a firestoreId, update Room and Firestore
            if (firestoreId != null && !firestoreId.isEmpty()) {
                existingPost.firestoreId = firestoreId;
            }
            // Update Room first
            try {
                postDao.update(existingPost);
            } catch (Exception e) {
                errorMessage.postValue("Failed to update post locally.");
                isSaving.postValue(false);
                return;
            }
            // Then update Firestore directly (the Room listener will pick up the change)
            if (existingPost.firestoreId != null && !existingPost.firestoreId.isEmpty()) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                LiveData<PostEntity> firestoreLiveData = getOrCreatePostRepository().updatePostInFirestore(existingPost);
                mainHandler.post(() -> {
                    final boolean[] handled = {false};
                    androidx.lifecycle.Observer<PostEntity> observer = new androidx.lifecycle.Observer<PostEntity>() {
                        @Override
                        public void onChanged(PostEntity result) {
                            if (handled[0]) return;
                            handled[0] = true;
                            firestoreLiveData.removeObserver(this);
                            if (result != null) {
                                isSaving.postValue(false);
                                postCreated.postValue(true);
                            } else {
                                // Room was updated, but Firestore update failed
                                // The post is in an inconsistent state
                                errorMessage.postValue("Post saved locally but failed to sync. Try again later.");
                                isSaving.postValue(false);
                            }
                        }
                    };
                    firestoreLiveData.observeForever(observer);
                });
            } else {
                // No firestoreId means this is a local-only post
                isSaving.postValue(false);
                postCreated.postValue(true);
            }
        });
    }

    // ---- Persist to Firestore --------------------------------------------

    /**
     * Uploads the selected image to Firebase Storage, then writes the post
     * document to Firestore. On success, {@link #postCreated} is set to
     * {@code true}; on failure, {@link #errorMessage} is set.
     */
    public void createPost() {
        String currentCaption = caption.getValue();
        Uri currentUri = imageUri.getValue();
        LocationEntity currentLocation = location.getValue();
        List<UserEntity> currentTags = taggedUsers.getValue();

        if (currentLocation == null
                || currentUri == null
                || currentCaption == null
                || currentCaption.trim().isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            errorMessage.setValue("Sign in to create a post.");
            return;
        }
        String uid = currentUser.getUid();
        String authorName = resolveAuthorName(currentUser);
        long authorLocalId = resolveAuthorId(currentUser);

        List<String> taggedUids = new ArrayList<>();
        if (currentTags != null) {
            for (UserEntity user : currentTags) {
                if (user.firebaseUid != null) {
                    taggedUids.add(user.firebaseUid);
                }
            }
        }

        isSaving.setValue(true);
        executorService.execute(() -> {
            refreshAuthSessionIfPossible(currentUser);

            UploadResult uploadResult = uploadImageToStorage(currentUri, uid);
            if (uploadResult.downloadUrl == null) {
                errorMessage.postValue(uploadResult.errorMessage != null
                        ? uploadResult.errorMessage
                        : "Failed to upload image. Please try again.");
                isSaving.postValue(false);
                return;
            }

            PostEntity newPost = new PostEntity(
                    currentLocation.getLocationId(),
                    currentLocation.getName(),
                    authorLocalId,
                    authorName,
                    uid,
                    currentCaption,
                    uploadResult.downloadUrl,
                    System.currentTimeMillis(),
                    taggedUids
            );
            newPost.setPortableLocationSnapshot(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    currentLocation.getAddress(),
                    null,
                    null
            );
            newPost.likeCount = 0;

            // Write to Firestore via a one-shot callback. The callback fires on
            // a background thread (Firestore worker), so we use postValue on the
            // already-existing LiveData fields; the Activity observes them on
            // the main thread. This avoids the previous observeForever-on-
            // background-thread FATAL that crashed the app on every post create.
            getOrCreatePostRepository().createPostInFirestoreAsync(newPost, (persisted, error) -> {
                if (error != null || persisted == null) {
                    errorMessage.postValue("Failed to share post. Please try again.");
                    isSaving.postValue(false);
                    return;
                }
                isSaving.postValue(false);
                postCreated.postValue(true);
            });
        });
    }

    /**
     * Uploads the image at {@code uri} to Firebase Storage under the path
     * {@code post_images/{uid}/{timestamp}.jpg} and returns the resulting
     * download URL. Returns {@code null} if the upload fails for any reason.
     *
     * This call is blocking; callers should invoke it from a background
     * executor.
     */
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

    private long resolveAuthorId(@Nullable FirebaseUser currentUser) {
        if (currentUser == null || currentUser.getUid() == null) {
            return 0L;
        }
        return Integer.toUnsignedLong(currentUser.getUid().hashCode());
    }

    @Nullable
    private String resolveAuthorName(@Nullable FirebaseUser currentUser) {
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
