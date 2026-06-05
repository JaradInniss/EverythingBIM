package com.example.everythingbim.ui.user;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import java.util.List;

public class ViewAccountVerificationRequestFragment extends Fragment {

    private static final String TAG = "AccVerificationReq";
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private AccountVerificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_account_verification_request,
                container, false);

        db = FirebaseFirestore.getInstance();

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.viewaccverreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with empty list
        adapter = new AccountVerificationAdapter(
                new ArrayList<>(),
                R.layout.item_view_account_verification_request
        );
        recyclerView.setAdapter(adapter);

        // Load data from Firestore
        loadAccountVerificationRequests(view);

        return view;
    }

    private void loadAccountVerificationRequests(View view) {
        // Get current user ID from SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        Log.d(TAG, "loadAccountVerificationRequests: userId = " + userId);

        // Show user ID in header
        android.widget.TextView userIdTv = view.findViewById(R.id.viewaccverreq_user_id_tv);
        if (userIdTv != null && userId != null && !userId.isEmpty()) {
            userIdTv.setText("BusinessUser #" + userId.substring(0, Math.min(6, userId.length())).toUpperCase());
        }

        if (userId.isEmpty()) {
            // No user logged in - show empty state
            Log.w(TAG, "loadAccountVerificationRequests: userId is empty, showing empty state");
            showEmptyState(view, true);
            return;
        }

        // Query businesses collection for this user's verification requests
        // Status "In Review" means not yet verified (verificationStatus != "Completed")
        Log.d(TAG, "loadAccountVerificationRequests: querying businesses collection with userId=" + userId);
        db.collection("businesses")
                .whereEqualTo("userId", userId)
                .whereEqualTo("verificationStatus", "In Review")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "loadAccountVerificationRequests: query returned " + (snapshot != null ? snapshot.size() : "null") + " documents");
                    if (snapshot != null && !snapshot.isEmpty()) {
                        List<DocumentSnapshot> docs = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Log.d(TAG, "loadAccountVerificationRequests: found doc id=" + doc.getId() + ", businessName=" + doc.getString("businessName"));
                            docs.add(doc);
                        }
                        adapter.setRequests(docs);
                        showEmptyState(view, false);
                    } else {
                        // Show empty state when no data
                        Log.w(TAG, "loadAccountVerificationRequests: no documents found, showing empty state");
                        adapter.setRequests(new ArrayList<>());
                        showEmptyState(view, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadAccountVerificationRequests: query failed", e);
                    // Show empty state on failure
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