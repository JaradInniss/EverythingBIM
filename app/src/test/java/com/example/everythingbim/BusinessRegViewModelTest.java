package com.example.everythingbim;

import static org.junit.Assert.*;

import android.net.Uri;

import com.example.everythingbim.data.models.File;
import com.example.everythingbim.ui.registration.BusinessRegViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Unit tests for BusinessRegViewModel validation methods.
 * Uses Robolectric to mock android.os.Looper for LiveData setValue operations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class BusinessRegViewModelTest {

    private BusinessRegViewModel viewModel;

    @Before
    public void setUp() {
        // ShadowLooper is automatically configured via @Config annotation
        // Use MockFirebaseProvider to avoid Firebase initialization
        viewModel = new BusinessRegViewModel(new MockFirebaseProvider());
    }

    // ========== Username Validation Tests ==========

    @Test
    public void validateUsername_nullInput_setsError() {
        viewModel.validateUsername(null);
        assertNotNull("Error should be set for null username", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain username field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.business_register_username_et));
    }

    @Test
    public void validateUsername_emptyString_setsError() {
        viewModel.validateUsername("");
        assertNotNull("Error should be set for empty username", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateUsername_whitespaceOnly_setsError() {
        viewModel.validateUsername("   ");
        assertNotNull("Error should be set for whitespace-only username", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateUsername_validInput_clearsError() {
        viewModel.validateUsername("businessuser");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid username",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.business_register_username_et));
        }
    }

    // ========== Email Validation Tests ==========

    @Test
    public void validateEmail_nullInput_setsError() {
        viewModel.validateEmail(null);
        assertNotNull("Error should be set for null email", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain email field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.business_register_email_et));
    }

    @Test
    public void validateEmail_emptyString_setsError() {
        viewModel.validateEmail("");
        assertNotNull("Error should be set for empty email", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateEmail_validInput_clearsError() {
        viewModel.validateEmail("business@example.com");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid email",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.business_register_email_et));
        }
    }

    // ========== Password Validation Tests ==========

    @Test
    public void validatePassword_nullInput_setsError() {
        viewModel.validatePassword(null);
        assertNotNull("Error should be set for null password", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePassword_emptyString_setsError() {
        viewModel.validatePassword("");
        assertNotNull("Error should be set for empty password", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePassword_tooShort_setsError() {
        viewModel.validatePassword("abc");
        assertNotNull("Error should be set for short password", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePassword_exactly7Chars_setsError() {
        viewModel.validatePassword("Passwor");
        assertNotNull("Error should be set for 7-char password", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePassword_exactly8Chars_clearsError() {
        viewModel.validatePassword("Password1");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for 8-char password",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.business_register_password_et));
        }
    }

    // ========== Re-Password Validation Tests ==========

    @Test
    public void validateRePassword_nullInput_setsError() {
        viewModel.validateRePassword(null);
        assertNotNull("Error should be set for null repassword", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateRePassword_emptyString_setsError() {
        viewModel.validateRePassword("");
        assertNotNull("Error should be set for empty repassword", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateRePassword_mismatch_setsError() {
        viewModel.validatePassword("Password123");
        viewModel.validateRePassword("Different123");
        assertNotNull("Error should be set for mismatched passwords", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateRePassword_match_clearsError() {
        viewModel.validatePassword("Password123");
        viewModel.validateRePassword("Password123");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for matching passwords",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.business_register_repassword_et));
        }
    }

    // ========== Business Name Validation Tests ==========

    @Test
    public void validateBusinessName_nullInput_setsError() {
        viewModel.validateBusinessName(null);
        assertNotNull("Error should be set for null business name", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain business name field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_name_et));
    }

    @Test
    public void validateBusinessName_emptyString_setsError() {
        viewModel.validateBusinessName("");
        assertNotNull("Error should be set for empty business name", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateBusinessName_whitespaceOnly_setsError() {
        viewModel.validateBusinessName("   ");
        assertNotNull("Error should be set for whitespace-only business name", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateBusinessName_validInput_clearsError() {
        viewModel.validateBusinessName("DAJ Enterprises");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid business name",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_name_et));
        }
    }

    // ========== Phone Number Validation Tests ==========

    @Test
    public void validatePhoneNumber_nullInput_setsError() {
        viewModel.validatePhoneNumber(null);
        assertNotNull("Error should be set for null phone number", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain phone field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_contact_number_et));
    }

    @Test
    public void validatePhoneNumber_emptyString_setsError() {
        viewModel.validatePhoneNumber("");
        assertNotNull("Error should be set for empty phone number", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePhoneNumber_whitespace_setsError() {
        viewModel.validatePhoneNumber("   ");
        assertNotNull("Error should be set for whitespace phone number", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePhoneNumber_validInput_clearsError() {
        viewModel.validatePhoneNumber("2465551234");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid phone number",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_contact_number_et));
        }
    }

    // ========== Address Validation Tests ==========

    @Test
    public void validateAddress_nullInput_setsError() {
        viewModel.validateAddress(null);
        assertNotNull("Error should be set for null address", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain address field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_address_et));
    }

    @Test
    public void validateAddress_emptyString_setsError() {
        viewModel.validateAddress("");
        assertNotNull("Error should be set for empty address", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateAddress_validInput_clearsError() {
        viewModel.validateAddress("123 Main Street, St. Michael, Barbados");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid address",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_address_et));
        }
    }

    // ========== Business Description Validation Tests ==========

    @Test
    public void validateBusinessDescription_nullInput_setsError() {
        viewModel.validateBusinessDescription(null);
        assertNotNull("Error should be set for null description", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain description field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_description_et));
    }

    @Test
    public void validateBusinessDescription_emptyString_setsError() {
        viewModel.validateBusinessDescription("");
        assertNotNull("Error should be set for empty description", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateBusinessDescription_validInput_clearsError() {
        viewModel.validateBusinessDescription("We provide excellent tourism services.");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid description",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_description_et));
        }
    }

    // ========== isPageValid Tests ==========

    @Test
    public void isPageValid_page0_allValid_returnsTrue() {
        // Call validation directly - isPageValid returns true when all fields valid
        boolean result = viewModel.isPageValid(0, "businessuser", "biz@example.com", "Password123", "Password123");
        assertTrue("Page 0 should be valid with all correct credentials", result);
    }

    @Test
    public void isPageValid_page0_invalidUsername_returnsFalse() {
        boolean result = viewModel.isPageValid(0, "", "biz@example.com", "Password123", "Password123");
        assertFalse("Page 0 should be invalid with empty username", result);
    }

    @Test
    public void isPageValid_page2_allValid_returnsTrue() {
        boolean result = viewModel.isPageValid(2, "DAJ Enterprises", "2465551234", "123 Main St", "Description");
        assertTrue("Page 2 should be valid with all business details", result);
    }

    @Test
    public void isPageValid_page2_emptyBusinessName_returnsFalse() {
        boolean result = viewModel.isPageValid(2, "", "2465551234", "123 Main St", "Description");
        assertFalse("Page 2 should be invalid with empty business name", result);
    }

    @Test
    public void isPageValid_page2_emptyPhone_returnsFalse() {
        boolean result = viewModel.isPageValid(2, "DAJ Enterprises", "", "123 Main St", "Description");
        assertFalse("Page 2 should be invalid with empty phone", result);
    }

    // ========== File List Management Tests ==========

    @Test
    public void addImage_incrementsList() {
        File file = new File("image1.jpg", "image/jpeg", Uri.parse("content://test/image1.jpg"), 1024);
        viewModel.addImage(file);
        assertEquals("Image list should have 1 item", 1, viewModel.getImageList().getValue().size());
    }

    @Test
    public void removeImage_decrementsList() {
        File file = new File("image1.jpg", "image/jpeg", Uri.parse("content://test/image1.jpg"), 1024);
        viewModel.addImage(file);
        viewModel.removeImage(0);
        assertEquals("Image list should be empty", 0, viewModel.getImageList().getValue().size());
    }

    @Test
    public void addFile_incrementsList() {
        File file = new File("doc1.pdf", "application/pdf", Uri.parse("content://test/doc1.pdf"), 2048);
        viewModel.addFile(file);
        assertEquals("File list should have 1 item", 1, viewModel.getFileList().getValue().size());
    }

    @Test
    public void removeFile_decrementsList() {
        File file = new File("doc1.pdf", "application/pdf", Uri.parse("content://test/doc1.pdf"), 2048);
        viewModel.addFile(file);
        viewModel.removeFile(0);
        assertEquals("File list should be empty", 0, viewModel.getFileList().getValue().size());
    }

    @Test
    public void addMultipleImages_incrementsCorrectly() {
        File file1 = new File("image1.jpg", "image/jpeg", Uri.parse("content://test/image1.jpg"), 1024);
        File file2 = new File("image2.jpg", "image/jpeg", Uri.parse("content://test/image2.jpg"), 1024);
        viewModel.addImage(file1);
        viewModel.addImage(file2);
        assertEquals("Image list should have 2 items", 2, viewModel.getImageList().getValue().size());
    }

    // ========== Error Field HashMap Tests ==========

    @Test
    public void setErrorField_addsErrorCorrectly() {
        viewModel.setErrorField(com.example.everythingbim.R.id.register_business_name_et, "Business name error");
        assertNotNull("ErrorFields should not be null", viewModel.getErrorFields().getValue());
    }

    @Test
    public void setErrorField_nullRemovesError() {
        viewModel.setErrorField(com.example.everythingbim.R.id.register_business_name_et, "error");
        viewModel.setErrorField(com.example.everythingbim.R.id.register_business_name_et, null);
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Field should be removed from error map",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_business_name_et));
        }
    }

    @Test
    public void setErrorField_emptyBecomesNull() {
        viewModel.setErrorField(com.example.everythingbim.R.id.register_business_name_et, "error1");
        viewModel.setErrorField(com.example.everythingbim.R.id.register_business_name_et, null);
        // Should be null when empty
        if (viewModel.getErrorFields().getValue() != null) {
            assertTrue("Map should be empty or null",
                viewModel.getErrorFields().getValue().isEmpty());
        }
    }

    // ========== Navigation Tests ==========

    @Test
    public void prevPage_atZero_doesNothing() {
        viewModel.prevPage();
        assertEquals("Page should remain 0", Integer.valueOf(0), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void nextPage_incrementsPage() {
        viewModel.nextPage();
        // Page 0 -> Page 1 (verification code page)
        assertEquals("Page should be 1", Integer.valueOf(1), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void prevPage_afterNext_goesBack() {
        viewModel.nextPage();
        viewModel.prevPage();
        assertEquals("Page should be 0 after prevPage", Integer.valueOf(0), viewModel.getCurrentPage().getValue());
    }
}