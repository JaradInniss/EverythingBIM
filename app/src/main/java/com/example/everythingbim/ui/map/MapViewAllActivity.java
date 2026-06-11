package com.example.everythingbim.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.posts.ViewPost;

public class MapViewAllActivity extends AppCompatActivity implements View.OnClickListener {

    private MapViewAllViewModel viewModel;
    private GalleryAdapter imagesAdapter;
    private ReviewsAdapter reviewsAdapter;
    private PostsAdapter postsAdapter;

    private RecyclerView imagesRv, reviewsRv, postsRv;
    private TextView locationNameTv, typeTv, countTv, overallRatingTv;
    private LinearLayout returnBttn, overallRatingContainer, filterContainer;
    private View filterBttn1, filterBttn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map_view_all);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUi();
        setupViewModel();
        handleIntentData();
    }

    private void initUi() {
        locationNameTv = findViewById(R.id.viewall_location_name_tv);
        typeTv = findViewById(R.id.viewall_type_tv);
        countTv = findViewById(R.id.viewall_type_count_tv);
        overallRatingTv = findViewById(R.id.viewall_location_overall_rating_tv);
        overallRatingContainer = findViewById(R.id.viewall_overall_rating_container);

        filterBttn1 = findViewById(R.id.viewall_filter_bttn_unselected);
        filterBttn1.setOnClickListener(this);
        filterBttn2 = findViewById(R.id.viewall_filter_bttn_selected);
        filterBttn2.setOnClickListener(this);
        filterContainer = findViewById(R.id.viewall_filter_container);
        returnBttn = findViewById(R.id.return_bttn);
        returnBttn.setOnClickListener(this);

        imagesRv = findViewById(R.id.viewall_images_rv);
        reviewsRv = findViewById(R.id.viewall_reviews_rv);
        postsRv = findViewById(R.id.viewall_posts_rv);

        // Setup Adapters
        imagesAdapter = new GalleryAdapter(this, post -> {});
        reviewsAdapter = new ReviewsAdapter(this);
        postsAdapter = new PostsAdapter(this);
        postsAdapter.setOnPostClickListener(post -> {
            Intent intent = new Intent(this, ViewPost.class);
            intent.putExtra("POST_ID", post.postId);
            startActivity(intent);
        });

        // Setup RecyclerViews
        imagesRv.setLayoutManager(new GridLayoutManager(this, 3));
        imagesRv.setAdapter(imagesAdapter);

        reviewsRv.setLayoutManager(new LinearLayoutManager(this));
        reviewsRv.setAdapter(reviewsAdapter);

        postsRv.setLayoutManager(new GridLayoutManager(this, 2));
        postsRv.setAdapter(postsAdapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MapViewAllViewModel.class);

        viewModel.getPosts().observe(this, posts -> {
            if (posts != null) {
                imagesAdapter.setPosts(posts);
                postsAdapter.setPosts(posts);
                updateCount(posts.size());
            }
        });

        viewModel.getLikedPostIds().observe(this, likedPostIds -> {
            if (likedPostIds != null) {
                postsAdapter.setLikedPostIds(likedPostIds);
            }
        });

        viewModel.getReviews().observe(this, reviews -> {
            if (reviews != null) {
                reviewsAdapter.setReviews(reviews);
                updateCount(reviews.size());
            }
        });

        viewModel.getOverallRating().observe(this, rating -> {
            overallRatingTv.setText(String.valueOf(rating));
        });
    }

    private void handleIntentData() {
        long locationId = getIntent().getLongExtra("LOCATION_ID", -1);
        String locationName = getIntent().getStringExtra("LOCATION_NAME");
        String viewType = getIntent().getStringExtra("VIEW_TYPE");

        if (locationName != null) locationNameTv.setText(locationName);
        if (viewType != null) {
            typeTv.setText(viewType);
            switchUiMode(viewType);
        }

        viewModel.setLocationId(locationId, viewType);
    }

    private void switchUiMode(String viewType) {
        // Reset visibilities
        imagesRv.setVisibility(View.GONE);
        reviewsRv.setVisibility(View.GONE);
        postsRv.setVisibility(View.GONE);
        overallRatingContainer.setVisibility(View.GONE);
        filterBttn1.setVisibility(View.GONE);

        if ("IMAGES".equalsIgnoreCase(viewType)) {
            imagesRv.setVisibility(View.VISIBLE);
        }
        else if ("REVIEWS".equalsIgnoreCase(viewType)) {
            reviewsRv.setVisibility(View.VISIBLE);
            overallRatingContainer.setVisibility(View.VISIBLE);
            filterBttn1.setVisibility(View.VISIBLE);
        }
        else if ("POSTS".equalsIgnoreCase(viewType)) {
            postsRv.setVisibility(View.VISIBLE);
        }
    }

    private void updateCount(int size) {
        countTv.setText(String.valueOf(size));
    }

    @Override
    public void onClick(View view) {
        int bttnId = view.getId();

        if (bttnId == R.id.viewall_filter_bttn_unselected) {
            filterContainer.setVisibility(View.VISIBLE);
        }
        else if (bttnId == R.id.viewall_filter_bttn_selected) {
            filterContainer.setVisibility(View.GONE);
        }
        else if (bttnId == R.id.return_bttn) {
            finish();
        }
    }
}
