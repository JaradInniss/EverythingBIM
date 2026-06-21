package com.example.everythingbim;

import static org.junit.Assert.*;

import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.ui.login.LoginViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Unit tests for LoginViewModel validation methods.
 * Uses Robolectric to mock android.os.Looper for LiveData setValue operations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class LoginViewModelTest {

    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        // ShadowLooper is automatically configured via @Config annotation
        // Use MockFirebaseProvider to avoid Firebase initialization
        viewModel = new LoginViewModel(new MockFirebaseProvider());
    }

    // ========== User Type Selection Tests ==========
    // These don't call Firebase methods

    @Test
    public void setSelectedUserType_admin_setsValue() {
        viewModel.setSelectedUserType(UserType.ADMIN);
        assertEquals("User type should be ADMIN", UserType.ADMIN, viewModel.getSelectedUserType().getValue());
    }

    @Test
    public void setSelectedUserType_business_setsValue() {
        viewModel.setSelectedUserType(UserType.BUSINESS);
        assertEquals("User type should be BUSINESS", UserType.BUSINESS, viewModel.getSelectedUserType().getValue());
    }

    @Test
    public void setSelectedUserType_general_setsValue() {
        viewModel.setSelectedUserType(UserType.GENERAL);
        assertEquals("User type should be GENERAL", UserType.GENERAL, viewModel.getSelectedUserType().getValue());
    }

    @Test
    public void defaultUserType_isGeneral() {
        assertEquals("Default user type should be GENERAL", UserType.GENERAL, viewModel.getSelectedUserType().getValue());
    }

    // ========== Error Field HashMap Tests ==========
    // These don't call Firebase methods

    @Test
    public void setErrorField_addsErrorCorrectly() {
        viewModel.setErrorField(com.example.everythingbim.R.id.login_username_et, "Email error");
        assertNotNull("ErrorFields should not be null", viewModel.getErrorFields().getValue());
        assertEquals("Error message should match", "Email error",
            viewModel.getErrorFields().getValue().get(com.example.everythingbim.R.id.login_username_et));
    }

    @Test
    public void setErrorField_nullRemovesError() {
        viewModel.setErrorField(com.example.everythingbim.R.id.login_username_et, "error");
        viewModel.setErrorField(com.example.everythingbim.R.id.login_username_et, null);
        if (viewModel.getErrorFields().getValue() != null) {
            assertFalse("Field should be removed from error map",
                viewModel.getErrorFields().getValue().containsKey(com.example.everythingbim.R.id.login_username_et));
        }
    }

    @Test
    public void setErrorField_multipleErrorsAndClear_becomesNull() {
        viewModel.setErrorField(com.example.everythingbim.R.id.login_username_et, "error1");
        viewModel.setErrorField(com.example.everythingbim.R.id.login_password_et, "error2");
        viewModel.setErrorField(com.example.everythingbim.R.id.login_username_et, null);
        viewModel.setErrorField(com.example.everythingbim.R.id.login_password_et, null);
        assertNull("ErrorFields should become null when empty", viewModel.getErrorFields().getValue());
    }

    // ========== Loading State Tests ==========

    @Test
    public void isLoading_initialState_isFalse() {
        assertEquals("Initial loading state should be false", Boolean.FALSE, viewModel.getIsLoading().getValue());
    }
}
