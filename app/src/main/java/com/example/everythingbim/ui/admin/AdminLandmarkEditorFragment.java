package com.example.everythingbim.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.everythingbim.R;
import com.example.everythingbim.data.repository.CanonicalLocationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminLandmarkEditorFragment extends Fragment {
    private static final String ARG_DOCUMENT_ID = "document_id";
    private static final String COLLECTION_LANDMARK_CATALOG = "landmark_catalog";

    private FirebaseFirestore firestore;
    private CanonicalLocationRepository canonicalLocationRepository;

    private String documentId;

    private TextView modeChipTv;
    private TextView titleTv;
    private EditText landmarkIdEt;
    private EditText classifierKeyEt;
    private EditText displayNameEt;
    private EditText categoryEt;
    private EditText mapSubtitleEt;
    private EditText descriptionEt;
    private EditText imageReferenceEt;
    private EditText latitudeEt;
    private EditText longitudeEt;
    private EditText nearbyRadiusEt;
    private EditText gpsRadiusEt;
    private EditText aliasesEt;
    private Switch cnnSupportedSwitch;
    private Switch activeSwitch;
    private Switch verifiedSwitch;
    private Button saveBtn;

    public static AdminLandmarkEditorFragment newInstance(@Nullable String documentId) {
        AdminLandmarkEditorFragment fragment = new AdminLandmarkEditorFragment();
        Bundle args = new Bundle();
        if (documentId != null) {
            args.putString(ARG_DOCUMENT_ID, documentId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_landmark_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        canonicalLocationRepository = new CanonicalLocationRepository(requireContext());
        documentId = getArguments() != null ? getArguments().getString(ARG_DOCUMENT_ID) : null;

        bindViews(view);
        bindActions(view);
        bindMode();
        if (documentId != null && !documentId.trim().isEmpty()) {
            loadExistingLandmark();
        } else {
            seedDefaultsForNewLandmark();
        }
    }

    private void bindViews(@NonNull View view) {
        modeChipTv = view.findViewById(R.id.admin_landmark_editor_mode_chip_tv);
        titleTv = view.findViewById(R.id.admin_landmark_editor_title_tv);
        landmarkIdEt = view.findViewById(R.id.admin_landmark_editor_landmark_id_input);
        classifierKeyEt = view.findViewById(R.id.admin_landmark_editor_classifier_key_input);
        displayNameEt = view.findViewById(R.id.admin_landmark_editor_display_name_input);
        categoryEt = view.findViewById(R.id.admin_landmark_editor_category_input);
        mapSubtitleEt = view.findViewById(R.id.admin_landmark_editor_map_subtitle_input);
        descriptionEt = view.findViewById(R.id.admin_landmark_editor_description_input);
        imageReferenceEt = view.findViewById(R.id.admin_landmark_editor_image_reference_input);
        latitudeEt = view.findViewById(R.id.admin_landmark_editor_latitude_input);
        longitudeEt = view.findViewById(R.id.admin_landmark_editor_longitude_input);
        nearbyRadiusEt = view.findViewById(R.id.admin_landmark_editor_nearby_radius_input);
        gpsRadiusEt = view.findViewById(R.id.admin_landmark_editor_gps_radius_input);
        aliasesEt = view.findViewById(R.id.admin_landmark_editor_aliases_input);
        cnnSupportedSwitch = view.findViewById(R.id.admin_landmark_editor_cnn_supported_switch);
        activeSwitch = view.findViewById(R.id.admin_landmark_editor_active_switch);
        verifiedSwitch = view.findViewById(R.id.admin_landmark_editor_verified_switch);
        saveBtn = view.findViewById(R.id.admin_landmark_editor_save_btn);
    }

    private void bindActions(@NonNull View view) {
        ImageButton backBtn = view.findViewById(R.id.admin_landmark_editor_back_btn);
        backBtn.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        saveBtn.setOnClickListener(v -> saveLandmark());
    }

    private void bindMode() {
        boolean isEditing = documentId != null && !documentId.trim().isEmpty();
        modeChipTv.setText(isEditing ? "Editing" : "Adding");
        titleTv.setText(isEditing ? "Edit Landmark Metadata" : "Add Landmark Metadata");
    }

    private void seedDefaultsForNewLandmark() {
        activeSwitch.setChecked(true);
        verifiedSwitch.setChecked(true);
    }

    private void loadExistingLandmark() {
        firestore.collection(COLLECTION_LANDMARK_CATALOG)
                .document(documentId)
                .get()
                .addOnSuccessListener(this::populateForm)
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            "Failed to load landmark details.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateForm(@NonNull DocumentSnapshot document) {
        if (!isAdded() || !document.exists()) {
            return;
        }

        String displayName = defaultString(document.getString("name"));
        if (displayName.isEmpty()) {
            displayName = defaultString(document.getString("displayName"));
        }

        String mapSubtitle = defaultString(document.getString("address"));
        if (mapSubtitle.isEmpty()) {
            mapSubtitle = defaultString(document.getString("mapSubtitle"));
        }

        landmarkIdEt.setText(firstNonEmpty(
                document.getString("legacyLandmarkId"),
                document.getId()));
        classifierKeyEt.setText(defaultString(document.getString("classifierKey")));
        displayNameEt.setText(displayName);
        categoryEt.setText(defaultString(document.getString("category")));
        mapSubtitleEt.setText(mapSubtitle);
        descriptionEt.setText(defaultString(document.getString("description")));
        imageReferenceEt.setText(defaultString(document.getString("imageUrl")));
        latitudeEt.setText(formatNullableDouble(document.get("latitude")));
        longitudeEt.setText(formatNullableDouble(document.get("longitude")));
        nearbyRadiusEt.setText(formatNullableInt(document.get("nearbyRadiusMeters")));
        gpsRadiusEt.setText(formatNullableInt(document.get("gpsPlausibilityRadiusMeters")));
        aliasesEt.setText(joinAliases(document.get("aliases")));

        boolean hasClassifier = !defaultString(document.getString("classifierKey")).isEmpty();
        String sourceType = defaultString(document.getString("sourceType")).toLowerCase(Locale.US);
        cnnSupportedSwitch.setChecked(hasClassifier || sourceType.contains("cnn"));
        Boolean active = document.getBoolean("isActive");
        Boolean verified = document.getBoolean("isVerified");
        activeSwitch.setChecked(active == null || active);
        verifiedSwitch.setChecked(verified == null || verified);
    }

    private void saveLandmark() {
        if (!isAdded()) {
            return;
        }

        String displayName = clean(displayNameEt);
        String latitudeText = clean(latitudeEt);
        String longitudeText = clean(longitudeEt);

        if (displayName.isEmpty()) {
            displayNameEt.setError("Display name is required");
            return;
        }
        if (latitudeText.isEmpty()) {
            latitudeEt.setError("Latitude is required");
            return;
        }
        if (longitudeText.isEmpty()) {
            longitudeEt.setError("Longitude is required");
            return;
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(latitudeText);
            longitude = Double.parseDouble(longitudeText);
        } catch (NumberFormatException error) {
            Toast.makeText(requireContext(),
                    "Latitude and longitude must be valid numbers.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Integer nearbyRadius = parseInteger(nearbyRadiusEt);
        Integer gpsRadius = parseInteger(gpsRadiusEt);
        if (nearbyRadiusEt.length() > 0 && nearbyRadius == null) {
            nearbyRadiusEt.setError("Enter a valid whole number");
            return;
        }
        if (gpsRadiusEt.length() > 0 && gpsRadius == null) {
            gpsRadiusEt.setError("Enter a valid whole number");
            return;
        }

        saveBtn.setEnabled(false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", displayName);
        payload.put("displayName", displayName);
        payload.put("category", clean(categoryEt));
        payload.put("address", clean(mapSubtitleEt));
        payload.put("mapSubtitle", clean(mapSubtitleEt));
        payload.put("description", clean(descriptionEt));
        payload.put("imageUrl", clean(imageReferenceEt));
        payload.put("latitude", latitude);
        payload.put("longitude", longitude);
        payload.put("sourceType", resolveSourceType());
        payload.put("isActive", activeSwitch.isChecked());
        payload.put("isVerified", verifiedSwitch.isChecked());
        payload.put("updatedAt", FieldValue.serverTimestamp());
        payload.put("addedBy", resolveAddedBy());
        payload.put("rating", 0d);

        putStringOrDelete(payload, "legacyLandmarkId", clean(landmarkIdEt));
        putStringOrDelete(payload, "classifierKey", clean(classifierKeyEt));
        putStringOrDelete(payload, "category", clean(categoryEt));
        putStringOrDelete(payload, "description", clean(descriptionEt));
        putStringOrDelete(payload, "imageUrl", clean(imageReferenceEt));
        putStringOrDelete(payload, "address", clean(mapSubtitleEt));
        putStringOrDelete(payload, "mapSubtitle", clean(mapSubtitleEt));

        if (nearbyRadius != null) {
            payload.put("nearbyRadiusMeters", nearbyRadius);
        } else {
            payload.put("nearbyRadiusMeters", FieldValue.delete());
        }
        if (gpsRadius != null) {
            payload.put("gpsPlausibilityRadiusMeters", gpsRadius);
        } else {
            payload.put("gpsPlausibilityRadiusMeters", FieldValue.delete());
        }

        List<String> aliases = parseAliases(clean(aliasesEt));
        if (aliases.isEmpty()) {
            payload.put("aliases", FieldValue.delete());
        } else {
            payload.put("aliases", aliases);
        }

        if (documentId == null || documentId.trim().isEmpty()) {
            payload.put("createdAt", FieldValue.serverTimestamp());
        }

        DocumentReference reference = documentId == null || documentId.trim().isEmpty()
                ? firestore.collection(COLLECTION_LANDMARK_CATALOG).document()
                : firestore.collection(COLLECTION_LANDMARK_CATALOG).document(documentId);

        reference.set(payload, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    canonicalLocationRepository.syncCanonicalLocations();
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            documentId == null ? "Landmark added." : "Landmark updated.",
                            Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(error -> {
                    saveBtn.setEnabled(true);
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            "Failed to save landmark.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @NonNull
    private String resolveSourceType() {
        return cnnSupportedSwitch.isChecked() ? "cnn_seed" : "canonical";
    }

    @NonNull
    private String resolveAddedBy() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return "admin";
    }

    @Nullable
    private Integer parseInteger(@NonNull EditText editText) {
        String value = clean(editText);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private void putStringOrDelete(@NonNull Map<String, Object> payload,
                                   @NonNull String key,
                                   @NonNull String value) {
        if (value.isEmpty()) {
            payload.put(key, FieldValue.delete());
        } else {
            payload.put(key, value);
        }
    }

    @NonNull
    private List<String> parseAliases(@NonNull String aliasesText) {
        List<String> aliases = new ArrayList<>();
        if (aliasesText.isEmpty()) {
            return aliases;
        }
        String[] rawAliases = aliasesText.split(",");
        for (String rawAlias : rawAliases) {
            String alias = rawAlias.trim();
            if (!alias.isEmpty()) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    @NonNull
    private String joinAliases(@Nullable Object aliasesObject) {
        if (!(aliasesObject instanceof List<?>)) {
            return "";
        }
        List<?> rawAliases = (List<?>) aliasesObject;
        List<String> aliases = new ArrayList<>();
        for (Object rawAlias : rawAliases) {
            if (rawAlias != null) {
                String alias = rawAlias.toString().trim();
                if (!alias.isEmpty()) {
                    aliases.add(alias);
                }
            }
        }
        return TextUtils.join(", ", aliases);
    }

    @NonNull
    private String formatNullableDouble(@Nullable Object value) {
        if (value instanceof Number) {
            return String.valueOf(((Number) value).doubleValue());
        }
        return "";
    }

    @NonNull
    private String formatNullableInt(@Nullable Object value) {
        if (value instanceof Number) {
            return String.valueOf(((Number) value).intValue());
        }
        return "";
    }

    @NonNull
    private String firstNonEmpty(@Nullable String first, @Nullable String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second != null ? second : "";
    }

    @NonNull
    private String clean(@NonNull EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    @NonNull
    private String defaultString(@Nullable String value) {
        return value == null ? "" : value;
    }
}
