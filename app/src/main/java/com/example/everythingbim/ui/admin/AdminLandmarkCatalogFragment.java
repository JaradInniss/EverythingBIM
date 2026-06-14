package com.example.everythingbim.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.LocationSeedProvider;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.repository.CanonicalLocationRepository;
import com.example.everythingbim.ui.home.Landmark;
import com.example.everythingbim.ui.home.LandmarkRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminLandmarkCatalogFragment extends Fragment {
    private static final String COLLECTION_LANDMARK_CATALOG = "landmark_catalog";

    private final List<LandmarkCatalogItem> allItems = new ArrayList<>();

    private FirebaseFirestore firestore;
    private CanonicalLocationRepository canonicalLocationRepository;
    private ListenerRegistration catalogListener;

    private EditText searchEt;
    private Switch showInactiveSwitch;
    private Button addLandmarkBtn;
    private Button importDefaultsBtn;
    private LinearLayout catalogContainer;
    private TextView summaryTv;
    private TextView totalsTv;
    private TextView emptyTv;
    private View backBtn;

    private String searchQuery = "";
    private boolean showInactive = false;
    private boolean attemptedEmptyStateBootstrap = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_landmark_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        canonicalLocationRepository = new CanonicalLocationRepository(requireContext());

        searchEt = view.findViewById(R.id.admin_landmark_search_et);
        showInactiveSwitch = view.findViewById(R.id.admin_landmark_show_inactive_switch);
        addLandmarkBtn = view.findViewById(R.id.admin_landmark_add_btn);
        importDefaultsBtn = view.findViewById(R.id.admin_landmark_import_defaults_btn);
        catalogContainer = view.findViewById(R.id.admin_landmark_catalog_container);
        summaryTv = view.findViewById(R.id.admin_landmark_summary_tv);
        totalsTv = view.findViewById(R.id.admin_landmark_totals_tv);
        emptyTv = view.findViewById(R.id.admin_landmark_empty_tv);
        backBtn = view.findViewById(R.id.admin_landmark_back_btn);

        bindSearch();
        bindActions();
    }

    @Override
    public void onStart() {
        super.onStart();
        startCatalogListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (catalogListener != null) {
            catalogListener.remove();
            catalogListener = null;
        }
    }

    private void bindSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim();
                renderCatalog();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void bindActions() {
        showInactiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showInactive = isChecked;
            renderCatalog();
        });
        addLandmarkBtn.setOnClickListener(v -> openEditor(null));
        importDefaultsBtn.setOnClickListener(v -> importStarterLandmarks(false));
        backBtn.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void startCatalogListener() {
        if (catalogListener != null) {
            catalogListener.remove();
        }
        catalogListener = firestore.collection(COLLECTION_LANDMARK_CATALOG)
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException error) {
                        if (!isAdded()) {
                            return;
                        }
                        if (error != null) {
                            Toast.makeText(requireContext(),
                                    "Failed to load landmark catalog.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        allItems.clear();
                        if (value != null) {
                            for (DocumentSnapshot document : value.getDocuments()) {
                                LandmarkCatalogItem item = LandmarkCatalogItem.fromDocument(document);
                                if (item != null) {
                                    allItems.add(item);
                                }
                            }
                        }
                        if (!attemptedEmptyStateBootstrap) {
                            attemptedEmptyStateBootstrap = true;
                            importStarterLandmarks(true);
                        }
                        renderCatalog();
                    }
                });
    }

    private void renderCatalog() {
        if (!isAdded()) {
            return;
        }

        catalogContainer.removeAllViews();

        List<LandmarkCatalogItem> filteredItems = new ArrayList<>();
        int activeCount = 0;
        int inactiveCount = 0;
        String normalizedQuery = searchQuery.toLowerCase(Locale.US);

        for (LandmarkCatalogItem item : allItems) {
            if (item.isActive) {
                activeCount++;
            } else {
                inactiveCount++;
            }
            if (!showInactive && !item.isActive) {
                continue;
            }
            if (!normalizedQuery.isEmpty() && !item.matchesQuery(normalizedQuery)) {
                continue;
            }
            filteredItems.add(item);
        }

        summaryTv.setText(String.format(Locale.US, "%d landmarks", allItems.size()));
        totalsTv.setText(String.format(
                Locale.US,
                "%d active • %d inactive",
                activeCount,
                inactiveCount
        ));
        emptyTv.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        importDefaultsBtn.setVisibility(View.INVISIBLE);
        importDefaultsBtn.setText(allItems.isEmpty()
                ? "Import starter landmarks"
                : "Import missing starter landmarks");

        for (LandmarkCatalogItem item : filteredItems) {
            item.isLast = false;
        }
        if (!filteredItems.isEmpty()) {
            filteredItems.get(filteredItems.size() - 1).isLast = true;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (LandmarkCatalogItem item : filteredItems) {
            View row = inflater.inflate(R.layout.item_admin_landmark, catalogContainer, false);
            bindRow(row, item);
            catalogContainer.addView(row);
        }
    }

    private void bindRow(@NonNull View row, @NonNull LandmarkCatalogItem item) {
        View dotView = row.findViewById(R.id.admin_landmark_dot_view);
        TextView nameTv = row.findViewById(R.id.admin_landmark_name_tv);
        TextView chipTv = row.findViewById(R.id.admin_landmark_chip_tv);
        TextView metaTv = row.findViewById(R.id.admin_landmark_meta_tv);
        TextView editBtn = row.findViewById(R.id.admin_landmark_edit_btn);
        View divider = row.findViewById(R.id.admin_landmark_divider);

        nameTv.setText(item.name);
        metaTv.setText(buildMetaLine(item));
        dotView.setAlpha(item.isActive ? 1f : 0.45f);
        chipTv.setText(item.isCnnLandmark() ? "CNN" : "Metadata");
        chipTv.setBackgroundResource(item.isCnnLandmark()
                ? R.drawable.bg_border_rectangle_space_indigo
                : R.drawable.bg_border_rectangle_gold);
        chipTv.setTextColor(row.getResources().getColor(
                item.isCnnLandmark() ? R.color.white : R.color.black, null));
        divider.setVisibility(item.isLast ? View.GONE : View.VISIBLE);

        editBtn.setOnClickListener(v -> openEditor(item));
        row.setOnClickListener(v -> openEditor(item));
    }

    @NonNull
    private String buildMetaLine(@NonNull LandmarkCatalogItem item) {
        List<String> parts = new ArrayList<>();
        if (!item.category.isEmpty()) {
            parts.add(item.category.toLowerCase(Locale.US));
        }
        if (!item.classifierKey.isEmpty()) {
            parts.add(item.classifierKey.toLowerCase(Locale.US));
        } else if (!item.legacyLandmarkId.isEmpty()) {
            parts.add(item.legacyLandmarkId.toLowerCase(Locale.US));
        } else if (!item.isActive) {
            parts.add("inactive");
        }
        return String.join(" • ", parts);
    }

    private void openEditor(@Nullable LandmarkCatalogItem existingItem) {
        Fragment parent = getParentFragment();
        if (parent instanceof AdminFragment) {
            ((AdminFragment) parent).navigateToLandmarkEditor(
                    existingItem != null ? existingItem.documentId : null);
            return;
        }
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container,
                        AdminLandmarkEditorFragment.newInstance(
                                existingItem != null ? existingItem.documentId : null))
                .addToBackStack(null)
                .commit();
    }

    @NonNull
    private String resolveAddedBy() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "admin";
    }

    private void importStarterLandmarks(boolean silent) {
        LandmarkRepository repository = new LandmarkRepository();
        List<Landmark> aiDefaults = repository.getAllLandmarks();
        List<LocationEntity> seedLocations = LocationSeedProvider.createSeedLocations();
        if (aiDefaults.isEmpty() && seedLocations.isEmpty()) {
            if (!silent) {
                Toast.makeText(requireContext(),
                        "No starter landmarks are available to import.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        WriteBatch batch = firestore.batch();
        int[] importCount = {0};
        Set<String> seenNames = new HashSet<>();

        for (LandmarkCatalogItem item : allItems) {
            seenNames.add(item.name.trim().toLowerCase(Locale.US));
        }

        for (LocationEntity location : seedLocations) {
            String normalizedName = location.name.trim().toLowerCase(Locale.US);
            if (!seenNames.add(normalizedName)) {
                continue;
            }

            DocumentReference doc = firestore.collection(COLLECTION_LANDMARK_CATALOG).document();
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", location.name);
            payload.put("displayName", location.name);
            payload.put("category", location.category);
            payload.put("latitude", location.latitude);
            payload.put("longitude", location.longitude);
            payload.put("address", location.address);
            payload.put("mapSubtitle", location.address);
            payload.put("description", location.description);
            payload.put("imageUrl", location.imageUrl);
            payload.put("placeId", "");
            payload.put("sourceType", location.sourceType);
            payload.put("isActive", location.isActive);
            payload.put("isVerified", location.isVerified);
            payload.put("rating", (double) location.rating);
            payload.put("addedBy", location.addedBy);
            payload.put("createdAt", FieldValue.serverTimestamp());
            payload.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(doc, payload);
            importCount[0]++;
        }

        for (Landmark landmark : aiDefaults) {
            String normalizedName = landmark.getDisplayName().trim().toLowerCase(Locale.US);
            if (!seenNames.add(normalizedName)) {
                continue;
            }

            DocumentReference doc = firestore.collection(COLLECTION_LANDMARK_CATALOG).document();
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", landmark.getDisplayName());
            payload.put("displayName", landmark.getDisplayName());
            payload.put("category", inferCategory(landmark));
            payload.put("latitude", landmark.getLatitude());
            payload.put("longitude", landmark.getLongitude());
            payload.put("address", landmark.getMapSubtitle());
            payload.put("mapSubtitle", landmark.getMapSubtitle());
            payload.put("description", landmark.getDescription());
            payload.put("imageUrl", "");
            payload.put("placeId", "");
            payload.put("sourceType", "cnn_seed");
            payload.put("isActive", true);
            payload.put("isVerified", true);
            payload.put("rating", 0d);
            payload.put("legacyLandmarkId", landmark.getId());
            payload.put("classifierKey", landmark.getToken());
            payload.put("nearbyRadiusMeters", landmark.getNearbyRadiusMeters());
            payload.put("addedBy", resolveAddedBy());
            payload.put("createdAt", FieldValue.serverTimestamp());
            payload.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(doc, payload);
            importCount[0]++;
        }

        if (importCount[0] == 0) {
            if (!silent) {
                Toast.makeText(requireContext(),
                        "Starter landmarks are already imported.",
                        Toast.LENGTH_SHORT).show();
            }
            return;
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    canonicalLocationRepository.syncCanonicalLocations();
                    if (!silent) {
                        Toast.makeText(requireContext(),
                                "Imported " + importCount[0] + " starter landmarks.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(error -> {
                    if (!silent) {
                        Toast.makeText(requireContext(),
                                "Failed to import starter landmarks.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @NonNull
    private String inferCategory(@NonNull Landmark landmark) {
        String id = landmark.getId().toLowerCase(Locale.US);
        if (id.contains("parliament")) {
            return "Government Building";
        }
        if (id.contains("kensington")) {
            return "Sports Venue";
        }
        if (id.contains("cathedral")) {
            return "Religious Site";
        }
        return "Landmark";
    }

    private static class LandmarkCatalogItem {
        String documentId;
        String name;
        String category;
        String address;
        String description;
        String imageUrl;
        String placeId;
        String sourceType;
        String classifierKey;
        String legacyLandmarkId;
        String addedBy;
        double latitude;
        double longitude;
        double rating;
        boolean isActive;
        boolean isVerified;
        boolean isLast;

        @Nullable
        static LandmarkCatalogItem fromDocument(@NonNull DocumentSnapshot document) {
            String name = defaultString(document.getString("name"));
            if (name.isEmpty()) {
                name = defaultString(document.getString("displayName"));
            }
            Double latitude = readDouble(document.get("latitude"));
            Double longitude = readDouble(document.get("longitude"));
            if (name.isEmpty() || latitude == null || longitude == null) {
                return null;
            }

            LandmarkCatalogItem item = new LandmarkCatalogItem();
            item.documentId = document.getId();
            item.name = name;
            item.category = defaultString(document.getString("category"));
            item.address = defaultString(document.getString("address"));
            if (item.address.isEmpty()) {
                item.address = defaultString(document.getString("mapSubtitle"));
            }
            item.description = defaultString(document.getString("description"));
            item.imageUrl = defaultString(document.getString("imageUrl"));
            item.placeId = defaultString(document.getString("placeId"));
            item.sourceType = defaultString(document.getString("sourceType"));
            item.classifierKey = defaultString(document.getString("classifierKey"));
            item.legacyLandmarkId = defaultString(document.getString("legacyLandmarkId"));
            if (item.sourceType.isEmpty() && !item.classifierKey.isEmpty()) {
                item.sourceType = "cnn_seed";
            }
            item.addedBy = defaultString(document.getString("addedBy"));
            item.latitude = latitude;
            item.longitude = longitude;
            Double ratingValue = readDouble(document.get("rating"));
            item.rating = ratingValue != null ? ratingValue : 0d;
            Boolean active = document.getBoolean("isActive");
            Boolean verified = document.getBoolean("isVerified");
            item.isActive = active == null || active;
            item.isVerified = verified == null || verified;
            return item;
        }

        boolean matchesQuery(@NonNull String query) {
            return name.toLowerCase(Locale.US).contains(query)
                    || category.toLowerCase(Locale.US).contains(query)
                    || address.toLowerCase(Locale.US).contains(query)
                    || description.toLowerCase(Locale.US).contains(query)
                    || classifierKey.toLowerCase(Locale.US).contains(query)
                    || legacyLandmarkId.toLowerCase(Locale.US).contains(query)
                    || placeId.toLowerCase(Locale.US).contains(query);
        }

        boolean isCnnLandmark() {
            return !classifierKey.isEmpty()
                    || sourceType.toLowerCase(Locale.US).contains("cnn");
        }

        @Nullable
        private static Double readDouble(@Nullable Object value) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return null;
        }

        @NonNull
        private static String defaultString(@Nullable String value) {
            return value == null ? "" : value;
        }
    }
}
