package com.example.everythingbim.ui.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.utils.CompletedInfoRequestAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;

public class ViewCompletedInformationRequestFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CompletedInfoRequestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_completed_information_request,
                container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.viewinforeq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CompletedInfoRequestAdapter(
                new ArrayList<DocumentSnapshot>(),
                R.layout.item_view_add_information_request
        );
        recyclerView.setAdapter(adapter);

        // Load user's "Completed" or "Rejected" information requests
        loadRequests();

        return view;
    }

    private void loadRequests() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        db.collection("add_information_requests")
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("Completed", "Rejected"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("ViewCompletedInfo", "Query succeeded, docs: " + (snapshot != null ? snapshot.size() : 0));
                    if (snapshot != null) {
                        adapter.setRequests(snapshot.getDocuments());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewCompletedInfo", "Query failed: " + e.getMessage(), e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}