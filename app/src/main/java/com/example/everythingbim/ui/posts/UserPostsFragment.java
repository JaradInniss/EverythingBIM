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
import com.example.everythingbim.data.repository.PostRepository;


// Fragment for User Posts Info in TabLayout in ViewUserProfile

public class UserPostsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_AUTHOR_UID = "author_uid";
    private long userId;
    private String authorUid;
    private PostAdapter adapter;
    private ViewUserProfileViewModel viewModel;


    public static UserPostsFragment newInstance(long userId) {
        UserPostsFragment fragment = new UserPostsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    public static UserPostsFragment newInstance(long userId, String authorUid) {
        UserPostsFragment fragment = new UserPostsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putString(ARG_AUTHOR_UID, authorUid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID, -1);
            authorUid = getArguments().getString(ARG_AUTHOR_UID);
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
        if (recyclerView == null) return;

        adapter = new PostAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(ViewUserProfileViewModel.class);
        if (viewModel == null) return;

        // If we have an authorUid, load posts by UID; otherwise load by local userId
        if (authorUid != null && !authorUid.isEmpty()) {
            try {
                viewModel.getPostsByAuthorUid(authorUid).observe(getViewLifecycleOwner(), posts -> {
                    if (posts != null && adapter != null) {
                        adapter.setPosts(posts);
                    }
                });
            } catch (Exception e) {
                // Handle error silently
            }
        } else if (userId > 0) {
            try {
                viewModel.getUserPosts().observe(getViewLifecycleOwner(), posts -> {
                    if (posts != null && adapter != null) {
                        adapter.setPosts(posts);
                    }
                });
            } catch (Exception e) {
                // Handle error silently
            }
        }
    }
}
