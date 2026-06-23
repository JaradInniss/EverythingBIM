package com.example.everythingbim.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.repository.PostRepository;
import com.example.everythingbim.ui.posts.PostAdapter;
import com.example.everythingbim.ui.posts.ViewPost;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Admin fragment for moderating posts.
 * Shows all posts from all users with delete capability.
 */
public class AdminModeratePostsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private PostRepository postRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_moderate_posts, container, false);

        recyclerView = view.findViewById(R.id.moderate_posts_recycler);
        postRepository = new PostRepository(requireContext());

        setupRecyclerView();
        loadPosts();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter();
        // Enable admin mode to show options button on ALL posts
        adapter.setAdminMode(true);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeaderPosition(position) ? 3 : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);

        // Handle post click - open ViewPost in admin mode
        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(getActivity(), ViewPost.class);
            intent.putExtra("POST_ID", post.postId);
            intent.putExtra(ViewPost.EXTRA_IS_ADMIN_MODE, true);
            startActivity(intent);
        });

        // Handle post delete via 3-dot menu
        adapter.setOnPostOptionsClickListener(new PostAdapter.OnPostOptionsClickListener() {
            @Override
            public void onEditPost(PostEntity post) {
                // Admin can edit posts too if needed
                Toast.makeText(requireContext(), "Admin edit not implemented", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeletePost(PostEntity post) {
                confirmDeletePost(post);
            }
        });
    }

    private void loadPosts() {
        // Get all posts (same as home feed - randomized order)
        LiveData<List<PostEntity>> postsLiveData = postRepository.getRandomizedPosts();
        postsLiveData.observe(getViewLifecycleOwner(), posts -> {
            if (posts != null && !posts.isEmpty()) {
                adapter.setPosts(posts);
            }
        });
    }

    private void confirmDeletePost(PostEntity post) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost(post))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost(PostEntity post) {
        postRepository.deletePost(post).observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                // Posts will refresh automatically via Firestore listener
            } else {
                Toast.makeText(requireContext(), "Failed to delete post", Toast.LENGTH_SHORT).show();
            }
        });
    }
}