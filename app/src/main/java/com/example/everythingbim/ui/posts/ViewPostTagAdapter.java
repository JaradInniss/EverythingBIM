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

public class ViewPostTagAdapter extends RecyclerView.Adapter<ViewPostTagAdapter.TaggedUserViewHolder> {

    private List<UserEntity> taggedUsers = new ArrayList<>();
    private OnTaggedUserClickListener clickListener;

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
