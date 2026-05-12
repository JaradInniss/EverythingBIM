package com.example.everythingbim.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;

import java.util.ArrayList;
import java.util.List;

public class NearbyLocationsAdapter extends RecyclerView.Adapter<NearbyLocationsAdapter.NearbyLocationViewHolder> {
    private final List<NearbySavedLocation> items = new ArrayList<>();
    private final OnNearbyLocationClickListener clickListener;

    public NearbyLocationsAdapter(@NonNull OnNearbyLocationClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(@NonNull List<NearbySavedLocation> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NearbyLocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_location, parent, false);
        return new NearbyLocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NearbyLocationViewHolder holder, int position) {
        holder.bind(items.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NearbyLocationViewHolder extends RecyclerView.ViewHolder {
        private final TextView label;

        NearbyLocationViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.nearby_attraction_item_tv);
        }

        void bind(@NonNull NearbySavedLocation location, @NonNull OnNearbyLocationClickListener clickListener) {
            label.setText(location.getDisplayLabel());
            itemView.setOnClickListener(v -> clickListener.onNearbyLocationClick(location));
        }
    }

    public interface OnNearbyLocationClickListener {
        void onNearbyLocationClick(@NonNull NearbySavedLocation location);
    }
}
