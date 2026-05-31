package com.example.everythingbim.ui.posts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserEntity;

import java.util.ArrayList;
import java.util.List;

public class UserTagAdapter extends RecyclerView.Adapter<UserTagAdapter.UserTagViewHolder> {

    private List<UserEntity> taggedUsers = new ArrayList<>();
    private final OnRemoveClickListener removeClickListener;

    public interface OnRemoveClickListener {
        void onRemoveClick(UserEntity user);
    }

    public UserTagAdapter(OnRemoveClickListener removeClickListener) {
        this.removeClickListener = removeClickListener;
    }

    public void setTaggedUsers(List<UserEntity> users) {
        this.taggedUsers = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserTagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_tag, parent, false);
        return new UserTagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserTagViewHolder holder, int position) {
        UserEntity user = taggedUsers.get(position);
        holder.usernameTv.setText(user.username);
        holder.removeBttn.setOnClickListener(v -> {
            if (removeClickListener != null) {
                removeClickListener.onRemoveClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taggedUsers.size();
    }

    static class UserTagViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTv;
        ImageView removeBttn;

        public UserTagViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.tagged_user_tv);
            removeBttn = itemView.findViewById(R.id.remove_tagged_user_bttn);
        }
    }
}
