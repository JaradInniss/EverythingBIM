package com.example.everythingbim.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.everythingbim.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ExpandedRequestMapDialogFragment extends DialogFragment {
    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_INITIAL_LAT = "initial_lat";
    private static final String ARG_INITIAL_LNG = "initial_lng";
    private static final String ARG_INITIAL_TITLE = "initial_title";

    public static final String RESULT_LAT = "result_lat";
    public static final String RESULT_LNG = "result_lng";

    private static final LatLng BARBADOS_CENTER = new LatLng(13.1939, -59.5432);
    private static final float DEFAULT_ZOOM = 11f;
    private static final float SELECTED_ZOOM = 16f;

    private MapView mapView;
    private GoogleMap googleMap;
    private Button useLocationBtn;
    private String requestKey;
    private LatLng selectedLatLng;
    private String selectedTitle;

    public static ExpandedRequestMapDialogFragment newInstance(
            @NonNull String requestKey,
            double initialLat,
            double initialLng,
            @Nullable String initialTitle
    ) {
        ExpandedRequestMapDialogFragment fragment = new ExpandedRequestMapDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putDouble(ARG_INITIAL_LAT, initialLat);
        args.putDouble(ARG_INITIAL_LNG, initialLng);
        args.putString(ARG_INITIAL_TITLE, initialTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        Bundle args = getArguments();
        if (args != null) {
            requestKey = args.getString(ARG_REQUEST_KEY, "");
            double initialLat = args.getDouble(ARG_INITIAL_LAT, 0.0d);
            double initialLng = args.getDouble(ARG_INITIAL_LNG, 0.0d);
            selectedTitle = args.getString(ARG_INITIAL_TITLE);
            if (initialLat != 0.0d || initialLng != 0.0d) {
                selectedLatLng = new LatLng(initialLat, initialLng);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_expanded_request_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id.expanded_request_map);
        TextView hintTv = view.findViewById(R.id.expanded_request_map_hint_tv);
        ImageButton closeBtn = view.findViewById(R.id.expanded_request_map_close_btn);
        useLocationBtn = view.findViewById(R.id.expanded_request_map_use_btn);

        hintTv.setText("Tap the map to place the pin, then use this location.");
        closeBtn.setOnClickListener(v -> dismiss());
        useLocationBtn.setOnClickListener(v -> deliverSelection());
        updateUseLocationEnabledState();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            googleMap = map;
            if (googleMap == null) {
                return;
            }
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            googleMap.setOnMapClickListener(latLng -> {
                selectedLatLng = latLng;
                updateMarker(true);
            });
            updateMarker(false);
        });
    }

    private void updateMarker(boolean animate) {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        if (selectedLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(selectedLatLng)
                    .title((selectedTitle != null && !selectedTitle.trim().isEmpty())
                            ? selectedTitle.trim()
                            : "Selected Location"));
            if (animate) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, SELECTED_ZOOM));
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, SELECTED_ZOOM));
            }
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BARBADOS_CENTER, DEFAULT_ZOOM));
        }
        updateUseLocationEnabledState();
    }

    private void updateUseLocationEnabledState() {
        if (useLocationBtn != null) {
            useLocationBtn.setEnabled(selectedLatLng != null);
            useLocationBtn.setAlpha(selectedLatLng != null ? 1f : 0.6f);
        }
    }

    private void deliverSelection() {
        if (selectedLatLng == null || requestKey == null || requestKey.trim().isEmpty()) {
            return;
        }
        Bundle result = new Bundle();
        result.putDouble(RESULT_LAT, selectedLatLng.latitude);
        result.putDouble(RESULT_LNG, selectedLatLng.longitude);
        getParentFragmentManager().setFragmentResult(requestKey, result);
        dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null) mapView.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
