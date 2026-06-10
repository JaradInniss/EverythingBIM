package com.example.everythingbim;

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

import com.example.everythingbim.ui.utils.ViewCompletedLocationToAddressAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ViewCompletedAddLocationToAddressFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ViewCompletedLocationToAddressAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_add_completed_location_to_address,
                container, false);

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Set user ID in toolbar
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "unknown";
        TextView userIdTv = view.findViewById(R.id.viewlocareq_user_id_tv);
        if (userIdTv != null) {
            userIdTv.setText("User #" + userId.substring(0, Math.min(8, userId.length())).toUpperCase());
        }

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.viewlocationtoaddreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ViewCompletedLocationToAddressAdapter(
                new java.util.ArrayList<>(),
                R.layout.item_view_add_completed_location_to_address
        );
        recyclerView.setAdapter(adapter);

        // Load completed requests
        loadCompletedRequests();

        return view;
    }

    private void loadCompletedRequests() {
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(getContext(), "Please sign in to view your requests",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.List<com.google.firebase.firestore.DocumentSnapshot> allCompleted = new java.util.ArrayList<>();
        final boolean[] queriesCompleted = {false, false};
        final boolean[] hasAnyData = {false};

        // Load regular completed location requests
        db.collection("add_location_requests")
                .whereEqualTo("userId", userId)
                .whereIn("status", java.util.Arrays.asList("Completed", "Rejected"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        allCompleted.addAll(snapshot.getDocuments());
                        hasAnyData[0] = true;
                    }
                    queriesCompleted[0] = true;
                    if (queriesCompleted[0] && queriesCompleted[1]) {
                        adapter.setRequests(allCompleted);
                        updateEmptyState(!hasAnyData[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewCompletedLocationToAddress", "Failed to load requests", e);
                    queriesCompleted[0] = true;
                    if (queriesCompleted[0] && queriesCompleted[1]) {
                        adapter.setRequests(allCompleted);
                        updateEmptyState(!hasAnyData[0]);
                    }
                });

        // Also load completed business location requests
        db.collection("add_business_location_requests")
                .whereEqualTo("userId", userId)
                .whereIn("status", java.util.Arrays.asList("Completed", "Rejected"))
                .get()
                .addOnSuccessListener(bizSnapshot -> {
                    if (bizSnapshot != null && !bizSnapshot.isEmpty()) {
                        allCompleted.addAll(bizSnapshot.getDocuments());
                        hasAnyData[0] = true;
                    }
                    queriesCompleted[1] = true;
                    if (queriesCompleted[0] && queriesCompleted[1]) {
                        adapter.setRequests(allCompleted);
                        updateEmptyState(!hasAnyData[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewCompletedLocationToAddress", "Failed to load business requests", e);
                    queriesCompleted[1] = true;
                    if (queriesCompleted[0] && queriesCompleted[1]) {
                        adapter.setRequests(allCompleted);
                        updateEmptyState(!hasAnyData[0]);
                    }
                });
    }

    private void updateEmptyState(boolean isEmpty) {
        View view = getView();
        if (view == null) return;

        TextView emptyTv = view.findViewById(R.id.empty_state_tv);
        RecyclerView recyclerView = view.findViewById(R.id.viewlocationtoaddreq_recycler);

        if (emptyTv != null) {
            emptyTv.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}