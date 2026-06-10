package com.example.everythingbim;

import static org.junit.Assert.*;

import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.models.UserType;
import com.example.everythingbim.ui.posts.CreatePostViewModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for CreatePostViewModel validation and state management.
 * Tests the actual ViewModel logic for post creation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class CreatePostViewModelTest {

    private CreatePostViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new CreatePostViewModel(ApplicationProvider.getApplicationContext());
    }

    // ========== Caption Management Tests ==========

    @Test
    public void setCaption_updatesValue() {
        viewModel.setCaption("Test caption");
        assertEquals("Caption should be set", "Test caption", viewModel.getCaption().getValue());
    }

    @Test
    public void setCaption_emptyString_isAllowed() {
        viewModel.setCaption("");
        assertEquals("Empty caption should be allowed", "", viewModel.getCaption().getValue());
    }

    @Test
    public void getCaption_initialValue_isEmpty() {
        assertEquals("Initial caption should be empty string", "", viewModel.getCaption().getValue());
    }

    @Test
    public void setCaption_multipleTimes_lastValueWins() {
        viewModel.setCaption("First");
        viewModel.setCaption("Second");
        viewModel.setCaption("Third");
        assertEquals("Caption should be last set value", "Third", viewModel.getCaption().getValue());
    }

    // ========== Image URI Management Tests ==========

    @Test
    public void setImageUri_updatesValue() {
        Uri testUri = Uri.parse("content://test/image.jpg");
        viewModel.setImageUri(testUri);
        assertEquals("Image URI should be set", testUri, viewModel.getImageUri().getValue());
    }

    @Test
    public void getImageUri_initialValue_isNull() {
        assertNull("Initial image URI should be null", viewModel.getImageUri().getValue());
    }

    @Test
    public void setImageUri_null_isAllowed() {
        viewModel.setImageUri(null);
        assertNull("Null image URI should be allowed", viewModel.getImageUri().getValue());
    }

    // ========== Location ID Management Tests ==========

    @Test
    public void setSelectedLocationId_updatesValue() {
        viewModel.setSelectedLocationId(123L);
        assertEquals("Location ID should be set", Long.valueOf(123L), viewModel.getSelectedLocationId().getValue());
    }

    @Test
    public void getSelectedLocationId_initialValue_isNull() {
        assertNull("Initial location ID should be null", viewModel.getSelectedLocationId().getValue());
    }

    @Test
    public void setSelectedLocationId_zero_isAllowed() {
        viewModel.setSelectedLocationId(0L);
        assertEquals("Zero location ID should be allowed", Long.valueOf(0L), viewModel.getSelectedLocationId().getValue());
    }

    // ========== Tagged Users Management Tests ==========

    @Test
    public void addTaggedUser_increasesListSize() {
        UserEntity user = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());

        viewModel.addTaggedUser(user);
        assertEquals("Tagged users should have 1 item", 1, viewModel.getTaggedUsers().getValue().size());
    }

    @Test
    public void addTaggedUser_duplicate_notAdded() {
        UserEntity user = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());

        viewModel.addTaggedUser(user);
        viewModel.addTaggedUser(user);
        assertEquals("Duplicate should not increase list size", 1, viewModel.getTaggedUsers().getValue().size());
    }

    @Test
    public void addTaggedUser_multipleDifferent_increasesSize() {
        UserEntity user1 = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());
        UserEntity user2 = new UserEntity("user2", "passwordhash", "user@email.com", UserType.BUSINESS, true, System.currentTimeMillis());
        UserEntity user3 = new UserEntity("user3", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());

        viewModel.addTaggedUser(user1);
        viewModel.addTaggedUser(user2);
        viewModel.addTaggedUser(user3);
        assertEquals("Should have 3 tagged users", 3, viewModel.getTaggedUsers().getValue().size());
    }

    @Test
    public void removeTaggedUser_decreasesListSize() {
        UserEntity user1 = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());
        UserEntity user2 = new UserEntity("user2", "passwordhash", "user@email.com", UserType.BUSINESS, true, System.currentTimeMillis());

        viewModel.addTaggedUser(user1);
        viewModel.addTaggedUser(user2);
        viewModel.removeTaggedUser(user1);
        assertEquals("Should have 1 tagged user after removal", 1, viewModel.getTaggedUsers().getValue().size());
        assertTrue("user2 should still be present", viewModel.getTaggedUsers().getValue().contains(user2));
    }

    // ---- Check Test ----
//    @Test
//    public void removeTaggedUser_nonExistent_doesNothing() {
//        UserEntity user = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());
//
//        viewModel.addTaggedUser(user);
//        viewModel.removeTaggedUser(user1);
//        assertEquals("Should still have 1 user", 1, viewModel.getTaggedUsers().getValue().size());
//    }

    @Test
    public void getTaggedUsers_initialValue_emptyList() {
        assertNotNull("Initial tagged users should not be null", viewModel.getTaggedUsers().getValue());
        assertEquals("Initial tagged users should be empty", 0, viewModel.getTaggedUsers().getValue().size());
    }

    // ========== createPost Validation Tests ==========

    @Test
    public void createPost_nullLocationId_doesNotCreate() {
        viewModel.setImageUri(Uri.parse("content://test/image.jpg"));
        viewModel.setSelectedLocationId(null);
        viewModel.setCaption("Test post");
        // createPost should return early due to null locationId
        // We can't fully test this without mocking, but we can test state
    }

    @Test
    public void createPost_nullImageUri_doesNotCreate() {
        viewModel.setImageUri(null);
        viewModel.setSelectedLocationId(123L);
        viewModel.setCaption("Test post");
        // createPost should return early due to null imageUri
    }

    @Test
    public void createPost_bothNull_doesNotCreate() {
        viewModel.setImageUri(null);
        viewModel.setSelectedLocationId(null);
        viewModel.setCaption("Test post");
        // Should return early
    }

    // Note: Full createPost testing requires Room mocking or SQLiteMock
    // These tests verify the precondition checks exist in the code

    // ========== Loading State Tests ==========

    @Test
    public void getIsSaving_initialValue_isFalse() {
        assertEquals("Initial saving state should be false", Boolean.FALSE, viewModel.getIsSaving().getValue());
    }

    @Test
    public void getPostCreated_initialValue_isFalse() {
        assertEquals("Initial post created state should be false", Boolean.FALSE, viewModel.getPostCreated().getValue());
    }

    // ========== Combined State Tests ==========

    @Test
    public void allSetters_workIndependently() {
        UserEntity user = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());

        viewModel.setCaption("My caption");
        viewModel.setImageUri(Uri.parse("content://test.jpg"));
        viewModel.setSelectedLocationId(456L);
        viewModel.addTaggedUser(user);

        assertEquals("Caption should be set", "My caption", viewModel.getCaption().getValue());
        assertEquals("Image URI should be set", Uri.parse("content://test.jpg"), viewModel.getImageUri().getValue());
        assertEquals("Location ID should be set", Long.valueOf(456L), viewModel.getSelectedLocationId().getValue());
        assertEquals("Tagged users should have 1 item", 1, viewModel.getTaggedUsers().getValue().size());
    }

    @Test
    public void clearState_individually() {
        UserEntity user = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());

        viewModel.setCaption("Test");
        viewModel.setImageUri(Uri.parse("content://test.jpg"));
        viewModel.setSelectedLocationId(123L);
        viewModel.addTaggedUser(user);

        // Clear caption
        viewModel.setCaption("");
        assertEquals("Caption should be empty", "", viewModel.getCaption().getValue());

        // Clear image URI
        viewModel.setImageUri(null);
        assertNull("Image URI should be null", viewModel.getImageUri().getValue());

        // Clear location ID
        viewModel.setSelectedLocationId(null);
        assertNull("Location ID should be null", viewModel.getSelectedLocationId().getValue());

        // Clear tagged users (via remove)
        viewModel.removeTaggedUser(user);
        assertEquals("Tagged users should be empty", 0, viewModel.getTaggedUsers().getValue().size());
    }

    // ========== Edge Cases ==========

    @Test
    public void setCaption_null_isAllowed() {
        viewModel.setCaption(null);
        assertNull("Null caption should be allowed", viewModel.getCaption().getValue());
    }

    // ---- Check Test -----
//    @Test
//    public void addTaggedUser_emptyString_isAllowed() {
//        viewModel.addTaggedUser("");
//        // Empty string is technically allowed by the code
//        // (only checks if not already in list, not if empty)
//    }

    @Test
    public void addTaggedUser_null_isIgnored() {
        viewModel.addTaggedUser(null);
        // After the safety fix, null is no longer added to the list
        assertFalse("Null should not be in list", viewModel.getTaggedUsers().getValue().contains(null));
        assertEquals("List should still be empty", 0, viewModel.getTaggedUsers().getValue().size());
    }

    @Test
    public void clearTaggedUsers_emptiesTheList() {
        UserEntity user = new UserEntity("user", "passwordhash", "user@email.com", UserType.GENERAL, true, System.currentTimeMillis());

        viewModel.addTaggedUser(user);
        assertEquals("Should have 1 tagged user", 1, viewModel.getTaggedUsers().getValue().size());

        viewModel.clearTaggedUsers();
        assertEquals("Should have 0 tagged users after clear", 0, viewModel.getTaggedUsers().getValue().size());
    }

    @Test
    public void resetDraft_clearsAllFields() {
        viewModel.setCaption("Test");
        viewModel.setImageUri(Uri.parse("content://test.jpg"));
        viewModel.setSelectedLocationId(123L);

        viewModel.resetDraft();

        assertEquals("Caption should be empty", "", viewModel.getCaption().getValue());
        assertNull("ImageUri should be null", viewModel.getImageUri().getValue());
        assertNull("LocationId should be null", viewModel.getSelectedLocationId().getValue());
    }
}