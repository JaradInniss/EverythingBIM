package com.example.everythingbim.ui.utils;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ViewCompletedLocationToAddressAdapter
        extends RecyclerView.Adapter<ViewCompletedLocationToAddressAdapter.ViewHolder> {

    private List<DocumentSnapshot> requests;
    private int itemLayoutRes;

    public ViewCompletedLocationToAddressAdapter(List<DocumentSnapshot> requests, int itemLayoutRes) {
        this.requests = requests;
        this.itemLayoutRes = itemLayoutRes;
    }

    public void setRequests(List<DocumentSnapshot> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
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
        DocumentSnapshot doc = requests.get(position);

        // ── Request number ───────────────────────
        if (holder.requestNo != null) {
            String docId = doc.getId();
            holder.requestNo.setText("Request #" +
                    docId.substring(0, Math.min(6, docId.length())).toUpperCase());
        }

        // ── Submission date ──────────────────────
        if (holder.submissionDate != null) {
            com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy/MM/dd", Locale.getDefault());
                holder.submissionDate.setText(
                        "Submitted: " + sdf.format(createdAt.toDate()));
            }
        }

        // ── Status text + colour ─────────────────
        if (holder.statusValue != null) {
            String status = doc.getString("status");
            if (status == null) status = "Completed";
            holder.statusValue.setText(status);

            if ("Completed".equals(status)) {
                holder.statusValue.setTextColor(
                        android.graphics.Color.parseColor("#009407"));
            } else if ("Rejected".equals(status)) {
                holder.statusValue.setTextColor(
                        android.graphics.Color.parseColor("#870000"));
            }
        }

        // ── Status icon tint ─────────────────────
        if (holder.statusIcon != null) {
            String status = doc.getString("status");
            if ("Completed".equals(status)) {
                holder.statusIcon.setColorFilter(
                        android.graphics.Color.parseColor("#009407"));
            } else if ("Rejected".equals(status)) {
                holder.statusIcon.setColorFilter(
                        android.graphics.Color.parseColor("#870000"));
            }
        }

        // ── Location name ────────────────────────
        if (holder.locationName != null) {
            String locationName = doc.getString("locationName");
            holder.locationName.setText(locationName != null ? locationName : "");
        }

        // ── Place type ────────────────────────────
        if (holder.placeType != null) {
            String placeType = doc.getString("placeType");
            holder.placeType.setText(placeType != null ? placeType : "");
        }

        // ── Images ────────────────────────────────
        if (holder.imageContainer != null) {
            holder.imageContainer.removeAllViews();

            List<String> imageUrls = (List<String>) doc.get("imageUrls");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (String url : imageUrls) {
                    try {
                        FirebaseStorage.getInstance()
                                .getReferenceFromUrl(url)
                                .getBytes(2 * 1024 * 1024)
                                .addOnSuccessListener(bytes -> {
                                    android.graphics.Bitmap bitmap =
                                            android.graphics.BitmapFactory.decodeByteArray(
                                                    bytes, 0, bytes.length);
                                    ImageView imageView = new ImageView(
                                            holder.imageContainer.getContext());
                                    LinearLayout.LayoutParams params =
                                            new LinearLayout.LayoutParams(120, 120);
                                    params.setMarginEnd(8);
                                    imageView.setLayoutParams(params);
                                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    imageView.setImageBitmap(bitmap);
                                    holder.imageContainer.addView(imageView);
                                });
                    } catch (Exception e) {
                        // skip failed images
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() { return requests.size(); }

    // ─── ViewHolder ──────────────────────────────
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView requestNo, submissionDate, statusValue;
        TextView locationName, placeType;
        LinearLayout imageContainer;
        ImageView statusIcon;

        @SuppressLint("WrongViewCast")
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            requestNo          = itemView.findViewById(R.id.request_no);
            submissionDate     = itemView.findViewById(R.id.submission_date);
            statusValue        = itemView.findViewById(R.id.status_value);
            statusIcon         = itemView.findViewById(R.id.status_icon);
            locationName       = itemView.findViewById(R.id.location_name_et);
            placeType          = itemView.findViewById(R.id.place_type_tv);
            imageContainer    = itemView.findViewById(R.id.img_icon_container);
        }
    }
}