package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

import com.example.everythingbim.data.models.RequestReportStatus;

import java.util.List;

// Business User Entity
@Entity(
        tableName = "business_users",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("userId")
)
public class BusinessUserEntity {
    @PrimaryKey(autoGenerate = true)
    public long userId;

    public String businessName;
    public String phoneNumber;
    public String address;
    public String businessDescription;
    public String profilePictureUrl;
    public String bio;
    public List<String> businessCertificates;
    public RequestReportStatus verificationStatus;
    public long verifiedAt;

    public BusinessUserEntity(String businessName, String phoneNumber, String address, String businessDescription, String profilePictureUrl, String bio, List<String> businessCertificates, RequestReportStatus verificationStatus, long verifiedAt) {
        this.businessName = businessName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.businessDescription = businessDescription;
        this.profilePictureUrl = profilePictureUrl;
        this.bio = bio;
        this.businessCertificates = businessCertificates;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBusinessDescription() {
        return businessDescription;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public List<String> getBusinessCertificates() {
        return businessCertificates;
    }

    public void setBusinessCertificates(List<String> businessCertificates) {
        this.businessCertificates = businessCertificates;
    }

    public RequestReportStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(RequestReportStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public long getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(long verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
