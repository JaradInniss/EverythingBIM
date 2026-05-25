package com.example.everythingbim;

import static org.junit.Assert.*;

import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.utils.NavigationCommand;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Unit tests for NavigationCommand model class.
 * Uses Robolectric because NavigationCommand takes android.os.Bundle which needs mocking.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class NavigationCommandTest {

    // ========== Constructor Tests ==========

    @Test
    public void constructor_withDestinationOnly_setsDestinationAndNullExtras() {
        NavigationCommand command = new NavigationCommand(MainActivity.class);

        assertEquals("Destination should match", MainActivity.class, command.getDestination());
        assertNull("Extras should be null", command.getExtras());
    }

    @Test
    public void constructor_withNullBundle_setsNullExtras() {
        NavigationCommand command = new NavigationCommand(MainActivity.class, null);

        assertEquals("Destination should match", MainActivity.class, command.getDestination());
        assertNull("Extras should be null", command.getExtras());
    }

    // ========== Getter Tests ==========

    @Test
    public void getDestination_returnsCorrectClass() {
        NavigationCommand cmd1 = new NavigationCommand(MainActivity.class);
        assertEquals(MainActivity.class, cmd1.getDestination());

        NavigationCommand cmd2 = new NavigationCommand(com.example.everythingbim.SplashActivity.class);
        assertEquals(com.example.everythingbim.SplashActivity.class, cmd2.getDestination());
    }

    @Test
    public void getExtras_withNoExtrasPassed_returnsNull() {
        NavigationCommand command = new NavigationCommand(MainActivity.class);
        assertNull(command.getExtras());
    }

    // ========== Equality Tests ==========

    @Test
    public void differentDestinations_produceDifferentCommands() {
        NavigationCommand cmd1 = new NavigationCommand(MainActivity.class);
        NavigationCommand cmd2 = new NavigationCommand(com.example.everythingbim.SplashActivity.class);

        assertNotEquals("Commands with different destinations should not be equal",
            cmd1.getDestination(), cmd2.getDestination());
    }
}