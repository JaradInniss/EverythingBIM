package com.example.everythingbim.ui.map;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static class MarkerDetails {
        final String title;
        final String subtitle;
        final String meta;
        final String contact;

        MarkerDetails(String title, String subtitle, String meta, String contact) {
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.contact = contact;
        }
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final List<MarkerEntity> storedMarkers = new ArrayList<>();
    private final List<AutocompletePrediction> autocompletePredictions = new ArrayList<>();
    private final List<String> predictionLabels = new ArrayList<>();
    private final List<Place.Field> placeFields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG,
            Place.Field.PHONE_NUMBER,
            Place.Field.WEBSITE_URI,
            Place.Field.RATING,
            Place.Field.USER_RATINGS_TOTAL,
            Place.Field.TYPES
    );
    private final Runnable pendingSearchRunnable = this::performSearch;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private MapViewModel mapViewModel;
    private PlacesClient placesClient;
    private AutocompleteSessionToken autocompleteSessionToken;

    private EditText searchInput;
    private ProgressBar searchProgress;
    private View resultsCard;
    private ListView resultsList;
    private View detailsCard;
    private TextView placeNameView;
    private TextView placeAddressView;
    private TextView placeMetaView;
    private TextView placeContactView;
    private ArrayAdapter<String> resultsAdapter;

    private Marker searchMarker;
    private Place selectedPlace;
    private boolean suppressSearchTextChange;
    private String selectedMarkerTitle = "";
    private String selectedMarkerSubtitle = "";
    private String selectedMarkerMeta = "";
    private String selectedMarkerContact = "";

    public MapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        initializePlacesClient();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        bindViews(view);
        setupSearchUi();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map, mapFragment)
                    .commitNow();
        }
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null);
        searchInput = null;
        searchProgress = null;
        resultsCard = null;
        resultsList = null;
        detailsCard = null;
        placeNameView = null;
        placeAddressView = null;
        placeMetaView = null;
        placeContactView = null;
        resultsAdapter = null;
        searchMarker = null;
        super.onDestroyView();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setOnMapClickListener(latLng -> hidePlaceDetails());
        map.setOnPoiClickListener(this::handlePointOfInterestClick);
        map.setOnMarkerClickListener(marker -> {
            if (searchMarker != null && marker.equals(searchMarker)) {
                showPlaceDetails(buildMarkerDetails(selectedPlace));
            } else {
                MarkerDetails details = getMarkerDetails(marker);
                showPlaceDetails(details);
            }
            marker.showInfoWindow();
            return true;
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mapViewModel.getAllMarkers().observe(getViewLifecycleOwner(), markers -> {
            storedMarkers.clear();
            if (markers != null) {
                storedMarkers.addAll(markers);
            }
            renderMarkers();
        });
    }

    private void bindViews(View root) {
        searchInput = root.findViewById(R.id.map_search_input);
        searchProgress = root.findViewById(R.id.map_search_progress);
        resultsCard = root.findViewById(R.id.map_search_results_card);
        resultsList = root.findViewById(R.id.map_search_results_list);
        detailsCard = root.findViewById(R.id.map_place_details_card);
        placeNameView = root.findViewById(R.id.map_place_name);
        placeAddressView = root.findViewById(R.id.map_place_address);
        placeMetaView = root.findViewById(R.id.map_place_meta);
        placeContactView = root.findViewById(R.id.map_place_contact);
    }

    private void setupSearchUi() {
        resultsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, predictionLabels);
        resultsList.setAdapter(resultsAdapter);
        autocompleteSessionToken = AutocompleteSessionToken.newInstance();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressSearchTextChange) {
                    return;
                }

                searchHandler.removeCallbacks(pendingSearchRunnable);
                if (s == null || s.toString().trim().isEmpty()) {
                    clearPredictions();
                    hidePlaceDetails();
                    selectedPlace = null;
                    renderMarkers();
                    return;
                }

                searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= autocompletePredictions.size()) {
                return;
            }

            AutocompletePrediction prediction = autocompletePredictions.get(position);
            suppressSearchTextChange = true;
            searchInput.setText(prediction.getFullText(null).toString());
            searchInput.setSelection(searchInput.getText().length());
            suppressSearchTextChange = false;

            clearPredictions();
            fetchSelectedPlace(prediction);
        });
    }

    private void initializePlacesClient() {
        String apiKey = getMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(requireContext().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());
    }

    @Nullable
    private String getMapsApiKey() {
        try {
            ApplicationInfo applicationInfo = requireContext()
                    .getPackageManager()
                    .getApplicationInfo(requireContext().getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo.metaData != null) {
                return applicationInfo.metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return null;
    }

    private void performSearch() {
        if (placesClient == null || searchInput == null) {
            return;
        }

        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            clearPredictions();
            return;
        }

        showSearchLoading(true);

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(autocompleteSessionToken)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    autocompletePredictions.clear();
                    predictionLabels.clear();
                    autocompletePredictions.addAll(response.getAutocompletePredictions());
                    for (AutocompletePrediction prediction : autocompletePredictions) {
                        predictionLabels.add(prediction.getFullText(null).toString());
                    }

                    if (resultsAdapter != null) {
                        resultsAdapter.notifyDataSetChanged();
                    }
                    resultsCard.setVisibility(predictionLabels.isEmpty() ? View.GONE : View.VISIBLE);
                    showSearchLoading(false);
                })
                .addOnFailureListener(error -> {
                    showSearchLoading(false);
                    clearPredictions();
                    Toast.makeText(requireContext(), "Unable to search places right now", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchSelectedPlace(AutocompletePrediction prediction) {
        if (placesClient == null) {
            return;
        }

        showSearchLoading(true);

        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), placeFields).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    selectedPlace = response.getPlace();
                    autocompleteSessionToken = AutocompleteSessionToken.newInstance();
                    updateSearchMarker();
                    showPlaceDetails(buildMarkerDetails(selectedPlace));
                    showSearchLoading(false);
                })
                .addOnFailureListener(error -> {
                    showSearchLoading(false);
                    Toast.makeText(requireContext(), "Unable to load place details", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderMarkers() {
        if (map == null) {
            return;
        }

        map.clear();
        searchMarker = null;

        for (MarkerEntity marker : storedMarkers) {
            LatLng position = new LatLng(marker.latitude, marker.longitude);
            Marker savedMarker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(buildStoredMarkerTitle(marker))
                    .snippet(buildStoredMarkerSnippet(marker)));
            if (savedMarker != null) {
                savedMarker.setTag(new MarkerDetails(
                        buildStoredMarkerTitle(marker),
                        buildStoredMarkerSnippet(marker),
                        "",
                        ""
                ));
            }
        }

        if (selectedPlace != null && selectedPlace.getLatLng() != null) {
            searchMarker = map.addMarker(new MarkerOptions()
                    .position(selectedPlace.getLatLng())
                    .title(getPlaceName(selectedPlace))
                    .snippet(getPlaceAddress(selectedPlace)));
            if (searchMarker != null) {
                searchMarker.setTag(buildMarkerDetails(selectedPlace));
            }
        }
    }

    private void updateSearchMarker() {
        renderMarkers();
        if (map != null && selectedPlace != null && selectedPlace.getLatLng() != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace.getLatLng(), 16f));
        }
    }

    private void showPlaceDetails() {
        showPlaceDetails(new MarkerDetails(
                selectedMarkerTitle,
                selectedMarkerSubtitle,
                selectedMarkerMeta,
                selectedMarkerContact
        ));
    }

    private void showPlaceDetails(@NonNull MarkerDetails details) {
        if (detailsCard == null) {
            return;
        }

        selectedMarkerTitle = details.title != null ? details.title : "";
        selectedMarkerSubtitle = details.subtitle != null ? details.subtitle : "";
        selectedMarkerMeta = details.meta != null ? details.meta : "";
        selectedMarkerContact = details.contact != null ? details.contact : "";

        String name = selectedMarkerTitle;
        String address = selectedMarkerSubtitle;
        String meta = selectedMarkerMeta;
        String contact = selectedMarkerContact;

        if (name.isEmpty()) {
            placeNameView.setVisibility(View.GONE);
        } else {
            placeNameView.setText(name);
            placeNameView.setVisibility(View.VISIBLE);
        }

        if (address.isEmpty()) {
            placeAddressView.setVisibility(View.GONE);
        } else {
            placeAddressView.setText(address);
            placeAddressView.setVisibility(View.VISIBLE);
        }

        if (meta.isEmpty()) {
            placeMetaView.setVisibility(View.GONE);
        } else {
            placeMetaView.setText(meta);
            placeMetaView.setVisibility(View.VISIBLE);
        }

        if (contact.isEmpty()) {
            placeContactView.setVisibility(View.GONE);
        } else {
            placeContactView.setText(contact);
            placeContactView.setVisibility(View.VISIBLE);
        }

        detailsCard.setVisibility(View.VISIBLE);
    }

    private void hidePlaceDetails() {
        if (detailsCard != null) {
            detailsCard.setVisibility(View.GONE);
        }
        selectedMarkerTitle = "";
        selectedMarkerSubtitle = "";
        selectedMarkerMeta = "";
        selectedMarkerContact = "";
    }

    private void clearPredictions() {
        autocompletePredictions.clear();
        predictionLabels.clear();
        if (resultsAdapter != null) {
            resultsAdapter.notifyDataSetChanged();
        }
        if (resultsCard != null) {
            resultsCard.setVisibility(View.GONE);
        }
        showSearchLoading(false);
    }

    private void showSearchLoading(boolean isLoading) {
        if (searchProgress != null) {
            searchProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    @NonNull
    private String getPlaceName(Place place) {
        String name = place.getName();
        return name != null ? name : "";
    }

    @NonNull
    private String getPlaceAddress(Place place) {
        String address = place.getAddress();
        return address != null ? address : "";
    }

    @NonNull
    private String buildStoredMarkerTitle(MarkerEntity marker) {
        if (marker.title != null && !marker.title.trim().isEmpty()) {
            return marker.title.trim();
        }
        return "Saved marker";
    }

    @NonNull
    private String buildStoredMarkerSnippet(MarkerEntity marker) {
        if (marker.snippet != null && !marker.snippet.trim().isEmpty()) {
            return marker.snippet.trim();
        }
        return "Coordinates: " + formatLatLng(marker.latitude, marker.longitude);
    }

    @NonNull
    private MarkerDetails getMarkerDetails(Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof MarkerDetails) {
            return (MarkerDetails) tag;
        }
        String title = marker.getTitle();
        String snippet = marker.getSnippet();
        if (title == null || title.trim().isEmpty()) {
            title = "Saved marker";
        }
        if (snippet == null || snippet.trim().isEmpty()) {
            LatLng position = marker.getPosition();
            snippet = "Coordinates: " + formatLatLng(position.latitude, position.longitude);
        }
        return new MarkerDetails(title, snippet, "", "");
    }

    private void handlePointOfInterestClick(PointOfInterest poi) {
        selectedPlace = null;
        selectedMarkerTitle = poi.name != null ? poi.name : "Point of interest";
        selectedMarkerSubtitle = "Coordinates: " + formatLatLng(poi.latLng.latitude, poi.latLng.longitude);
        selectedMarkerMeta = "Loading place details...";
        selectedMarkerContact = "";

        if (map != null) {
            searchMarker = map.addMarker(new MarkerOptions()
                    .position(poi.latLng)
                    .title(selectedMarkerTitle)
                    .snippet(selectedMarkerSubtitle));
            if (searchMarker != null) {
                searchMarker.setTag(new MarkerDetails(
                        selectedMarkerTitle,
                        selectedMarkerSubtitle,
                        selectedMarkerMeta,
                        selectedMarkerContact
                ));
                searchMarker.showInfoWindow();
            }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, 16f));
        }

        showPlaceDetails(new MarkerDetails(
                selectedMarkerTitle,
                selectedMarkerSubtitle,
                selectedMarkerMeta,
                selectedMarkerContact
        ));

        if (placesClient != null && poi.placeId != null && !poi.placeId.isEmpty()) {
            fetchPlaceById(poi.placeId);
        }
    }

    @NonNull
    private String formatLatLng(double latitude, double longitude) {
        return String.format(java.util.Locale.US, "Lat: %.5f, Lng: %.5f", latitude, longitude);
    }

    @NonNull
    private MarkerDetails buildMarkerDetails(@Nullable Place place) {
        if (place == null) {
            return new MarkerDetails("", "", "", "");
        }

        return new MarkerDetails(
                getPlaceName(place),
                getPlaceAddress(place),
                buildPlaceMeta(place),
                buildPlaceContact(place)
        );
    }

    private void fetchPlaceById(@NonNull String placeId) {
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    selectedPlace = response.getPlace();
                    MarkerDetails details = buildMarkerDetails(selectedPlace);
                    if (searchMarker != null) {
                        searchMarker.setTitle(details.title);
                        searchMarker.setSnippet(details.subtitle);
                        searchMarker.setTag(details);
                        searchMarker.showInfoWindow();
                    }
                    showPlaceDetails(details);
                })
                .addOnFailureListener(error -> selectedMarkerMeta = "");
    }

    @NonNull
    private String buildPlaceMeta(@NonNull Place place) {
        List<String> parts = new ArrayList<>();

        if (place.getRating() != null) {
            String ratingText = String.format(java.util.Locale.US, "Rating %.1f", place.getRating());
            if (place.getUserRatingsTotal() != null) {
                ratingText += " (" + place.getUserRatingsTotal() + ")";
            }
            parts.add(ratingText);
        }

        String typeLabel = getPrimaryTypeLabel(place);
        if (!typeLabel.isEmpty()) {
            parts.add(typeLabel);
        }

        return joinParts(parts);
    }

    @NonNull
    private String buildPlaceContact(@NonNull Place place) {
        List<String> parts = new ArrayList<>();

        if (place.getPhoneNumber() != null && !place.getPhoneNumber().trim().isEmpty()) {
            parts.add(place.getPhoneNumber().trim());
        }

        if (place.getWebsiteUri() != null) {
            parts.add(place.getWebsiteUri().toString());
        }

        return joinParts(parts);
    }

    @NonNull
    private String getPrimaryTypeLabel(@NonNull Place place) {
        if (place.getTypes() == null || place.getTypes().isEmpty()) {
            return "";
        }

        String rawType = place.getTypes().get(0).name().toLowerCase(java.util.Locale.US);
        String[] words = rawType.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    @NonNull
    private String joinParts(@NonNull List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(part.trim());
        }
        return builder.toString();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
