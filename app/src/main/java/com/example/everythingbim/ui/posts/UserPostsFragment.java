package com.example.everythingbim.ui.posts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;

public class UserPostsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private long userId;
    private PostAdapter adapter;
    private ViewPostViewModel viewModel;

    public static UserPostsFragment newInstance(long userId) {
        UserPostsFragment fragment = new UserPostsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        RecyclerView recyclerView = view.findViewById(R.id.user_posts_rv);
        adapter = new PostAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ViewPostViewModel.class);
        
        // Use the existing getPostsByUserId in ViewModel/Repository if it exists
        // For now, assuming ViewModel can filter or we add a specific method
        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                // Filter posts by userId locally if repository doesn't have a specific method yet
                // In a real scenario, the repository should handle this.
                adapter.setPosts(posts); // Placeholder: replace with filtered list
            }
        });
    }
}
