package com.example.everythingbim.ui.map;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;

import java.util.ArrayList;
import java.util.List;

final class RouteDirectionsAdapter extends RecyclerView.Adapter<RouteDirectionsAdapter.RouteStepViewHolder> {
    private final List<RouteDirectionsBottomSheet.RouteStepItem> items = new ArrayList<>();

    RouteDirectionsAdapter(@NonNull List<RouteDirectionsBottomSheet.RouteStepItem> steps) {
        items.addAll(steps);
    }

    @NonNull
    @Override
    public RouteStepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route_direction_step, parent, false);
        return new RouteStepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteStepViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class RouteStepViewHolder extends RecyclerView.ViewHolder {
        private final TextView stepNumber;
        private final TextView stepInstruction;
        private final TextView stepMeta;

        RouteStepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumber = itemView.findViewById(R.id.route_step_number);
            stepInstruction = itemView.findViewById(R.id.route_step_instruction);
            stepMeta = itemView.findViewById(R.id.route_step_meta);
        }

        void bind(@NonNull RouteDirectionsBottomSheet.RouteStepItem item) {
            stepNumber.setText(String.valueOf(item.index));
            stepInstruction.setText(item.instruction);
            stepMeta.setText(item.distanceLabel + " | " + item.durationLabel);
        }
    }
}
