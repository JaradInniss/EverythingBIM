package com.example.everythingbim.ui.posts;

import android.app.Application;
import android.net.Uri;
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
import com.google.firebase.storage.FirebaseStorage;
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

    private final PostDao postDao;
    private final LocationDao locationDao;
    private final UserDao userDao;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
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
        postRepository = new PostRepository(application);
        userRepository = new UserRepository(application);
        availableLocations = locationDao.getAllLocations();

        // Recompute validity whenever caption or image changes.
        isPostValid.addSource(caption, text -> validatePost());
        isPostValid.addSource(imageUri, uri -> validatePost());

        // The previous implementation searched the local Room database, which
        // only knows about the two seed users. Searching Firestore instead
        // finds any user that has registered through the app, regardless of
        // whether they've been mirrored to the local cache.
        userSearchResults = Transformations.switchMap(userQuery, userRepository::searchUsersInFirestore);
    }

    // ---- Getters / Setters -------------------------------------------------

    @NonNull public LiveData<SelectedImage> getNavigationEvent() { return navigationEvent; }
    @NonNull public LiveData<String> getErrorMessage() { return errorMessage; }

    public LiveData<LocationEntity> getLocation() { return location; }

    public void setLocation(LocationEntity newLocation) { location.setValue(newLocation); }

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
        if (currentTags != null && !currentTags.contains(user)) {
            currentTags.add(user);
            taggedUsers.setValue(currentTags);
        }
    }

    public void removeTaggedUser(@Nullable UserEntity user) {
        if (user == null) return;
        List<UserEntity> currentTags = taggedUsers.getValue();
        if (currentTags != null) {
            currentTags.remove(user);
            taggedUsers.setValue(currentTags);
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
            LocationEntity newLocation = new LocationEntity(
                    place.getName(),
                    place.getLatLng().latitude,
                    place.getLatLng().longitude,
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

        boolean isValid = (currentCaption != null && !currentCaption.trim().isEmpty())
                && (currentUri != null);
        isPostValid.setValue(isValid);
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
        String uid = currentUser != null ? currentUser.getUid() : null;
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
            String imageDownloadUrl = uploadImageToStorage(currentUri, uid);
            if (imageDownloadUrl == null) {
                errorMessage.postValue("Failed to upload image. Please try again.");
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
                    imageDownloadUrl,
                    System.currentTimeMillis(),
                    taggedUids
            );

            // Write to Firestore via a one-shot callback. The callback fires on
            // a background thread (Firestore worker), so we use postValue on the
            // already-existing LiveData fields; the Activity observes them on
            // the main thread. This avoids the previous observeForever-on-
            // background-thread FATAL that crashed the app on every post create.
            postRepository.createPostInFirestoreAsync(newPost, (persisted, error) -> {
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
    private String uploadImageToStorage(@NonNull Uri uri, @Nullable String uid) {
        try {
            String safeUid = (uid == null || uid.isEmpty()) ? "anonymous" : uid;
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("post_images/" + safeUid + "/" + System.currentTimeMillis() + ".jpg");

            UploadTask.TaskSnapshot snapshot = Tasks.await(storageRef.putFile(uri));
            if (snapshot == null) {
                Log.w(TAG, "Upload snapshot was null");
                return null;
            }
            return Tasks.await(snapshot.getStorage().getDownloadUrl()).toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to upload post image", e);
            return null;
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
