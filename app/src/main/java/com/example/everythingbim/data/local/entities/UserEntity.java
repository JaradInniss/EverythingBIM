package com.example.everythingbim.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.everythingbim.data.models.UserType;

import java.util.Objects;


/**
 * User Entity
 *
 * Represents a user account that exists in both the local Room database and the
 * remote Firestore {@code users} collection. The {@code firebaseUid} field links
 * the local row to the Firebase Auth UID used for cross-device sign-in and for
 * Firestore lookups.
 */
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

    /**
     * Firebase Auth UID for this user. May be {@code null} for guest accounts
     * or users that were created before Firebase Auth was integrated.
     */
    public String firebaseUid;

    public UserEntity(String username, String passwordHash, String email, UserType userType, boolean emailVerified, long createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.userType = userType;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    /**
     * No-arg constructor required by Firestore for {@code DocumentSnapshot.toObject()}.
     */
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

    /**
     * Equality is based on {@link #userId} so that two in-memory representations
     * of the same row compare as equal, which is required for tag list de-duplication
     * and {@link java.util.List#contains(Object)} lookups.
     */
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

    @NonNull
    @Override
    public String toString() {
        return "UserEntity{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", userType=" + userType +
                ", firebaseUid='" + firebaseUid + '\'' +
                '}';
    }
}
