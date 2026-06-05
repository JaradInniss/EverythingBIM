package com.example.everythingbim.ui.utils;

import android.annotation.SuppressLint;
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

public class AccountVerificationAdapter
        extends RecyclerView.Adapter<AccountVerificationAdapter.ViewHolder> {

    private static final String TAG = "AccVerificationAdapter";

    private List<DocumentSnapshot> requests = new ArrayList<>();
    private int itemLayoutRes;
    private FirebaseStorage storage;

    public AccountVerificationAdapter(List<DocumentSnapshot> requests, int itemLayoutRes) {
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

        // Request number
        if (holder.requestNo != null) {
            holder.requestNo.setText("Request #" +
                    doc.getId().substring(0, Math.min(6, doc.getId().length())).toUpperCase());
        }

        // Submission date
        if (holder.submissionDate != null) {
            Timestamp createdAt = doc.getTimestamp("createdAt");
            if (createdAt != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                holder.submissionDate.setText("Submitted: " + sdf.format(createdAt.toDate()));
            } else {
                holder.submissionDate.setText("Submitted: N/A");
            }
        }

        // Status - check both verificationStatus and status fields
        String status = doc.getString("verificationStatus");
        if (status == null) status = doc.getString("status");
        if (status == null) status = "In Review";
        if (holder.statusValue != null) {
            holder.statusValue.setText(status);
        }

        // Business Name - check multiple possible field names
        if (holder.nameEt != null) {
            String name = doc.getString("businessName"); // Actual Firestore field (lowercase)
            if (name == null || name.isEmpty()) name = doc.getString("BusinessName");
            if (name == null || name.isEmpty()) name = doc.getString("companyName");
            if (name == null || name.isEmpty()) name = doc.getString("name");
            holder.nameEt.setText(name != null ? name : "");
        }

        // Phone - Firestore field is "phone"
        if (holder.phoneEt != null) {
            String phone = doc.getString("phone");
            holder.phoneEt.setText(phone != null ? phone : "");
        }

        // Email - check multiple possible field names
        if (holder.emailEt != null) {
            String email = doc.getString("businessEmail"); // Actual Firestore field
            if (email == null || email.isEmpty()) email = doc.getString("email");
            holder.emailEt.setText(email != null ? email : "");
        }

        // Location - Firestore field is "address"
        if (holder.locationEt != null) {
            String address = doc.getString("address");
            holder.locationEt.setText(address != null ? address : "");
        }

        // Business Type - check multiple possible field names
        if (holder.businessTypeEt != null) {
            String type = doc.getString("businessType"); // Actual Firestore field
            if (type == null || type.isEmpty()) type = doc.getString("type");
            if (type == null || type.isEmpty()) type = doc.getString("businessCategory");
            holder.businessTypeEt.setText(type != null ? type : "");
        }

        // Description - Firestore field is "description"
        if (holder.descEt != null) {
            String desc = doc.getString("description");
            holder.descEt.setText(desc != null ? desc : "");
        }

        // Business Type - Firestore field is "businessType"
        if (holder.businessTypeEt != null) {
            String type = doc.getString("businessType");
            holder.businessTypeEt.setText(type != null ? type : "");
        }

        // Load business images
        if (holder.imageContainer != null) {
            holder.imageContainer.removeAllViews();
            List<String> imageUrls = (List<String>) doc.get("imageUrls");
            if (imageUrls != null) {
                for (String url : imageUrls) {
                    loadImage(url, holder.imageContainer);
                }
            }
        }

        // Load business certificates/documents
        if (holder.certificatesContainer != null) {
            holder.certificatesContainer.removeAllViews();
            List<String> fileUrls = (List<String>) doc.get("fileUrls");
            if (fileUrls != null) {
                for (String url : fileUrls) {
                    loadImage(url, holder.certificatesContainer);
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
                        // Try loading as file icon on failure
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

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView requestNo;
        TextView submissionDate;
        TextView statusValue;
        ImageView statusIcon;

        TextView nameEt;
        TextView phoneEt;
        TextView emailEt;
        TextView locationEt;
        TextView descEt;
        TextView businessTypeEt;

        LinearLayout imageContainer;
        LinearLayout certificatesContainer;

        ViewHolder(View view) {
            super(view);
            requestNo = view.findViewById(R.id.business_request_no);
            submissionDate = view.findViewById(R.id.business_submission_date);
            statusValue = view.findViewById(R.id.status_value);
            statusIcon = view.findViewById(R.id.status_icon);

            nameEt = view.findViewById(R.id.viewaccverreq_name_et);
            phoneEt = view.findViewById(R.id.viewaccverreq_phone_no_et);
            emailEt = view.findViewById(R.id.viewaccverreq_email_et);
            locationEt = view.findViewById(R.id.viewaccverreq_location_et);
            descEt = view.findViewById(R.id.viewaccverreq_desc_et);
            businessTypeEt = view.findViewById(R.id.viewaccverreq_business_type_et);

            imageContainer = view.findViewById(R.id.viewaccverreq_image_container);
            certificatesContainer = view.findViewById(R.id.viewaccverreq_certificates_container);
        }
    }
}