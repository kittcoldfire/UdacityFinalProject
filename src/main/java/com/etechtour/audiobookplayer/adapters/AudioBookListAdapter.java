package com.etechtour.audiobookplayer.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.etechtour.audiobookplayer.R;
import com.etechtour.audiobookplayer.Utilities;
import com.etechtour.audiobookplayer.database.AudioBookContract;

/**
 * Created by Phil on 4/30/2016.
 */
public class AudioBookListAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private ViewHolder holder;
    private ArrayMap durations = new ArrayMap();

    public AudioBookListAdapter(Context context, Cursor c) {
        super(context, c);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class ViewHolder{
        public ImageView imgAlbum;
        public TextView txtAlbum, txtDuration;
        public RelativeLayout item;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        View v = view;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_audiobook_item, null);
            holder = new ViewHolder();
            holder.txtAlbum = (TextView) v.findViewById(R.id.list_audiobook_album);
            holder.txtDuration = (TextView) v.findViewById(R.id.list_audiobook_duration);
            holder.imgAlbum = (ImageView) v.findViewById(R.id.list_audiobook_albumart);
            holder.item = (RelativeLayout) v.findViewById(R.id.list_audiobook_item);
            v.setTag(holder);
        } else {
            holder=(ViewHolder)v.getTag();
        }

        long albumId = cursor.getLong(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID));
        String album = cursor.getString(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM));
        String albumFull = null;
        String userArt = cursor.getString(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ART));
        //Cursor mCursor = MApp.database.getMusicByColumn(MySQLiteHelper.TAC_COLUMN_ALBUM_ID, albumId, null);

        if (null != album && album.length() > 0 )
        {
            int endIndex = album.lastIndexOf("/");
            if (endIndex != -1)
            {
                album = album.substring(0, endIndex); // not forgot to put check if(endIndex != -1)
                endIndex = album.lastIndexOf("/");
                if (endIndex != -1)
                {
                    albumFull = album.substring(endIndex + 1, album.length());
                }
            } else {
                albumFull = album;
            }
        }

        holder.txtAlbum.setText(albumFull);

        if(userArt != null && !userArt.equals("")) {
            Glide
                    .with(context)
                    .load(userArt)
                    .placeholder(R.drawable.placeholder)
                    .crossFade()
                    .into(holder.imgAlbum);
        } else {
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);
            Glide
                    .with(context)
                    .load(albumArtUri)
                    .placeholder(R.drawable.placeholder)
                    .crossFade()
                    .into(holder.imgAlbum);
        }

        if(durations.isEmpty()) {
            durations = Utilities.getTotalBookDuration(context, cursor);
        }

        holder.txtDuration.setText(Utilities.milliSecondsToTime((Long) durations.get(albumId)));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.list_audiobook_item, parent, false);

        ViewHolder holder;
        holder = new ViewHolder();
        holder.txtAlbum = (TextView) v.findViewById(R.id.list_audiobook_album);
        holder.txtDuration = (TextView) v.findViewById(R.id.list_audiobook_duration);
        holder.imgAlbum = (ImageView) v.findViewById(R.id.list_audiobook_albumart);
        holder.item = (RelativeLayout) v.findViewById(R.id.list_audiobook_item);
        v.setTag(holder);

        return v;
    }
}
