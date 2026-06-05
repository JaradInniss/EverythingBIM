package com.example.everythingbim.ui.user;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.File;
import com.example.everythingbim.ui.admin.AdminNotificationHelper;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.FileAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Tasks;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddInformationRequestFragment extends Fragment {

    // ─── Map ────────────────────────────────────
    private MapView mapView;
    private GoogleMap googleMap;

    // ─── Places SDK ─────────────────────────────
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;

    // ─── Views ──────────────────────────────────
    private EditText locationSearchEt;
    private TextInputEditText locationNameEt;
    private TextInputEditText locationDescEt;
    private ListView searchResultsList;
    private Spinner placeTypeSpinner;
    private Button submitBtn;
    private RecyclerView imgIconContainer;
    private FileAdapter imageAdapter;

    // ─── Autocomplete ────────────────────────────
    private ArrayAdapter<String> suggestionsAdapter;
    private List<String> suggestionPlaceIds = new ArrayList<>();
    private List<AutocompletePrediction> autocompletePredictions = new ArrayList<>();

    // ─── Search debounce ─────────────────────────
    private static final long SEARCH_DEBOUNCE_MS = 300L;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearchRunnable = this::performSearch;

    // ─── Form values ─────────────────────────────
    private String selectedPlaceType = "";
    private double selectedLat       = 0.0;
    private double selectedLng       = 0.0;

    // ─── Selected images ──
    private List<File> selectedImages = new ArrayList<>();

    // ─── Firebase ───────────────────────────────
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    // ─── Permission + picker constants  ───
    private static final int MEDIA_PERMISSION_REQUEST_CODE = 100;
    private static final int FILE_PICKER_IMAGE_REQUEST_CODE = 105;

    // ─── Barbados bounding box ───────────────────
    private static final LatLngBounds BARBADOS_BOUNDS = new LatLngBounds(
            new LatLng(12.9950, -59.6500),
            new LatLng(13.3350, -59.4200)
    );

    // ─── Spinner data ────────────────────────────
    private static final String[] PLACE_TYPES = {
            "Select Place Type",
            "Restaurant",
            "Bar / Lounge",
            "Hotel / Accommodation",
            "Shop / Store",
            "Supermarket / Grocery",
            "Pharmacy",
            "Bank / ATM",
            "Gas Station",
            "Church / Place of Worship",
            "School / University",
            "Hospital / Clinic",
            "Government Building",
            "Beach / Park",
            "Sports Facility",
            "Entertainment Venue",
            "Other"
    };

    // ────────────────────────────────────────────────────────
    // LIFECYCLE
    // ────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_information_request, container, false);

        if (isGuestUser()) {
            showAuthRequiredDialog();
            return view;
        }

        // Initialize Firebase
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth    = FirebaseAuth.getInstance();

        // Initialize Places SDK
        initializePlacesClient();

        // Bind views
        locationSearchEt  = view.findViewById(R.id.search_et);
        locationNameEt    = view.findViewById(R.id.location_name_et);
        locationDescEt    = view.findViewById(R.id.location_desc_et);
        searchResultsList = view.findViewById(R.id.search_results_list);
        placeTypeSpinner  = view.findViewById(R.id.place_type_spinner);
        submitBtn         = view.findViewById(R.id.submit_bttn);

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Image upload button
        view.findViewById(R.id.upload_img_bttn).setOnClickListener(v ->
                handleImageUpload());

        // Image RecyclerView + adapter
        imgIconContainer = view.findViewById(R.id.img_icon_container);
        imgIconContainer.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new FileAdapter(requireContext(), new ArrayList<>(), position -> removeImage(position));
        imgIconContainer.setAdapter(imageAdapter);

        // Tap suggestion to pin and fill name
        suggestionsAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_suggestion, new ArrayList<>());
        searchResultsList.setAdapter(suggestionsAdapter);
        searchResultsList.setOnItemClickListener((parent, v, position, id) -> {
            if (position < autocompletePredictions.size()) {
                locationSearchEt.setText(autocompletePredictions.get(position).getFullText(null).toString());
                searchResultsList.setVisibility(View.GONE);
                fetchSelectedPlace(autocompletePredictions.get(position));
                hideKeyboard();
            }
        });

        // Autocomplete dropdown as user types (with debounce)
        locationSearchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.removeCallbacks(pendingSearchRunnable);
                String query = s != null ? s.toString().trim() : "";
                if (query.isEmpty()) {
                    searchResultsList.setVisibility(View.GONE);
                    return;
                }
                searchHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        submitBtn.setOnClickListener(v -> validateAndSubmit());

        setupPlaceTypeSpinner();
        setupMap(view, savedInstanceState);

        return view;
    }

    private boolean isGuestUser() {
        SharedPreferences preferences = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userType = preferences.getString("userType", MainActivity.USER_TYPE_GUEST);
        return MainActivity.USER_TYPE_GUEST.equals(userType);
    }

    private void showAuthRequiredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log in to submit a request")
                .setMessage("Guests can explore the app, but you'll need an account before submitting an information request.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), Login.class);
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_ADD_INFORMATION_REQUEST);
                    startActivity(intent);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Create Account", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), GeneralRegistration.class);
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_ADD_INFORMATION_REQUEST);
                    startActivity(intent);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setOnCancelListener(dialog -> requireActivity().getSupportFragmentManager().popBackStack())
                .show();
    }

    // ────────────────────────────────────────────────────────
    // IMAGE SELECTION
    // ────────────────────────────────────────────────────────

    private void handleImageUpload() {
        if (hasFilePermissions()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(
                    Intent.createChooser(intent, "Select Images"),
                    FILE_PICKER_IMAGE_REQUEST_CODE);
        } else {
            requestFilePermissions();
        }
    }

    private boolean hasFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                    MEDIA_PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    MEDIA_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null) return;

        if (requestCode == FILE_PICKER_IMAGE_REQUEST_CODE) {
            processSelectedFiles(data);
        }
    }

    private void processSelectedFiles(Intent data) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                addImageToList(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            addImageToList(data.getData());
        }
    }


    private void addImageToList(Uri uri) {
        Cursor cursor = requireActivity().getContentResolver()
                .query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
            long size = cursor.getLong(
                    cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
            cursor.close();
            String mimeType = requireActivity().getContentResolver().getType(uri);
            selectedImages.add(new File(name, mimeType, uri, size));
            imageAdapter.updateList(selectedImages);
        }
        Toast.makeText(getContext(),
                selectedImages.size() + " image(s) selected",
                Toast.LENGTH_SHORT).show();
    }

    private void removeImage(int position) {
        if (position >= 0 && position < selectedImages.size()) {
            selectedImages.remove(position);
            imageAdapter.updateList(selectedImages);
        }
    }

    // ────────────────────────────────────────────────────────
    // MAP SETUP
    // ────────────────────────────────────────────────────────

    private void setupMap(View view, Bundle savedInstanceState) {
        mapView = view.findViewById(R.id.viewinforeq_map);
        if (mapView == null) {
            Log.d("AddInformationRequest", "Map view not found in layout");
            return;
        }

        // Check Google Play services
        int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (result != ConnectionResult.SUCCESS) {
            Toast.makeText(getContext(), "Google Play services not available: " + result, Toast.LENGTH_LONG).show();
            return;
        }

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(map -> {
            googleMap = map;
            if (googleMap == null) {
                Toast.makeText(getContext(), "Map failed to load (API key?)", Toast.LENGTH_SHORT).show();
                return;
            }
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(13.1939, -59.5432), 11f));
            googleMap.setOnMapClickListener(latLng -> {
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                reverseGeocode(latLng);
                // Clear search bar when user taps map
                locationSearchEt.setText("");
                clearPredictions();
                hideKeyboard();
            });
        });
    }

    // ────────────────────────────────────────────────────────
    // SPINNERS
    // ────────────────────────────────────────────────────────

    private void setupPlaceTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(), android.R.layout.simple_spinner_item, PLACE_TYPES) {

            @Override public boolean isEnabled(int position) { return position != 0; }

            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((android.widget.TextView) v).setTextColor(
                        position == 0 ? android.graphics.Color.GRAY
                                : android.graphics.Color.BLACK);
                return v;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        placeTypeSpinner.setAdapter(adapter);
        placeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedPlaceType = pos != 0 ? PLACE_TYPES[pos] : "";
            }
            @Override public void onNothingSelected(AdapterView<?> p) { selectedPlaceType = ""; }
        });
    }

    // ────────────────────────────────────────────────────────
    // PLACES SEARCH
    // ────────────────────────────────────────────────────────

    private void initializePlacesClient() {
        String apiKey = getMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e("AddInformationRequest", "Google Maps API key not found");
            return;
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(requireContext().getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(requireContext());
        sessionToken = AutocompleteSessionToken.newInstance();
    }

    @Nullable
    private String getMapsApiKey() {
        try {
            ApplicationInfo ai = requireContext().getPackageManager()
                    .getApplicationInfo(requireContext().getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                return ai.metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException ignored) {}
        return null;
    }

    private void performSearch() {
        if (placesClient == null) return;
        String query = locationSearchEt.getText().toString().trim();
        if (query.isEmpty()) {
            clearPredictions();
            return;
        }

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(RectangularBounds.newInstance(BARBADOS_BOUNDS))
                .setSessionToken(sessionToken)
                .setQuery(query)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    autocompletePredictions.clear();
                    suggestionsAdapter.clear();
                    suggestionPlaceIds.clear();
                    for (AutocompletePrediction p : response.getAutocompletePredictions()) {
                        suggestionsAdapter.add(p.getFullText(null).toString());
                        suggestionPlaceIds.add(p.getPlaceId());
                        autocompletePredictions.add(p);
                    }
                    suggestionsAdapter.notifyDataSetChanged();
                    searchResultsList.setVisibility(suggestionPlaceIds.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Places", "Autocomplete failed", e);
                    Toast.makeText(getContext(), "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    searchResultsList.setVisibility(View.GONE);
                });
    }

    private void clearPredictions() {
        autocompletePredictions.clear();
        suggestionPlaceIds.clear();
        suggestionsAdapter.clear();
        suggestionsAdapter.notifyDataSetChanged();
        searchResultsList.setVisibility(View.GONE);
    }

    private void fetchSelectedPlace(AutocompletePrediction prediction) {
        if (placesClient == null) return;

        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(prediction.getPlaceId(), fields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        selectedLat = latLng.latitude;
                        selectedLng = latLng.longitude;

                        // Set the location name: use name if available, otherwise the address
                        String locationName = place.getName();
                        if (locationName == null || locationName.isEmpty()) {
                            locationName = place.getAddress();
                        }
                        locationNameEt.setText(locationName != null ? locationName : "");

                        // Update map if available
                        if (googleMap != null) {
                            googleMap.clear();
                            googleMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(locationName));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Could not fetch location details: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ────────────────────────────────────────────────────────
    // REVERSE GEOCODE
    // ────────────────────────────────────────────────────────

    private void reverseGeocode(LatLng latLng) {
        if (!Geocoder.isPresent()) {
            Toast.makeText(getContext(),
                    "Geocoder not available - please use search", Toast.LENGTH_SHORT).show();
            return;
        }
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 5);

            if (addresses == null || addresses.isEmpty()) {
                locationNameEt.setText("Unable to determine name - please use search");
                return;
            }

            String bestName = null;
            for (Address address : addresses) {
                String candidate = pickBestName(address);
                if (candidate == null) continue;
                // Skip Plus Codes like "4FP5+PMF"
                if (candidate.contains("+")) continue;
                // Skip postal codes and strings with 4+ digits
                if (candidate.matches("[A-Z]{2}\\d.*")) continue;
                if (candidate.matches(".*\\d{4,}.*")) continue;
                // Skip short codes that look like coordinates
                if (candidate.matches(".*[+-]\\d+\\.\\d+.*")) continue;
                bestName = candidate;
                break;
            }

            if (bestName != null) {
                locationNameEt.setText(bestName);
            } else {
                // Try to get a meaningful locality or landmark name
                Address fallback = addresses.get(0);
                String locality = fallback.getLocality();
                String featureName = fallback.getFeatureName();
                String subLocality = fallback.getSubLocality();

                // Prefer meaningful names over addresses
                if (featureName != null && !featureName.matches("\\d+.*") && featureName.length() > 2) {
                    String name = featureName;
                    if (locality != null) name = name + ", " + locality;
                    locationNameEt.setText(name);
                } else if (locality != null) {
                    locationNameEt.setText(locality);
                } else if (subLocality != null) {
                    locationNameEt.setText(subLocality);
                } else if (fallback.getAddressLine(0) != null) {
                    // Last resort - first address line
                    locationNameEt.setText(fallback.getAddressLine(0));
                } else {
                    locationNameEt.setText("Location not recognized - please use search");
                }
            }
        } catch (IOException e) {
            Toast.makeText(getContext(),
                    "Could not determine location name", Toast.LENGTH_SHORT).show();
            locationNameEt.setText("Location not recognized - please use search");
        }
    }

    private String pickBestName(Address address) {
        if (address.getFeatureName() != null
                && !address.getFeatureName().matches("\\d+")
                && address.getFeatureName().length() > 3)
            return address.getFeatureName();
        if (address.getThoroughfare() != null
                && !address.getThoroughfare().matches("\\d+.*")) {
            String street = address.getThoroughfare();
            if (address.getLocality() != null)
                street = street + ", " + address.getLocality();
            return street;
        }
        if (address.getSubLocality() != null) return address.getSubLocality();
        if (address.getLocality() != null) return address.getLocality();
        return null;
    }

    // ────────────────────────────────────────────────────────
    // VALIDATION
    // ────────────────────────────────────────────────────────

    private void validateAndSubmit() {
        String locationName = locationNameEt.getText() != null
                ? locationNameEt.getText().toString().trim() : "";
        String locationDesc = locationDescEt.getText() != null
                ? locationDescEt.getText().toString().trim() : "";

        if (locationName.isEmpty()) {
            locationNameEt.setError("Location name is required");
            return;
        }
        if (locationDesc.isEmpty()) {
            locationDescEt.setError("Description is required");
            return;
        }
        if (selectedPlaceType.isEmpty()) {
            Toast.makeText(getContext(),
                    "Please select a place type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedLat == 0.0 && selectedLng == 0.0) {
            Toast.makeText(getContext(),
                    "Please pin a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText(getString(R.string.processing));

        if (!selectedImages.isEmpty()) {
            uploadAllFilesAndSaveRequest(locationName, locationDesc);
        } else {
            saveRequestToFirestore(locationName, locationDesc, new ArrayList<>());
        }
    }

    private void uploadAllFilesAndSaveRequest(String locationName, String locationDesc) {
        List<String> imageUrls = new ArrayList<>();
        List<com.google.android.gms.tasks.Task<Uri>> uploadTasks = new ArrayList<>();

        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "anon";

        for (File file : selectedImages) {
            StorageReference ref = storage.getReference()
                    .child("information_requests")
                    .child(userId)
                    .child("images")
                    .child(UUID.randomUUID().toString());

            UploadTask uploadTask = ref.putFile(file.getUri());
            com.google.android.gms.tasks.Task<Uri> urlTask =
                    uploadTask.continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    });

            urlTask.addOnSuccessListener(uri -> imageUrls.add(uri.toString()));
            uploadTasks.add(urlTask);
        }

        // Waits for ALL uploads before saving
         Tasks.whenAllSuccess(uploadTasks)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveRequestToFirestore(locationName, locationDesc, imageUrls);
                    } else {
                        submissionFailed("Image upload failed: " +
                                (task.getException() != null
                                        ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void saveRequestToFirestore(String locationName, String locationDesc,
                                        List<String> imageUrls) {
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "anonymous";

        Map<String, Object> request = new HashMap<>();
        request.put("userId",        userId);
        request.put("locationName",  locationName);
        request.put("description",   locationDesc);
        request.put("placeType",     selectedPlaceType);
        request.put("latitude",      selectedLat);
        request.put("longitude",     selectedLng);
        request.put("imageUrls",     imageUrls);
        request.put("status",        "In Review");
        request.put("createdAt",     Timestamp.now());

        db.collection("add_information_requests")
                .add(request)
                .addOnSuccessListener(doc -> {
                    AdminNotificationHelper.createNotification(
                            db,
                            AdminNotificationHelper.TYPE_INFO_REQUEST,
                            "New Information Request",
                            "A new information request for " + locationName + " was submitted.",
                            "In Review",
                            doc.getId(),
                            "add_information_requests",
                            AdminNotificationHelper.TARGET_INFO_DETAIL,
                            AdminNotificationHelper.TYPE_INFO_REQUEST,
                            locationName,
                            userId
                    );
                    Toast.makeText(getContext(),
                            "Request submitted!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        submissionFailed("Submission failed: " + e.getMessage()));
    }

    // ────────────────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────────────────

    private void submissionFailed(String message) {
        submitBtn.setEnabled(true);
        submitBtn.setText(getString(R.string.submit));
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = requireActivity().getCurrentFocus();
        if (focused != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
    }

    // ────────────────────────────────────────────────────────
    // MAP LIFECYCLE
    // ────────────────────────────────────────────────────────

    @Override public void onResume()    { super.onResume();    if (mapView != null) mapView.onResume(); }
    @Override public void onPause()     { super.onPause();     if (mapView != null) mapView.onPause(); }
    @Override public void onStart()     { super.onStart();     if (mapView != null) mapView.onStart(); }
    @Override public void onStop()      { super.onStop();      if (mapView != null) mapView.onStop(); }
    @Override public void onDestroy()   { super.onDestroy();   if (mapView != null) mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
