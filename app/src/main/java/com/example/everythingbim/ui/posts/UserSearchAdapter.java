package com.example.everythingbim.ui.posts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.UserWithProfile;

import java.util.List;

/**
 * User Search Adapter - Adapter for displaying user search results in a ListView.
 */
public class UserSearchAdapter extends ArrayAdapter<UserWithProfile> {

    public UserSearchAdapter(@NonNull Context context, @NonNull List<UserWithProfile> users) {
        super(context, 0, users);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_search_result, parent, false);
        }

        UserWithProfile userWithProfile = getItem(position);
        
        ImageView profilePic = convertView.findViewById(R.id.search_result_profile_pic);
        TextView username = convertView.findViewById(R.id.search_result_username);
        TextView userType = convertView.findViewById(R.id.search_result_user_type);

        if (userWithProfile != null) {
            username.setText(userWithProfile.user.username);
            userType.setText(userWithProfile.user.userType.toString());

            String photoUrl = null;
            if (userWithProfile.generalUser != null) {
                photoUrl = userWithProfile.generalUser.profilePictureUrl;
            } else if (userWithProfile.businessUser != null) {
                photoUrl = userWithProfile.businessUser.profilePictureUrl;
            }

            Glide.with(getContext())
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_user_circle)
                    .circleCrop()
                    .into(profilePic);
        }

        return convertView;
    }
}
