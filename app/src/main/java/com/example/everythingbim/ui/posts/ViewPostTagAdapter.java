package com.example.everythingbim.ui.posts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only adapter used by {@link ViewPost} to render the list of users
 * tagged in a post. Unlike {@link UserTagAdapter} (which is used in the
 * Create Post flow and supports removing tags), this adapter has no remove
 * button and only forwards click events to the activity.
 *
 * <p>The list item layout is {@code R.layout.item_viewpost_user_tag}, and
 * the username is bound into the {@code tagged_user_name_tv} TextView.</p>
 */
public class ViewPostTagAdapter extends RecyclerView.Adapter<ViewPostTagAdapter.TaggedUserViewHolder> {

    private List<UserEntity> taggedUsers = new ArrayList<>();
    private OnTaggedUserClickListener clickListener;

    /**
     * Click listener that hands the underlying {@link UserEntity} back to the
     * caller. The caller is responsible for deciding how to navigate (e.g.
     * using the local {@code userId} when available, or the
     * {@code firebaseUid} when the user is not in the local Room cache).
     */
    public interface OnTaggedUserClickListener {
        void onTaggedUserClick(UserEntity user);
    }

    public ViewPostTagAdapter(OnTaggedUserClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setTaggedUsers(List<UserEntity> users) {
        this.taggedUsers = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaggedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_viewpost_user_tag, parent, false);
        return new TaggedUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaggedUserViewHolder holder, int position) {
        UserEntity user = taggedUsers.get(position);
        holder.usernameTv.setText(user.username);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaggedUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taggedUsers.size();
    }

    static class TaggedUserViewHolder extends RecyclerView.ViewHolder {
        final TextView usernameTv;

        TaggedUserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.tagged_user_name_tv);
        }
    }
}
