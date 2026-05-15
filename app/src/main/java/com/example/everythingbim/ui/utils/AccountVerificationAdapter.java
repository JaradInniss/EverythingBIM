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

    private List<DocumentSnapshot> requests = new ArrayList<>();
    private int itemLayoutRes;

    public AccountVerificationAdapter(List<DocumentSnapshot> requests, int itemLayoutRes) {
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

        // Status
        String status = doc.getString("status");
        if (holder.statusValue != null) {
            holder.statusValue.setText(status != null ? status : "In Review");
        }

        // Business Name
        if (holder.nameEt != null) {
            String name = doc.getString("businessName");
            holder.nameEt.setText(name != null ? name : "");
        }

        // Phone
        if (holder.phoneEt != null) {
            String phone = doc.getString("phone");
            holder.phoneEt.setText(phone != null ? phone : "");
        }

        // Email
        if (holder.emailEt != null) {
            String email = doc.getString("email");
            holder.emailEt.setText(email != null ? email : "");
        }

        // Location
        if (holder.locationEt != null) {
            String location = doc.getString("location");
            holder.locationEt.setText(location != null ? location : "");
        }

        // Description
        if (holder.descEt != null) {
            String desc = doc.getString("description");
            holder.descEt.setText(desc != null ? desc : "");
        }

        // Business Type
        if (holder.businessTypeEt != null) {
            String type = doc.getString("businessType");
            holder.businessTypeEt.setText(type != null ? type : "");
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
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