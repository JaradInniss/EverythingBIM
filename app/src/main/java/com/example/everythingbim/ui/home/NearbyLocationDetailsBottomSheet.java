package com.example.everythingbim.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.everythingbim.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class NearbyLocationDetailsBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_LOCATION = "location";

    public static NearbyLocationDetailsBottomSheet newInstance(@NonNull NearbySavedLocation location) {
        NearbyLocationDetailsBottomSheet sheet = new NearbyLocationDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_LOCATION, location);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_nearby_location_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NearbySavedLocation location = readLocation();
        if (location == null) {
            dismiss();
            return;
        }

        ImageView imageView = view.findViewById(R.id.nearby_detail_image);
        TextView nameView = view.findViewById(R.id.nearby_detail_name);
        TextView categoryView = view.findViewById(R.id.nearby_detail_category);
        TextView distanceView = view.findViewById(R.id.nearby_detail_distance);
        TextView addressView = view.findViewById(R.id.nearby_detail_address);
        TextView descriptionView = view.findViewById(R.id.nearby_detail_description);
        TextView closeButton = view.findViewById(R.id.nearby_detail_close_button);

        nameView.setText(location.getName());
        categoryView.setText(location.getCategoryLabel());
        distanceView.setText(location.getDistanceLabel());
        addressView.setText(location.getAddressOrFallback());
        descriptionView.setText(location.getDescriptionOrFallback());
        closeButton.setOnClickListener(v -> dismiss());

        bindImage(imageView, location);
    }

    @Nullable
    private NearbySavedLocation readLocation() {
        Bundle args = getArguments();
        if (args == null) {
            return null;
        }
        Object value = args.getSerializable(ARG_LOCATION);
        if (value instanceof NearbySavedLocation) {
            return (NearbySavedLocation) value;
        }
        return null;
    }

    private void bindImage(@NonNull ImageView imageView, @NonNull NearbySavedLocation location) {
        NearbyLocationImageHelper.loadInto(imageView, location.getImageUrl());
    }
}
