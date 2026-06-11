package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.everythingbim.data.models.UserType;

import java.util.Objects;


// User Entity

@Entity( tableName = "users" )
public class UserEntity {
    @PrimaryKey( autoGenerate = true )
    public long userId;

    public String username;
    public String passwordHash;
    public String email;
    public UserType userType;
    public boolean emailVerified;
    public long createdAt;
    public String firebaseUid;

    public UserEntity(String username, String passwordHash, String email, UserType userType, boolean emailVerified, long createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.userType = userType;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    public UserEntity() {
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserEntity)) return false;
        UserEntity that = (UserEntity) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
