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

import com.example.everythingbim.ui.utils.AddLocationToAddressAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ViewAddLocationToAddressFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private AddLocationToAddressAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
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
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "unknown";
        TextView userIdTv = view.findViewById(R.id.viewlocareq_user_id_tv);
        if (userIdTv != null) {
            userIdTv.setText("User #" + userId.substring(0, Math.min(8, userId.length())).toUpperCase());
        }

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.viewlocationtoaddreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AddLocationToAddressAdapter(
                new java.util.ArrayList<>(),
                R.layout.item_view_add_location_to_address,
                requireContext(),
                requireActivity()
        );
        recyclerView.setAdapter(adapter);

        // Load pending requests
        loadPendingRequests();

        return view;
    }

    private void loadPendingRequests() {
        String userId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(getContext(), "Please sign in to view your requests",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("add_location_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "In Review")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        adapter.setRequests(snapshot.getDocuments());
                    }
                    updateEmptyState(snapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewAddLocationToAddress", "Failed to load requests", e);
                    Toast.makeText(getContext(),
                            "Failed to load requests: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState(com.google.firebase.firestore.QuerySnapshot snapshot) {
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