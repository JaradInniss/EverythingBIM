package com.example.everythingbim;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminFragment extends Fragment {

    private BottomNavigationView bottomNavigationView;
    private AdminViewModel viewModel;
    private AdminReportsFragment reportsFragment;

    public void navigateToReportDetail(
            String docId, String number, String title, String type,
            String severity, String date, String reportedUser,
            String caption, String imageUrl, String status) {

        AdminReportDetailFragment detailFragment = AdminReportDetailFragment.newInstance(
                docId, number, title, type, severity, date, reportedUser, caption, imageUrl, status);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate to AdminUserRequestsFragment for View All
    public void navigateToUserRequests(String requestType) {
        AdminUserRequestsFragment requestsFragment = AdminUserRequestsFragment.newInstance(requestType);
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, requestsFragment)
                .addToBackStack(null)
                .commit();
    }

    // Navigate to location or info request detail
    public void navigateToLocationRequestDetail(String docId) {
        AdminLocationRequestDetailsFragment detailFragment = AdminLocationRequestDetailsFragment.newInstance(docId);
        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.admin_fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToInfoRequestDetail(String docId) {
        AdminInfoRequestDetailFragment detailFragment = AdminInfoRequestDetailFragment.newInstance(docId);
        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.admin_fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    public void navigateToBizVerificationDetail(String docId) {
        AdminBizVerificationDetailFragment detailFragment = AdminBizVerificationDetailFragment.newInstance(docId);
        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.admin_fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    // Unified navigation for request details
    public void navigateToRequestDetail(String requestType, String docId) {
        if ("location".equals(requestType)) {
            navigateToLocationRequestDetail(docId);
        } else if ("info".equals(requestType)) {
            navigateToInfoRequestDetail(docId);
        } else if ("business_verification".equals(requestType)) {
            navigateToBizVerificationDetail(docId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout first
        View view = inflater.inflate(R.layout.fragment_admin, container, false);

        // Now find the BottomNavigationView from the inflated view
        bottomNavigationView = view.findViewById(R.id.admin_navigation_bar);

        // Initialize ViewModel with fragment's lifecycle
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // Set up the listener (after observer is ready)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            viewModel.setNavbarItemId(item.getItemId());
            return true;
        });

        // Observe the selected item
        setupObservers();

        // Set a default selection (this will trigger the first fragment load)
        viewModel.setNavbarItemId(R.id.admin_navbar_home);

        return view;
    }

    private void setupObservers() {
        viewModel.getNavbarItemId().observe(getViewLifecycleOwner(), id -> {
            Fragment fragment = null;
            if (id == R.id.admin_navbar_home) {
                fragment = new AdminHomeFragment();
            } else if (id == R.id.admin_navbar_settings) {
                fragment = new AdminSettingsFragment();
            } else if (id == R.id.admin_navbar_user) {
                fragment = new AdminUserFragment();
            } else if (id == R.id.admin_navbar_reports) {
                reportsFragment = new AdminReportsFragment();
                fragment = reportsFragment;
            }

            if (fragment != null) {
                loadFragment(fragment);
            }

            // Sync the BottomNavigationView selection (avoid loop)
            if (bottomNavigationView.getSelectedItemId() != id) {
                bottomNavigationView.setSelectedItemId(id);
            }
        });
    }

    private void loadFragment(Fragment fragment) {
                getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .commit();
    }
}