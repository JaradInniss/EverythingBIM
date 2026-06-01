package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

import com.example.everythingbim.data.local.entities.BusinessUserEntity;
import com.example.everythingbim.data.local.entities.GeneralUserEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity userEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGeneralUser(GeneralUserEntity generalUser);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBusinessUser(BusinessUserEntity businessUser);

    @Query("SELECT * FROM users")
    LiveData<List<UserEntity>> getAllUsers();

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserEntity> getUserById(long userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserWithProfile> getUserWithProfile(long userId);

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%'")
    LiveData<List<UserWithProfile>> searchUsers(String query);

    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}
