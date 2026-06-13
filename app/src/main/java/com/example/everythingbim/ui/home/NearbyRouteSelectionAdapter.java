package com.example.everythingbim.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NearbyRouteSelectionAdapter extends RecyclerView.Adapter<NearbyRouteSelectionAdapter.RouteLocationViewHolder> {
    private final List<NearbySavedLocation> items = new ArrayList<>();
    private final Set<Long> selectedIds = new LinkedHashSet<>();
    private final OnSelectionChangedListener selectionChangedListener;
    private final OnLocationDetailsClickListener detailsClickListener;

    public NearbyRouteSelectionAdapter(@NonNull OnSelectionChangedListener selectionChangedListener,
                                       @NonNull OnLocationDetailsClickListener detailsClickListener) {
        this.selectionChangedListener = selectionChangedListener;
        this.detailsClickListener = detailsClickListener;
    }

    public void submitList(@NonNull List<NearbySavedLocation> locations, @NonNull Set<Long> currentSelection) {
        items.clear();
        items.addAll(locations);
        selectedIds.clear();
        selectedIds.addAll(currentSelection);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteLocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_route_location, parent, false);
        return new RouteLocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteLocationViewHolder holder, int position) {
        holder.bind(
                items.get(position),
                selectedIds.contains(items.get(position).getLocationId()),
                detailsClickListener
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class RouteLocationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView nameView;
        private final TextView descriptionView;
        private final TextView distanceView;
        private final LinearLayout addButton;
        private final LinearLayout removeButton;

        RouteLocationViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.nearby_route_image);
            nameView = itemView.findViewById(R.id.nearby_route_name);
            descriptionView = itemView.findViewById(R.id.nearby_route_description);
            distanceView = itemView.findViewById(R.id.nearby_route_distance);
            addButton = itemView.findViewById(R.id.nearby_route_add_button);
            removeButton = itemView.findViewById(R.id.nearby_route_remove_button);
        }

        void bind(@NonNull NearbySavedLocation location,
                  boolean isSelected,
                  @NonNull OnLocationDetailsClickListener detailsClickListener) {
            nameView.setText(location.getName());
            descriptionView.setText(location.getDescriptionOrFallback());
            distanceView.setText(location.getDistanceLabel());
            NearbyLocationImageHelper.loadInto(imageView, location.getImageUrl());

            addButton.setVisibility(isSelected ? View.GONE : View.VISIBLE);
            removeButton.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            addButton.setOnClickListener(v -> {
                selectedIds.add(location.getLocationId());
                notifyItemChanged(getAdapterPosition());
                selectionChangedListener.onLocationAdded(location);
            });

            removeButton.setOnClickListener(v -> {
                selectedIds.remove(location.getLocationId());
                notifyItemChanged(getAdapterPosition());
                selectionChangedListener.onLocationRemoved(location);
            });

            imageView.setOnClickListener(v -> detailsClickListener.onLocationDetailsClick(location));
        }
    }

    public interface OnSelectionChangedListener {
        void onLocationAdded(@NonNull NearbySavedLocation location);

        void onLocationRemoved(@NonNull NearbySavedLocation location);
    }

    public interface OnLocationDetailsClickListener {
        void onLocationDetailsClick(@NonNull NearbySavedLocation location);
    }
}
