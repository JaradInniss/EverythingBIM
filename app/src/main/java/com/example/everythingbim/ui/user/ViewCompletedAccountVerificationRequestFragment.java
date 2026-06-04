package com.example.everythingbim.ui.user;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.utils.AccountVerificationAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ViewCompletedAccountVerificationRequestFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private AccountVerificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_completed_account_verification_request,
                container, false);

        db = FirebaseFirestore.getInstance();

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.viewcompleted_accverreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with empty list
        adapter = new AccountVerificationAdapter(
                new ArrayList<>(),
                R.layout.item_view_completed_account_verification_request
        );
        recyclerView.setAdapter(adapter);

        // Load data from Firestore
        loadCompletedAccountVerificationRequests(view);

        return view;
    }

    private void loadCompletedAccountVerificationRequests(View view) {
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        if (userId.isEmpty()) {
            return;
        }

        // Query businesses collection for completed requests (Completed or Rejected)
        db.collection("businesses")
                .whereEqualTo("userId", userId)
                .whereIn("verificationStatus", Arrays.asList("Completed", "Rejected"))
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && !snapshot.isEmpty()) {
                        List<DocumentSnapshot> docs = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            docs.add(doc);
                        }
                        adapter.setRequests(docs);
                        showEmptyState(view, false);
                    } else {
                        adapter.setRequests(new ArrayList<>());
                        showEmptyState(view, true);
                    }
                })
                .addOnFailureListener(e -> {
                    adapter.setRequests(new ArrayList<>());
                    showEmptyState(view, true);
                });
    }

    private void showEmptyState(View view, boolean show) {
        android.widget.TextView emptyTv = view.findViewById(R.id.empty_state_tv);
        if (emptyTv != null) {
            emptyTv.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}