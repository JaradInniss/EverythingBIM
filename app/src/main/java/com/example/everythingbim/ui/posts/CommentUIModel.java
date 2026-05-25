package com.example.everythingbim.ui.posts;

import com.example.everythingbim.data.local.entities.CommentEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * UI Model class representing a comment and its nested replies.
 * Used to manage the hierarchical state and UI properties (like expansion) for threaded comments.
 */
public class CommentUIModel {
    private final CommentEntity comment;
    private final List<CommentUIModel> replies;
    private boolean isExpanded = false;

    public CommentUIModel(CommentEntity comment) {
        this.comment = comment;
        this.replies = new ArrayList<>();
    }

    /**
     * Returns the underlying comment entity data.
     */
    public CommentEntity getComment() {
        return comment;
    }

    /**
     * Returns the list of immediate replies to this comment.
     */
    public List<CommentUIModel> getReplies() {
        return replies;
    }

    /**
     * Adds a reply to this comment's list of replies.
     */
    public void addReply(CommentUIModel reply) {
        this.replies.add(reply);
    }

    /**
     * Checks if the replies for this comment are currently expanded in the UI.
     */
    public boolean isExpanded() {
        return isExpanded;
    }

    /**
     * Sets whether the replies for this comment should be expanded in the UI.
     */
    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    /**
     * Recursively calculates the total count of all nested replies in the hierarchy below this comment.
     * @return Total number of replies at all levels.
     */
    public int getTotalRepliesCount() {
        int count = replies.size();
        for (CommentUIModel reply : replies) {
            count += reply.getTotalRepliesCount();
        }
        return count;
    }
}
