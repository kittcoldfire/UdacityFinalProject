package com.etechtour.audiobookplayer.adapters;

import android.content.Context;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.etechtour.audiobookplayer.AlbumArt;
import com.etechtour.audiobookplayer.R;

import java.util.ArrayList;

/**
 * Created by Phil on 5/21/2016.
 */
public class AlbumArtAdapter extends ArrayAdapter<AlbumArt> {

    private LayoutInflater mInflater;
    private ArrayMap durations = new ArrayMap();

    public AlbumArtAdapter(Context context, ArrayList<AlbumArt> a) {
        super(context, 0, a);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class ViewHolder{
        public ImageView imgAlbum;
        public TextView txtAlbum;
    }

    @Override
    public AlbumArt getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        AlbumArt art = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.list_coverart_item, parent, false);
            viewHolder.imgAlbum = (ImageView) convertView.findViewById(R.id.item_album_image);
            viewHolder.txtAlbum = (TextView) convertView.findViewById(R.id.item_album_title);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String url = art.getUrl();
        String title = art.getTitle();

        // Populate the data into the template view using the data object
        Glide
                .with(getContext())
                .load(url)
                .placeholder(R.drawable.placeholder)
                .crossFade()
                .into(viewHolder.imgAlbum);
        viewHolder.txtAlbum.setText(title);
        // Return the completed view to render on screen
        return convertView;
    }
}
