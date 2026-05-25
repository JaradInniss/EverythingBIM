package com.example.everythingbim;

import static org.junit.Assert.*;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import com.example.everythingbim.data.models.MapDetailsState;
import com.example.everythingbim.ui.map.MapViewModel;
import com.example.everythingbim.ui.utils.NavigationCommand;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Unit tests for MapViewModel.
 * Uses Robolectric to mock android.os.Looper for LiveData setValue operations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class  MapViewModelTest {

    private MapViewModel viewModel;

    @Before
    public void setUp() {
        Application application = ApplicationProvider.getApplicationContext();
        viewModel = new MapViewModel(application);
    }

    // ========== Barbados Map Constants Tests ==========

    @Test
    public void barbadosBounds_notNull() {
        assertNotNull("Barbados bounds should not be null", MapViewModel.BARBADOS_BOUNDS);
    }

    @Test
    public void barbadosBounds_center_isReasonable() {
        LatLng center = viewModel.getBarbadosCenter();
        // Center should be roughly in the middle of the bounding box
        // Latitude should be between 13.01 and 13.34
        assertTrue("Center latitude should be between 13.0 and 13.35",
            center.latitude > 13.0 && center.latitude < 13.35);
        // Longitude should be between -59.68 and -59.38
        assertTrue("Center longitude should be between -59.7 and -59.35",
            center.longitude > -59.7 && center.longitude < -59.35);
    }

    // ========== Zoom Constants Tests ==========

    @Test
    public void minZoom_is10Point5() {
        assertEquals("Min zoom should be 10.5", 10.5f, MapViewModel.MIN_ZOOM, 0.0f);
    }

    @Test
    public void initialZoom_is11() {
        assertEquals("Initial zoom should be 11.0", 11.0f, MapViewModel.INITIAL_ZOOM, 0.0f);
    }

    // ========== Details UI State Tests ==========

    @Test
    public void detailsUIState_initial_isHidden() {
        assertEquals("Initial state should be HIDDEN",
            MapDetailsState.HIDDEN,
            viewModel.getDetailsUIState().getValue());
    }

    @Test
    public void setDetailsUIState_toPeek_updatesValue() {
        viewModel.setDetailsUIState(MapDetailsState.PEEK);
        assertEquals("State should be PEEK",
            MapDetailsState.PEEK,
            viewModel.getDetailsUIState().getValue());
    }

    @Test
    public void setDetailsUIState_toFull_updatesValue() {
        viewModel.setDetailsUIState(MapDetailsState.FULL);
        assertEquals("State should be FULL",
            MapDetailsState.FULL,
            viewModel.getDetailsUIState().getValue());
    }

    @Test
    public void setDetailsUIState_toHidden_updatesValue() {
        viewModel.setDetailsUIState(MapDetailsState.PEEK);
        viewModel.setDetailsUIState(MapDetailsState.HIDDEN);
        assertEquals("State should be HIDDEN",
            MapDetailsState.HIDDEN,
            viewModel.getDetailsUIState().getValue());
    }

    // ========== Focused Location Tests ==========

    @Test
    public void focusedLocation_initial_isNull() {
        assertNull("Initial focused location should be null",
            viewModel.getFocusedLocation().getValue());
    }

    @Test
    public void setFocusedLocation_setsValue() {
        LatLng location = new LatLng(13.15, -59.55);
        viewModel.setFocusedLocation(location);
        assertEquals("Focused location should match",
            location,
            viewModel.getFocusedLocation().getValue());
    }

    @Test
    public void setFocusedLocation_null_clearsValue() {
        LatLng location = new LatLng(13.15, -59.55);
        viewModel.setFocusedLocation(location);
        viewModel.setFocusedLocation(null);
        assertNull("Focused location should be null after clearing",
            viewModel.getFocusedLocation().getValue());
    }

    // ========== Selected Location Metadata Tests ==========

    @Test
    public void setSelectedLocationMetadata_storesValues() {
        viewModel.setSelectedLocationMetadata(123L, "Test Location");

        // We can't directly access the private fields, but we can test
        // the behavior through navigation events that use these values
    }

    // ========== Navigation Event Tests ==========

    @Test
    public void onViewAllClicked_setsNavigationEvent() {
        NavCounter counter = new NavCounter();
        viewModel.getNavigationEvent().observeForever(counter);

        // First set the metadata (required for onViewAllClicked)
        viewModel.setSelectedLocationMetadata(456L, "My Business");

        // Trigger navigation
        viewModel.onViewAllClicked("account");

        assertTrue("Navigation should be triggered", counter.called);
        assertNotNull("Navigation command should not be null", counter.command);
        assertEquals("Destination should be MapViewAllActivity",
            com.example.everythingbim.ui.map.MapViewAllActivity.class,
            counter.command.getDestination());
    }

    @Test
    public void onViewAllClicked_withDifferentViewType_setsNavigation() {
        NavCounter counter = new NavCounter();
        viewModel.getNavigationEvent().observeForever(counter);

        viewModel.setSelectedLocationMetadata(789L, "Another Place");
        viewModel.onViewAllClicked("location");

        assertTrue("Navigation should be triggered", counter.called);
    }

    // ========== Helper Classes ==========

    private static class NavCounter implements Observer<NavigationCommand> {
        boolean called = false;
        NavigationCommand command;

        @Override
        public void onChanged(NavigationCommand navCommand) {
            called = true;
            command = navCommand;
        }
    }
}