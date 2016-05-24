package com.etechtour.audiobookplayer.service;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;

import com.etechtour.audiobookplayer.PlayerActivity;
import com.etechtour.audiobookplayer.R;
import com.etechtour.audiobookplayer.Utilities;
import com.etechtour.audiobookplayer.database.AudioBookContract;
import com.etechtour.audiobookplayer.widget.AudiobookWidgetProvider;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Phil on 4/16/2016.
 */
public class MediaPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mPlayer;

    private int mCurrentTrackInCursor; //the position in the cursor we are currently on
    private int mCurrentTrackProgress; //the position in the track they have listened until
    private int mCurrentAlbumID;

    private String mCurrentAlbum, mCurrentTitle, mCurrentArtURL;
    private Bitmap mCurrentArtwork;

    private Cursor mCursor;
    private final IBinder mediaBind = new MediaBinder();

    public static final String UPDATE_ACTION = "com.etechtour.audiobook.notification.action.update";
    public static final String REMOVE_ACTION = "com.etechtour.audiobook.notification.action.remove";
    public static final String PREV_ACTION = "com.etechtour.audiobook.notification.action.prev";
    public static final String PLAY_ACTION = "com.etechtour.audiobook.notification.action.play";
    public static final String NEXT_ACTION = "com.etechtour.audiobook.notification.action.next";
    private boolean mForeground = false;
    private int mState;

    //Audio Focus
    AudioManager mAudioManager;
    private boolean mPlayOnFocusGain;
    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED  = 2;
    // Type of audio focus we have:
    private int mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;

    private Intent noisy;

    //MediaSession
    MediaSessionCompat mMediaSession;
    private final String TAG_MEDIA_SESSION = "MediaSesson";

    public void onCreate() {
        super.onCreate();

        mCurrentTrackInCursor = -1;
        initMediaPlayer();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mMediaSession = new MediaSessionCompat(getApplicationContext(), TAG_MEDIA_SESSION);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new MediaSessionCallback());
        setSessionToken(mMediaSession.getSessionToken());

        mState = PlaybackState.STATE_NONE;
        IntentFilter filter = new IntentFilter(PLAY_ACTION);
        registerReceiver(receiver, filter);
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        private final String TAG = "MediaSessionCallback";

        @Override
        public void onPlay() {
            Log.v(TAG, "Play called!!");
            //playFile();
            resumePlayback();
        }

        @Override
        public void onSkipToQueueItem(long queueId) {

        }

        @Override
        public void onSeekTo(long position) {
            mPlayer.seekTo((int) position);
            updateCurrentTrackInDB();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

        }

        @Override
        public void onPause() {
            pausePlayback();
        }

        @Override
        public void onStop() {
            PlaybackStateCompat.Builder playbackBuilder = new PlaybackStateCompat.Builder();
            playbackBuilder.setState(PlaybackStateCompat.STATE_STOPPED, 0, 0);

            PlaybackStateCompat mPlaybackState = playbackBuilder.build();
            mMediaSession.setPlaybackState(mPlaybackState);
            stopSelf();
        }

        @Override
        public void onSkipToNext() {

        }

        @Override
        public void onSkipToPrevious() {

        }

        @Override
        public void onCustomAction(String action, Bundle extras) {

        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {

        }
    }

    public void initMediaPlayer() {
        mPlayer = new MediaPlayer();
        //set partial wake lock so we will continue playing even after the phone has been locked
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        //Set the listeners to handle the different types of events
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);

        mState = PlaybackState.STATE_BUFFERING;
    }

    private BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                //Headphones disconnected
                if (isPlaying()) {
                    pausePlayback();
                }
            }
        }
    };

    /**
     * Helper method to set the cursor of audio files that the MediaPlayer will be playing
     * @param c Cursor pointing to the list of media to play
     */
    public void setCursor(Cursor c) {
        this.mCursor = c;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    /**
     * Helper method to set media player position in the current cursor of media files playing
     * @param position Set to -1 to find furthest started track, otherwise set the track you want to start with
     */
    public void setTrackPositionInCursor(int position, boolean startPlayback) {
        mCurrentTrackInCursor = position;
        if(mCurrentTrackInCursor != -1) {

        } else {
            mCurrentTrackInCursor = findMostRecentTrack();
        }
        mCursor.moveToPosition(mCurrentTrackInCursor);

        String url = mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA));
        try {
            setTrack(url, startPlayback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTrack(String mUrl, boolean startPlayback) throws IOException {
        if(mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
        try {
            mPlayer.reset();
            mPlayer.setDataSource(mUrl);
        } catch (IllegalArgumentException e) {
            // ...
        } catch (IllegalStateException e) {
            // ...
        } catch (IOException e) {
            // ...
        }
        try {
            if(startPlayback) {
                mState = PlaybackState.STATE_BUFFERING;
            } else {
                mState = PlaybackState.STATE_CONNECTING;
            }

            mPlayer.prepare(); // prepare async to not block main thread
        } catch (IllegalStateException e) {
            // ...
        }
    }

    public int getTrackPositionInCursor() {
        return mCurrentTrackInCursor;
    }

    public int getTrackDuration() {
        return mPlayer.getDuration();
    }

    /**
     * Helper method to set the current playback progress of the current file. This should be in ms.
     * @param p ms of track currently listening to
     */
    public void setTrackProgress(int p) {
        mCurrentTrackProgress = p;
        if(mCurrentTrackProgress != -1) {
            mPlayer.seekTo(mCurrentTrackProgress);
        } else {
            mCurrentTrackProgress = findTrackProgress();
            mPlayer.seekTo(mCurrentTrackProgress);
        }
    }

    public int getTrackProgress() {
        return mCurrentTrackProgress;
    }

    public int getCurrentTrackPosition() {
        return mPlayer.getCurrentPosition();
    }

    public void setAlbumID(int albumID) {
        mCurrentAlbumID = albumID;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefAudioBookLocation = sharedPrefs.getString(getString(R.string.pref_audiobook_location), "0");

        String[] selectionArgs;
        String selectionClause;
        String sortOrder;

        if(prefAudioBookLocation.equals("0")) {
            selectionClause = AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + " = ?";
            selectionArgs = new String[] { albumID + "" };
        } else {
            selectionClause = AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA + " LIKE ? AND " + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + " = ?";
            selectionArgs = new String[] { "%" + prefAudioBookLocation + "%", albumID + "" };
        }

        sortOrder = AudioBookContract.AudioBookEntry.TAC_COLUMN_TRACK;

        mCursor = getApplicationContext().getContentResolver().query(
                AudioBookContract.AudioBookEntry.CONTENT_URI,
                null,
                selectionClause,
                selectionArgs,
                sortOrder
        );
    }

    public int getAlbumID() {
        return mCurrentAlbumID;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. We could handle ducking here, but we don't want to duck since dialog is so important, so no ducking allowed
            boolean canDuck = false;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackState.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            //Error
        }
        configMediaPlayerState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //When the service is done, release audio focus for other apps to take over again
        mAudioManager.abandonAudioFocus(this);
        //Clear the media session so other apps can take media controls
        mMediaSession.setActive(false);
        mMediaSession.release();

        stopForeground(true);

        unregisterReceiver(receiver);
    }

    public class MediaBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private void tryToGetAudioFocus() {
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void configMediaPlayerState() {
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                pausePlayback();
            }
        } else {  // we have audio focus:
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                pausePlayback(); // because we're playing audiobooks we don't want to duck since it's all dialog, so pause here instead of lowering volume
            } else {
                if (mPlayer != null) {
                    mPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mPlayer != null && !mPlayer.isPlaying()) {
                    if (mCurrentTrackInCursor == mPlayer.getCurrentPosition()) {
                        mPlayer.start();
                        mState = PlaybackState.STATE_PLAYING;
                    } else {
                        mPlayer.seekTo(mCurrentTrackProgress);
                        mState = PlaybackState.STATE_PLAYING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
    }

    public void playFile() {
        //Set this so we know that no matter what we want to be playing when we get audio focus
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        //Set media session to active so we take over media control button actions
        mMediaSession.setActive(true);
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        noisy = registerReceiver(mAudioNoisyReceiver, filter);

        if(mPlayer == null || mCurrentTrackInCursor == -1 || mState == PlaybackState.STATE_STOPPED) {
            initMediaPlayer();
        } else if (mState == PlaybackState.STATE_PAUSED) {
            resumePlayback();
        }

        if(mState == PlaybackState.STATE_BUFFERING) {
            String mediaUri;

            //If we don't have the albumID we don't have the cursor so we can't play anything
            if(mCurrentAlbumID != -1 && mCurrentAlbumID != 0) {
                if(mCurrentTrackInCursor == -1) {
                    mCurrentTrackInCursor = findMostRecentTrack();
                }
                if(mCurrentTrackProgress == -1) {
                    mCurrentTrackProgress = findTrackProgress();
                }

                mCursor.moveToPosition(mCurrentTrackInCursor);
                mediaUri = mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA));

                try {
                    mPlayer.setDataSource(mediaUri);
                } catch (Exception e) {
                    Log.e("Music Service", "Error setting data source" +  e);
                }

                mPlayer.prepareAsync();
            }
        }
    }

    public void updateMetaData() {
        if(mCursor != null) {
            mCurrentTitle = mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE));
            mCurrentAlbum = mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM));
            final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, mCurrentTitle);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mCurrentAlbum);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID)));
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST)));
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mCursor.getLong(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION)));

            String userArt = mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ART));
            if(userArt != null && !userArt.equals("")) {
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, userArt);
            } else {
                //we still need to put the art in the meta data for the notifications
            }

            Bitmap b = Utilities.getAlbumArtwork(
                    getApplicationContext(),
                    mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID)),
                    750,
                    750);

            if(b != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, b);
                mCurrentArtwork = b;
            }
            //metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, b);
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mCursor.getCount());
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mCursor.getPosition() + 1);

            MediaMetadataCompat mMediaMetadata = metadataBuilder.build();

            mMediaSession.setMetadata(mMediaMetadata);

            updateWidgets(mCurrentTitle, mCurrentAlbum, b);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(PLAY_ACTION)){
                if(mPlayer != null && mCurrentTrackInCursor >= 0) {
                    if(isPlaying()) {
                        pausePlayback();
                    } else {
                        resumePlayback();
                    }
                }
            } else if (action.equals(UPDATE_ACTION)) {
                updateWidgets(mCurrentTitle, mCurrentAlbum, mCurrentArtwork);
            } else {
                Intent i = new Intent(getApplicationContext(), PlayerActivity.class);
                //If the media player has been garbage recycled launch the app to make sure everything is initialized
                startActivity(i);
            }
        }
    };

    private void updateWidgets(String title, String album, Bitmap b) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

        ComponentName thisAppWidget = new ComponentName(getApplicationContext().getPackageName(), AudiobookWidgetProvider.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_simple_player);
            if(album != null && title != null && b != null) {
                views.setTextViewText(R.id.widget_album, album);
                views.setTextViewText(R.id.widget_file, title);
                views.setImageViewBitmap(R.id.widget_art, b);
            }

            views.setViewVisibility(R.id.widget_button, View.VISIBLE);
            views.setViewVisibility(R.id.widget_art, View.VISIBLE);
            views.setViewVisibility(R.id.widget_file, View.VISIBLE);

            if(mState == PlaybackStateCompat.STATE_PLAYING) {
                views.setImageViewResource(R.id.widget_button, R.drawable.pause_white);
            } else {
                views.setImageViewResource(R.id.widget_button, R.drawable.play_white);
            }

            Intent x = new Intent(PLAY_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 800, x, PendingIntent.FLAG_CANCEL_CURRENT);

            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
            PendingIntent activityIntent = PendingIntent.getActivity(getApplicationContext(), 900, intent, 0);

            views.setOnClickPendingIntent(R.id.widget_layout, activityIntent);
            views.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

            //getApplicationContext().sendBroadcast(updateIntent);
            appWidgetManager.updateAppWidget(widgetId, views);
            //appWidgetManager.partiallyUpdateAppWidget(widgetId, views);
        }
    }

    private void updateNotification() {
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mMediaSession);
        builder.setDeleteIntent(MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_STOP));
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        //builder.setUsesChronometer(true);

        if(mState == PlaybackState.STATE_PLAYING) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.pause, "Pause",
                    MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_PAUSE))
            );
            builder.setWhen(System.currentTimeMillis() - mPlayer.getCurrentPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);

            builder.setSmallIcon(R.drawable.play_white);
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.play, "Play",
                    MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_PLAY))
            );
            builder.setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);

            builder.setSmallIcon(R.drawable.pause_white);
        }

        builder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(
                        new int[]{ 0 })  // show only play/pause in compact view
                .setMediaSession(mMediaSession.getSessionToken())
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaStyleHelper.getActionIntent(this, KeyEvent.KEYCODE_MEDIA_STOP))
        );

        startForeground(55, builder.build());
        if(mState == PlaybackStateCompat.STATE_PAUSED) {
            stopForeground(false);
        }
    }

    //Since we have the cursor, find the track with the highest date modified otherwise return the first track
    private int findMostRecentTrack() {
        mCursor.moveToFirst();

        int date = 0;
        int position = 0;
        do {
            int newDate = mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED));
            if(newDate > date) {
                date = newDate;
                position = mCursor.getPosition();
            }
        } while(mCursor.moveToNext());

        if(mCurrentTrackInCursor == -1) {
            mCurrentTrackInCursor = 0; //make sure we are pointing at the first row of the cursor
            mCursor.moveToPosition(0);
        }

        return position;
    }

    private int findTrackProgress() {
        int progress = mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_PROGRESS));

        if(progress == -1) {
            progress = 0;
        }

        return progress;
    }

    public void setPlaybackToStopped() {
        mState = PlaybackState.STATE_STOPPED;
    }

    public void pausePlayback() {
        if(mPlayer !=  null && mState == PlaybackState.STATE_PLAYING) {
            mPlayer.pause();
            //mCurrentTrackInCursor = mPlayer.getCurrentPosition();
        }

        PlaybackStateCompat.Builder playbackBuilder = new PlaybackStateCompat.Builder();
        playbackBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
        playbackBuilder.setActions(PlaybackStateCompat.ACTION_PLAY);

        PlaybackStateCompat mPlaybackState = playbackBuilder.build();
        mMediaSession.setPlaybackState(mPlaybackState);

        mState = PlaybackState.STATE_PAUSED;
        giveUpAudioFocus();
        if(noisy != null) {
            unregisterReceiver(mAudioNoisyReceiver);
        }

        updateCurrentTrackInDB();

        updateNotification();
        updateWidgets(null, null, null);
    }

    public void updateCurrentTrackInDB() {
        //Updating Database fields for next playback
        Date date = Calendar.getInstance().getTime();
        ContentValues cv = new ContentValues();
        cv.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_PROGRESS, mPlayer.getCurrentPosition());
        cv.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED, date.getTime());

        int update = getApplicationContext().getContentResolver().update(
                AudioBookContract.AudioBookEntry.CONTENT_URI,
                cv,
                AudioBookContract.AudioBookEntry.COLUMN_ID + " = ?",
                new String[] { mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.COLUMN_ID)) + "" }
        );

        mCurrentTrackProgress = mPlayer.getCurrentPosition();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(getString(R.string.shared_album_id), mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID)));
        editor.putInt(getString(R.string.shared_track_id), mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_TRACK)));
        editor.putInt(getString(R.string.shared_current_track_in_cursor), mCurrentTrackInCursor);
        editor.putInt(getString(R.string.shared_current_track_progress), mCurrentTrackProgress);
        editor.commit();
    }

    private void giveUpAudioFocus() {
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    public void resumePlayback() {
        mState = PlaybackState.STATE_PLAYING;

        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        //Set media session to active so we take over media control button actions
        mMediaSession.setActive(true);
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        noisy = registerReceiver(mAudioNoisyReceiver, filter);

        //Setting playback state builds helpful things like lock screen notifications and android wear notifications
        PlaybackStateCompat.Builder playbackBuilder = new PlaybackStateCompat.Builder();
        playbackBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
        playbackBuilder.setActions(PlaybackStateCompat.ACTION_PAUSE);
        PlaybackStateCompat mPlaybackState = playbackBuilder.build();
        mMediaSession.setPlaybackState(mPlaybackState);

        updateNotification();
        updateWidgets(null, null, null);

        mPlayer.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mediaBind;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        //normally we would stop the player here, but we don't want to stop playing when the user
        //exits the app, so don't do anything here
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mCursor.moveToPosition(mCurrentTrackInCursor);
        if(mCursor.moveToNext()) {
            String url = mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA));
            mCurrentTrackInCursor = mCursor.getPosition();
            setTrackProgress(-1);
            try {
                setTrack(url, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mCursor.moveToPrevious();
            //We've reached the end, update the database and pause
            pausePlayback();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        if(mState == PlaybackState.STATE_BUFFERING) {
            //Request audio focus to play nice with the rest of the system
            int result = mAudioManager.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (mCurrentTrackProgress != -1) {
                    mp.seekTo(mCurrentTrackProgress);
                }
                mState = PlaybackState.STATE_PLAYING;

                //Setting playback state builds helpful things like lock screen notifications and android wear notifications
                PlaybackStateCompat.Builder playbackBuilder = new PlaybackStateCompat.Builder();
                playbackBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
                playbackBuilder.setActions(PlaybackStateCompat.ACTION_PAUSE);
                PlaybackStateCompat mPlaybackState = playbackBuilder.build();
                mMediaSession.setPlaybackState(mPlaybackState);
            }
        }

        updateMetaData();

        if(mState == PlaybackState.STATE_PLAYING) {
            updateNotification();
            mp.start();
        }

    }
}
