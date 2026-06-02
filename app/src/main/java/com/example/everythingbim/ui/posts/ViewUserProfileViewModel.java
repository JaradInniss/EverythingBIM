package com.example.everythingbim.ui.posts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;
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
    private final LiveData<UserWithProfile> userWithProfile;
    private final LiveData<List<PostEntity>> userPosts;
    private final MutableLiveData<Long> userId = new MutableLiveData<>();

    public ViewUserProfileViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
        postRepository = new PostRepository(application);
        
        // SwitchMap ensures we fetch a new user whenever the userId changes
        userWithProfile = Transformations.switchMap(userIdLiveData, userRepository::getUserWithProfile);

        userPosts = Transformations.switchMap(userIdLiveData, postRepository::getPostsByUserId);
    }

    public LiveData<UserWithProfile> getUserWithProfile() {
        return userWithProfile;
    }

    public LiveData<List<PostEntity>> getUserPosts() {
        return userPosts;
    }

    /**
     * Set the target user ID to fetch profile data.
     */
    public void setUserId(long userId) {
        userIdLiveData.setValue(userId);
    }

    public void reportAccount(long userId, String reason, String description) {
        ReportEntity report = new ReportEntity(userId, reason, description, RequestReportStatus.PENDING, System.currentTimeMillis());
        postRepository.insertReport(report);
    }

}
