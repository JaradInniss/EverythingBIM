package com.example.everythingbim;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.ui.utils.ViewAddLocationToAddressAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewAddLocationToAddressFragment extends Fragment {

    private FirebaseFirestore db;
    private ViewAddLocationToAddressAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_add_location_to_address,
                container, false);

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Set user ID in toolbar
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        TextView userIdTv = view.findViewById(R.id.viewlocareq_user_id_tv);
        if (userIdTv != null && userId != null && !userId.isEmpty()) {
            userIdTv.setText("User #" + userId.substring(0, Math.min(8, userId.length())).toUpperCase());
        }

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.viewlocationtoaddreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ViewAddLocationToAddressAdapter(
                new java.util.ArrayList<>(),
                R.layout.item_view_add_location_to_address
        );
        recyclerView.setAdapter(adapter);

        // Load pending requests
        loadPendingRequests();

        return view;
    }

    private void loadPendingRequests() {
        // Get userId from SharedPreferences (consistent with other fragments)
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        if (userId.isEmpty()) {
            Toast.makeText(getContext(), "Please sign in to view your requests",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Query both collections: regular location requests AND business location requests
        db.collection("add_location_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "In Review")
                .get()
                .addOnSuccessListener(locationSnapshot -> {
                    // Also check business location requests
                    loadBusinessLocationRequests(locationSnapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewAddLocationToAddress", "Failed to load requests", e);
                    Toast.makeText(getContext(),
                            "Failed to load requests: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBusinessLocationRequests(QuerySnapshot locationSnapshot) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        db.collection("add_business_location_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "In Review")
                .get()
                .addOnSuccessListener(bizSnapshot -> {
                    // Combine both document lists
                    List<com.google.firebase.firestore.DocumentSnapshot> combinedDocs = new java.util.ArrayList<>();

                    if (locationSnapshot != null && !locationSnapshot.isEmpty()) {
                        combinedDocs.addAll(locationSnapshot.getDocuments());
                    }
                    if (bizSnapshot != null && !bizSnapshot.isEmpty()) {
                        combinedDocs.addAll(bizSnapshot.getDocuments());
                    }

                    adapter.setRequests(combinedDocs);
                    updateEmptyState(combinedDocs.isEmpty() ? null : createMergedSnapshot(locationSnapshot, bizSnapshot));
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewAddLocationToAddress", "Failed to load business requests", e);
                    // Still show location requests if business ones fail
                    if (locationSnapshot != null && !locationSnapshot.isEmpty()) {
                        adapter.setRequests(locationSnapshot.getDocuments());
                    }
                    updateEmptyState(locationSnapshot);
                });
    }

    private QuerySnapshot createMergedSnapshot(
            QuerySnapshot s1,
            QuerySnapshot s2) {
        // Return s1 if available, otherwise s2 (for empty state calculation)
        return s1 != null && !s1.isEmpty() ? s1 : s2;
    }

    private void updateEmptyState(QuerySnapshot snapshot) {
        View view = getView();
        if (view == null) return;

        TextView emptyTv = view.findViewById(R.id.empty_state_tv);
        RecyclerView recyclerView = view.findViewById(R.id.viewlocationtoaddreq_recycler);

        boolean isEmpty = snapshot == null || snapshot.isEmpty();
        if (emptyTv != null) {
            emptyTv.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}