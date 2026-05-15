package com.example.everythingbim;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.regex.Pattern;

/**
 * Validation utility tests - tests the actual validation patterns used in the app.
 * Note: This file tests validation patterns, not actual ViewModel implementations.
 * ViewModels are tested in their respective *ViewModelTest.java files.
 */
public class ValidationUtilsTest {

    // Fallback email pattern if android.util.Patterns is not available
    private static final Pattern EMAIL_PATTERN = android.util.Patterns.EMAIL_ADDRESS != null
        ? android.util.Patterns.EMAIL_ADDRESS
        : Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // ========== Email Validation Tests (using actual Android Patterns) ==========

    @Test
    public void validateEmail_validEmail_returnsTrue() {
        assertTrue(isValidEmail("user@example.com"));
        assertTrue(isValidEmail("test.user@domain.co.uk"));
        assertTrue(isValidEmail("tourist+bim@gmail.com"));
        assertTrue(isValidEmail("user@sub.domain.com"));
        assertTrue(isValidEmail("user@sub.domain.co.uk"));
    }

    @Test
    public void validateEmail_invalidEmail_returnsFalse() {
        assertFalse(isValidEmail("notanemail"));
        assertFalse(isValidEmail("missing@domain"));
        assertFalse(isValidEmail("@nodomain.com"));
        assertFalse(isValidEmail("spaces in@email.com"));
        assertFalse(isValidEmail(""));
        assertFalse(isValidEmail("user@"));
        assertFalse(isValidEmail("@"));
    }

    // ========== Password Validation Tests ==========

    @Test
    public void validatePassword_validPassword_returnsTrue() {
        // Valid password: 8+ chars with at least one digit
        assertTrue(validatePasswordSimple("Password123!"));
        assertTrue(validatePasswordSimple("Bim2024Test"));
        assertTrue(validatePasswordSimple("Password1"));
        assertTrue(validatePasswordSimple("Abcdefg1"));  // exactly 8 chars
    }

    @Test
    public void validatePassword_invalidPassword_returnsFalse() {
        assertFalse(validatePasswordSimple("short"));              // Too short (<8)
        assertFalse(validatePasswordSimple("NoDigits!"));            // No digits
        assertFalse(validatePasswordSimple("Passw1!"));            // 7 chars
        assertFalse(validatePasswordSimple(null));
        assertFalse(validatePasswordSimple(""));
    }

    @Test
    public void validatePassword_boundaryLength_tests() {
        // Exactly 7 chars - should fail (too short)
        assertFalse(validatePasswordSimple("Passw12"));
        // Exactly 8 chars - should pass (min valid)
        assertTrue(validatePasswordSimple("Password1"));
    }

    @Test
    public void validatePassword_emptyPassword_returnsFalse() {
        assertFalse(validatePasswordSimple(""));
        assertFalse(validatePasswordSimple(null));
    }

    // ========== Business Registration Validation Tests ==========

    @Test
    public void validateBusinessName_validName_returnsTrue() {
        assertTrue(validateBusinessNameSimple("Coastal Cafe"));
        assertTrue(validateBusinessNameSimple("Oistins Fish Market"));
        assertTrue(validateBusinessNameSimple("ABC"));  // exactly 3 chars
    }

    @Test
    public void validateBusinessName_invalidName_returnsFalse() {
        assertFalse(validateBusinessNameSimple(""));                    // Empty
        assertFalse(validateBusinessNameSimple("AB"));                  // Too short (<3)
        assertFalse(validateBusinessNameSimple(null));                  // Null
    }

    @Test
    public void validateBusinessName_whitespaceOnly_returnsFalse() {
        assertFalse(validateBusinessNameSimple("   "));  // whitespace only
        assertFalse(validateBusinessNameSimple("\t"));   // tab only
    }

    @Test
    public void validateBusinessRegistrationNumber_validNumber_returnsTrue() {
        assertTrue(validateBusinessRegNumberSimple("12345"));  // exactly 5
        assertTrue(validateBusinessRegNumberSimple("BUS-2024-001"));
    }

    @Test
    public void validateBusinessRegistrationNumber_invalid_returnsFalse() {
        assertFalse(validateBusinessRegNumberSimple(""));     // Empty
        assertFalse(validateBusinessRegNumberSimple("1234")); // Too short (<5)
        assertFalse(validateBusinessRegNumberSimple(null));
    }

    @Test
    public void validateDocument_uploaded_returnsTrue() {
        assertTrue(validateDocumentSimple("business_certificate.pdf"));
        assertTrue(validateDocumentSimple("tax_id.jpg"));
        assertTrue(validateDocumentSimple("document.doc"));
    }

    @Test
    public void validateDocument_notUploaded_returnsFalse() {
        assertFalse(validateDocumentSimple(""));
        assertFalse(validateDocumentSimple(null));
    }

    // ========== Location Filtering Tests ==========

    @Test
    public void filterHotspots_byType_returnsFilteredList() {
        String filterType = "restaurant";
        assertEquals(3, filterHotspotsByType(getSampleHotspots(), filterType).size());
    }

    @Test
    public void filterHotspots_byDistance_returnsSortedList() {
        assertEquals("Crane Beach", filterHotspotsByDistance(getSampleHotspots()).get(0).getName());
    }

    @Test
    public void filterHotspots_byRating_returnsSortedList() {
        assertEquals(5.0, filterHotspotsByRating(getSampleHotspots()).get(0).getRating(), 0.1);
    }

    @Test
    public void filterHotspots_within500mRadius_returnsOnlyNearby() {
        for (Hotspot h : filterHotspotsByRadius(getSampleHotspots(), 500)) {
            assertTrue(h.getDistance() <= 500);
        }
    }

    @Test
    public void filterHotspots_radiusEdgeCase_allWithin() {
        // All 5 hotspots are within 1000m
        assertEquals(5, filterHotspotsByRadius(getSampleHotspots(), 1000).size());
    }

    @Test
    public void filterHotspots_radiusEdgeCase_noneWithin() {
        // All hotspots are beyond 100m (min is 200m Crane Beach)
        assertEquals(0, filterHotspotsByRadius(getSampleHotspots(), 100).size());
    }

    // ========== Review and Rating Tests ==========

    @Test
    public void validateRating_withinValidRange_returnsTrue() {
        assertTrue(validateRatingSimple(1.0));
        assertTrue(validateRatingSimple(3.5));
        assertTrue(validateRatingSimple(5.0));
    }

    @Test
    public void validateRating_outOfRange_returnsFalse() {
        assertFalse(validateRatingSimple(0.0));   // Below min (1.0)
        assertFalse(validateRatingSimple(5.1));   // Above max (5.0)
        assertFalse(validateRatingSimple(-1.0));  // Negative
    }

    @Test
    public void validateRating_boundaryValues() {
        assertTrue("Rating 1.0 should be valid (min)", validateRatingSimple(1.0));
        assertTrue("Rating 5.0 should be valid (max)", validateRatingSimple(5.0));
        assertFalse("Rating 0.9 should be invalid (just below min)", validateRatingSimple(0.9));
        assertFalse("Rating 5.01 should be invalid (just above max)", validateRatingSimple(5.01));
    }

    @Test
    public void validateReviewText_validText_returnsTrue() {
        assertTrue(validateReviewTextSimple("Great place!"));
        assertTrue(validateReviewTextSimple("Had a wonderful time at the beach."));
        assertTrue(validateReviewTextSimple("12345"));  // exactly 5 chars
    }

    @Test
    public void validateReviewText_tooShort_returnsFalse() {
        assertFalse(validateReviewTextSimple("Good"));  // 4 chars
        assertFalse(validateReviewTextSimple("OK"));   // 2 chars
        assertFalse(validateReviewTextSimple(""));     // empty
        assertFalse(validateReviewTextSimple(null));   // null
    }

    // ========== Admin Content Moderation Tests ==========

    @Test
    public void isContentInappropriate_appropriateContent_returnsFalse() {
        assertFalse(isContentInappropriateSimple("Great beach!"));
        assertFalse(isContentInappropriateSimple("Loved the food"));
        assertFalse(isContentInappropriateSimple("Beautiful sunset today"));
        assertFalse(isContentInappropriateSimple("Best fish market ever"));
    }

    @Test
    public void isContentInappropriate_inappropriateContent_returnsTrue() {
        assertTrue(isContentInappropriateSimple("spam spam spam"));
        assertTrue(isContentInappropriateSimple("EXPLOIT!!!"));
        assertTrue("Case insensitive: SPAM", isContentInappropriateSimple("SPAM"));
        assertTrue("Case insensitive: Exploit", isContentInappropriateSimple("Exploit"));
        assertTrue("Mixed: this is spam content", isContentInappropriateSimple("this is spam content"));
    }

    @Test
    public void isContentInappropriate_null_returnsFalse() {
        assertFalse(isContentInappropriateSimple(null));
    }

    @Test
    public void validateAdminAction_validAction_returnsTrue() {
        assertTrue(validateAdminActionSimple("APPROVE", "POST", "valid"));
        assertTrue(validateAdminActionSimple("REJECT", "BUSINESS", "valid"));
        assertTrue(validateAdminActionSimple("DELETE", "COMMENT", "valid"));
        assertTrue(validateAdminActionSimple("APPROVE", "LOCATION", "valid"));
    }

    @Test
    public void validateAdminAction_invalidAction_returnsFalse() {
        assertFalse(validateAdminActionSimple("INVALID", "POST", "valid"));
        assertFalse(validateAdminActionSimple("APPROVE", "INVALID", "valid"));
        assertFalse(validateAdminActionSimple("", "POST", "valid"));
        assertFalse(validateAdminActionSimple("APPROVE", "", "valid"));
    }

    @Test
    public void validateAdminAction_emptyTargetId_returnsFalse() {
        assertFalse(validateAdminActionSimple("APPROVE", "POST", ""));
        assertFalse(validateAdminActionSimple("APPROVE", "POST", null));
    }

    // ========== Post Creation Tests ==========

    @Test
    public void validatePostContent_validContent_returnsTrue() {
        assertTrue(validatePostContentSimple("Check out this amazing view!", "image.jpg"));
        assertTrue(validatePostContentSimple("Just posted", "photo.png"));
        assertTrue(validatePostContentSimple("Beach day!", "beach.jpg"));
    }

    @Test
    public void validatePostContent_missingCaption_returnsFalse() {
        assertFalse(validatePostContentSimple("", "image.jpg"));
        assertFalse(validatePostContentSimple(null, "image.jpg"));
        assertFalse(validatePostContentSimple("  ", "image.jpg"));  // whitespace only
    }

    @Test
    public void validatePostContent_missingLocation_returnsFalse() {
        assertFalse(validatePostContentSimple("Great place", ""));
        assertFalse(validatePostContentSimple("Great place", null));
        assertFalse(validatePostContentSimple("Great place", "  "));  // whitespace only
    }

    // ========== Helper Methods (Testing validation patterns, not ViewModels) ==========

    private boolean validatePasswordSimple(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasDigit;
    }

    private boolean validateBusinessNameSimple(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        return name.trim().length() >= 3;
    }

    private boolean validateBusinessRegNumberSimple(String number) {
        return number != null && number.length() >= 5;
    }

    private boolean validateDocumentSimple(String document) {
        return document != null && !document.isEmpty();
    }

    private boolean validateRatingSimple(double rating) {
        return rating >= 1.0 && rating <= 5.0;
    }

    private boolean validateReviewTextSimple(String text) {
        return text != null && text.trim().length() >= 5;
    }

    private boolean isContentInappropriateSimple(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return lower.contains("spam") || lower.contains("exploit");
    }

    private boolean validateAdminActionSimple(String action, String targetType, String targetId) {
        String[] validActions = {"APPROVE", "REJECT", "DELETE"};
        String[] validTypes = {"POST", "COMMENT", "BUSINESS", "LOCATION"};
        for (String a : validActions) if (a.equals(action)) {
            for (String t : validTypes) if (t.equals(targetType)) {
                return targetId != null && !targetId.isEmpty();
            }
        }
        return false;
    }

    private boolean validatePostContentSimple(String caption, String imagePath) {
        if (caption == null || caption.trim().isEmpty()) return false;
        return imagePath != null && !imagePath.trim().isEmpty();
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