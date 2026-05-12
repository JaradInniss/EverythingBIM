package com.example.everythingbim.ui.map;

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

public class RouteDirectionsBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_STEPS = "steps";
    private static final String ARG_TITLE = "title";
    private static final String ARG_SUMMARY = "summary";

    public static RouteDirectionsBottomSheet newInstance(@NonNull ArrayList<RouteStepItem> steps,
                                                         @NonNull String title,
                                                         @NonNull String summary) {
        RouteDirectionsBottomSheet sheet = new RouteDirectionsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STEPS, steps);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SUMMARY, summary);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_route_directions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView closeButton = view.findViewById(R.id.route_directions_close_button);
        TextView titleView = view.findViewById(R.id.route_directions_title);
        TextView summaryView = view.findViewById(R.id.route_directions_summary);
        RecyclerView recyclerView = view.findViewById(R.id.route_directions_recycler);

        closeButton.setOnClickListener(v -> dismiss());
        titleView.setText(getArguments() != null ? getArguments().getString(ARG_TITLE, "Directions") : "Directions");
        summaryView.setText(getArguments() != null ? getArguments().getString(ARG_SUMMARY, "") : "");

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(new RouteDirectionsAdapter(readSteps()));
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private ArrayList<RouteStepItem> readSteps() {
        Serializable value = getArguments() != null ? getArguments().getSerializable(ARG_STEPS) : null;
        if (value instanceof ArrayList) {
            return (ArrayList<RouteStepItem>) value;
        }
        return new ArrayList<>();
    }

    public static final class RouteStepItem implements Serializable {
        public final int index;
        public final String instruction;
        public final String distanceLabel;
        public final String durationLabel;

        public RouteStepItem(int index,
                             @NonNull String instruction,
                             @NonNull String distanceLabel,
                             @NonNull String durationLabel) {
            this.index = index;
            this.instruction = instruction;
            this.distanceLabel = distanceLabel;
            this.durationLabel = durationLabel;
        }
    }
}
