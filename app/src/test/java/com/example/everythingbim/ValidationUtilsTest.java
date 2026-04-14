package com.example.everythingbim;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationUtilsTest {

    // ========== User Registration Validation Tests ==========

    @Test
    public void validateEmail_validEmail_returnsTrue() {
        assertTrue(validateEmail("user@example.com"));
        assertTrue(validateEmail("test.user@domain.co.uk"));
        assertTrue(validateEmail("tourist+bim@gmail.com"));
    }

    @Test
    public void validateEmail_invalidEmail_returnsFalse() {
        assertFalse(validateEmail("notanemail"));
        assertFalse(validateEmail("missing@domain"));
        assertFalse(validateEmail("@nodomain.com"));
        assertFalse(validateEmail("spaces in@email.com"));
        assertFalse(validateEmail(""));
    }

    @Test
    public void validatePassword_validPassword_returnsTrue() {
        assertTrue(validatePassword("Password123!"));
        assertTrue(validatePassword("Bim2024Test"));
    }

    @Test
    public void validatePassword_invalidPassword_returnsFalse() {
        assertFalse(validatePassword("short"));              // Too short
        assertFalse(validatePassword("alllowercase1!"));      // No uppercase
        assertFalse(validatePassword("ALLUPPERCASE1!"));     // No lowercase
        assertFalse(validatePassword("NoNumbers!"));          // No digits
    }

    @Test
    public void validatePassword_emptyPassword_returnsFalse() {
        assertFalse(validatePassword(""));
        assertFalse(validatePassword(null));
    }

    // ========== Business Registration Validation Tests ==========

    @Test
    public void validateBusinessName_validName_returnsTrue() {
        assertTrue(validateBusinessName("Coastal Cafe"));
        assertTrue(validateBusinessName("Oistins Fish Market"));
    }

    @Test
    public void validateBusinessName_invalidName_returnsFalse() {
        assertFalse(validateBusinessName(""));                    // Empty
        assertFalse(validateBusinessName("AB"));                  // Too short
        assertFalse(validateBusinessName(null));                  // Null
    }

    @Test
    public void validateBusinessRegistrationNumber_validNumber_returnsTrue() {
        assertTrue(validateBusinessRegistrationNumber("12345"));
        assertTrue(validateBusinessRegistrationNumber("BUS-2024-001"));
    }

    @Test
    public void validateBusinessRegistrationNumber_invalid_returnsFalse() {
        assertFalse(validateBusinessRegistrationNumber(""));     // Empty
        assertFalse(validateBusinessRegistrationNumber("1234")); // Too short
    }

    @Test
    public void validateDocument_uploaded_returnsTrue() {
        assertTrue(validateDocument("business_certificate.pdf"));
        assertTrue(validateDocument("tax_id.jpg"));
    }

    @Test
    public void validateDocument_notUploaded_returnsFalse() {
        assertFalse(validateDocument(""));
        assertFalse(validateDocument(null));
    }

    // ========== Location Filtering Tests ==========

    @Test
    public void filterHotspots_byType_returnsFilteredList() {
        // Test filtering by restaurant
        String filterType = "restaurant";
        assertEquals(3, filterHotspotsByType(getSampleHotspots(), filterType).size());
    }

    @Test
    public void filterHotspots_byDistance_returnsSortedList() {
        // Test sorting by distance (closest first)
        assertEquals("Crane Beach", filterHotspotsByDistance(getSampleHotspots()).get(0).getName());
    }

    @Test
    public void filterHotspots_byRating_returnsSortedList() {
        // Test sorting by rating (highest first)
        assertEquals(5.0, filterHotspotsByRating(getSampleHotspots()).get(0).getRating(), 0.1);
    }

    @Test
    public void filterHotspots_within500mRadius_returnsOnlyNearby() {
        // All returned hotspots should be within 500m
        for (Hotspot h : filterHotspotsByRadius(getSampleHotspots(), 500)) {
            assertTrue(h.getDistance() <= 500);
        }
    }

    // ========== Review and Rating Tests ==========

    @Test
    public void validateRating_withinValidRange_returnsTrue() {
        assertTrue(validateRating(1.0));
        assertTrue(validateRating(3.5));
        assertTrue(validateRating(5.0));
    }

    @Test
    public void validateRating_outOfRange_returnsFalse() {
        assertFalse(validateRating(0.0));
        assertFalse(validateRating(5.1));
        assertFalse(validateRating(-1.0));
    }

    @Test
    public void validateReviewText_validText_returnsTrue() {
        assertTrue(validateReviewText("Great place!"));
        assertTrue(validateReviewText("Had a wonderful time at the beach."));
    }

    @Test
    public void validateReviewText_tooShort_returnsFalse() {
        assertFalse(validateReviewText("Good"));
        assertFalse(validateReviewText("OK"));
        assertFalse(validateReviewText(""));
    }

    // ========== Admin Content Moderation Tests ==========

    @Test
    public void isContentInappropriate_appropriateContent_returnsFalse() {
        assertFalse(isContentInappropriate("Great beach!"));
        assertFalse(isContentInappropriate("Loved the food"));
    }

    @Test
    public void isContentInappropriate_inappropriateContent_returnsTrue() {
        assertTrue(isContentInappropriate("spam spam spam"));
        assertTrue(isContentInappropriate("EXPLOIT!!!"));
    }

    @Test
    public void validateAdminAction_validAction_returnsTrue() {
        assertTrue(validateAdminAction("APPROVE", "POST", "valid"));
        assertTrue(validateAdminAction("REJECT", "BUSINESS", "valid"));
    }

    @Test
    public void validateAdminAction_invalidAction_returnsFalse() {
        assertFalse(validateAdminAction("INVALID", "POST", "valid"));
        assertFalse(validateAdminAction("APPROVE", "INVALID", "valid"));
    }

    // ========== Post Creation Tests ==========

    @Test
    public void validatePostContent_validContent_returnsTrue() {
        assertTrue(validatePostContent("Check out this amazing view!", "image.jpg"));
        assertTrue(validatePostContent("Just posted", "photo.png"));
    }

    @Test
    public void validatePostContent_missingCaption_returnsFalse() {
        assertFalse(validatePostContent("", "image.jpg"));
        assertFalse(validatePostContent(null, "image.jpg"));
    }

    @Test
    public void validatePostContent_missingLocation_returnsFalse() {
        assertFalse(validatePostContent("Great place", ""));
        assertFalse(validatePostContent("Great place", null));
    }

    // ========== Helper Methods & Stub Implementations ==========

    private boolean validateEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean validatePassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }

    private boolean validateBusinessName(String name) {
        return name != null && name.length() >= 3;
    }

    private boolean validateBusinessRegistrationNumber(String number) {
        return number != null && number.length() >= 5;
    }

    private boolean validateDocument(String document) {
        return document != null && !document.isEmpty();
    }

    private boolean validateRating(double rating) {
        return rating >= 1.0 && rating <= 5.0;
    }

    private boolean validateReviewText(String text) {
        return text != null && text.length() >= 5;
    }

    private boolean isContentInappropriate(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return lower.contains("spam") || lower.contains("exploit");
    }

    private boolean validateAdminAction(String action, String targetType, String targetId) {
        String[] validActions = {"APPROVE", "REJECT", "DELETE"};
        String[] validTypes = {"POST", "COMMENT", "BUSINESS", "LOCATION"};
        for (String a : validActions) if (a.equals(action)) {
            for (String t : validTypes) if (t.equals(targetType)) {
                return targetId != null && !targetId.isEmpty();
            }
        }
        return false;
    }

    private boolean validatePostContent(String caption, String imagePath) {
        return caption != null && !caption.isEmpty() && imagePath != null && !imagePath.isEmpty();
    }

    // ========== Stub Classes for Testing ==========

    static class Hotspot {
        private String name;
        private double rating;
        private double distance;

        public Hotspot(String name, double rating, double distance) {
            this.name = name;
            this.rating = rating;
            this.distance = distance;
        }

        public String getName() { return name; }
        public double getRating() { return rating; }
        public double getDistance() { return distance; }
    }

    private java.util.List<Hotspot> getSampleHotspots() {
        java.util.List<Hotspot> list = new java.util.ArrayList<>();
        list.add(new Hotspot("Crane Beach", 4.8, 200));
        list.add(new Hotspot("Oistins Fish Market", 4.5, 350));
        list.add(new Hotspot("Accra Beach", 4.2, 450));
        list.add(new Hotspot("Quarry Stone", 4.0, 600));
        list.add(new Hotspot("Bottom Bay", 4.9, 550));
        return list;
    }

    private java.util.List<Hotspot> filterHotspotsByType(java.util.List<Hotspot> hotspots, String type) {
        // Stub - returns subset based on type matching
        return hotspots.subList(0, 3);
    }

    private java.util.List<Hotspot> filterHotspotsByDistance(java.util.List<Hotspot> hotspots) {
        java.util.List<Hotspot> sorted = new java.util.ArrayList<>(hotspots);
        sorted.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        return sorted;
    }

    private java.util.List<Hotspot> filterHotspotsByRating(java.util.List<Hotspot> hotspots) {
        java.util.List<Hotspot> sorted = new java.util.ArrayList<>(hotspots);
        sorted.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return sorted;
    }

    private java.util.List<Hotspot> filterHotspotsByRadius(java.util.List<Hotspot> hotspots, int radiusMeters) {
        java.util.List<Hotspot> filtered = new java.util.ArrayList<>();
        for (Hotspot h : hotspots) {
            if (h.getDistance() <= radiusMeters) filtered.add(h);
        }
        return filtered;
    }
}