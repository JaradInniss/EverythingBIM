package com.example.everythingbim.ui.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.models.File;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddLocationToAddressAdapter
        extends RecyclerView.Adapter<AddLocationToAddressAdapter.ViewHolder> {

    private List<DocumentSnapshot> requests;
    private int itemLayoutRes;
    private Context context;
    private Activity activity;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    private static final long SEARCH_DEBOUNCE_MS = 300L;
    private static final int FILE_PICKER_IMAGE_REQUEST_CODE = 106;
    private static final int MEDIA_PERMISSION_REQUEST_CODE = 101;
    private static final LatLngBounds BARBADOS_BOUNDS = new LatLngBounds(
            new LatLng(12.9950, -59.6500),
            new LatLng(13.3350, -59.4200)
    );

    private static final String[] PLACE_TYPES = {
            "Select Place Type",
            "Restaurant", "Bar / Lounge", "Hotel / Accommodation",
            "Shop / Store", "Supermarket / Grocery", "Pharmacy",
            "Bank / ATM", "Gas Station", "Church / Place of Worship",
            "School / University", "Hospital / Clinic",
            "Government Building", "Beach / Park",
            "Sports Facility", "Entertainment Venue", "Other"
    };

    public AddLocationToAddressAdapter(List<DocumentSnapshot> requests, int itemLayoutRes,
                                       Context context, Activity activity) {
        this.requests = requests;
        this.itemLayoutRes = itemLayoutRes;
        this.context = context;
        this.activity = activity;
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public void setRequests(List<DocumentSnapshot> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DocumentSnapshot doc = requests.get(position);

        // ── Request number ───────────────────────
        if (holder.requestNo != null) {
            holder.requestNo.setText("Request #" + doc.getId().substring(0, 6).toUpperCase());
        }

        // ── Submission date ──────────────────────
        if (holder.submissionDate != null) {
            Timestamp createdAt = doc.getTimestamp("createdAt");
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                holder.submissionDate.setText("Submitted: " + sdf.format(createdAt.toDate()));
            }
        }

        // ── Status ───────────────────────────────
        if (holder.statusValue != null) {
            String status = doc.getString("status");
            if (status == null) status = "In Review";
            holder.statusValue.setText(status);
        }

        // ── Pre-fill form fields from Firestore ───
        String locationName = doc.getString("locationName");
        if (holder.locationNameEt != null && locationName != null) {
            holder.locationNameEt.setText(locationName);
        }

        String placeType = doc.getString("placeType");
        if (placeType != null && holder.placeTypeSpinner != null) {
            int pos = Arrays.asList(PLACE_TYPES).indexOf(placeType);
            if (pos >= 0) holder.placeTypeSpinner.setSelection(pos);
        }

        // ── Per-item state ────────────────────────
        holder.selectedPlaceType = (placeType != null) ? placeType : "";
        holder.selectedLat = (doc.getDouble("latitude") != null) ? doc.getDouble("latitude") : 0.0;
        holder.selectedLng = (doc.getDouble("longitude") != null) ? doc.getDouble("longitude") : 0.0;
        holder.selectedImages = new ArrayList<>();
        holder.autocompletePredictions = new ArrayList<>();
        holder.suggestionPlaceIds = new ArrayList<>();
        holder.sessionToken = AutocompleteSessionToken.newInstance();

        // ── Image adapter ─────────────────────────
        holder.imageAdapter = new FileAdapter(context, new ArrayList<>(),
                idx -> holder.selectedImages.remove(idx));
        holder.imgIconContainer.setLayoutManager(
                new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        holder.imgIconContainer.setAdapter(holder.imageAdapter);

        // ── Place type spinner ────────────────────
        if (holder.placeTypeSpinner != null) {
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, PLACE_TYPES) {
                @Override
                public boolean isEnabled(int pos) { return pos != 0; }
                @Override
                public View getDropDownView(int pos, @Nullable View convertView,
                                            @NonNull ViewGroup parent) {
                    View v = super.getDropDownView(pos, convertView, parent);
                    if (pos == 0) v.setEnabled(false);
                    return v;
                }
            };
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.placeTypeSpinner.setAdapter(spinnerAdapter);
            holder.placeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    holder.selectedPlaceType = (pos == 0) ? "" : PLACE_TYPES[pos];
                }
                @Override public void onNothingSelected(AdapterView<?> p) {
                    holder.selectedPlaceType = "";
                }
            });
            if (placeType != null) {
                int pos = Arrays.asList(PLACE_TYPES).indexOf(placeType);
                if (pos >= 0) holder.placeTypeSpinner.setSelection(pos);
            }
        }

        // ── Image upload button ───────────────────
        holder.uploadBtn.setOnClickListener(v -> {
            if (hasFilePermissions()) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                activity.startActivityForResult(
                        Intent.createChooser(intent, "Select Images"),
                        FILE_PICKER_IMAGE_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(activity,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ? new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}
                                : new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        MEDIA_PERMISSION_REQUEST_CODE);
            }
        });

        // Register this holder for image result callbacks
        holder.position = position;

        // ── Autocomplete setup ────────────────────
        initializePlacesClient(holder);

        ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(context,
                R.layout.item_suggestion, new ArrayList<>());
        holder.searchResultsList.setAdapter(suggestionsAdapter);
        holder.searchResultsList.setVisibility(View.GONE);

        holder.searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                holder.searchHandler.removeCallbacks(holder.pendingSearchRunnable);
                String query = s != null ? s.toString().trim() : "";
                if (query.isEmpty()) {
                    holder.searchResultsList.setVisibility(View.GONE);
                    return;
                }
                holder.searchHandler.postDelayed(holder.pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }
        });

        holder.searchResultsList.setOnItemClickListener((parent, v, pos, id) -> {
            if (pos < holder.autocompletePredictions.size()) {
                holder.searchEt.setText(holder.autocompletePredictions.get(pos).getFullText(null).toString());
                holder.searchResultsList.setVisibility(View.GONE);
                fetchSelectedPlace(holder, holder.autocompletePredictions.get(pos));
                hideKeyboard();
            }
        });

        holder.searchBtn.setOnClickListener(v -> {
            holder.searchHandler.removeCallbacks(holder.pendingSearchRunnable);
            performSearch(holder);
        });

        // ── Submit button ────────────────────────
        holder.submitBtn.setOnClickListener(v -> validateAndSubmit(holder, doc.getId()));
    }

    private void initializePlacesClient(ViewHolder holder) {
        String apiKey = getMapsApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e("AddLocationToAddress", "Google Maps API key not found");
            return;
        }
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(context.getApplicationContext(), apiKey);
        }
        holder.placesClient = Places.createClient(context);
        holder.sessionToken = AutocompleteSessionToken.newInstance();
    }

    @Nullable
    private String getMapsApiKey() {
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                return ai.metaData.getString("com.google.android.geo.API_KEY");
            }
        } catch (PackageManager.NameNotFoundException ignored) {}
        return null;
    }

    private void performSearch(ViewHolder holder) {
        if (holder.placesClient == null) return;
        String query = holder.searchEt.getText().toString().trim();
        if (query.isEmpty()) return;

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationRestriction(RectangularBounds.newInstance(BARBADOS_BOUNDS))
                .setSessionToken(holder.sessionToken)
                .setQuery(query)
                .build();

        holder.placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    holder.autocompletePredictions.clear();
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) holder.searchResultsList.getAdapter();
                    if (adapter != null) adapter.clear();
                    holder.suggestionPlaceIds.clear();
                    for (AutocompletePrediction p : response.getAutocompletePredictions()) {
                        holder.autocompletePredictions.add(p);
                        holder.suggestionPlaceIds.add(p.getPlaceId());
                        if (adapter != null) adapter.add(p.getFullText(null).toString());
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                    holder.searchResultsList.setVisibility(
                            holder.autocompletePredictions.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Places", "Search failed", e);
                    holder.searchResultsList.setVisibility(View.GONE);
                });
    }

    private void fetchSelectedPlace(ViewHolder holder, AutocompletePrediction prediction) {
        if (holder.placesClient == null) return;

        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS,
                Place.Field.LAT_LNG, Place.Field.TYPES);

        holder.placesClient.fetchPlace(
                        FetchPlaceRequest.builder(prediction.getPlaceId(), fields).build())
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        holder.selectedLat = latLng.latitude;
                        holder.selectedLng = latLng.longitude;

                        String rawName = place.getName();
                        String rawAddr = place.getAddress();
                        final String displayName = (rawName != null && !rawName.isEmpty()) ? rawName : (rawAddr != null ? rawAddr : "");
                        if (holder.locationNameEt != null)
                            holder.locationNameEt.setText(displayName);

                        final double finalLat = holder.selectedLat;
                        final double finalLng = holder.selectedLng;
                        final String finalName = displayName;
                        // Location selected - map preview not available in this layout
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Could not fetch location: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void validateAndSubmit(ViewHolder holder, String docId) {
        String locationName = holder.locationNameEt.getText() != null
                ? holder.locationNameEt.getText().toString().trim() : "";

        if (locationName.isEmpty()) {
            if (holder.locationNameEt != null)
                holder.locationNameEt.setError("Location name is required");
            return;
        }
        if (holder.selectedPlaceType.isEmpty()) {
            Toast.makeText(context, "Please select a place type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (holder.selectedLat == 0.0 && holder.selectedLng == 0.0) {
            Toast.makeText(context, "Please pin a location on the map", Toast.LENGTH_SHORT).show();
            return;
        }

        holder.submitBtn.setEnabled(false);
        holder.submitBtn.setText("Processing...");

        if (!holder.selectedImages.isEmpty()) {
            uploadImagesAndSave(holder, docId, locationName);
        } else {
            saveRequestToFirestore(holder, docId, locationName, new ArrayList<>());
        }
    }

    private void uploadImagesAndSave(ViewHolder holder, String docId, String locationName) {
        if (auth.getCurrentUser() == null) {
            submissionFailed(holder, "Not authenticated. Please log in and try again.");
            return;
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<com.google.android.gms.tasks.Task<Uri>> uploadTasks = new ArrayList<>();

        for (File file : holder.selectedImages) {
            StorageReference ref = storage.getReference()
                    .child("location_requests")
                    .child("images")
                    .child(UUID.randomUUID().toString());
            UploadTask uploadTask = ref.putFile(file.getUri());
            com.google.android.gms.tasks.Task<Uri> urlTask = uploadTask.continueWithTask(t -> {
                if (!t.isSuccessful()) throw t.getException();
                return ref.getDownloadUrl();
            });
            urlTask.addOnSuccessListener(uri -> uploadedUrls.add(uri.toString()));
            uploadTasks.add(urlTask);
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(uploadTasks)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveRequestToFirestore(holder, docId, locationName, uploadedUrls);
                    } else {
                        submissionFailed(holder, "Image upload failed");
                    }
                });
    }

    private void saveRequestToFirestore(ViewHolder holder, String docId,
                                         String locationName, List<String> imageUrls) {
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "anonymous";

        Map<String, Object> updates = new HashMap<>();
        updates.put("locationName", locationName);
        updates.put("placeType", holder.selectedPlaceType);
        updates.put("latitude", holder.selectedLat);
        updates.put("longitude", holder.selectedLng);
        updates.put("imageUrls", imageUrls);
        updates.put("status", "In Review");
        updates.put("updatedAt", Timestamp.now());

        db.collection("add_location_requests").document(docId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(context, "Request updated!", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        submissionFailed(holder, "Update failed: " + e.getMessage()));
    }

    private void submissionFailed(ViewHolder holder, String message) {
        holder.submitBtn.setEnabled(true);
        holder.submitBtn.setText("SUBMIT");
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private boolean hasFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void hideKeyboard() {
        View v = activity.getCurrentFocus();
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public int getItemCount() { return requests.size(); }

    // ─── ViewHolder ──────────────────────────────
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView requestNo, submissionDate, statusValue;
        EditText searchEt;
        ImageView searchBtn;
        ListView searchResultsList;
        TextInputEditText locationNameEt;
        Spinner placeTypeSpinner;
        RecyclerView imgIconContainer;
        ImageButton uploadBtn;
        Button submitBtn;

        // Per-item state
        int position;
        PlacesClient placesClient;
        AutocompleteSessionToken sessionToken;
        List<AutocompletePrediction> autocompletePredictions = new ArrayList<>();
        List<String> suggestionPlaceIds = new ArrayList<>();
        final Handler searchHandler = new Handler(Looper.getMainLooper());
        final Runnable pendingSearchRunnable = () -> performSearchInline();
        String selectedPlaceType = "";
        double selectedLat = 0.0, selectedLng = 0.0;
        List<File> selectedImages = new ArrayList<>();
        FileAdapter imageAdapter;

        @SuppressLint("WrongViewCast")
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            requestNo = itemView.findViewById(R.id.request_no);
            submissionDate = itemView.findViewById(R.id.submission_date);
            statusValue = itemView.findViewById(R.id.status_value);
            searchEt = itemView.findViewById(R.id.search_et);
            searchBtn = itemView.findViewById(R.id.search_bttn);
            searchResultsList = itemView.findViewById(R.id.search_results_list);
            locationNameEt = itemView.findViewById(R.id.location_name_et);
            placeTypeSpinner = itemView.findViewById(R.id.place_type_spinner);
            imgIconContainer = itemView.findViewById(R.id.img_icon_container);
            uploadBtn = itemView.findViewById(R.id.upload_img_bttn);
            submitBtn = itemView.findViewById(R.id.submit_bttn);
        }

        private void performSearchInline() {
            if (placesClient == null) return;
            String query = searchEt.getText().toString().trim();
            if (query.isEmpty()) return;

            FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                    .setLocationRestriction(RectangularBounds.newInstance(BARBADOS_BOUNDS))
                    .setSessionToken(sessionToken)
                    .setQuery(query)
                    .build();

            placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener(response -> {
                        autocompletePredictions.clear();
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) searchResultsList.getAdapter();
                        if (adapter != null) adapter.clear();
                        suggestionPlaceIds.clear();
                        for (AutocompletePrediction p : response.getAutocompletePredictions()) {
                            autocompletePredictions.add(p);
                            suggestionPlaceIds.add(p.getPlaceId());
                            if (adapter != null) adapter.add(p.getFullText(null).toString());
                        }
                        if (adapter != null) adapter.notifyDataSetChanged();
                        searchResultsList.setVisibility(
                                autocompletePredictions.isEmpty() ? View.GONE : View.VISIBLE);
                    })
                    .addOnFailureListener(e -> {
                        searchResultsList.setVisibility(View.GONE);
                    });
        }
    }
}