package com.example.everythingbim.ui.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ViewAddLocationToAddressAdapter
        extends RecyclerView.Adapter<ViewAddLocationToAddressAdapter.ViewHolder> {

    private static final String TAG = "ViewAddLocToAddrAdapter";
    private List<DocumentSnapshot> requests;
    private int itemLayoutRes;
    private FirebaseStorage storage;

    public ViewAddLocationToAddressAdapter(List<DocumentSnapshot> requests, int itemLayoutRes) {
        this.requests = requests;
        this.itemLayoutRes = itemLayoutRes;
        this.storage = FirebaseStorage.getInstance();
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

        // Request number - check both field naming conventions
        String requestNo = doc.getId().substring(0, Math.min(6, doc.getId().length())).toUpperCase();
        if (holder.requestNo != null) {
            holder.requestNo.setText("Request #" + requestNo);
        } else if (holder.businessRequestNo != null) {
            holder.businessRequestNo.setText("Request #" + requestNo);
        }

        // Submission date
        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (holder.submissionDate != null) {
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                holder.submissionDate.setText("Submitted: " + sdf.format(createdAt.toDate()));
            } else {
                holder.submissionDate.setText("Submitted: N/A");
            }
        } else if (holder.businessSubmissionDate != null) {
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                holder.businessSubmissionDate.setText("Submitted: " + sdf.format(createdAt.toDate()));
            } else {
                holder.businessSubmissionDate.setText("Submitted: N/A");
            }
        }

        // Status
        String status = doc.getString("status");
        if (status == null) status = "In Review";
        if (holder.statusValue != null) {
            holder.statusValue.setText(status);
        }

        // Location Name - check both field naming conventions
        String locationName = doc.getString("locationName");
        if (holder.locationNameEt != null) {
            holder.locationNameEt.setText(locationName != null ? locationName : "");
        }

        // Place Type
        String placeType = doc.getString("placeType");
        if (holder.placeTypeEt != null) {
            holder.placeTypeEt.setText(placeType != null ? placeType : "");
        }

        // Images of Location
        if (holder.imageContainer != null) {
            holder.imageContainer.removeAllViews();
            List<String> imageUrls = (List<String>) doc.get("imageUrls");
            if (imageUrls != null) {
                for (String url : imageUrls) {
                    loadImage(url, holder.imageContainer);
                }
            }
        }
    }

    private void loadImage(String url, LinearLayout container) {
        try {
            StorageReference ref = storage.getReferenceFromUrl(url);
            ref.getBytes(2 * 1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        ImageView iv = new ImageView(container.getContext());
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(80, 60);
                        p.setMarginEnd(8);
                        iv.setLayoutParams(p);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setImageBitmap(bmp);
                        container.addView(iv);
                    })
                    .addOnFailureListener(e -> {
                        ImageView iv = new ImageView(container.getContext());
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(80, 60);
                        p.setMarginEnd(8);
                        iv.setLayoutParams(p);
                        iv.setImageResource(R.drawable.ic_file_pdf);
                        container.addView(iv);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to load image: " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        int count = requests.size();
        Log.d(TAG, "getItemCount: returning " + count);
        return count;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // General user layout fields
        TextView requestNo;
        TextView submissionDate;

        // Business user layout fields
        TextView businessRequestNo;
        TextView businessSubmissionDate;

        TextView statusValue;
        ImageView statusIcon;

        // Common fields
        TextView locationNameEt;
        TextView placeTypeEt;
        LinearLayout imageContainer;

        ViewHolder(View view) {
            super(view);
            requestNo = view.findViewById(R.id.request_no);
            submissionDate = view.findViewById(R.id.submission_date);
            businessRequestNo = view.findViewById(R.id.business_request_no);
            businessSubmissionDate = view.findViewById(R.id.business_submission_date);
            statusValue = view.findViewById(R.id.status_value);
            statusIcon = view.findViewById(R.id.status_icon);
            locationNameEt = view.findViewById(R.id.location_name_et);
            placeTypeEt = view.findViewById(R.id.place_type_et);
            imageContainer = view.findViewById(R.id.viewlocreq_image_container);
        }
    }
}