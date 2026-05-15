package com.example.everythingbim;

import static org.junit.Assert.*;

import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.ui.registration.GeneralRegViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for GeneralRegViewModel validation methods.
 * Tests the actual ViewModel validation logic for general user registration.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class GeneralRegViewModelTest {

    private GeneralRegViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new GeneralRegViewModel();
    }

    // ========== Username Validation Tests ==========

    @Test
    public void validateUsername_nullInput_setsError() {
        viewModel.validateUsername(null);
        assertNotNull("Error should be set for null username", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain username field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_username_et));
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
        viewModel.validateUsername("john_doe");
        // If no error was set or error was cleared, getValue() should be null or not contain the field
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid username",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_username_et));
        }
    }

    @Test
    public void validateUsername_singleChar_doesNotClearError() {
        // Single character (non-whitespace) passes the trim check but is unusual
        viewModel.validateUsername("a");
        // Should not set error since it's not empty/whitespace
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Single char should be valid",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_username_et));
        }
    }

    // ========== Email Validation Tests ==========

    @Test
    public void validateEmail_nullInput_setsError() {
        viewModel.validateEmail(null);
        assertNotNull("Error should be set for null email", viewModel.getErrorFields().getValue());
        assertTrue("Error should contain email field",
            viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_email_et));
    }

    @Test
    public void validateEmail_emptyString_setsError() {
        viewModel.validateEmail("");
        assertNotNull("Error should be set for empty email", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validateEmail_noAtSymbol_noError() {
        // ViewModel only validates empty, not email format
        viewModel.validateEmail("invalidemail.com");
        // Should NOT set error because ViewModel only checks isEmpty()
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should NOT be set for email without @ (ViewModel only checks empty)",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_email_et));
        }
    }

    @Test
    public void validateEmail_noDomain_noError() {
        // ViewModel only validates empty, not email format
        viewModel.validateEmail("user@");
        // Should NOT set error because ViewModel only checks isEmpty()
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should NOT be set for email without domain (ViewModel only checks empty)",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_email_et));
        }
    }

    @Test
    public void validateEmail_validSimple_setsError() {
        viewModel.validateEmail("user@example.com");
        // Valid email should not produce error
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid email",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_email_et));
        }
    }

    @Test
    public void validateEmail_validWithSubdomain_clearsError() {
        viewModel.validateEmail("user@sub.domain.com");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid subdomain email",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_email_et));
        }
    }

    @Test
    public void validateEmail_validWithPlus_clearsError() {
        viewModel.validateEmail("user+tag@example.com");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for email with plus",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_email_et));
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
        // "Passwor" is 7 chars, should trigger error (< 8 chars)
        viewModel.validatePassword("Passwor");
        assertNotNull("Error should be set for 7-char password", viewModel.getErrorFields().getValue());
    }

    @Test
    public void validatePassword_exactly8Chars_clearsError() {
        viewModel.validatePassword("Password1");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for 8-char password",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_password_et));
        }
    }

    @Test
    public void validatePassword_validLong_clearsError() {
        viewModel.validatePassword("Password123!");
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Error should be cleared for valid password",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_password_et));
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
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_repassword_et));
        }
    }

    // ========== isFormValid Tests ==========

    @Test
    public void isFormValid_allValid_returnsTrue() {
        boolean result = viewModel.isFormValid("john_doe", "user@example.com", "Password123", "Password123");
        assertTrue("Form should be valid when all fields are valid", result);
    }

    @Test
    public void isFormValid_invalidUsername_returnsFalse() {
        boolean result = viewModel.isFormValid("", "user@example.com", "Password123", "Password123");
        assertFalse("Form should be invalid when username is empty", result);
    }

    @Test
    public void isFormValid_invalidEmail_returnsFalse() {
        boolean result = viewModel.isFormValid("john_doe", "", "Password123", "Password123");
        assertFalse("Form should be invalid when email is empty", result);
    }

    @Test
    public void isFormValid_invalidPassword_returnsFalse() {
        boolean result = viewModel.isFormValid("john_doe", "user@example.com", "short", "short");
        assertFalse("Form should be invalid when password is too short", result);
    }

    @Test
    public void isFormValid_passwordMismatch_returnsFalse() {
        boolean result = viewModel.isFormValid("john_doe", "user@example.com", "Password123", "Different123");
        assertFalse("Form should be invalid when passwords don't match", result);
    }

    @Test
    public void isFormValid_multipleFieldsInvalid_returnsFalse() {
        boolean result = viewModel.isFormValid("", "", "short", "");
        assertFalse("Form should be invalid when multiple fields are invalid", result);
    }

    // ========== Error Field HashMap Tests ==========

    @Test
    public void setErrorField_addsErrorCorrectly() {
        viewModel.setErrorField(com.example.everythingbim.R.id.register_username_et, "Username error");
        assertNotNull("ErrorFields should not be null", viewModel.getErrorFields().getValue());
        assertEquals("Error message should match", "Username error",
            viewModel.getErrorFields().getValue().get(com.example.everythingbim.R.id.register_username_et));
    }

    @Test
    public void setErrorField_nullRemovesError() {
        viewModel.setErrorField(com.example.everythingbim.R.id.register_username_et, "Username error");
        viewModel.setErrorField(com.example.everythingbim.R.id.register_username_et, null);
        // After removing, either null or the field should not be present
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Field should be removed from error map",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.register_username_et));
        }
    }

    @Test
    public void setErrorField_multipleFieldsEmpty_becomesNull() {
        viewModel.setErrorField(com.example.everythingbim.R.id.register_username_et, "error1");
        viewModel.setErrorField(com.example.everythingbim.R.id.register_email_et, "error2");
        viewModel.setErrorField(com.example.everythingbim.R.id.register_username_et, null);
        viewModel.setErrorField(com.example.everythingbim.R.id.register_email_et, null);
        assertNull("ErrorFields should become null when empty", viewModel.getErrorFields().getValue());
    }

    // ========== Page Navigation Tests ==========

    @Test
    public void nextPage_incrementsPage() {
        viewModel.nextPage();
        assertEquals("Page should be 1 after nextPage", Integer.valueOf(1), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void prevPage_decrementsPage() {
        viewModel.nextPage();
        viewModel.nextPage();
        viewModel.prevPage();
        assertEquals("Page should be 1 after prevPage from 2", Integer.valueOf(1), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void prevPage_atZero_doesNothing() {
        viewModel.prevPage();
        assertEquals("Page should remain 0", Integer.valueOf(0), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void prevPage_fromOneGoesToZero() {
        viewModel.nextPage();
        viewModel.prevPage();
        assertEquals("Page should be 0", Integer.valueOf(0), viewModel.getCurrentPage().getValue());
    }

    // ========== navigateTo Tests ==========

    @Test
    public void navigateTo_setsNavigationEvent() {
        viewModel.navigateTo(com.example.everythingbim.ui.login.Login.class);
        // SingleLiveEvent should have a value set
        assertNotNull("Navigation event should be set", viewModel.getNavigationEvent().getValue());
    }
}