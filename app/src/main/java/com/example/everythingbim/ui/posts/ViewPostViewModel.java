package com.example.everythingbim.ui.posts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.RequestReportStatus;
import com.example.everythingbim.data.repository.PostRepository;
import com.example.everythingbim.data.repository.UserRepository;
import com.example.everythingbim.ui.utils.NavigationCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * ViewPost ViewModel for managing post-related data and logic.
 * Handles fetching posts, filtering, and building the comment hierarchy.
 */


public class ViewPostViewModel extends AndroidViewModel {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LiveData<List<PostEntity>> posts;
    private final SingleLiveEvent<NavigationCommand> navigationEvent = new SingleLiveEvent<>();
    private final MutableLiveData<String> filterType = new MutableLiveData<>("account");

    public ViewPostViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        userRepository = new UserRepository(application);
        posts = postRepository.getRandomizedPosts();
        
        // Seed placeholder data if database is empty
        userRepository.seedUsersIfEmpty();
        postRepository.seedDataIfEmpty();
        
        // Repair any posts that still use temporary content URIs
        postRepository.repairLegacyPosts();
    }

     // Returns an observable list of randomized posts.
    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    // Fetches a specific post by its ID.
    public LiveData<PostEntity> getPostById(long postId) {
        return postRepository.getPostById(postId);
    }

    // Returns the current search filter type (e.g., "account" or "location").
    public LiveData<String> getFilterType() {
        return filterType;
    }

    public SingleLiveEvent<NavigationCommand> getNavigationEvent() { return navigationEvent; }

    // Updates the active search filter type.
    public void setFilterType(String type) {
        filterType.setValue(type);
    }

    // Search for Users
    public LiveData<List<UserWithProfile>> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return postRepository.getLocationById(locationId);
    }

    /**
     * Retrieves comments for a post and transforms them into a hierarchical UI model structure.
     * This method converts a flat list of comments from the database into a tree structure
     * where replies are nested within their parent comments.
     * 
     * @param postId The ID of the post to get comments for.
     * @return A LiveData list of top-level CommentUIModels, each containing its nested replies.
     */
    public LiveData<List<CommentUIModel>> getCommentsForPost(long postId) {
        return Transformations.map(postRepository.getCommentsForPost(postId), comments -> {
            if (comments == null) return new ArrayList<>();
            
            Map<Long, CommentUIModel> lookup = new HashMap<>();
            List<CommentUIModel> topLevelComments = new ArrayList<>();

            // First pass: Create UI models for all comments and store in a lookup map
            for (CommentEntity comment : comments) {
                lookup.put(comment.commentId, new CommentUIModel(comment));
            }

            // Second pass: Link replies to their parent comments to build the tree hierarchy
            for (CommentEntity comment : comments) {
                CommentUIModel uiModel = lookup.get(comment.commentId);
                if (comment.parentCommentId == null) {
                    // It's a top-level comment
                    topLevelComments.add(uiModel);
                } else {
                    // It's a reply; find the parent in the lookup map and add it
                    CommentUIModel parent = lookup.get(comment.parentCommentId);
                    if (parent != null) {
                        parent.addReply(uiModel);
                    }
                }
            }
            return topLevelComments;
        });
    }

    /**
     * Adds a new comment or reply to the database.
     * 
     * @param postId ID of the post.
     * @param parentCommentId ID of the parent comment (null if top-level).
     * @param authorName Name of the person commenting.
     * @param parentAuthorName Name of the author being replied to (for display purposes).
     * @param body The text content of the comment.
     */
    public void addComment(long postId, Long parentCommentId, String authorName, String parentAuthorName, String body) {
        CommentEntity comment = new CommentEntity(
                postId,
                parentCommentId,
                authorName,
                parentAuthorName,
                body,
                System.currentTimeMillis()
        );
        postRepository.insertComment(comment);
    }

    public void reportPost(long postId, long reporterId, String reason, String description) {
        ReportEntity report = new ReportEntity(postId, reporterId, reason, description, RequestReportStatus.PENDING, System.currentTimeMillis());
        postRepository.insertReport(report);
    }


    /**
     * SingleLiveEvent - ensures the event is delivered only once.
     */
    public static class SingleLiveEvent<T> extends MutableLiveData<T> {
        private final AtomicBoolean pending = new AtomicBoolean(false);

        @Override
        public void setValue(T value) {
            pending.set(true);
            super.setValue(value);
        }

        @Override
        public void observe(@NonNull androidx.lifecycle.LifecycleOwner owner,
                            @NonNull androidx.lifecycle.Observer<? super T> observer) {
            super.observe(owner, t -> {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            });
        }
    }
}
