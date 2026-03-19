package com.example.everythingbim.ui.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;

import java.util.List;

public class LocationRequestAdapter extends RecyclerView.Adapter<LocationRequestAdapter.ViewHolder> {

    private List<Object> requests;
    private int itemLayoutRes;

    public LocationRequestAdapter(List<Object> requests, int itemLayoutRes) {
        this.requests = requests;
        this.itemLayoutRes = itemLayoutRes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind data here when backend is ready
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView requestNo, submissionDate, statusValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            requestNo      = itemView.findViewById(R.id.request_no);
            submissionDate = itemView.findViewById(R.id.submission_date);
            statusValue    = itemView.findViewById(R.id.status_value);
        }
    }
}