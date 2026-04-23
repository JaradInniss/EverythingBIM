package com.example.everythingbim.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.entities.PostEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostRepository {
    private final PostDao postDao;
    private final ExecutorService executorService;

    public PostRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        postDao = db.postDao();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<PostEntity>> getRandomizedPosts() {
        return postDao.getRandomizedPosts();
    }

    public void insert(PostEntity post) {
        executorService.execute(() -> postDao.insert(post));
    }
}
