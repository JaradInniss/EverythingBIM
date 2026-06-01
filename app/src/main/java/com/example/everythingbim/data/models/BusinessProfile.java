package com.example.everythingbim.data.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class BusinessProfile {
    private String userId;
    private String companyName;
    private String businessEmail;
    private String phone;
    private String address;
    private String description;
    private List<String> imageUrls;
    private List<String> fileUrls;
    private Timestamp createdAt;
    private String userType = "business";

    // Required empty constructor for Firestore
    public BusinessProfile() {}

    public BusinessProfile(String userId, String companyName, String businessEmail,
                           String phone, String address, String description,
                           List<String> imageUrls, List<String> fileUrls, Timestamp createdAt) {
        this.userId = userId;
        this.companyName = companyName;
        this.businessEmail = businessEmail;
        this.phone = phone;
        this.address = address;
        this.description = description;
        this.imageUrls = imageUrls;
        this.fileUrls = fileUrls;
        this.createdAt = createdAt;
    }

    // Getters and setters (generate via IDE)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }

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
}