package com.example.everythingbim.ui.posts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.RequestReportStatus;
import com.example.everythingbim.data.repository.PostRepository;
import com.example.everythingbim.data.repository.UserRepository;

import java.util.List;


// ViewModel for Viewing A User's Profile Information

public class ViewUserProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final MutableLiveData<Long> userIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> userUidLiveData = new MutableLiveData<>();
    private final LiveData<UserWithProfile> userWithProfile;
    private final LiveData<UserWithProfile> userWithProfileByUid;
    private final LiveData<List<PostEntity>> userPosts;
    private final MutableLiveData<Long> userId = new MutableLiveData<>();

    public ViewUserProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        postRepository = new PostRepository(application);

        // SwitchMap ensures we fetch a new user whenever the userId changes
        userWithProfile = Transformations.switchMap(userIdLiveData, userRepository::getUserWithProfile);

        // SwitchMap ensures we fetch a new user whenever the firebaseUid
        // changes. Used when the user is not in the local Room cache
        // (e.g. navigating from a tagged-user chip on the View Post page).
        userWithProfileByUid = Transformations.switchMap(userUidLiveData, userRepository::getUserByFirebaseUid);

        userPosts = Transformations.switchMap(userIdLiveData, postRepository::getPostsByUserId);
    }

    public LiveData<UserWithProfile> getUserWithProfile() {
        // The activity observes a single LiveData. Prefer the UID-based
        // stream when a UID has been set (i.e. the user is not in the
        // local Room cache); otherwise fall back to the local-id-based
        // stream. This is implemented as a MediatorLiveData so the
        // activity doesn't have to know which path produced the value.
        androidx.lifecycle.MediatorLiveData<UserWithProfile> merged = new androidx.lifecycle.MediatorLiveData<>();
        merged.addSource(userWithProfile, value -> {
            if (userUidLiveData.getValue() == null) {
                merged.setValue(value);
            }
        });
        merged.addSource(userWithProfileByUid, value -> {
            if (userUidLiveData.getValue() != null) {
                merged.setValue(value);
            }
        });
        return merged;
    }

    public LiveData<List<PostEntity>> getUserPosts() {
        return userPosts;
    }

    /**
     * Get posts by author's Firebase UID. Used when the user is not in local cache.
     */
    public LiveData<List<PostEntity>> getPostsByAuthorUid(@NonNull String authorUid) {
        return postRepository.getPostsByAuthorUid(authorUid);
    }

    /**
     * Get a user by their local Room ID. Used to look up firebaseUid.
     */
    public LiveData<UserEntity> getUserById(long userId) {
        return userRepository.getUserById(userId);
    }

    /**
     * Set the target user ID to fetch profile data.
     */
    public void setUserId(long userId) {
        userIdLiveData.setValue(userId);
    }

    /**
     * Load the user profile by their Firebase Auth UID. Use this when the
     * user is not in the local Room cache (e.g. the user is being viewed
     * from a tagged-user chip on the View Post page). Note that posts for
     * such users are not available - {@link #getUserPosts()} continues to
     * read from the local-id path and will simply stay empty.
     */
    public void loadUserByFirebaseUid(@NonNull String firebaseUid) {
        userUidLiveData.setValue(firebaseUid);
    }

    public void reportAccount(long userId, String reason, String description) {
        ReportEntity report = new ReportEntity(userId, reason, description, RequestReportStatus.PENDING, System.currentTimeMillis());
        postRepository.insertReport(report);
    }

}
