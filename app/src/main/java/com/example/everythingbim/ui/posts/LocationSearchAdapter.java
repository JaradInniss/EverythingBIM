package com.example.everythingbim.ui.posts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.LocationEntity;

import java.util.List;

public class LocationSearchAdapter extends ArrayAdapter<String> {

    public LocationSearchAdapter(@NonNull Context context, @NonNull List<String> locations) {
        super(context, android.R.layout.simple_list_item_1, locations);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        String location = getItem(position);
        TextView textView = convertView.findViewById(android.R.id.text1);

        if (location != null) {
            textView.setText(location.trim().toString());
            textView.setTextColor(getContext().getResources().getColor(R.color.black));
            textView.setTextSize(14);
        }

        return convertView;
    }
}
