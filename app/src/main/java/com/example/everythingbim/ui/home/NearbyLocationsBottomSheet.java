package com.example.everythingbim.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
    private static final String ARG_LOCATIONS = "locations";
    private static final String ARG_SELECTED_IDS = "selected_ids";

    private final LinkedHashSet<Long> selectedIds = new LinkedHashSet<>();
    private final LinkedHashSet<NearbySavedLocation> selectedLocations = new LinkedHashSet<>();
    private NearbyActionsListener actionsListener;
    private NearbyRouteSelectionAdapter adapter;
    private TextView routeButton;
    private TextView externalRouteButton;
    private TextView selectedCountView;

    public static NearbyLocationsBottomSheet newInstance(@NonNull ArrayList<NearbySavedLocation> locations,
                                                         @NonNull ArrayList<Long> selectedIds) {
        NearbyLocationsBottomSheet sheet = new NearbyLocationsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LOCATIONS, locations);
        args.putSerializable(ARG_SELECTED_IDS, selectedIds);
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
        selectedCountView = view.findViewById(R.id.nearby_sheet_selected_count);
        routeButton = view.findViewById(R.id.nearby_sheet_route_button);
        externalRouteButton = view.findViewById(R.id.nearby_sheet_external_route_button);
        RecyclerView recyclerView = view.findViewById(R.id.nearby_sheet_recycler);

        closeButton.setOnClickListener(v -> dismiss());

        ArrayList<NearbySavedLocation> locations = readLocationsFromArguments();
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
        adapter.submitList(locations, selectedIds);

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
        if (actionsListener != null) {
            actionsListener.onLocationDetailsRequested(location);
        }
    }

    private void notifySelectionChanged() {
        updateSelectionSummary();
        if (actionsListener != null) {
            actionsListener.onSelectionChanged(new ArrayList<>(selectedLocations));
        }
    }

    private void updateSelectionSummary() {
        int count = selectedLocations.size();
        selectedCountView.setText(count == 0
                ? "Choose locations to build a route."
                : count + " location" + (count == 1 ? "" : "s") + " selected");
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
