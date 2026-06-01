package com.example.everythingbim.ui.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LocationRequestAdapter
        extends RecyclerView.Adapter<LocationRequestAdapter.ViewHolder> {

    // Uses DocumentSnapshot so Firestore fields are read directly
    private List<DocumentSnapshot> requests;
    private int itemLayoutRes;

    public LocationRequestAdapter(List<DocumentSnapshot> requests, int itemLayoutRes) {
        this.requests    = requests;
        this.itemLayoutRes = itemLayoutRes;
    }

    // Update list when Firestore data arrives — call from Fragment after query
    public void setRequests(List<DocumentSnapshot> newRequests) {
        this.requests = newRequests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(itemLayoutRes, parent, false);
        ViewHolder holder = new ViewHolder(view);

        // MapView requires manual lifecycle calls inside RecyclerView
        if (holder.mapView != null) {
            holder.mapView.onCreate(null);
            holder.mapView.onResume();
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = requests.get(position);

        // ── Request number ───────────────────────
        if (holder.requestNo != null) {
            holder.requestNo.setText("Request #" +
                    doc.getId().substring(0, 6).toUpperCase());
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
            if (status == null) status = "In Review";
            holder.statusValue.setText(status);

            switch (status) {
                case "Completed":
                    holder.statusValue.setTextColor(
                            android.graphics.Color.parseColor("#009407")); // green
                    break;
                case "Rejected":
                    holder.statusValue.setTextColor(
                            android.graphics.Color.parseColor("#870000")); // red
                    break;
                default: // In Review
                    holder.statusValue.setTextColor(
                            android.graphics.Color.parseColor("#1F37B2")); // blue
                    break;
            }
        }

        // ── Map — shows the pinned location ─────
        if (holder.mapView != null) {
            Double lat = doc.getDouble("latitude");
            Double lng = doc.getDouble("longitude");

            holder.mapView.setTag(position);
            holder.mapView.getMapAsync(map -> {
                map.getUiSettings().setAllGesturesEnabled(false);
                map.getUiSettings().setZoomControlsEnabled(false);
                map.clear();

                LatLng location = (lat != null && lng != null)
                        ? new LatLng(lat, lng)
                        : new LatLng(13.1939, -59.5432); // default Barbados center

                map.addMarker(new MarkerOptions().position(location));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 14f));
            });
        }

        // ── Location name ────────────────────────
        if (holder.locationName != null) {
            holder.locationName.setText(doc.getString("locationName"));
        }

        // ── Description ──────────────────────────
        if (holder.locationDesc != null) {
            holder.locationDesc.setText(doc.getString("description"));
        }

        // ── Reason ───────────────────────────────
        if (holder.locationReason != null) {
            holder.locationReason.setText(doc.getString("reason"));
        }

        // ── Place Type ───────────────────────────
        if (holder.placeType != null) {
            holder.placeType.setText(doc.getString("placeType"));
        }

        // ── Images — loaded from Firebase Storage URLs ──
        if (holder.imageContainer != null) {
            holder.imageContainer.removeAllViews(); // clear before rebinding

            List<String> imageUrls = (List<String>) doc.get("imageUrls");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                for (String url : imageUrls) {
                    StorageReference ref = FirebaseStorage.getInstance()
                            .getReferenceFromUrl(url);

                    ref.getBytes(2 * 1024 * 1024) // max 2MB per image
                            .addOnSuccessListener(bytes -> {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(
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
                            })
                            .addOnFailureListener(e -> {
                                // Silently skip images that fail to load
                            });
                }
            }
        }

        // ── Accepted / Rejected detail containers ──
        if (holder.acceptedContainer != null && holder.rejectedContainer != null) {
            String status = doc.getString("status");

            if ("Completed".equals(status)) {
                holder.acceptedContainer.setVisibility(View.VISIBLE);
                holder.rejectedContainer.setVisibility(View.GONE);

                if (holder.acceptedDate != null)
                    holder.acceptedDate.setText(doc.getString("acceptedDate"));
                if (holder.acceptedBy != null)
                    holder.acceptedBy.setText(doc.getString("acceptedBy"));

            } else if ("Rejected".equals(status)) {
                holder.rejectedContainer.setVisibility(View.VISIBLE);
                holder.acceptedContainer.setVisibility(View.GONE);

                if (holder.rejectedDate != null)
                    holder.rejectedDate.setText(doc.getString("rejectedDate"));
                if (holder.rejectedBy != null)
                    holder.rejectedBy.setText(doc.getString("rejectedBy"));
                if (holder.rejectionReason != null)
                    holder.rejectionReason.setText(doc.getString("rejectionReason"));

            } else {
                holder.acceptedContainer.setVisibility(View.GONE);
                holder.rejectedContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.mapView != null) holder.mapView.onDestroy();
    }

    @Override
    public int getItemCount() { return requests.size(); }

    // ─── ViewHolder ──────────────────────────────
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView requestNo, submissionDate, statusValue;
        TextView locationName, locationDesc, locationReason, placeType;
        MapView mapView;
        LinearLayout imageContainer;
        LinearLayout acceptedContainer, rejectedContainer;
        TextView acceptedDate, acceptedBy;
        TextView rejectedDate, rejectedBy, rejectionReason;

        @SuppressLint("WrongViewCast")
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            requestNo         = itemView.findViewById(R.id.request_no);
            submissionDate    = itemView.findViewById(R.id.submission_date);
            statusValue       = itemView.findViewById(R.id.status_value);
            locationName      = itemView.findViewById(R.id.viewlocreq_name_et);
            locationDesc      = itemView.findViewById(R.id.viewlocreq_desc_et);
            locationReason    = itemView.findViewById(R.id.viewlocreq_reason_et);
            placeType         = itemView.findViewById(R.id.viewlocreq_place_type_et);
            mapView           = itemView.findViewById(R.id.viewlocreq_map);
            imageContainer    = itemView.findViewById(R.id.viewlocreq_image_container);
            acceptedContainer = itemView.findViewById(R.id.viewlocreq_accepted_container);
            rejectedContainer = itemView.findViewById(R.id.viewlocreq_rejected_container);
            acceptedDate      = itemView.findViewById(R.id.viewlocreq_accepted_date_tv);
            acceptedBy        = itemView.findViewById(R.id.viewlocreq_accepted_by_tv);
            rejectedDate      = itemView.findViewById(R.id.viewlocreq_rejected_date_tv);
            rejectedBy        = itemView.findViewById(R.id.viewlocreq_rejected_by_tv);
            rejectionReason   = itemView.findViewById(R.id.viewlocreq_rejection_reason_tv);
        }
    }
}