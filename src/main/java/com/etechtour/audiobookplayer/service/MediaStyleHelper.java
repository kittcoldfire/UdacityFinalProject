package com.etechtour.audiobookplayer.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
public class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     * @param context Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    public static android.support.v7.app.NotificationCompat.Builder from(
            Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(controller.getSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }

    /**
     * Create a {@link PendingIntent} appropriate for a MediaStyle notification's action. Assumes
     * you are using a media button receiver.
     * @param context Context used to contruct the pending intent.
     * @param mediaKeyEvent KeyEvent code to send to your media button receiver.
     * @return An appropriate pending intent for sending a media button to your media button
     *      receiver.
     */
    public static PendingIntent getActionIntent(
            Context context, int mediaKeyEvent) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(context.getPackageName());
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_DOWN, mediaKeyEvent));
        return PendingIntent.getBroadcast(context, mediaKeyEvent, intent, 0);
    }
}