package com.example.everythingbim.data.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class BusinessProfile {
    private String userId;
    private String username;
    private String BusinessName;  // Firestore field name - capital B and N
    private String businessEmail;
    private String phone;
    private String address;
    private String description;
    private List<String> imageUrls;
    private List<String> fileUrls;
    private Timestamp createdAt;
    private String userType = "business";
    private String businessType;  // Restaurant, Retail, etc.
    private String bio;  // Personal/about text, separate from description
    private List<String> addresses;  // Multiple addresses for business
    // Verification fields - these control admin workflow
    private String verificationStatus = "In Review";  // "In Review", "Completed", "Rejected"
    private Boolean verified = false;                // true when approved
    private Boolean read = false;                    // true when admin has viewed
    private Timestamp resolvedAt;
    private String resolvedBy;
    private String rejectionReason;

    // Required empty constructor for Firestore
    public BusinessProfile() {}

    public BusinessProfile(String userId, String username, String BusinessName, String businessEmail,
                           String businessType, String phone, String address, String description, String bio,
                           List<String> imageUrls, List<String> fileUrls, Timestamp createdAt) {
        this.userId = userId;
        this.username = username;
        this.BusinessName = BusinessName;
        this.businessEmail = businessEmail;
        this.businessType = businessType;
        this.phone = phone;
        this.address = address;
        this.description = description;
        this.bio = bio;
        this.imageUrls = imageUrls;
        this.fileUrls = fileUrls;
        this.createdAt = createdAt;
        this.verificationStatus = "In Review";
        this.verified = false;
        this.read = false;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getBusinessName() { return BusinessName; }
    public void setBusinessName(String BusinessName) { this.BusinessName = BusinessName; }
    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public List<String> getAddresses() { return addresses; }
    public void setAddresses(List<String> addresses) { this.addresses = addresses; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<String> getFileUrls() { return fileUrls; }
    public void setFileUrls(List<String> fileUrls) { this.fileUrls = fileUrls; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getUserType() { return userType; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}