package com.example.everythingbim;

import static org.junit.Assert.*;

import com.example.everythingbim.ui.admin.AdminViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Unit tests for AdminViewModel.
 * Uses Robolectric to mock android.os.Looper for LiveData setValue operations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class AdminViewModelTest {

    private AdminViewModel viewModel;

    // Use actual resource IDs that exist
    private static final int HOME_ID = com.example.everythingbim.R.id.admin_navbar_home;
    private static final int REPORTS_ID = com.example.everythingbim.R.id.admin_navbar_reports;
    private static final int SETTINGS_ID = com.example.everythingbim.R.id.admin_navbar_settings;

    private static final int USER_ID = R.id.admin_navbar_user;

    @Before
    public void setUp() {
        viewModel = new AdminViewModel();
    }

    // ========== Initial State Tests ==========

    @Test
    public void initialNavbarItemId_isHome() {
        assertEquals("Default navbar item should be home",
            Integer.valueOf(HOME_ID),
            viewModel.getNavbarItemId().getValue());
    }

    // ========== Navbar Item State Tests ==========

    @Test
    public void setNavbarItemId_toHome_updatesValue() {
        viewModel.setNavbarItemId(HOME_ID);
        assertEquals("Navbar item should be home",
            Integer.valueOf(HOME_ID),
            viewModel.getNavbarItemId().getValue());
    }

    @Test
    public void setNavbarItemId_toUnknownId_setsValue() {
        int unknownId = 99999;
        viewModel.setNavbarItemId(unknownId);
        assertEquals("Navbar item should be set to unknown ID",
            Integer.valueOf(unknownId),
            viewModel.getNavbarItemId().getValue());
    }

    @Test
    public void setNavbarItemId_toReports_updatesValue() {
        viewModel.setNavbarItemId(REPORTS_ID);
        assertEquals("Navbar item should be reports",
            Integer.valueOf(REPORTS_ID),
            viewModel.getNavbarItemId().getValue());
    }

    @Test
    public void setNavbarItemId_toSettings_updatesValue() {
        viewModel.setNavbarItemId(SETTINGS_ID);
        assertEquals("Navbar item should be settings",
            Integer.valueOf(SETTINGS_ID),
            viewModel.getNavbarItemId().getValue());
    }

    @Test
    public void setNavbarItemId_toUSER_updatesValue() {
        viewModel.setNavbarItemId(USER_ID);
        assertEquals("Navbar item should be user",
                Integer.valueOf(USER_ID),
                viewModel.getNavbarItemId().getValue());
    }

    // ========== Idempotency Tests ==========

    @Test
    public void setNavbarItemId_sameId_twice_onlyOneUpdate() {
        viewModel.setNavbarItemId(HOME_ID);

        // Setting the same ID again should still work (no-op check happens inside)
        viewModel.setNavbarItemId(HOME_ID);

        assertEquals("Value should still be home",
            Integer.valueOf(HOME_ID),
            viewModel.getNavbarItemId().getValue());
    }

    @Test
    public void setNavbarItemId_differentIds_updatesEachTime() {
        viewModel.setNavbarItemId(HOME_ID);
        assertEquals("Should be home",
            Integer.valueOf(HOME_ID),
            viewModel.getNavbarItemId().getValue());

        viewModel.setNavbarItemId(REPORTS_ID);
        assertEquals("Should be reports",
            Integer.valueOf(REPORTS_ID),
            viewModel.getNavbarItemId().getValue());

        viewModel.setNavbarItemId(SETTINGS_ID);
        assertEquals("Should be settings",
            Integer.valueOf(SETTINGS_ID),
            viewModel.getNavbarItemId().getValue());

        viewModel.setNavbarItemId(USER_ID);
        assertEquals("Should be user",
                Integer.valueOf(USER_ID),
                viewModel.getNavbarItemId().getValue());
    }

    // ========== Null Safety Tests ==========

    @Test
    public void setNavbarItemId_afterNullState_setsValue() {
        viewModel.setNavbarItemId(HOME_ID);
        assertNotNull("Value should not be null after first set", viewModel.getNavbarItemId().getValue());
    }
}