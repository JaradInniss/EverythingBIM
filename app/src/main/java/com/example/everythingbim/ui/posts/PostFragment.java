package com.example.everythingbim.ui.posts;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;

/**
 * Fragment that displays a grid of posts and provides search/filtering functionality.
 */
public class PostFragment extends Fragment {

    private ViewPostViewModel viewModel;
    private PostAdapter adapter;
    private UserSearchAdapter searchAdapter;
    private ListView searchResultsList;
    private RecyclerView recyclerView;
    private LinearLayout createPostButton;
    private LinearLayout searchBar;
    private EditText searchEditText;
    private CardView searchResultsCard;
    private TextView filterAccount;
    private TextView filterLocation;

    public PostFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ViewPostViewModel.class);

        // Find views by ID
        recyclerView = view.findViewById(R.id.posts_rv);
        createPostButton = view.findViewById(R.id.prev_bttn2);
        searchBar = view.findViewById(R.id.posts_search_bar);
        searchEditText = view.findViewById(R.id.posts_search_et);
        searchResultsCard = view.findViewById(R.id.posts_search_results_card);
        searchResultsList = view.findViewById(R.id.posts_search_results_list);
        filterAccount = view.findViewById(R.id.posts_search_filter_account);
        filterLocation = view.findViewById(R.id.posts_search_filter_location);

        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    /**
     * Configures the RecyclerView with a GridLayoutManager and sets up the click listener for posts.
     */
    private void setupRecyclerView() {
        adapter = new PostAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns grid
        recyclerView.setAdapter(adapter);

        // Handle post selection: navigate to ViewPost activity
        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(getActivity(), ViewPost.class);
            intent.putExtra("POST_ID", post.postId);
            startActivity(intent);
        });
    }

    /**
     * Observes LiveData from the ViewModel to update the UI when data changes.
     */
    private void setupObservers() {
        // Observe the list of posts
        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                adapter.setPosts(posts);
            }
        });

        // Observe the current filter type (Account vs Location)
        viewModel.getFilterType().observe(getViewLifecycleOwner(), type -> {
            updateFilterUI(type);
        });

        // Observe search input
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String query = charSequence.toString();
                if ("account".equals(viewModel.getFilterType().getValue())) {
                    performUserSearch(query);
                }
            }
        });
    }

    /**
     * Sets up click listeners for the search bar, filter buttons, and create post button.
     */
    private void setupListeners() {
        // Navigate to create post screen
        createPostButton.setOnClickListener(v -> {
            if (isGuestUser()) {
                showAuthRequiredDialog();
                return;
            }
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        // Handle account selection from search
        searchResultsList.setOnItemClickListener((parent, view, position, id) -> {
            if ("account".equals(viewModel.getFilterType().getValue())) {
                UserWithProfile selected = (UserWithProfile) parent.getItemAtPosition(position);
                Intent intent = new Intent(getActivity(), ViewUserProfileActivity.class);
                intent.putExtra("USER_ID", selected.user.userId);
                startActivity(intent);

                // Cleanup UI
                searchEditText.clearFocus();
                searchResultsCard.setVisibility(View.GONE);
            }
        });

        // Show search results when search bar gains focus
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchResultsCard.setVisibility(View.VISIBLE);
            }
        });

        // Show search results on click
        searchEditText.setOnClickListener(v -> searchResultsCard.setVisibility(View.VISIBLE));

        // Handle filter type selection
        filterAccount.setOnClickListener(v -> viewModel.setFilterType("account"));
        filterLocation.setOnClickListener(v -> viewModel.setFilterType("location"));
    }

    /**
     * Updates the visual state of the filter buttons based on the selected type.
     * @param type The active filter type ("account" or "location").
     */
    private void updateFilterUI(String type) {
        if ("account".equals(type)) {
            filterAccount.setBackgroundResource(R.drawable.bg_search_filter_active);
            filterAccount.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            
            filterLocation.setBackgroundResource(R.drawable.bg_search_filter_inactive);
            filterLocation.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
        } else {
            filterLocation.setBackgroundResource(R.drawable.bg_search_filter_active);
            filterLocation.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            
            filterAccount.setBackgroundResource(R.drawable.bg_search_filter_inactive);
            filterAccount.setTextColor(ContextCompat.getColor(getContext(), R.color.black));
        }
    }

    /**
     * Performs a search for users based on the provided query.
     * @param query The search query.
     */
    private void performUserSearch(String query) {
        viewModel.searchUsers(query).observe(getViewLifecycleOwner(), users -> {
            if (users != null && "account".equals(viewModel.getFilterType().getValue())) {
                searchAdapter = new UserSearchAdapter(getContext(), users);
                searchResultsList.setAdapter(searchAdapter);
                searchResultsCard.setVisibility(users.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
    }

    private boolean isGuestUser() {
        SharedPreferences preferences = requireActivity()
                .getSharedPreferences("app_prefs", requireActivity().MODE_PRIVATE);
        String userType = preferences.getString("userType", MainActivity.USER_TYPE_GUEST);
        return MainActivity.USER_TYPE_GUEST.equals(userType);
    }

    private void showAuthRequiredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log in to create a post")
                .setMessage("Guests can browse posts, but you'll need an account before sharing your own.")
                .setPositiveButton("Log In", (dialog, which) ->
                        startActivity(buildLoginIntent()))
                .setNegativeButton("Create Account", (dialog, which) ->
                        startActivity(buildGeneralRegistrationIntent()))
                .setNeutralButton("Not now", null)
                .show();
    }

    private Intent buildLoginIntent() {
        Intent intent = new Intent(requireContext(), Login.class);
        intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_CREATE_POST);
        return intent;
    }

    private Intent buildGeneralRegistrationIntent() {
        Intent intent = new Intent(requireContext(), GeneralRegistration.class);
        intent.putExtra(MainActivity.EXTRA_PENDING_ACTION, MainActivity.ACTION_CREATE_POST);
        return intent;
    }
}
