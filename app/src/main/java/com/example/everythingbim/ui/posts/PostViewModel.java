package com.example.everythingbim.ui.posts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.repository.PostRepository;

import java.util.List;

public class PostViewModel extends AndroidViewModel {
    private final PostRepository repository;
    private final LiveData<List<PostEntity>> posts;
    private final MutableLiveData<String> filterType = new MutableLiveData<>("account");

    public PostViewModel(@NonNull Application application) {
        super(application);
        repository = new PostRepository(application);
        posts = repository.getRandomizedPosts();
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<String> getFilterType() {
        return filterType;
    }

    public void setFilterType(String type) {
        filterType.setValue(type);
    }
}
