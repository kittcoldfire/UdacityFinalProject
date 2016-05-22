package com.etechtour.audiobookplayer.service;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.etechtour.audiobookplayer.PlayerActivity;
import com.etechtour.audiobookplayer.R;

/**
 * Created by Phil on 5/10/2016.
 */
@SuppressLint("ParcelCreator")
public class SingleLineRemoteNotification extends RemoteViews {

    Context mContext;
    public static final String PLAY_ACTION = "com.etechtour.notification.action.play";

    public SingleLineRemoteNotification(Context context, String packageName, int layoutId) {
        super(packageName, layoutId);

        mContext = context;
        Intent notificationIntent = new Intent(mContext, PlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(mContext, MediaPlayerService.class);
        playIntent.setAction(PLAY_ACTION);
        PendingIntent pplayIntent = PendingIntent.getService(mContext, 0,
                playIntent, 0);

        setOnClickPendingIntent(R.id.noti_playpause, pplayIntent);
    }
}