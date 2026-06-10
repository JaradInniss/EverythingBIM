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
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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

import com.example.everythingbim.BuildConfig;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.LocationWithDetails;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.models.MapDetailsState;
import com.example.everythingbim.data.models.MarkerDetails;
import com.example.everythingbim.databinding.FragmentMapBinding;
import com.example.everythingbim.ui.home.NearbySavedLocation;
import com.example.everythingbim.ui.main.MainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment implements OnMapReadyCallback, View.OnClickListener {
    public static final String ARG_OPEN_FOCUS_LOCATION = "arg_open_focus_location";
    public static final String ARG_FOCUS_LOCATION_ID = "arg_focus_location_id";
    public static final String ARG_FOCUS_LATITUDE = "arg_focus_latitude";
    public static final String ARG_FOCUS_LONGITUDE = "arg_focus_longitude";
    public static final String ARG_FOCUS_TITLE = "arg_focus_title";
    public static final String ARG_FOCUS_SUBTITLE = "arg_focus_subtitle";
    public static final String ARG_OPEN_ROUTE_PREVIEW = "arg_open_route_preview";
    public static final String ARG_ROUTE_LOCATIONS = "arg_route_locations";
    public static final String EXTRA_OPEN_MAP = "EXTRA_OPEN_MAP";
    public static final String EXTRA_LATITUDE = "EXTRA_LATITUDE";
    public static final String EXTRA_LONGITUDE = "EXTRA_LONGITUDE";
    public static final String EXTRA_LOCATION_NAME = "EXTRA_LOCATION_NAME";


    private static final double PARLIAMENT_LATITUDE = 13.0969861d;
    private static final double PARLIAMENT_LONGITUDE = -59.6139194d;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService routeExecutor = Executors.newSingleThreadExecutor();
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
    private FragmentMapBinding binding;
    private MapViewModel mapViewModel;
    private PlacesClient placesClient;
    private AutocompleteSessionToken autocompleteSessionToken;

    // UI Elements
    private EditText searchInput, overviewInput, newReviewInput;
    private ProgressBar searchProgress;
    private View searchResultsContainer, detailsContainer, zoomInButton, zoomOutButton, fixLocationButton, returnButton;
    private ListView searchResultsList;
    private LinearLayout viewAllImagesBttn, viewAllReviewsBttn, viewAllPostsBttn, writeReviewBttn, writeReviewContainer, submitReviewBttn, directionsButton;
    private HorizontalScrollView imagesField, reviewsField, postsField;
    private RelativeLayout detailsHeader;
    private TextView barbadosText, placeName, placeAddress, placeRating, reviewsCount, imagesCount, postsCount, noImagesText, noReviewsText, noPostsText, placeMeta, placeContact;
    private ImageView ratingStar1, ratingStar2, ratingStar3, ratingStar4, ratingStar5;

    private ArrayAdapter<String> searchResultsAdapter;

    private Marker searchMarker;
    private Place selectedPlace;
    private boolean suppressSearchTextChange;
    private long focusedSavedLocationId = -1L;
    private MarkerDetails focusedSavedMarkerDetails;
    private LatLng externalFocusLatLng;
    private String externalFocusTitle;
    private String externalFocusSubtitle;
    private final ArrayList<NearbySavedLocation> routePreviewLocations = new ArrayList<>();
    private Polyline routePreviewPolyline;
    private Polyline routePreviewOutlinePolyline;
    private RoutesApiService.RoutePreviewData routePreviewData;
    private boolean routePreviewFetchInProgress;
    private String routePreviewErrorMessage;

    public MapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        initializePlacesClient();
        readFocusArguments();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        bindViews(view);
        setupSearchUi();
        setUpObservers();
        observeFocusedSavedLocation();

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
        // Linear Layouts
        writeReviewBttn = binding.writeReviewBttn;
        writeReviewBttn.setOnClickListener(this);
        submitReviewBttn = binding.submitReviewBttn;
        submitReviewBttn.setOnClickListener(this);
        viewAllImagesBttn = binding.locationImagesViewAllBttn;
        viewAllImagesBttn.setOnClickListener(this);
        viewAllReviewsBttn = binding.locationReviewsViewAllBttn;
        viewAllReviewsBttn.setOnClickListener(this);
        viewAllPostsBttn = binding.locationPostsViewAllBttn;
        viewAllPostsBttn.setOnClickListener(this);
        directionsButton = binding.directionsBttn;
        directionsButton.setOnClickListener(this);
        writeReviewContainer = binding.writeReviewContainer;

        // Progress Bar
        searchProgress = binding.mapSearchProgress;

        // Views
        zoomInButton = binding.mapZoomInBttn;
        zoomOutButton = binding.mapZoomOutBttn;
        fixLocationButton = binding.mapFixLocationBttn;
        returnButton = binding.mapReturnBttn;
        returnButton.setOnClickListener(this);
        searchResultsContainer = binding.mapSearchResultsCard;
        detailsContainer = binding.mapLocationDetailsContainer;

        // List View
        searchResultsList = binding.mapSearchResultsList;

        // Relative Layout
        detailsHeader = binding.detailsPeekHeader;

        // Text View
        barbadosText = binding.mapBarbadosTv;
        placeName = binding.mapLocationName;
        placeAddress = binding.mapLocationAddress;
        placeRating = binding.locationOverallRatingTv;
        placeMeta = binding.mapPlaceMeta;
        placeContact = binding.mapPlaceContact;
        noImagesText = binding.noImagesTv;
        noPostsText = binding.noPostsTv;
        noReviewsText = binding.noReviewsTv;
        imagesCount = binding.locationImagesCountTv;
        reviewsCount = binding.locationReviewsCountTv;
        postsCount = binding.locationPostsCountTv;

        // Image Views
        ratingStar1 = binding.ratingStar1;
        ratingStar1.setOnClickListener(v -> mapViewModel.setRating(1));
        ratingStar2 = binding.ratingStar2;
        ratingStar2.setOnClickListener(v -> mapViewModel.setRating(2));
        ratingStar3 = binding.ratingStar3;
        ratingStar3.setOnClickListener(v -> mapViewModel.setRating(3));
        ratingStar4 = binding.ratingStar4;
        ratingStar4.setOnClickListener(v -> mapViewModel.setRating(4));
        ratingStar5 = binding.ratingStar5;
        ratingStar5.setOnClickListener(v -> mapViewModel.setRating(5));

        // Horizontal Scroll Views
        imagesField = binding.locationImagesField;
        reviewsField = binding.locationReviewsField;
        postsField = binding.locationPostsField;

        // Edit Texts
        searchInput = binding.mapSearchInput;
        overviewInput = binding.mapOverviewInput;
        newReviewInput = binding.newReviewInput;
        newReviewInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override public void afterTextChanged(Editable s) {
                mapViewModel.setReviewBody(s.toString());
            }
        });
    }

    @Override
    public void onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null);
        searchInput = null;
        overviewInput = null;
        searchProgress = null;
        searchResultsContainer = null;
        searchResultsList = null;
        detailsContainer = null;
        detailsHeader = null;
        placeName = null;
        placeAddress = null;
        placeRating = null;
        placeMeta = null;
        placeContact = null;
        reviewsCount = null;
        searchResultsAdapter = null;
        searchMarker = null;
        zoomInButton = null;
        zoomOutButton = null;
        fixLocationButton = null;
        returnButton = null;
        selectedPlace = null;

        placesClient = null;

        routeExecutor.shutdownNow();
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
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else if (bttnId == R.id.map_zoom_in_bttn) {
            if (map != null) map.animateCamera(CameraUpdateFactory.zoomIn());
        } else if (bttnId == R.id.map_zoom_out_bttn) {
            if (map != null) map.animateCamera(CameraUpdateFactory.zoomOut());
        } else if (bttnId == R.id.map_return_bttn) {
            if (detailsContainer != null) {
                detailsContainer.setVisibility(View.GONE);
            }
        } else if (bttnId == R.id.write_review_bttn) {
            toggleWriteReview();
        } else if (bttnId == R.id.submit_review_bttn) {
            mapViewModel.submitReview();
        }
    }

    private void setUpObservers() {
        mapViewModel.getDetailsUIState().observe(getViewLifecycleOwner(), this::updateUIState);

        mapViewModel.getNavigationEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null) return;

            Intent intent = new Intent(requireContext(), event.getDestination());
            intent.putExtras(event.getExtras());
            startActivity(intent);
        });

        mapViewModel.getRating().observe(getViewLifecycleOwner(), rating -> {
            List<ImageView> starsList = Arrays.asList(ratingStar1, ratingStar2, ratingStar3, ratingStar4, ratingStar5);
            for (int i = 0; i < starsList.size(); i++) {
                if (i < rating) {
                    starsList.get(i).setImageResource(R.drawable.ic_star_fill);
                    starsList.get(i).setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.gold));
                } else {
                    starsList.get(i).setImageResource(R.drawable.ic_star_outlined);
                    starsList.get(i).setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.dim_grey));
                }
            }
        });

        mapViewModel.getIsReviewValid().observe(getViewLifecycleOwner(), isValid -> {
            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
            submitReviewBttn.setEnabled(isValid);
            if (isValid) {
                submitReviewBttn.setBackgroundResource(R.drawable.bg_rectangle_blue);
            } else {
                submitReviewBttn.setBackgroundResource(R.drawable.bg_rectangle_dim_grey);
            }
        });

        mapViewModel.getReviewSubmited().observe(getViewLifecycleOwner(), submitted -> {
            if (submitted) {
                Toast.makeText(requireContext(), "Review submitted", Toast.LENGTH_SHORT).show();
                toggleWriteReview();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // 1. Basic Setup
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setLatLngBoundsForCameraTarget(MapViewModel.BARBADOS_BOUNDS);
        map.setMinZoomPreference(MapViewModel.MIN_ZOOM);

        // 2. Click Listeners (Consolidated)
        map.setOnMapClickListener(latLng -> {
            MapDetailsState currentState = mapViewModel.getDetailsUIState().getValue();
            mapViewModel.setDetailsUIState(currentState == MapDetailsState.FULL ? MapDetailsState.PEEK : MapDetailsState.HIDDEN);
        });

        map.setOnPoiClickListener(this::handlePointOfInterestClick);

        map.setOnMarkerClickListener(marker -> {
            LatLng position = marker.getPosition();
            MarkerDetails details = getMarkerDetails(marker);
            showPlaceDetails(details);
            marker.showInfoWindow();

            mapViewModel.setFocusedLocation(position);
            mapViewModel.setSelectedLocationMetadata(details.id, details.title);
            mapViewModel.setDetailsUIState(MapDetailsState.FULL);
            return true;
        });

        // 3. Permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            requestLocationPermissions();
        }

        // 4. Marker Observer & Focus Logic
        mapViewModel.getAllMarkers().observe(getViewLifecycleOwner(), markers -> {
            storedMarkers.clear();
            if (markers != null) storedMarkers.addAll(markers);
            renderMarkers();

            // Check for focus AFTER markers are rendered
            if (externalFocusLatLng != null && routePreviewLocations.isEmpty()) {
                processExternalFocus();
            }
        });

        if (!routePreviewLocations.isEmpty()) {
            mapViewModel.setFocusedLocation(new LatLng(PARLIAMENT_LATITUDE, PARLIAMENT_LONGITUDE));
            mapViewModel.setSelectedLocationMetadata(-1, "Parliament route preview");
            mapViewModel.setDetailsUIState(MapDetailsState.FULL);
            showRoutePreview();
        } else if (externalFocusLatLng != null) {
            mapViewModel.setFocusedLocation(externalFocusLatLng);
            mapViewModel.setSelectedLocationMetadata(
                    focusedSavedMarkerDetails != null ? focusedSavedMarkerDetails.id : -1,
                    externalFocusTitle != null ? externalFocusTitle : "Selected location"
            );
            mapViewModel.setDetailsUIState(MapDetailsState.FULL);
            showExternalFocusedLocation();
        }

        // 5. Initial Camera Position
        if (!routePreviewLocations.isEmpty()) {
            showRoutePreview();
        } else if (externalFocusLatLng == null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(mapViewModel.getBarbadosCenter(), MapViewModel.INITIAL_ZOOM));
        }

        zoomInButton.setOnClickListener(this);
        zoomOutButton.setOnClickListener(this);
        fixLocationButton.setOnClickListener(this);
    }

    private void processExternalFocus() {
        if (map == null || externalFocusLatLng == null) return;

        // Capture values into local variables so we can clear the class members immediately
        final LatLng targetCoords = externalFocusLatLng;
        final String targetTitle = externalFocusTitle;

        // Clear class members immediately so this logic doesn't trigger again on the next DB update
        if (focusedSavedLocationId > 0L) {
            return;
        }

        long foundId = -1;
        for (MarkerEntity entity : storedMarkers) {
            if (Math.abs(entity.latitude - targetCoords.latitude) < 0.0001 &&
                    Math.abs(entity.longitude - targetCoords.longitude) < 0.0001) {
                foundId = entity.id;
                break;
            }
        }

        if (foundId != -1) {
            mapViewModel.setFocusedLocation(targetCoords);
            mapViewModel.setSelectedLocationMetadata(foundId, targetTitle);
            mapViewModel.setDetailsUIState(MapDetailsState.FULL);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(targetCoords, 15f));
        } else {
            // Pass the captured local variables
            searchPlacesByName(targetTitle, targetCoords);
        }
    }

    private void searchPlacesByName(String name, LatLng latLng) {
        if (placesClient == null) return;

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(name)
                .setSessionToken(AutocompleteSessionToken.newInstance())
                .setCountries("BB")
                .build();

        placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
            if (!response.getAutocompletePredictions().isEmpty()) {
                // Get first result's Place ID
                String placeId = response.getAutocompletePredictions().get(0).getPlaceId();

                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build();
                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(fetchResponse -> {
                    Place place = fetchResponse.getPlace();
                    handlePlaceSelection(place);
                    mapViewModel.setDetailsUIState(MapDetailsState.PEEK);
                });
            }
            else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                Toast.makeText(getContext(), "Location details unavailable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handlePlaceSelection(Place place) {
        if (place == null || place.getLatLng() == null) return;

        selectedPlace = place;

        // Clear previous search marker
        if (searchMarker != null) searchMarker.remove();

        // Create a new marker for this place
        searchMarker = map.addMarker(new MarkerOptions()
                .position(place.getLatLng())
                .title(place.getName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // Update ViewModel
        mapViewModel.setFocusedLocation(place.getLatLng());
        mapViewModel.setSelectedLocationMetadata(-1, place.getName());

        // Populate the UI fields (Name, Address, etc.)
        placeName.setText(place.getName());
        placeAddress.setText(place.getAddress());

        if (place.getRating() != null) {
            placeRating.setText(String.format(Locale.getDefault(), "%.1f", place.getRating()));
        } else {
            placeRating.setText("N/A");
        }

        // Zoom into the new location
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15f));
    }

    private void readFocusArguments() {
        Bundle args = getArguments();
        Intent intent = requireActivity().getIntent();

        if (args != null) {
            focusedSavedLocationId = args.getLong(ARG_FOCUS_LOCATION_ID, -1L);

            if (args.getBoolean(ARG_OPEN_FOCUS_LOCATION, false)) {
                externalFocusLatLng = new LatLng(
                        args.getDouble(ARG_FOCUS_LATITUDE, 0d),
                        args.getDouble(ARG_FOCUS_LONGITUDE, 0d)
                );
                externalFocusTitle = args.getString(ARG_FOCUS_TITLE, "Selected location");
                externalFocusSubtitle = args.getString(ARG_FOCUS_SUBTITLE, "");
            }

            if (args.getBoolean(ARG_OPEN_ROUTE_PREVIEW, false)) {
                routePreviewLocations.clear();
                Serializable value = args.getSerializable(ARG_ROUTE_LOCATIONS);
                if (value instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<NearbySavedLocation> restored = (ArrayList<NearbySavedLocation>) value;
                    routePreviewLocations.addAll(restored);
                }
            }

            return;
        }

        if (intent != null && intent.getBooleanExtra(MainActivity.EXTRA_OPEN_MAP, false)) {
            focusedSavedLocationId = intent.getLongExtra(MainActivity.EXTRA_MAP_FOCUS_LOCATION_ID, -1L);

            double lat = intent.getDoubleExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, 0d);
            double lng = intent.getDoubleExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, 0d);

            externalFocusLatLng = new LatLng(lat, lng);
            externalFocusTitle = intent.getStringExtra(MainActivity.EXTRA_MAP_FOCUS_NAME);
            externalFocusSubtitle = intent.getStringExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE);

            if (externalFocusTitle == null) {
                externalFocusTitle = "Selected location";
            }
        }
    }

    private void observeFocusedSavedLocation() {
        if (focusedSavedLocationId <= 0L) {
            return;
        }

        AppDatabase.getInstance(requireContext())
                .locationDao()
                .getLocationWithDetailsById(focusedSavedLocationId)
                .observe(getViewLifecycleOwner(), item -> {
                    if (item == null || item.location == null) {
                        return;
                    }

                    LocationEntity location = item.location;
                    externalFocusLatLng = new LatLng(location.latitude, location.longitude);
                    externalFocusTitle = location.name != null ? location.name : "Saved location";
                    externalFocusSubtitle = location.address != null ? location.address : "";
                    focusedSavedMarkerDetails = buildSavedLocationDetails(item);

                    if (map != null) {
                        mapViewModel.setFocusedLocation(externalFocusLatLng);
                        mapViewModel.setSelectedLocationMetadata(location.locationId, externalFocusTitle);
                        showExternalFocusedLocation();
                        mapViewModel.setDetailsUIState(MapDetailsState.PEEK);
                    }
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

    private void toggleWriteReview() {
        if (writeReviewBttn.getVisibility() == View.VISIBLE) {
            writeReviewBttn.setVisibility(View.GONE);
            writeReviewContainer.setVisibility(View.VISIBLE);
            mapViewModel.resetReviewForm();
        } else {
            writeReviewBttn.setVisibility(View.VISIBLE);
            writeReviewContainer.setVisibility(View.GONE);
            mapViewModel.resetReviewForm();
        }
    }

//    private void handleReviewSubmit() {
//        Boolean isValid = mapViewModel.getIsReviewValid().getValue();
//
//        if (isValid != null && isValid) {
//            mapViewModel.submitReview();
//        } else {
//            StringBuilder missingFields = new StringBuilder("Please Enter: ");
//            boolean first = true;
//            if (mapViewModel.getRating().getValue() == null) {
//                missingFields.append("Rating");
//                first = false;
//            }
//            if (mapViewModel.getReviewBody().getValue() == null || mapViewModel.getReviewBody().getValue().trim().isEmpty()) {
//                if (!first) missingFields.append(", ");
//                missingFields.append("Review Body");
//            }
//
//            Toast.makeText(requireContext(), missingFields.toString(), Toast.LENGTH_LONG).show();
//        }
//    }

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
            Places.initializeWithNewPlacesApiEnabled(
                    requireContext().getApplicationContext(),
                    apiKey
            );
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
        routePreviewPolyline = null;
        routePreviewOutlinePolyline = null;

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
        } else if (externalFocusLatLng != null) {
            searchMarker = map.addMarker(new MarkerOptions()
                    .position(externalFocusLatLng)
                    .title(externalFocusTitle != null ? externalFocusTitle : "Selected location")
                    .snippet(externalFocusSubtitle != null ? externalFocusSubtitle : ""));
            if (searchMarker != null) {
                searchMarker.setTag(focusedSavedMarkerDetails != null
                        ? focusedSavedMarkerDetails
                        : new MarkerDetails(
                                -1,
                                externalFocusTitle != null ? externalFocusTitle : "Selected location",
                                externalFocusSubtitle != null ? externalFocusSubtitle : "",
                                "",
                                "",
                                0.0f,
                                "",
                                "",
                                new ArrayList<String>(),
                                new ArrayList<ReviewEntity>(),
                                new ArrayList<PostEntity>()
                        ));
            }
        }

        if (!routePreviewLocations.isEmpty()) {
            drawRoutePreview();
        }
    }

    private void showExternalFocusedLocation() {
        renderMarkers();
        if (searchMarker != null) {
            MarkerDetails details = getMarkerDetails(searchMarker);
            showPlaceDetails(details);
            searchMarker.showInfoWindow();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(externalFocusLatLng, 17f));
        }
    }

    private void showRoutePreview() {
        loadRoutePreviewIfNeeded();

        float totalDistanceMeters = routePreviewData != null
                ? routePreviewData.distanceMeters
                : calculateRoutePreviewDistanceMeters();
        int estimatedMinutes = routePreviewData != null
                ? Math.max(1, (int) Math.ceil(routePreviewData.durationSeconds / 60d))
                : estimateWalkingMinutes(totalDistanceMeters);
        renderMarkers();

        MarkerDetails details = new MarkerDetails(
                -1,
                "Parliament route preview",
                routePreviewLocations.size() + " selected stop" + (routePreviewLocations.size() == 1 ? "" : "s"),
                "Walking preview | " + formatDistance(totalDistanceMeters) + " | " + estimatedMinutes + " min walk",
                buildRouteStopSummary(),
                0.0f,
                buildRouteOverview(),
                "route",
                new ArrayList<String>(),
                new ArrayList<ReviewEntity>(),
                new ArrayList<PostEntity>()
        );
        showPlaceDetails(details);
        fitRoutePreviewBounds();
    }

    private void drawRoutePreview() {
        if (map == null || routePreviewLocations.isEmpty()) {
            return;
        }

        ArrayList<LatLng> routePoints = new ArrayList<>();
        LatLng parliament = new LatLng(PARLIAMENT_LATITUDE, PARLIAMENT_LONGITUDE);
        routePoints.add(parliament);

        Marker parliamentMarker = map.addMarker(new MarkerOptions()
                .position(parliament)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("Barbados Parliament Buildings")
                .snippet("Start"));
        if (parliamentMarker != null) {
            parliamentMarker.setTag(new MarkerDetails(
                    -1,
                    "Barbados Parliament Buildings",
                    "Route starting point",
                    "",
                    "",
                    0.0f,
                    "",
                    "",
                    new ArrayList<String>(),
                    new ArrayList<ReviewEntity>(),
                    new ArrayList<PostEntity>()
            ));
        }

        for (int index = 0; index < routePreviewLocations.size(); index++) {
            NearbySavedLocation stop = routePreviewLocations.get(index);
            LatLng stopLatLng = new LatLng(stop.getLatitude(), stop.getLongitude());
            routePoints.add(stopLatLng);

            float markerHue = index == routePreviewLocations.size() - 1
                    ? BitmapDescriptorFactory.HUE_RED
                    : BitmapDescriptorFactory.HUE_ORANGE;
            String markerTitle = (index + 1) + ". " + stop.getName();
            String markerSnippet = index == routePreviewLocations.size() - 1
                    ? "Destination | " + stop.getDistanceLabel()
                    : "Stop " + (index + 1) + " | " + stop.getDistanceLabel();

            Marker routeMarker = map.addMarker(new MarkerOptions()
                    .position(stopLatLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerHue))
                    .title(markerTitle)
                    .snippet(markerSnippet));
            if (routeMarker != null) {
                routeMarker.setTag(new MarkerDetails(
                        stop.getLocationId(),
                        markerTitle,
                        stop.getDescriptionOrFallback(),
                        markerSnippet,
                        "",
                        stop.getRating(),
                        stop.getDescriptionOrFallback(),
                        stop.getCategory() != null ? stop.getCategory() : "",
                        new ArrayList<String>(),
                        new ArrayList<ReviewEntity>(),
                        new ArrayList<PostEntity>()
                ));
            }
        }

        List<LatLng> polylinePoints = routePreviewData != null
                && routePreviewData.path != null
                && !routePreviewData.path.isEmpty()
                ? routePreviewData.path
                : routePoints;

        routePreviewOutlinePolyline = map.addPolyline(new PolylineOptions()
                .addAll(polylinePoints)
                .width(16f)
                .color(ContextCompat.getColor(requireContext(), R.color.prussian_blue)));

        routePreviewPolyline = map.addPolyline(new PolylineOptions()
                .addAll(polylinePoints)
                .width(9f)
                .color(ContextCompat.getColor(requireContext(), R.color.space_indigo)));
    }

    private void fitRoutePreviewBounds() {
        if (map == null || routePreviewLocations.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (routePreviewData != null && routePreviewData.path != null && !routePreviewData.path.isEmpty()) {
            for (LatLng point : routePreviewData.path) {
                builder.include(point);
            }
        } else {
            builder.include(new LatLng(PARLIAMENT_LATITUDE, PARLIAMENT_LONGITUDE));
            for (NearbySavedLocation stop : routePreviewLocations) {
                builder.include(new LatLng(stop.getLatitude(), stop.getLongitude()));
            }
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
    }

    @NonNull
    private String buildRouteStopSummary() {
        StringBuilder summary = new StringBuilder("Stops: Parliament");
        for (int index = 0; index < routePreviewLocations.size(); index++) {
            summary.append(" -> ")
                    .append(index + 1)
                    .append(". ")
                    .append(routePreviewLocations.get(index).getName());
        }
        return summary.toString();
    }

    private float calculateRoutePreviewDistanceMeters() {
        if (routePreviewLocations.isEmpty()) {
            return 0f;
        }

        float totalDistance = 0f;
        double currentLat = PARLIAMENT_LATITUDE;
        double currentLng = PARLIAMENT_LONGITUDE;
        for (NearbySavedLocation stop : routePreviewLocations) {
            float[] result = new float[1];
            android.location.Location.distanceBetween(
                    currentLat,
                    currentLng,
                    stop.getLatitude(),
                    stop.getLongitude(),
                    result
            );
            totalDistance += result[0];
            currentLat = stop.getLatitude();
            currentLng = stop.getLongitude();
        }
        return totalDistance;
    }

    private int estimateWalkingMinutes(float meters) {
        if (meters <= 0f) {
            return 0;
        }
        double metersPerMinute = 5000d / 60d;
        return Math.max(1, (int) Math.ceil(meters / metersPerMinute));
    }

    private void loadRoutePreviewIfNeeded() {
        if (routePreviewData != null || routePreviewFetchInProgress || routePreviewLocations.isEmpty()) {
            return;
        }

        String apiKey = BuildConfig.ROUTES_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            routePreviewErrorMessage = "Routes API key unavailable. Showing direct preview instead.";
            return;
        }

        routePreviewFetchInProgress = true;
        routeExecutor.execute(() -> {
            try {
                RoutesApiService.RoutePreviewData fetched = RoutesApiService.fetchWalkingRoute(
                        apiKey,
                        new LatLng(PARLIAMENT_LATITUDE, PARLIAMENT_LONGITUDE),
                        new ArrayList<>(routePreviewLocations)
                );
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    routePreviewFetchInProgress = false;
                    routePreviewErrorMessage = null;
                    routePreviewData = fetched;
                    if (map != null) {
                        showRoutePreview();
                    }
                });
            } catch (Exception error) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    routePreviewFetchInProgress = false;
                    routePreviewErrorMessage = "Unable to load road-following directions yet. Showing direct preview.";
                    Toast.makeText(requireContext(), routePreviewErrorMessage, Toast.LENGTH_SHORT).show();
                    if (map != null) {
                        showRoutePreview();
                    }
                });
            }
        });
    }

    @NonNull
    private String buildRouteOverview() {
        if (routePreviewFetchInProgress) {
            return "Loading walking directions...";
        }

        if (routePreviewData != null && routePreviewData.steps != null && !routePreviewData.steps.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            int stepCount = Math.min(routePreviewData.steps.size(), 5);
            for (int index = 0; index < stepCount; index++) {
                RoutesApiService.RouteStep step = routePreviewData.steps.get(index);
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(index + 1)
                        .append(". ")
                        .append(step.instructions);
                if (step.distanceMeters > 0) {
                    builder.append(" (")
                            .append(formatDistance(step.distanceMeters))
                            .append(")");
                }
            }
            if (routePreviewData.steps.size() > stepCount) {
                builder.append("\n...");
            }
            return builder.toString();
        }

        if (routePreviewErrorMessage != null && !routePreviewErrorMessage.trim().isEmpty()) {
            return routePreviewErrorMessage;
        }

        return "Walking preview from Parliament through the selected nearby locations.";
    }

    private void openDirectionsSheet() {
        if (routePreviewData == null || routePreviewData.steps == null || routePreviewData.steps.isEmpty()) {
            Toast.makeText(requireContext(), "Directions are still loading.", Toast.LENGTH_SHORT).show();
            return;
        }

        float totalDistanceMeters = routePreviewData.distanceMeters;
        int estimatedMinutes = Math.max(1, (int) Math.ceil(routePreviewData.durationSeconds / 60d));
        String summary = routePreviewLocations.size() + " stop"
                + (routePreviewLocations.size() == 1 ? "" : "s")
                + " | "
                + formatDistance(totalDistanceMeters)
                + " | "
                + estimatedMinutes
                + " min walk";

        ArrayList<RouteDirectionsBottomSheet.RouteStepItem> steps = new ArrayList<>();
        for (int index = 0; index < routePreviewData.steps.size(); index++) {
            RoutesApiService.RouteStep step = routePreviewData.steps.get(index);
            steps.add(new RouteDirectionsBottomSheet.RouteStepItem(
                    index + 1,
                    step.instructions,
                    formatDistance(step.distanceMeters),
                    formatDuration(step.durationSeconds)
            ));
        }

        RouteDirectionsBottomSheet sheet = RouteDirectionsBottomSheet.newInstance(
                steps,
                "Walking directions",
                summary
        );
        sheet.show(getChildFragmentManager(), "route_directions_sheet");
    }

    @NonNull
    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(Locale.US, "%.1f km", meters / 1000f);
        }
        return String.format(Locale.US, "%.0f m", meters);
    }

    @NonNull
    private String formatDuration(long seconds) {
        if (seconds <= 0L) {
            return "0 min";
        }
        long minutes = Math.max(1L, Math.round(seconds / 60.0));
        return minutes + " min";
    }

    private void showPlaceDetails(@NonNull MarkerDetails details) {
        if (detailsContainer == null) return;
        placeName.setText(details.title);
        placeAddress.setText(details.subtitle);
        placeRating.setText(String.format(Locale.US, "%.1f", details.rating));
        if (overviewInput != null) {
            overviewInput.setText(details.overview != null ? details.overview : "");
        }
        if (placeMeta != null) {
            if (details.meta != null && !details.meta.trim().isEmpty()) {
                placeMeta.setText(details.meta);
                placeMeta.setVisibility(View.VISIBLE);
            } else {
                placeMeta.setText("");
                placeMeta.setVisibility(View.GONE);
            }
        }

        if (placeContact != null) {
            if (details.contact != null && !details.contact.trim().isEmpty()) {
                placeContact.setText(details.contact);
                placeContact.setVisibility(View.VISIBLE);
            } else {
                placeContact.setText("");
                placeContact.setVisibility(View.GONE);
            }
        }

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
    private MarkerDetails buildSavedLocationDetails(@NonNull LocationWithDetails item) {
        LocationEntity location = item.location;
        String title = location.name != null ? location.name : "Saved location";
        String subtitle = location.address != null && !location.address.trim().isEmpty()
                ? location.address
                : formatLatLng(location.latitude, location.longitude);

        StringBuilder metaBuilder = new StringBuilder();
        if (location.category != null && !location.category.trim().isEmpty()) {
            metaBuilder.append(location.category.trim());
        }
        if (location.isVerified) {
            if (metaBuilder.length() > 0) {
                metaBuilder.append(" | ");
            }
            metaBuilder.append("Verified");
        }

        return new MarkerDetails(
                location.locationId,
                title,
                subtitle,
                metaBuilder.toString(),
                "",
                location.rating,
                location.description != null ? location.description : "",
                location.category != null ? location.category : "",
                new ArrayList<>(),
                item.reviews != null ? item.reviews : new ArrayList<>(),
                item.posts != null ? item.posts : new ArrayList<>()
        );
    }

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
