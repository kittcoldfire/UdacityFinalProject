package com.etechtour.audiobookplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.etechtour.audiobookplayer.PlayerActivity;
import com.etechtour.audiobookplayer.R;

/**
 * Created by Phil on 5/23/2016.
 */
public class AudiobookWidgetProvider extends AppWidgetProvider {

    public static final String UPDATE_ACTION = "com.etechtour.audiobook.notification.action.update";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];

            //We are updating our layout in our service directly.
            //This should only be called when we create the widget, so lets send a broadcast for our Service to update this right away

            Intent x = new Intent(UPDATE_ACTION);
            context.sendBroadcast(x);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_simple_player);
            views.setTextViewText(R.id.widget_album, context.getString(R.string.widget_empty));
            views.setViewVisibility(R.id.widget_button, View.GONE);
            views.setViewVisibility(R.id.widget_art, View.GONE);
            views.setViewVisibility(R.id.widget_file, View.GONE);

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, PlayerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent activityIntent = PendingIntent.getActivity(context, 900, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            views.setOnClickPendingIntent(R.id.widget_layout, activityIntent);

            //getApplicationContext().sendBroadcast(updateIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }


}
