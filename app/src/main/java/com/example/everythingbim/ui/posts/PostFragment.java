package com.example.everythingbim.ui.posts;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.ui.login.Login;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.registration.GeneralRegistration;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fragment that displays a grid of posts and provides search/filtering functionality.
 */
public class PostFragment extends Fragment {
    private static final String PREF_POST_SEARCH_SCROLL_HINT_SEEN = "post_search_scroll_hint_seen";

    private PostViewModel viewModel;
    private PostAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout createPostButton;
    private LinearLayout searchBar;
    private EditText searchEditText;
    private ImageView searchButton;
    private CardView searchResultsCard;
    private ListView searchResultsList;
    private TextView filterAccount;
    private TextView filterLocation;
    private ArrayAdapter<String> searchResultsAdapter;
    private final List<String> activeSearchResults = new ArrayList<>();
    private final List<PostEntity> allPosts = new ArrayList<>();
    private final List<LocationEntity> allLocations = new ArrayList<>();

    public PostFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

        recyclerView = view.findViewById(R.id.posts_rv);
        createPostButton = view.findViewById(R.id.prev_bttn2);
        searchBar = view.findViewById(R.id.posts_search_bar);
        searchEditText = view.findViewById(R.id.posts_search_et);
        searchButton = view.findViewById(R.id.posts_search_bttn);
        searchResultsCard = view.findViewById(R.id.posts_search_results_card);
        searchResultsList = view.findViewById(R.id.posts_search_results_list);
        filterAccount = view.findViewById(R.id.posts_search_filter_account);
        filterLocation = view.findViewById(R.id.posts_search_filter_location);

        searchResultsAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, activeSearchResults);
        searchResultsList.setAdapter(searchResultsAdapter);

        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);
        int initialLeft = recyclerView.getPaddingLeft();
        int initialTop = recyclerView.getPaddingTop();
        int initialRight = recyclerView.getPaddingRight();
        int initialBottom = recyclerView.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                requireView(),
                searchEditText,
                recyclerView,
                PREF_POST_SEARCH_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> {
                    recyclerView.setClipToPadding(false);
                    recyclerView.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + keyboardExtraBottom
                    );
                }
        );

        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(getActivity(), ViewPost.class);
            intent.putExtra("POST_ID", post.postId);
            startActivity(intent);
        });
    }

    private void setupObservers() {
        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            allPosts.clear();
            if (posts != null) {
                allPosts.addAll(posts);
            }
            applySearchAndSuggestions();
        });

        viewModel.getLocations().observe(getViewLifecycleOwner(), locations -> {
            allLocations.clear();
            if (locations != null) {
                allLocations.addAll(locations);
            }
            applySearchAndSuggestions();
        });

        viewModel.getFilterType().observe(getViewLifecycleOwner(), type -> {
            updateFilterUI(type);
            applySearchAndSuggestions();
        });
    }

    private void setupListeners() {
        createPostButton.setOnClickListener(v -> {
            if (!isPostingAuthorized()) {
                showAuthRequiredDialog();
                return;
            }
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                applySearchAndSuggestions();
            } else {
                searchResultsCard.setVisibility(View.GONE);
            }
        });

        searchEditText.setOnClickListener(v -> applySearchAndSuggestions());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applySearchAndSuggestions();
            }
        });

        filterAccount.setOnClickListener(v -> viewModel.setFilterType("account"));
        filterLocation.setOnClickListener(v -> viewModel.setFilterType("location"));

        searchButton.setOnClickListener(v -> {
            applySearchAndSuggestions();
            searchResultsCard.setVisibility(View.GONE);
        });

        searchResultsList.setOnItemClickListener((parent, view, position, id) -> {
            String selected = activeSearchResults.get(position);
            searchEditText.setText(selected);
            searchEditText.setSelection(selected.length());
            applySearchAndSuggestions();
            searchResultsCard.setVisibility(View.GONE);
            searchEditText.clearFocus();
        });
    }

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

    private void applySearchAndSuggestions() {
        String query = searchEditText.getText() == null
                ? ""
                : searchEditText.getText().toString().trim();
        boolean accountMode = "account".equals(viewModel.getFilterType().getValue());

        adapter.setPosts(filterPosts(query, accountMode));
        updateSuggestions(query, accountMode);
    }

    private List<PostEntity> filterPosts(String query, boolean accountMode) {
        if (query.isEmpty()) {
            return new ArrayList<>(allPosts);
        }

        String normalizedQuery = query.toLowerCase(Locale.US);
        List<PostEntity> filtered = new ArrayList<>();
        for (PostEntity post : allPosts) {
            if (accountMode) {
                String authorLabel = buildAuthorLabel(post.authorId);
                if (authorLabel.toLowerCase(Locale.US).contains(normalizedQuery)) {
                    filtered.add(post);
                }
            } else {
                String locationName = getLocationName(post);
                if (locationName.toLowerCase(Locale.US).contains(normalizedQuery)) {
                    filtered.add(post);
                }
            }
        }
        return filtered;
    }

    private void updateSuggestions(String query, boolean accountMode) {
        activeSearchResults.clear();

        if (!query.isEmpty()) {
            String normalizedQuery = query.toLowerCase(Locale.US);
            Set<String> uniqueSuggestions = new LinkedHashSet<>();

            if (accountMode) {
                for (PostEntity post : allPosts) {
                    String authorLabel = buildAuthorLabel(post.authorId);
                    if (authorLabel.toLowerCase(Locale.US).contains(normalizedQuery)) {
                        uniqueSuggestions.add(authorLabel);
                    }
                }
            } else {
                for (LocationEntity location : allLocations) {
                    if (location == null || location.name == null) {
                        continue;
                    }
                    if (location.name.toLowerCase(Locale.US).contains(normalizedQuery)) {
                        uniqueSuggestions.add(location.name);
                    }
                }
            }

            activeSearchResults.addAll(uniqueSuggestions);
        }

        searchResultsAdapter.notifyDataSetChanged();
        boolean shouldShow = searchEditText.hasFocus();
        searchResultsCard.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    private String buildAuthorLabel(long authorId) {
        for (PostEntity post : allPosts) {
            if (post.authorId == authorId && post.authorName != null && !post.authorName.trim().isEmpty()) {
                return post.authorName.trim();
            }
        }
        return "User " + authorId;
    }

    private String getLocationName(@NonNull PostEntity post) {
        for (LocationEntity location : allLocations) {
            if (location != null && location.locationId == post.locationId) {
                return location.name != null ? location.name : "Unknown location";
            }
        }
        if (post.locationName != null && !post.locationName.trim().isEmpty()) {
            return post.locationName.trim();
        }
        return "Unknown location";
    }
    private boolean isPostingAuthorized() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return false;
        }
        return !isGuestUser();
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
