package com.example.everythingbim.ui.posts;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
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
import com.google.android.libraries.places.api.model.Place;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CreatePostViewModel extends AndroidViewModel {

    private final PostDao postDao;
    private final LocationDao locationDao;
    private final UserDao userDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final SingleLiveEvent<SelectedImage> navigationEvent = new SingleLiveEvent<>();

    // Data binding variables
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<LocationEntity> location = new MutableLiveData<>();
    private final MutableLiveData<String> caption = new MutableLiveData<>("");
    private final MutableLiveData<Uri> selectedImageUri = new MutableLiveData<>();
    private final MutableLiveData<List<UserEntity>> taggedUsers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> postCreated = new MutableLiveData<>(false);
    private final MediatorLiveData<Boolean> isPostValid = new MediatorLiveData<>();

    // Search results (Users only, Location handled via Places API in Activity)
    private final MutableLiveData<String> userQuery = new MutableLiveData<>("");
    private final LiveData<List<UserWithProfile>> userSearchResults;

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        postDao = db.postDao();
        locationDao = db.locationDao();
        userDao = db.userDao();

        // Logic for isPostValid
        isPostValid.addSource(location, loc -> validatePost());
        isPostValid.addSource(caption, text -> validatePost());
        isPostValid.addSource(selectedImageUri, uri -> validatePost());

        userSearchResults = Transformations.switchMap(userQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return new MutableLiveData<>(new ArrayList<>());
            }
            return userDao.searchUsers(query);
        });
        
        // Repair legacy posts on initialization
        repairLegacyPosts();
    }

    @NonNull
    public LiveData<SelectedImage> getNavigationEvent() {
        return navigationEvent;
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<LocationEntity> getLocation() {
        return location;
    }

    public void setLocation(LocationEntity location) {
        this.location.setValue(location);
    }

    public LiveData<String> getCaption() {
        return caption;
    }

    public void setCaption(String text) {
        caption.setValue(text);
    }

    public LiveData<Uri> getSelectedImageUri() {
        return selectedImageUri;
    }

    public void setSelectedImageUri(Uri uri) {
        selectedImageUri.setValue(uri);
    }

    public LiveData<List<UserEntity>> getTaggedUsers() {
        return taggedUsers;
    }

    public void onImageSelected(@NonNull SelectedImage selectedImage) { navigationEvent.setValue(selectedImage); }

    public void onSelectionError(@NonNull String message) {
        errorMessage.setValue(message);
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getPostCreated() {
        return postCreated;
    }

    public LiveData<Boolean> getIsPostValid() {
        return isPostValid;
    }

    public void validatePost() {
        LocationEntity currentLocation = location.getValue();
        String currentCaption = caption.getValue();
        Uri currentUri = selectedImageUri.getValue();

        boolean isValid = (currentLocation != null) && 
                          (currentCaption != null && !currentCaption.trim().isEmpty()) && 
                          (currentUri != null);

        isPostValid.setValue(isValid);
    }

    public void searchUsers(String query) {
        userQuery.setValue(query);
    }

    public LiveData<List<UserWithProfile>> getUserSearchResults() {
        return userSearchResults;
    }

    public void setLocationFromPlaces(Place place) {
        executorService.execute(() -> {
            // Check if location exists or create new
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
        });
    }

    public void addTaggedUser(UserEntity user) {
        List<UserEntity> currentTags = taggedUsers.getValue();
        if (currentTags != null && !currentTags.contains(user)) {
            currentTags.add(user);
            taggedUsers.setValue(currentTags);
        }
    }

    public void removeTaggedUser(UserEntity user) {
        List<UserEntity> currentTags = taggedUsers.getValue();
        if (currentTags != null) {
            currentTags.remove(user);
            taggedUsers.setValue(currentTags);
        }
    }

    private String saveImageToInternalStorage(Uri uri) {
        if (uri == null) return null;
        
        // If it's already a file URI, no need to copy
        if ("file".equals(uri.getScheme())) return uri.toString();
        
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "POST_IMG_" + timeStamp + ".jpg";
            File file = new File(getApplication().getFilesDir(), fileName);
            
            try (InputStream inputStream = getApplication().getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(file)) {
                
                if (inputStream == null) return uri.toString();
                
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return Uri.fromFile(file).toString();
            }
        } catch (SecurityException e) {
            Log.e("CreatePostViewModel", "Permission expired for URI: " + uri);
            return uri.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return uri.toString();
        }
    }

    public void repairLegacyPosts() {
        executorService.execute(() -> {
            List<PostEntity> allPosts = postDao.getAllPostsSync(); // Assume this exists or add it
            if (allPosts == null) return;

            for (PostEntity post : allPosts) {
                if (post.imageUrl != null && post.imageUrl.startsWith("content://")) {
                    Uri uri = Uri.parse(post.imageUrl);
                    String localPath = saveImageToInternalStorage(uri);
                    
                    // Only update if we successfully changed the path to a file URI
                    if (localPath != null && localPath.startsWith("file://")) {
                        post.imageUrl = localPath;
                        postDao.update(post);
                        Log.d("CreatePostViewModel", "Repaired post " + post.postId + " with local image.");
                    }
                }
            }
        });
    }

    public void createPost() {
        String currentCaption = caption.getValue();
        Uri currentUri = selectedImageUri.getValue();
        LocationEntity currentLocation = location.getValue();
        List<UserEntity> currentTags = taggedUsers.getValue();

        if (currentLocation == null || currentUri == null) {
            return;
        }

        isSaving.setValue(true);
        executorService.execute(() -> {
            String savedImageUrl = saveImageToInternalStorage(currentUri);
            
            List<Long> taggedUserIds = new ArrayList<>();
            if (currentTags != null) {
                for (UserEntity user : currentTags) {
                    taggedUserIds.add(user.getUserId());
                }
            }

            PostEntity newPost = new PostEntity(
                    currentLocation.getLocationId(),
                    currentLocation.getName(),
                    0, // authorId (should come from Auth)
                    "Username", // authorName (should come from Auth)
                    currentCaption,
                    savedImageUrl,
                    System.currentTimeMillis(),
                    taggedUserIds
            );
            
            postDao.insert(newPost);
            
            isSaving.postValue(false);
            postCreated.postValue(true);
        });
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return Transformations.map(locationDao.getLocationWithDetailsById(locationId), details -> details != null ? details.location : null);
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
