package com.etechtour.audiobookplayer.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.etechtour.audiobookplayer.R;
import com.etechtour.audiobookplayer.Utilities;
import com.etechtour.audiobookplayer.database.AudioBookContract;

/**
 * Created by Phil on 5/18/2016.
 */
public class LeftDrawerListAdapter extends CursorAdapter {
    private LayoutInflater mInflater;
    private ViewHolder holder;
    private ArrayMap durations = new ArrayMap();

    public LeftDrawerListAdapter(Context context, Cursor c) {
        super(context, c);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class ViewHolder{
        public TextView txtTitle, txtDuration;
        public RelativeLayout item;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        View v = view;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.list_leftdrawer_item, null);
            holder = new ViewHolder();
            holder.txtTitle = (TextView) v.findViewById(R.id.list_leftdrawer_title);
            holder.txtDuration = (TextView) v.findViewById(R.id.list_leftdrawer_duration);
            holder.item = (RelativeLayout) v.findViewById(R.id.list_leftdrawer_item);
            v.setTag(holder);
        } else {
            holder=(ViewHolder)v.getTag();
        }

        String title = cursor.getString(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE));
        long albumId = cursor.getLong(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID));

        holder.txtTitle.setText(title);
        long duration = cursor.getLong(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION));
        holder.txtDuration.setText(Utilities.milliSecondsToTime(duration));

        long progress = cursor.getLong(cursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_PROGRESS));
        int[] progressColors = Utilities.getProgressColors(progress, duration);

        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                progressColors);
        gd.setCornerRadius(0f);

        holder.item.setBackgroundDrawable(gd);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.list_leftdrawer_item, parent, false);

        ViewHolder holder;
        holder = new ViewHolder();
        holder.txtTitle = (TextView) v.findViewById(R.id.list_leftdrawer_title);
        holder.txtDuration = (TextView) v.findViewById(R.id.list_leftdrawer_duration);
        holder.item = (RelativeLayout) v.findViewById(R.id.list_leftdrawer_item);
        v.setTag(holder);

        return v;
    }
}