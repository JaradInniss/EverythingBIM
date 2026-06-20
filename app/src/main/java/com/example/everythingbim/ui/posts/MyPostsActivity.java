package com.example.everythingbim.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity implements View.OnClickListener {

    private PostViewModel viewModel;
    private PostAdapter adapter;
    private RecyclerView postsRv;
    private TextView countTv;
    private TextView emptyTv;
    private View returnBttn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_posts);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUi();
        setupRecycler();
        setupViewModel();
        observePosts();
    }

    private void initUi() {
        returnBttn = findViewById(R.id.return_bttn);
        countTv = findViewById(R.id.my_posts_count_tv);
        emptyTv = findViewById(R.id.my_posts_empty_tv);
        postsRv = findViewById(R.id.my_posts_rv);

        returnBttn.setOnClickListener(this);
    }

    private void setupRecycler() {
        adapter = new PostAdapter();
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeaderPosition(position) ? 3 : 1;
            }
        });
        postsRv.setLayoutManager(layoutManager);
        postsRv.setAdapter(adapter);

        adapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, ViewPost.class);
            intent.putExtra("POST_ID", post.postId);
            startActivity(intent);
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PostViewModel.class);
    }

    private void observePosts() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null || currentUser.getUid().trim().isEmpty()) {
            renderPosts(Collections.emptyList());
            return;
        }

        viewModel.setAuthorUidFilter(currentUser.getUid());
        viewModel.getPostsByAuthorUid().observe(this, this::renderPosts);
    }

    private void renderPosts(@Nullable List<PostEntity> posts) {
        List<PostEntity> safePosts = posts != null ? posts : Collections.emptyList();
        adapter.setPosts(safePosts);
        countTv.setText(String.valueOf(safePosts.size()));
        emptyTv.setVisibility(safePosts.isEmpty() ? View.VISIBLE : View.GONE);
        postsRv.setVisibility(safePosts.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.return_bttn) {
            finish();
        }
    }
}
