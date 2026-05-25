package com.example.everythingbim.ui.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.BusinessRegistration;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddBusinessLocationRequestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddBusinessLocationRequestFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AddBusinessLocationRequestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AddBusinessLocationRequestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddBusinessLocationRequestFragment newInstance(String param1, String param2) {
        AddBusinessLocationRequestFragment fragment = new AddBusinessLocationRequestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_business_location_request, container, false);
        if (isGuestUser()) {
            showAuthRequiredDialog();
        }
        return view;
    }

    private boolean isGuestUser() {
        SharedPreferences preferences = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userType = preferences.getString("userType", MainActivity.USER_TYPE_GUEST);
        return MainActivity.USER_TYPE_GUEST.equals(userType);
    }

    private void showAuthRequiredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log in to submit a business request")
                .setMessage("Guests can explore the app, but you'll need a business account before submitting this request.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), Login.class);
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_ADD_BUSINESS_LOCATION_REQUEST);
                    startActivity(intent);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setNegativeButton("Register Business", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), BusinessRegistration.class);
                    intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_ADD_BUSINESS_LOCATION_REQUEST);
                    startActivity(intent);
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .setOnCancelListener(dialog -> requireActivity().getSupportFragmentManager().popBackStack())
                .show();
    }
}
