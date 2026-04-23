package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.everythingbim.data.local.entities.PostEntity;

import java.util.List;

@Dao
public interface PostDao {
    @Query("SELECT * FROM posts")
    LiveData<List<PostEntity>> getAllPosts();

    @Insert
    long insert(PostEntity post);

    @Query("SELECT * FROM posts ORDER BY RANDOM()")
    LiveData<List<PostEntity>> getRandomizedPosts();
}
