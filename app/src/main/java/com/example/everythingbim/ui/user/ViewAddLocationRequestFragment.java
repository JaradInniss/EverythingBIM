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

import com.example.everythingbim.*;
import com.example.everythingbim.ui.utils.LocationRequestAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewAddLocationRequestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_view_add_location_request, container, false);

        // Back button
        view.findViewById(R.id.back_btn).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        RecyclerView recyclerView = view.findViewById(R.id.viewlocreq_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        // Placeholder empty list for now — backend will populate this later
//        recyclerView.setAdapter(new LocationRequestAdapter(
//                new ArrayList<>(),
//                R.layout.item_view_add_location_request
//        ));

        // serves as a dummy so you can see something displayed
        List<Object> dummyList = new ArrayList<>();
        dummyList.add(new Object()); // dummy item 1
        dummyList.add(new Object()); // dummy item 2
        recyclerView.setAdapter(new LocationRequestAdapter(
                dummyList,
                R.layout.item_view_add_location_request
        ));

        return view;
    }
}