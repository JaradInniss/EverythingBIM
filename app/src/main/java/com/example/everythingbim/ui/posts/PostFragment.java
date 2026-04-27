package com.example.everythingbim.ui.posts;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.everythingbim.R;

public class PostFragment extends Fragment {

    private PostViewModel viewModel;
    private PostAdapter adapter;
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
        searchResultsCard = view.findViewById(R.id.posts_search_results_card);
        filterAccount = view.findViewById(R.id.posts_search_filter_account);
        filterLocation = view.findViewById(R.id.posts_search_filter_location);

        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);

        adapter.setOnPostClickListener(post -> {
            // TODO: Navigate to post viewing screen
            Toast.makeText(getContext(), "Post clicked: " + post.postId, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupObservers() {
        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                adapter.setPosts(posts);
            }
        });

        viewModel.getFilterType().observe(getViewLifecycleOwner(), type -> {
            updateFilterUI(type);
        });
    }

    private void setupListeners() {
        createPostButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchResultsCard.setVisibility(View.VISIBLE);
            }
        });

        // Also show when clicked if already focused or to ensure visibility
        searchEditText.setOnClickListener(v -> searchResultsCard.setVisibility(View.VISIBLE));

        filterAccount.setOnClickListener(v -> viewModel.setFilterType("account"));
        filterLocation.setOnClickListener(v -> viewModel.setFilterType("location"));
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
}
