package com.example.everythingbim.ui.user;

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
import com.example.everythingbim.ui.utils.LocationRequestAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;

public class ViewCompletedLocationRequestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_completed_location_request,
                container, false);

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.viewlocreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Empty list for now — backend will populate this
        // Query when ready:
        //   db.collection("add_location_requests")
        //     .whereEqualTo("userId", auth.getCurrentUser().getUid())
        //     .whereIn("status", Arrays.asList("Completed", "Rejected"))
        //     .get()
        //     .addOnSuccessListener(snapshot -> adapter.setRequests(snapshot.getDocuments()));
        recyclerView.setAdapter(new LocationRequestAdapter(
                new ArrayList<DocumentSnapshot>(),
                R.layout.item_view_completed_location_request
        ));

        return view;
    }
}