package com.example.everythingbim.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.UserDao;
import com.example.everythingbim.data.local.entities.BusinessUserEntity;
import com.example.everythingbim.data.local.entities.GeneralUserEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.RequestReportStatus;
import com.example.everythingbim.data.models.UserType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    private final UserDao userDao;
    private final ExecutorService executorService;

    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();
        executorService = Executors.newFixedThreadPool(2);
    }


    public void insert(UserEntity user) {
        executorService.execute(() -> userDao.insert(user));
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return userDao.getAllUsers();
    }

    public LiveData<UserEntity> getUserById(long userId) {
        return userDao.getUserById(userId);
    }

    public LiveData<UserWithProfile> getUserWithProfile(long userId) {
        return userDao.getUserWithProfile(userId);
    }

    public LiveData<List<UserWithProfile>> searchUsers(String query) {
        return userDao.searchUsers(query);
    }

    public void seedUsersIfEmpty() {
        executorService.execute(() -> {
            if (userDao.getUserCount() == 0) {
                // 1. Create a General User
                UserEntity generalUser = new UserEntity(
                        "NatureLover",
                        "hashed_password",
                        "nature@example.com",
                        UserType.GENERAL,
                        true,
                        System.currentTimeMillis()
                );
                long genId = userDao.insert(generalUser);
                GeneralUserEntity genProfile = new GeneralUserEntity(
                        "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d",
                        "Passionate about the outdoors and Barbados' natural beauty! 🌴🌊"
                );
                genProfile.userId = genId;
                userDao.insertGeneralUser(genProfile);

                // 2. Create a Business User
                UserEntity businessUser = new UserEntity(
                        "Chefette",
                        "hashed_password",
                        "info@chefette.com",
                        UserType.BUSINESS,
                        true,
                        System.currentTimeMillis()
                );
                long bizId = userDao.insert(businessUser);

                BusinessUserEntity bizProfile = new BusinessUserEntity(
                        "Chefette Restaurant",
                        "+1(246) 430-3339",
                        "Broad Street, Bridgetown, Barbados",
                        "The legendary local favorite. Home of the famous Broasted Chicken and Roti!",
                        "https://www.chefette.com/images/chefette-logo.png",
                        "Quality Food, Quick Service, Great Value!",
                        null, // businessCertificates
                        RequestReportStatus.ACCEPTED,
                        System.currentTimeMillis()
                );
                bizProfile.userId = bizId;
                userDao.insertBusinessUser(bizProfile);
            }
        });
    }
}
