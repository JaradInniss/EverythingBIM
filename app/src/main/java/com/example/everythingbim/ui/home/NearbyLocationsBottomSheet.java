package com.example.everythingbim.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NearbyLocationsBottomSheet extends BottomSheetDialogFragment implements
        NearbyRouteSelectionAdapter.OnSelectionChangedListener,
        NearbyRouteSelectionAdapter.OnLocationDetailsClickListener {
    private static final int PAGE_SIZE = 5;
    private static final String ARG_LOCATIONS = "locations";
    private static final String ARG_SELECTED_IDS = "selected_ids";
    private static final String ARG_ANCHOR_NAME = "anchor_name";
    private static final String ARG_ANCHOR_LATITUDE = "anchor_latitude";
    private static final String ARG_ANCHOR_LONGITUDE = "anchor_longitude";

    private final LinkedHashSet<Long> selectedIds = new LinkedHashSet<>();
    private final LinkedHashSet<NearbySavedLocation> selectedLocations = new LinkedHashSet<>();
    private final ArrayList<NearbySavedLocation> allLocations = new ArrayList<>();
    private NearbyActionsListener actionsListener;
    private NearbyRouteSelectionAdapter adapter;
    private TextView titleView;
    private TextView seeMoreButton;
    private TextView routeButton;
    private TextView externalRouteButton;
    private TextView selectedCountView;
    private int visibleLocationCount = PAGE_SIZE;
    private String anchorName = "anchor";
    private double anchorLatitude;
    private double anchorLongitude;

    public static NearbyLocationsBottomSheet newInstance(@NonNull ArrayList<NearbySavedLocation> locations,
                                                         @NonNull ArrayList<Long> selectedIds,
                                                         @Nullable String anchorName,
                                                         double anchorLatitude,
                                                         double anchorLongitude) {
        NearbyLocationsBottomSheet sheet = new NearbyLocationsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LOCATIONS, locations);
        args.putSerializable(ARG_SELECTED_IDS, selectedIds);
        args.putString(ARG_ANCHOR_NAME, anchorName);
        args.putDouble(ARG_ANCHOR_LATITUDE, anchorLatitude);
        args.putDouble(ARG_ANCHOR_LONGITUDE, anchorLongitude);
        sheet.setArguments(args);
        return sheet;
    }

    public void setActionsListener(@Nullable NearbyActionsListener actionsListener) {
        this.actionsListener = actionsListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_nearby_locations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView closeButton = view.findViewById(R.id.nearby_sheet_close_button);
        titleView = view.findViewById(R.id.nearby_sheet_title);
        selectedCountView = view.findViewById(R.id.nearby_sheet_selected_count);
        seeMoreButton = view.findViewById(R.id.nearby_sheet_see_more_button);
        routeButton = view.findViewById(R.id.nearby_sheet_route_button);
        externalRouteButton = view.findViewById(R.id.nearby_sheet_external_route_button);
        RecyclerView recyclerView = view.findViewById(R.id.nearby_sheet_recycler);

        closeButton.setOnClickListener(v -> dismiss());

        ArrayList<NearbySavedLocation> locations = readLocationsFromArguments();
        readAnchorFromArguments();
        allLocations.clear();
        allLocations.addAll(locations);
        visibleLocationCount = Math.min(PAGE_SIZE, allLocations.size());
        ArrayList<Long> restoredSelectedIds = readSelectedIdsFromArguments();
        selectedIds.clear();
        selectedIds.addAll(restoredSelectedIds);
        selectedLocations.clear();
        for (NearbySavedLocation location : locations) {
            if (selectedIds.contains(location.getLocationId())) {
                selectedLocations.add(location);
            }
        }

        adapter = new NearbyRouteSelectionAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        titleView.setText(allLocations.isEmpty()
                ? "Nearby " + anchorName + " Locations"
                : "Nearby " + anchorName + " Locations (" + allLocations.size() + ")");
        seeMoreButton.setOnClickListener(v -> showMoreLocations());
        updateVisibleLocations();
        Toast.makeText(requireContext(), "Tip: tap a location image for more options.", Toast.LENGTH_SHORT).show();

        routeButton.setOnClickListener(v -> {
            if (actionsListener != null && !selectedLocations.isEmpty()) {
                actionsListener.onCreateRouteRequested(new ArrayList<>(selectedLocations));
            }
            dismiss();
        });

        externalRouteButton.setOnClickListener(v -> {
            if (actionsListener != null && !selectedLocations.isEmpty()) {
                actionsListener.onOpenRouteExternallyRequested(new ArrayList<>(selectedLocations));
            }
            dismiss();
        });

        updateSelectionSummary();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private ArrayList<NearbySavedLocation> readLocationsFromArguments() {
        Serializable value = getArguments() != null ? getArguments().getSerializable(ARG_LOCATIONS) : null;
        if (value instanceof ArrayList) {
            return (ArrayList<NearbySavedLocation>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private ArrayList<Long> readSelectedIdsFromArguments() {
        Serializable value = getArguments() != null ? getArguments().getSerializable(ARG_SELECTED_IDS) : null;
        if (value instanceof ArrayList) {
            return (ArrayList<Long>) value;
        }
        return new ArrayList<>();
    }

    private void readAnchorFromArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        String restoredAnchorName = args.getString(ARG_ANCHOR_NAME);
        if (restoredAnchorName != null && !restoredAnchorName.trim().isEmpty()) {
            anchorName = restoredAnchorName.trim();
        }
        anchorLatitude = args.getDouble(ARG_ANCHOR_LATITUDE, 0d);
        anchorLongitude = args.getDouble(ARG_ANCHOR_LONGITUDE, 0d);
    }

    @Override
    public void onLocationAdded(@NonNull NearbySavedLocation location) {
        selectedIds.add(location.getLocationId());
        selectedLocations.add(location);
        notifySelectionChanged();
    }

    @Override
    public void onLocationRemoved(@NonNull NearbySavedLocation location) {
        selectedIds.remove(location.getLocationId());
        selectedLocations.remove(location);
        notifySelectionChanged();
    }

    @Override
    public void onLocationDetailsClick(@NonNull NearbySavedLocation location) {
        CharSequence[] options = new CharSequence[]{"View more information", "View on map"};
        new AlertDialog.Builder(requireContext())
                .setTitle(location.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        NearbyLocationDetailsBottomSheet.newInstance(location)
                                .show(getParentFragmentManager(), "nearby_location_details_sheet");
                    } else if (which == 1 && actionsListener != null) {
                        actionsListener.onLocationDetailsRequested(location);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void notifySelectionChanged() {
        updateSelectionSummary();
        if (actionsListener != null) {
            actionsListener.onSelectionChanged(new ArrayList<>(selectedLocations));
        }
    }

    private void showMoreLocations() {
        if (visibleLocationCount >= allLocations.size()) {
            return;
        }
        visibleLocationCount = Math.min(allLocations.size(), visibleLocationCount + PAGE_SIZE);
        updateVisibleLocations();
    }

    private void updateVisibleLocations() {
        adapter.submitList(new ArrayList<>(allLocations.subList(0, visibleLocationCount)), selectedIds);

        boolean hasMore = visibleLocationCount < allLocations.size();
        seeMoreButton.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        if (hasMore) {
            int nextBatch = Math.min(PAGE_SIZE, allLocations.size() - visibleLocationCount);
            seeMoreButton.setText("See " + nextBatch + " more");
        }
    }

    private void updateSelectionSummary() {
        int count = selectedLocations.size();
        if (count == 0) {
            selectedCountView.setText("Choose locations to build a route.");
        } else {
            ArrayList<NearbySavedLocation> orderedSelection = new ArrayList<>(selectedLocations);
            float estimatedDistance = RouteEstimateHelper.calculateRouteDistanceMeters(
                    anchorLatitude,
                    anchorLongitude,
                    orderedSelection
            );
            int estimatedMinutes = RouteEstimateHelper.estimateWalkingMinutes(estimatedDistance);
            String summary = count + " location" + (count == 1 ? "" : "s") + " selected";
            if (estimatedMinutes > 0) {
                summary += " | Est. " + estimatedMinutes + " min walk";
            }
            selectedCountView.setText(summary);
        }
        routeButton.setEnabled(count > 0);
        routeButton.setAlpha(count > 0 ? 1f : 0.45f);
        routeButton.setText(count == 0 ? "Preview in App" : "Preview in App (" + count + ")");
        externalRouteButton.setEnabled(count > 0);
        externalRouteButton.setAlpha(count > 0 ? 1f : 0.45f);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NearbyActionsListener && actionsListener == null) {
            actionsListener = (NearbyActionsListener) context;
        }
    }

    public interface NearbyActionsListener {
        void onSelectionChanged(@NonNull List<NearbySavedLocation> selectedLocations);

        void onCreateRouteRequested(@NonNull List<NearbySavedLocation> selectedLocations);

        void onOpenRouteExternallyRequested(@NonNull List<NearbySavedLocation> selectedLocations);

        void onLocationDetailsRequested(@NonNull NearbySavedLocation location);
    }
}
