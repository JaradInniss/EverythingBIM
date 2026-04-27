package com.example.everythingbim.ui.map;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.models.MapDetailsState;
import com.example.everythingbim.data.models.MarkerDetails;
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

public class MapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {

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
            Place.Field.TYPES,
            Place.Field.EDITORIAL_SUMMARY
    );
    private final Runnable pendingSearchRunnable = this::performSearch;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private MapViewModel mapViewModel;
    private PlacesClient placesClient;
    private AutocompleteSessionToken autocompleteSessionToken;

    private EditText searchInput;
    private ProgressBar searchProgress;
    private View searchResultsContainer, detailsContainer;
    private ListView searchResultsList;
    private LinearLayout viewAllImagesBttn, viewAllReviewsBttn, viewAllPostsBttn;
    private HorizontalScrollView imagesField, reviewsField, postsField;
    private RelativeLayout detailsHeader;
    private TextView barbadosText, placeName, placeAddress, placeRating, reviewsCount, imagesCount, postsCount, noImagesText, noReviewsText, noPostsText;
    private ArrayAdapter<String> searchResultsAdapter;

    // Custom UI Buttons
    private View zoomInButton, zoomOutButton, fixLocationButton, returnButton;

    private Marker searchMarker;
    private Place selectedPlace;
    private boolean suppressSearchTextChange;

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
        setUpObservers();

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

    private void bindViews(View root) {
        searchInput = root.findViewById(R.id.map_search_input);
        searchProgress = root.findViewById(R.id.map_search_progress);
        searchResultsContainer = root.findViewById(R.id.map_search_results_card);
        searchResultsList = root.findViewById(R.id.map_search_results_list);
        detailsContainer = root.findViewById(R.id.map_location_details_container);
        detailsHeader = root.findViewById(R.id.details_peek_header);
        barbadosText = root.findViewById(R.id.map_barbados_tv);
        placeName = root.findViewById(R.id.map_location_name);
        placeAddress = root.findViewById(R.id.map_location_address);
        placeRating = root.findViewById(R.id.location_overall_rating_tv);
        noImagesText = root.findViewById(R.id.no_images_tv);
        noPostsText = root.findViewById(R.id.no_posts_tv);
        noReviewsText = root.findViewById(R.id.no_reviews_tv);
        imagesCount = root.findViewById(R.id.location_images_count_tv);
        reviewsCount = root.findViewById(R.id.location_reviews_count_tv);
        postsCount = root.findViewById(R.id.location_posts_count_tv);
        imagesField = root.findViewById(R.id.location_images_field);
        reviewsField = root.findViewById(R.id.location_reviews_field);
        postsField = root.findViewById(R.id.location_posts_field);
        zoomInButton = root.findViewById(R.id.map_zoom_in_bttn);
        zoomOutButton = root.findViewById(R.id.map_zoom_out_bttn);
        fixLocationButton = root.findViewById(R.id.map_fix_location_bttn);
        returnButton = root.findViewById(R.id.map_return_bttn);
        returnButton.setOnClickListener(this);
        viewAllImagesBttn = root.findViewById(R.id.location_images_view_all_bttn);
        viewAllImagesBttn.setOnClickListener(this);
        viewAllReviewsBttn = root.findViewById(R.id.location_reviews_view_all_bttn);
        viewAllReviewsBttn.setOnClickListener(this);
        viewAllPostsBttn = root.findViewById(R.id.location_posts_view_all_bttn);
        viewAllPostsBttn.setOnClickListener(this);
    }

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null);
        searchInput = null;
        searchProgress = null;
        searchResultsContainer = null;
        searchResultsList = null;
        detailsContainer = null;
        detailsHeader = null;
        placeName = null;
        placeAddress = null;
        placeRating = null;
        reviewsCount = null;
        searchResultsAdapter = null;
        searchMarker = null;
        zoomInButton = null;
        zoomOutButton = null;
        fixLocationButton = null;
        returnButton = null;
        selectedPlace = null;
        super.onDestroyView();
    }

    @Override
    public void onClick(View view) {
        int bttnId = view.getId();

        if (bttnId == R.id.map_fix_location_bttn) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (MapViewModel.BARBADOS_BOUNDS.contains(userLocation)) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                        } else {
                            Toast.makeText(requireContext(), "You are currently outside of Barbados", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                requestLocationPermissions();
            }
        }
        else if (bttnId == R.id.map_zoom_in_bttn) {
            map.animateCamera(CameraUpdateFactory.zoomIn());
        }
        else if (bttnId == R.id.map_zoom_out_bttn) {
            map.animateCamera(CameraUpdateFactory.zoomOut());
        }
        else if (bttnId == R.id.map_return_bttn) {
            mapViewModel.setDetailsUIState(MapDetailsState.HIDDEN);
        }
        else if (bttnId == R.id.location_images_view_all_bttn)
        {
            mapViewModel.onViewAllClicked("IMAGES");
        }
        else if (bttnId == R.id.location_reviews_view_all_bttn) {
            mapViewModel.onViewAllClicked("REVIEWS");
        }
        else if (bttnId == R.id.location_posts_view_all_bttn) {
            mapViewModel.onViewAllClicked("POSTS");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // --- Restrict Map to Barbados (MVVM) ---
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        map.setLatLngBoundsForCameraTarget(MapViewModel.BARBADOS_BOUNDS);
        map.setMinZoomPreference(MapViewModel.MIN_ZOOM);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(mapViewModel.getBarbadosCenter(), MapViewModel.INITIAL_ZOOM));

        zoomInButton.setOnClickListener(this);
        zoomOutButton.setOnClickListener(this);
        fixLocationButton.setOnClickListener(this);

        map.setOnMapClickListener(latLng -> {
            MapDetailsState currentState = mapViewModel.getDetailsUIState().getValue();
            if (currentState == MapDetailsState.FULL) {
                mapViewModel.setDetailsUIState(MapDetailsState.PEEK);
            } else {
                mapViewModel.setDetailsUIState(MapDetailsState.HIDDEN);
            }
        });

        map.setOnPoiClickListener(this::handlePointOfInterestClick);

        map.setOnMarkerClickListener(marker -> {
            LatLng position = marker.getPosition();
            MarkerDetails details = getMarkerDetails(marker);
            showPlaceDetails(details);
            marker.showInfoWindow();

            mapViewModel.setFocusedLocation(position);
            
            // Set metadata for navigation
            mapViewModel.setSelectedLocationMetadata(details.id, details.title);
            
            mapViewModel.setDetailsUIState(MapDetailsState.FULL);
            return true;
        });

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestLocationPermissions();
        }

        mapViewModel.getAllMarkers().observe(getViewLifecycleOwner(), markers -> {
            storedMarkers.clear();
            if (markers != null) {
                storedMarkers.addAll(markers);
            }
            renderMarkers();
        });
    }

    private void setUpObservers() {
        mapViewModel.getDetailsUIState().observe(getViewLifecycleOwner(), this::updateUIState);

        mapViewModel.getNavigationEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;

            Intent intent = new Intent(requireContext(), event.getDestination());
            intent.putExtras(event.getExtras());
            startActivity(intent);
        });
    }

    private void updateUIState(MapDetailsState state) {
        if (map == null || detailsContainer == null || detailsHeader == null) return;

        // Force a measurement pass to get the accurate height of the header (which is wrap_content)
        // This handles long names/addresses by calculating the height based on current text content.
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                getResources().getDisplayMetrics().widthPixels, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        detailsHeader.measure(widthMeasureSpec, heightMeasureSpec);
        
        int headerHeightPx = detailsHeader.getMeasuredHeight() + 15;
        int fullHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, getResources().getDisplayMetrics());

        if (state == MapDetailsState.FULL) {
            barbadosText.setVisibility(View.GONE);
            returnButton.setVisibility(View.VISIBLE);
            detailsContainer.setVisibility(View.VISIBLE);

            // Natural bottom position
            detailsContainer.animate().translationY(0).setDuration(300).start();
            animateButtons(-fullHeightPx);

            map.setPadding(0, 0, 0, fullHeightPx);
            moveCameraToFocus(16f);

        } else if (state == MapDetailsState.PEEK) {
            barbadosText.setVisibility(View.GONE);
            returnButton.setVisibility(View.VISIBLE);
            detailsContainer.setVisibility(View.VISIBLE);

            // Move DOWN by (FullHeight - HeaderHeight) so only the header is visible
            float translationY = fullHeightPx - headerHeightPx;
            detailsContainer.animate().translationY(translationY).setDuration(300).start();
            
            // Buttons sit exactly on top of the peeked header
            animateButtons(-headerHeightPx);

            map.setPadding(0, 0, 0, headerHeightPx);
            moveCameraToFocus(15f);

        } else { // HIDDEN
            barbadosText.setVisibility(View.VISIBLE);
            returnButton.setVisibility(View.GONE);
            detailsContainer.setVisibility(View.GONE);

            animateButtons(0);
            map.setPadding(0, 0, 0, 0);
            map.clear();
            searchMarker = null;
            selectedPlace = null;
            renderMarkers();

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(mapViewModel.getBarbadosCenter(), MapViewModel.INITIAL_ZOOM));

            if (searchInput != null) {
                searchInput.clearFocus();
                hideKeyboard();
                suppressSearchTextChange = true;
                searchInput.setText("");
                suppressSearchTextChange = false;
            }
        }
    }

    private void animateButtons(float translationY) {
        fixLocationButton.animate().translationY(translationY).setDuration(300).start();
        zoomInButton.animate().translationY(translationY).setDuration(300).start();
        zoomOutButton.animate().translationY(translationY).setDuration(300).start();
    }

    private void moveCameraToFocus(float zoom) {
        LatLng focus = mapViewModel.getFocusedLocation().getValue();
        if (focus != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(focus, zoom));
        }
    }

    private void hideKeyboard() {
        if (getView() != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void setupSearchUi() {
        searchResultsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, predictionLabels);
        searchResultsList.setAdapter(searchResultsAdapter);
        autocompleteSessionToken = AutocompleteSessionToken.newInstance();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressSearchTextChange) return;
                searchHandler.removeCallbacks(pendingSearchRunnable);
                if (s == null || s.toString().trim().isEmpty()) {
                    clearPredictions();
                    if (mapViewModel.getDetailsUIState().getValue() != MapDetailsState.HIDDEN) {
                        mapViewModel.setDetailsUIState(MapDetailsState.HIDDEN);
                    }
                    return;
                }
                searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        searchResultsList.setOnItemClickListener((parent, view, position, id) -> {
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
        if (apiKey == null || apiKey.isEmpty()) return;
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(requireContext().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());
    }

    @Nullable
    private String getMapsApiKey() {
        try {
            ApplicationInfo ai = requireContext().getPackageManager().getApplicationInfo(requireContext().getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) return ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException ignored) {}
        return null;
    }

    private void performSearch() {
        if (placesClient == null || searchInput == null) return;
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            clearPredictions();
            return;
        }
        showSearchLoading(true);
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(autocompleteSessionToken).setQuery(query).build();
        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            autocompletePredictions.clear();
            predictionLabels.clear();
            autocompletePredictions.addAll(response.getAutocompletePredictions());
            for (AutocompletePrediction prediction : autocompletePredictions) {
                predictionLabels.add(prediction.getFullText(null).toString());
            }
            if (searchResultsAdapter != null) searchResultsAdapter.notifyDataSetChanged();
            searchResultsContainer.setVisibility(predictionLabels.isEmpty() ? View.GONE : View.VISIBLE);
            showSearchLoading(false);
        }).addOnFailureListener(error -> {
            showSearchLoading(false);
            clearPredictions();
            Toast.makeText(requireContext(), "Unable to search places", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchSelectedPlace(AutocompletePrediction prediction) {
        if (placesClient == null) return;
        showSearchLoading(true);
        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), placeFields).build();
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            selectedPlace = response.getPlace();
            autocompleteSessionToken = AutocompleteSessionToken.newInstance();

            if (selectedPlace != null && selectedPlace.getLatLng() != null) {
                renderMarkers();
                mapViewModel.setFocusedLocation(selectedPlace.getLatLng());
                
                // Set metadata for navigation
                mapViewModel.setSelectedLocationMetadata(-1, selectedPlace.getName());
                
                mapViewModel.setDetailsUIState(MapDetailsState.FULL);
                showPlaceDetails(buildMarkerDetails(selectedPlace));
            }
            showSearchLoading(false);
        }).addOnFailureListener(error -> {
            showSearchLoading(false);
            Toast.makeText(requireContext(), "Unable to load place details", Toast.LENGTH_SHORT).show();
        });
    }

    private void renderMarkers() {
        if (map == null) return;
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
                        marker.id,
                        buildStoredMarkerTitle(marker),
                        buildStoredMarkerSnippet(marker),
                        "", "", 0.0f, "", "",
                        new ArrayList<String>(), new ArrayList<ReviewEntity>(), new ArrayList<PostEntity>()
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

    private void showPlaceDetails(@NonNull MarkerDetails details) {
        if (detailsContainer == null) return;
        placeName.setText(details.title);
        placeAddress.setText(details.subtitle);
        placeRating.setText(String.format(java.util.Locale.US, "%.1f", details.rating));

        if (details.imageUrls == null || details.imageUrls.isEmpty()) {
            noImagesText.setVisibility(View.VISIBLE);
            imagesCount.setVisibility(View.GONE);
            imagesField.setVisibility(View.GONE);
            //viewAllImagesBttn.setVisibility(View.GONE);
        } else {
            noImagesText.setVisibility(View.GONE);
            imagesCount.setText(String.format(java.util.Locale.US, "(%d)", details.imageUrls.size()));
            imagesField.setVisibility(View.VISIBLE);
            viewAllImagesBttn.setVisibility(View.VISIBLE);
        }

        if (details.reviews == null || details.reviews.isEmpty()) {
            noReviewsText.setVisibility(View.VISIBLE);
            reviewsCount.setVisibility(View.GONE);
            reviewsField.setVisibility(View.GONE);
            //viewAllReviewsBttn.setVisibility(View.GONE);
        } else {
            noReviewsText.setVisibility(View.GONE);
            reviewsCount.setText(String.format(java.util.Locale.US, "(%d)", details.reviews.size()));
            reviewsField.setVisibility(View.VISIBLE);
            viewAllReviewsBttn.setVisibility(View.VISIBLE);
        }

        if (details.posts == null || details.posts.isEmpty()) {
            noPostsText.setVisibility(View.VISIBLE);
            postsCount.setVisibility(View.GONE);
            postsField.setVisibility(View.GONE);
            //viewAllPostsBttn.setVisibility(View.GONE);
        } else {
            noPostsText.setVisibility(View.GONE);
            postsCount.setText(String.format(java.util.Locale.US, "(%d)", details.posts.size()));
            postsField.setVisibility(View.VISIBLE);
            viewAllPostsBttn.setVisibility(View.VISIBLE);
        }

        detailsContainer.setVisibility(View.VISIBLE);
    }

    private void hidePlaceDetails() {
        if (detailsContainer != null) detailsContainer.setVisibility(View.GONE);
    }

    private void clearPredictions() {
        autocompletePredictions.clear();
        predictionLabels.clear();
        if (searchResultsAdapter != null) searchResultsAdapter.notifyDataSetChanged();
        if (searchResultsContainer != null) searchResultsContainer.setVisibility(View.GONE);
        showSearchLoading(false);
    }

    private void showSearchLoading(boolean isLoading) {
        if (searchProgress != null) searchProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @NonNull private String getPlaceName(Place place) { String name = place.getName(); return name != null ? name : ""; }
    @NonNull private String getPlaceAddress(Place place) { String address = place.getAddress(); return address != null ? address : ""; }
    @NonNull private String buildStoredMarkerTitle(MarkerEntity marker) { return (marker.title != null && !marker.title.trim().isEmpty()) ? marker.title.trim() : "Saved marker"; }
    @NonNull private String buildStoredMarkerSnippet(MarkerEntity marker) { return (marker.snippet != null && !marker.snippet.trim().isEmpty()) ? marker.snippet.trim() : "Coordinates: " + formatLatLng(marker.latitude, marker.longitude); }

    @NonNull
    private MarkerDetails getMarkerDetails(Marker marker) {
        Object tag = marker.getTag();
        if (tag instanceof MarkerDetails) return (MarkerDetails) tag;
        String title = marker.getTitle() != null ? marker.getTitle() : "Saved marker";
        String snippet = marker.getSnippet() != null ? marker.getSnippet() : "Coordinates: " + formatLatLng(marker.getPosition().latitude, marker.getPosition().longitude);

        return new MarkerDetails(-1, title, snippet, "", "", 0.0f, "", "",
                new ArrayList<String>(), new ArrayList<ReviewEntity>(), new ArrayList<PostEntity>());
    }

    private void handlePointOfInterestClick(PointOfInterest poi) {
        selectedPlace = null;
        renderMarkers();
        searchMarker = map.addMarker(new MarkerOptions().position(poi.latLng).title(poi.name));

        MarkerDetails details = new MarkerDetails(-1, poi.name, "Loading address...", "", "", 0.0f, "", "",
                new ArrayList<String>(), new ArrayList<ReviewEntity>(), new ArrayList<PostEntity>());

        if (searchMarker != null) {
            searchMarker.setTag(details);
            searchMarker.showInfoWindow();
        }
        showPlaceDetails(details);
        mapViewModel.setFocusedLocation(poi.latLng);
        
        // Set metadata for navigation
        mapViewModel.setSelectedLocationMetadata(-1, details.title);
        
        mapViewModel.setDetailsUIState(MapDetailsState.FULL);

        if (placesClient != null && poi.placeId != null) {
            fetchPlaceById(poi.placeId);
        }
    }

    private void fetchPlaceById(@NonNull String placeId) {
        if (placesClient == null) return;
        showSearchLoading(true);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            selectedPlace = response.getPlace();
            MarkerDetails details = buildMarkerDetails(selectedPlace);
            if (searchMarker != null) {
                searchMarker.setTag(details);
                showPlaceDetails(details);
            }
            
            // Update metadata once details are fetched
            mapViewModel.setSelectedLocationMetadata(-1, details.title);
            
            showSearchLoading(false);
        }).addOnFailureListener(error -> {
            showSearchLoading(false);
            Toast.makeText(requireContext(), "Unable to load place details", Toast.LENGTH_SHORT).show();
        });
    }

    @NonNull private String formatLatLng(double lat, double lng) { return String.format(java.util.Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng); }

    @NonNull
    private MarkerDetails buildMarkerDetails(@Nullable Place place) {
        if (place == null) return new MarkerDetails(-1, "", "", "", "", 0.0f, "", "",
                new ArrayList<String>(), new ArrayList<ReviewEntity>(), new ArrayList<PostEntity>());

        float rating = (place.getRating() != null) ? place.getRating().floatValue() : 0.0f;
        String overview = (place.getEditorialSummary() != null) ? place.getEditorialSummary() : "";
        String type = (place.getTypes() != null && !place.getTypes().isEmpty()) ? place.getTypes().get(0).name() : "";

        return new MarkerDetails(
                -1,
                getPlaceName(place),
                getPlaceAddress(place),
                "",
                (place.getPhoneNumber() != null) ? place.getPhoneNumber() : "",
                rating,
                overview,
                type,
                new ArrayList<String>(),
                new ArrayList<ReviewEntity>(),
                new ArrayList<PostEntity>()
        );
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }
}
