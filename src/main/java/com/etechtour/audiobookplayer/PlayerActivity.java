package com.etechtour.audiobookplayer;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.etechtour.audiobookplayer.adapters.LeftDrawerListAdapter;
import com.etechtour.audiobookplayer.database.AudioBookContract;
import com.etechtour.audiobookplayer.service.MediaPlayerService;

import java.util.ArrayList;
import java.util.Vector;

public class PlayerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SeekBar.OnSeekBarChangeListener {

    private MediaPlayerService mMediaService;
    private Intent playIntent;
    private boolean mServiceBound = false;
    private ImageButton btnPlayPause, btnRWBig, btnRWSmall, btnFFSmall, btnFFBig;

    //MediaBrowser
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;

    private TextView txtTitle, txtAlbum, txtAuthor;
    private ImageView imgPlayPause;
    private SeekBar sbSeekBar;
    private RatingBar ratingBar;

    private TextView txtNavTitle, txtNavAlbum, txtNumFiles, txtPercentage, txtSeekbarText;
    private ImageView imgNavAlbumArt;

    private ListView mDrawerList;
    private LeftDrawerListAdapter ldAdapter;

    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();
    boolean wasCurrentlyPlaying = false;

    private int albumID;
    private String albumTitle;

    private int mRWBig, mRWSmall, mFFSmall, mFFBig;

    private boolean mTabletMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnPlayPause = (ImageButton) findViewById(R.id.content_btnPlayPause);
        btnRWBig = (ImageButton) findViewById(R.id.content_btnRWBig);
        btnRWSmall = (ImageButton) findViewById(R.id.content_btnRWSmall);
        btnFFBig = (ImageButton) findViewById(R.id.content_btnFFBig);
        btnFFSmall = (ImageButton) findViewById(R.id.content_btnFFSmall);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int firstLaunch = sharedPrefs.getInt(getString(R.string.pref_firstlaunch), 0);

        mRWBig = 60000; //1 min in ms, eventually make this configurable so we can get this from SharedPreferences
        mFFBig = 60000; //1 min in ms, eventually make this configurable so we can get this from SharedPreferences
        mRWSmall = 10000; //10 s in ms, eventually make this configurable so we can get this from SharedPreferences
        mFFSmall = 10000; //10 s in ms, eventually make this configurable so we can get this from SharedPreferences

        //txtTitle = (TextView) findViewById(R.id.content_title);
        txtAlbum = (TextView) findViewById(R.id.content_album);
        txtAuthor = (TextView) findViewById(R.id.content_author);
        imgPlayPause = (ImageView) findViewById(R.id.content_img_playpause);
        sbSeekBar = (SeekBar) findViewById(R.id.content_seekbar);
        txtSeekbarText = (TextView) findViewById(R.id.content_seekbar_textview);
        // Listeners
        sbSeekBar.setOnSeekBarChangeListener(this); // Important

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer == null) {
            mTabletMode = true;
        } else {
            toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }

        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        ldAdapter = new LeftDrawerListAdapter(getApplicationContext(), null);
        mDrawerList.setAdapter(ldAdapter);

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mServiceBound) {
                    if(mMediaService.getAlbumID() > 0) {
                        if(mMediaService.isPlaying()) {
                            if(mTransportControls != null) {
                                mTransportControls.pause();
                                mHandler.removeCallbacks(mUpdateTimeTask);
                            }
                        } else if (mMediaService.getTrackPositionInCursor() != -1) {
                            //mMediaService.resumePlayback();
                            if(mTransportControls != null) {
                                mTransportControls.play();
                                updateSeekBar();
                            }
                        }
                    } else {
                        missingAlbumIDAction();
                    }
                }
            }
        });

        btnRWBig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mServiceBound) {
                    if(mMediaService.getAlbumID() > 0) {
                        int current = mMediaService.getCurrentTrackPosition() - mRWBig;
                        if (current > 0) {
                            mTransportControls.seekTo(current);
                        } else {
                            current = 0;
                            mTransportControls.seekTo(current);

                        }
                        if (!mMediaService.isPlaying()) {
                            sbSeekBar.setProgress(current);
                            txtSeekbarText.setText("" + Utilities.milliSecondsToTime(current));
                        }
                    } else {
                        missingAlbumIDAction();
                    }
                }

            }
        });

        btnRWSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mServiceBound) {
                    if(mMediaService.getAlbumID() > 0) {
                        int current = mMediaService.getCurrentTrackPosition() - mRWSmall;
                        if (current > 0) {
                            mTransportControls.seekTo(current);
                        } else {
                            current = 0;
                            mTransportControls.seekTo(current);

                        }
                        if (!mMediaService.isPlaying()) {
                            sbSeekBar.setProgress(current);
                            txtSeekbarText.setText("" + Utilities.milliSecondsToTime(current));
                        }
                    } else {
                        missingAlbumIDAction();
                    }
                }

            }
        });

        btnFFBig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mServiceBound) {
                    if(mMediaService.getAlbumID() > 0) {
                        int current = mMediaService.getCurrentTrackPosition() + mFFBig;
                        if (current < mMediaService.getTrackDuration()) {
                            mTransportControls.seekTo(current);
                        } else {
                            current = mMediaService.getTrackDuration();
                            mTransportControls.seekTo(current);

                        }
                        if (!mMediaService.isPlaying()) {
                            sbSeekBar.setProgress(current);
                            txtSeekbarText.setText("" + Utilities.milliSecondsToTime(current));
                        }
                    } else {
                        missingAlbumIDAction();
                    }
                }

            }
        });

        btnFFSmall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mServiceBound) {
                    if(mMediaService.getAlbumID() > 0) {
                        int current = mMediaService.getCurrentTrackPosition() + mFFSmall;
                        if (current < mMediaService.getTrackDuration()) {
                            mTransportControls.seekTo(current);
                        } else {
                            current = mMediaService.getTrackDuration();
                            mTransportControls.seekTo(current);

                        }
                        if (!mMediaService.isPlaying()) {
                            sbSeekBar.setProgress(current);
                            txtSeekbarText.setText("" + Utilities.milliSecondsToTime(current));
                        }
                    } else {
                        missingAlbumIDAction();
                    }
                }

            }
        });

        if(firstLaunch == 0) {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putInt(getString(R.string.pref_firstlaunch), 1);
            editor.commit();

            Intent i = new Intent(this, Settings.class);
            i.putExtra(getString(R.string.pref_firstlaunch), 0);
            startActivityForResult(i, 300);
        } else {
            new SelectDataTask().execute("");
        }
    }

    /**
     * Helper method to launch the library activity when user is trying to click a button and we don't have a
     * valid cursor to play.
     */
    private void missingAlbumIDAction() {
        Intent i = new Intent(this, LibraryActivity.class);
        //startActivity(i);
        startActivityForResult(i, 100);
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() {
        MediaSessionCompat.Token freshToken = mMediaService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            try {
                mController = new MediaControllerCompat(mMediaService, mSessionToken);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mTransportControls = mController.getTransportControls();
            mController.registerCallback(mCb);
        }
    }

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            if(state != null) {
                //Updates the UI whenever the playbackstate changes
                if(state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    imgPlayPause.setImageResource(R.drawable.pause);
                } else if (state.getState() == PlaybackStateCompat.STATE_PAUSED) {
                    imgPlayPause.setImageResource(R.drawable.play);
                    if(mHandler != null) {
                        mHandler.removeCallbacks(mUpdateTimeTask);
                    }
                } else if (state.getState() == PlaybackStateCompat.STATE_STOPPED) {
                    finish();
                }
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);

            albumTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            albumID = Integer.parseInt(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID));
            txtAlbum.setText(albumTitle);
            txtAlbum.setSelected(true);
            txtAuthor.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
            txtAuthor.setSelected(true);

            if(sbSeekBar.getMax() != (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)) {
                sbSeekBar.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            }

            String userArt = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
            Bitmap albumArt = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
            if(userArt != null && !userArt.equals("")) {
                Glide
                        .with(getApplicationContext())
                        .load(userArt)
                        .placeholder(R.drawable.placeholder)
                        .crossFade()
                        .into(btnPlayPause);
            } else {
                if(albumArt != null) {
                    btnPlayPause.setImageBitmap(albumArt);
                }
            }

            if(mDrawerList != null) {
                txtNavTitle = (TextView) findViewById(R.id.nav_title);
                txtNavAlbum = (TextView) findViewById(R.id.nav_audiobook) ;
                imgNavAlbumArt = (ImageView) findViewById(R.id.nav_albumart);

                txtNumFiles = (TextView) findViewById(R.id.nav_numfiles);
                txtPercentage = (TextView) findViewById(R.id.nav_percentage);
                ratingBar = (RatingBar) findViewById(R.id.nav_ratingbar);

                ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                    @Override
                    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                        if (!fromUser)
                            return;

                        ContentValues cv = new ContentValues();
                        cv.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_RATING, rating);

                        if(albumID > 0) {
                            int update = getApplicationContext().getContentResolver().update(
                                    AudioBookContract.AudioBookEntry.CONTENT_URI,
                                    cv,
                                    AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + " = ?",
                                    new String[]{albumID + ""}
                            );
                        }
                    }
                });

                if(txtNavTitle != null) {
                    txtNavTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                    txtNavAlbum.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM));

                    if(userArt != null && !userArt.equals("")) {
                        Glide
                                .with(getApplicationContext())
                                .load(userArt)
                                .placeholder(R.drawable.placeholder)
                                .crossFade()
                                .into(imgNavAlbumArt);
                    } else {
                        if(albumArt != null) {
                            imgNavAlbumArt.setImageBitmap(albumArt);
                        }
                    }

                    // Define the text of the number of files.
                    String numFileText = String.format(getApplicationContext().getString(R.string.format_num_files),
                            metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER),
                            metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS));

                    txtNumFiles.setText(numFileText);

                    String percentText = String.format(getApplicationContext().getString(R.string.format_percentage_complete),
                            Utilities.getPercentageString(Utilities.getPercentage(
                                    metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER),
                                    metadata.getLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS))
                            )
                    );
                    txtPercentage.setText(percentText);
                }
            }

            final Cursor c = mMediaService.getCursor();

            ldAdapter.swapCursor(c);
            ldAdapter.notifyDataSetChanged();
            mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            int nowplaying = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER) - 1;
            mDrawerList.setSelection(nowplaying);

            c.moveToPosition(nowplaying);
            int trackProgress = mMediaService.getTrackProgress();
            float rating = c.getFloat(c.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_RATING));
            if(rating >= 0) {
                ratingBar.setRating(rating);
            } else {
                ratingBar.setRating(0);
            }
            if(trackProgress == -1) {
                trackProgress = c.getInt(c.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_PROGRESS));
                mMediaService.setTrackProgress(trackProgress);
            }
            if(trackProgress != -1) {
                sbSeekBar.setProgress(trackProgress);
                txtSeekbarText.setText(Utilities.milliSecondsToTime(trackProgress));
            } else {
                sbSeekBar.setProgress(0);
                txtSeekbarText.setText(Utilities.milliSecondsToTime(0));
            }

            mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(!mTabletMode) {
                        drawer.closeDrawer(Gravity.LEFT);
                    }
                    boolean wasPlaying = true;
                    if(mMediaService.isPlaying()) {
                        mTransportControls.pause();
                    }

                    updateServicePlayFile(albumID, position, -1, wasPlaying);
                }
            });
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            updateSessionToken();
        }
    };

    /**
     * Update time on seekbar
     */
    public void updateSeekBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long currentProgress = mMediaService.getCurrentTrackPosition();

            //Displaying time completed playing
            txtSeekbarText.setText("" + Utilities.milliSecondsToTime(currentProgress));
            sbSeekBar.setProgress((int) currentProgress);

            //Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(!fromUser)
            return;

        if(albumID > 0) {
            //Displaying time completed playing
            txtSeekbarText.setText("" + Utilities.milliSecondsToTime(progress) + "");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if(albumID > 0) {
            //remove message Handler from updating progress bar
            mHandler.removeCallbacks(mUpdateTimeTask);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(albumID > 0) {
            mHandler.removeCallbacks(mUpdateTimeTask);

            int progress = seekBar.getProgress();
            //forward or backward to certain seconds
            mTransportControls.seekTo(progress);

            if(mMediaService.isPlaying()) {
                updateSeekBar();
            }
        }
    }

    private ServiceConnection mediaConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MediaBinder binder = (MediaPlayerService.MediaBinder) service;

            mMediaService = binder.getService();
            updateSessionToken();

            mServiceBound = true;

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            //Now load the last listened to audiobook
            albumID = sharedPrefs.getInt(getString(R.string.shared_album_id), -1);
            int mCurrentTrackInCursor = sharedPrefs.getInt(getString(R.string.shared_current_track_in_cursor), -1);
            int mCurrentTrackProgress = sharedPrefs.getInt(getString(R.string.shared_current_track_progress), -1);

            if(!mMediaService.isPlaying() && albumID != -1) {
                updateServicePlayFile(albumID, mCurrentTrackInCursor, mCurrentTrackProgress, false);
            } else {
                //Most likely a rotation change, update metadata to update the display
                mMediaService.updateMetaData();
                if(mMediaService.isPlaying()) {
                    updateSeekBar();
                    imgPlayPause.setImageResource(R.drawable.pause);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent == null) {
            playIntent = new Intent(this, MediaPlayerService.class);
            getApplicationContext().bindService(playIntent, mediaConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mServiceBound) {
            if(mediaConnection != null) {
                getApplicationContext().unbindService(mediaConnection);
                mServiceBound = false;
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, Settings.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_library) {
            Intent i = new Intent(this, LibraryActivity.class);
            //startActivity(i);
            startActivityForResult(i, 100);
            return true;
        } else if (id == R.id.action_clear) {
            int rowsDeleted = getApplicationContext().getContentResolver().delete(
                    AudioBookContract.AudioBookEntry.CONTENT_URI,
                    null,
                    null
                );
            Toast.makeText(getApplicationContext(), "Rows Deleted: " + rowsDeleted, Toast.LENGTH_LONG).show();
        } else if (id == R.id.action_cover) {
            Intent i = new Intent(this, CoverArt.class);
            i.putExtra(getString(R.string.cover_albumid), albumID);
            i.putExtra(getString(R.string.cover_title), albumTitle);
            //startActivity(i);
            startActivityForResult(i, 200);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == 100) {
            if(mServiceBound) {
                albumID = data.getExtras().getInt("albumId");

                updateServicePlayFile(albumID, -1, -1, true);
            }
        } else if(resultCode == 200) {
            String albumArtURL = data.getExtras().getString("url");

            ContentValues cv = new ContentValues();
            cv.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ART, albumArtURL);

            if(albumID > 0) {
                int update = getApplicationContext().getContentResolver().update(
                        AudioBookContract.AudioBookEntry.CONTENT_URI,
                        cv,
                        AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + " = ?",
                        new String[] { albumID + "" }
                );
                if(update > 0) {
                    boolean playing = false;
                    if(mMediaService.isPlaying()) {
                        mTransportControls.pause();
                        playing = true;
                    }
                    mMediaService.setAlbumID(mMediaService.getAlbumID());
                    mMediaService.setTrackPositionInCursor(mMediaService.getTrackPositionInCursor(), playing);
                    mMediaService.setTrackProgress(mMediaService.getTrackProgress());
                    //mMediaService.updateMetaData();
                }
            }
        } else if (resultCode == 300) {
            new SelectDataTask().execute("");

            missingAlbumIDAction();
        }
    }

    private void updateServicePlayFile(int albID, int trackPosition, int trackProgress, boolean startPlayback) {
        if(albID == 0 || albID == -1) {
            return;
        }
        mMediaService.setAlbumID(albID);

        mMediaService.setTrackPositionInCursor(trackPosition, startPlayback);
        mMediaService.setTrackProgress(trackProgress);

        if(startPlayback) {
            mTransportControls.play();
            updateSeekBar();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class SelectDataTask extends AsyncTask<String, Integer, String> {
        private final ProgressDialog dialog = new ProgressDialog(PlayerActivity.this);

        final String[] CURSOR_COLS = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                //Artist Info
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                //MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS,
                //MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS,
                //Album Info
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                //MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS,
                //Track Info
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.BOOKMARK,
                MediaStore.Audio.Media.IS_ALARM,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.IS_PODCAST,
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.SIZE
        };

        final String[] CURSOR_COLS_AUDIOBOOKS = new String[] {
                AudioBookContract.AudioBookEntry.COLUMN_ID,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST_ID,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_TRACK,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_BOOKMARK,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_ALARM,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_NOTIFICATION,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_PODCAST,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_RINGTONE,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_YEAR,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_SIZE,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_TIMES_COMPLETED,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_RATING,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_REVIEW,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ART,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_ON_DEVICE,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_PROGRESS,
                AudioBookContract.AudioBookEntry.TAC_COLUMN_FILETYPE
        };
        // Defines a string to contain the selection clause
        String selectionClause = null;

        // An array to contain selection arguments
        String[] selectionArgs = null;

        // An ORDER BY clause, or null to get results in the default sort order
        String sortOrder = null;

        Uri mBaseUri;

        Cursor mCursor;
        Cursor mAudioBookCursor;
        int mNumFiles, mAudioFiles;

        // can use UI thread here
        protected void onPreExecute() {
            this.dialog.setMessage("Updating music database... 0%");
            this.dialog.show();
        }

        // automatically done on worker thread (separate from UI thread)
        @Override
        protected String doInBackground(final String... args) {
            int count = 0;
            mBaseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String prefAudioBookLocation = sharedPrefs.getString(getString(R.string.pref_audiobook_location), "0");
            mAudioFiles = sharedPrefs.getInt(getString(R.string.shared_num_files), 0);

            if(prefAudioBookLocation.equals("0")) {
                selectionArgs = null;
                selectionClause = null;
            } else {
                selectionClause = MediaStore.Audio.Media.DATA + " LIKE ?";
                selectionArgs = new String[] { "%" + prefAudioBookLocation + "%" };
            }

            sortOrder = MediaStore.Audio.Media.TITLE;
            mCursor = getApplicationContext().getContentResolver().query(
                    mBaseUri,  			// The content URI of the music table
                    CURSOR_COLS,        // The columns to return for each row
                    selectionClause,	// Either null, or the word the user entered
                    selectionArgs,      // Either empty, or the string the user entered
                    sortOrder			// The sort order for the returned rows
            );

            mAudioBookCursor = getApplicationContext().getContentResolver().query(
                    AudioBookContract.AudioBookEntry.CONTENT_URI,
                    CURSOR_COLS_AUDIOBOOKS,
                    selectionClause,
                    selectionArgs,
                    sortOrder
            );

            if(mCursor != null) {
                mNumFiles = mCursor.getCount();
            } else {
                mNumFiles = -1;
            }

            //If the cursor doesn't have any files we need to copy them into our database
            //if it does have info in the DB than we need to compare to see if any have changed
            if(mAudioBookCursor != null && mAudioBookCursor.getCount() > 0) {

                //If both cursors have the same number of files, chances are really good we don't need to update
                if(mAudioBookCursor.getCount() != mNumFiles) {
                    ArrayList<Long> ids = new ArrayList<Long>();

                    mAudioBookCursor.moveToFirst();
                    do {
                        long id = mAudioBookCursor.getLong(mAudioBookCursor.getColumnIndex(AudioBookContract.AudioBookEntry.COLUMN_ID));
                        ids.add(id);
                    } while (mAudioBookCursor.moveToNext());

                    mCursor.moveToFirst();
                    Vector<ContentValues> cVVector = new Vector<ContentValues>(mNumFiles);
                    do {
                        long audioId = mCursor.getLong(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.COLUMN_ID));

                        if(!ids.contains(audioId)) {
                            cVVector.add(getAudiobookInfoFromCursor(mCursor));
                        }
                    } while (mCursor.moveToNext());

                    if ( cVVector.size() > 0 ) {
                        ContentValues[] cvArray = new ContentValues[cVVector.size()];
                        cVVector.toArray(cvArray);
                        mAudioFiles = getApplicationContext().getContentResolver().bulkInsert(
                                AudioBookContract.AudioBookEntry.CONTENT_URI,
                                cvArray
                        );
                    }

                    mAudioFiles = mAudioBookCursor.getCount();
                }
            } else {
                mAudioFiles = -1;

                Vector<ContentValues> cVVector = new Vector<ContentValues>(mNumFiles);

                mCursor.moveToFirst();

                do {
                    cVVector.add(getAudiobookInfoFromCursor(mCursor));
                } while(mCursor.moveToNext());

                if ( cVVector.size() > 0 ) {
                    ContentValues[] cvArray = new ContentValues[cVVector.size()];
                    cVVector.toArray(cvArray);
                    mAudioFiles = getApplicationContext().getContentResolver().bulkInsert(
                            AudioBookContract.AudioBookEntry.CONTENT_URI,
                            cvArray
                    );
                }
            }

            if(mCursor != null) {
                mCursor.close();
            }
            if(mAudioBookCursor != null) {
                mAudioBookCursor.close();
            }

            return "";
        }

        private ContentValues getAudiobookInfoFromCursor(Cursor c) {
            ContentValues copyData = new ContentValues();
            if(c.getCount() > 0) {
                copyData.put(AudioBookContract.AudioBookEntry.COLUMN_ID, c.getString(c.getColumnIndex(MediaStore.Audio.Media._ID)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE, c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST, c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST_ID, c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM, c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID, c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION, c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_TRACK, c.getString(c.getColumnIndex(MediaStore.Audio.Media.TRACK)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_BOOKMARK, c.getString(c.getColumnIndex(MediaStore.Audio.Media.BOOKMARK)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_ALARM, c.getString(c.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC, c.getString(c.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_NOTIFICATION, c.getString(c.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_PODCAST, c.getString(c.getColumnIndex(MediaStore.Audio.Media.IS_PODCAST)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_RINGTONE, c.getString(c.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_YEAR, c.getString(c.getColumnIndex(MediaStore.Audio.Media.YEAR)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA, c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED, c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED, c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_SIZE, c.getString(c.getColumnIndex(MediaStore.Audio.Media.SIZE)));
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_TIMES_COMPLETED, 0);
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_RATING, -1);
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_REVIEW, "");
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ART, "");
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_ON_DEVICE, AudioBookContract.AudioBookEntry.ON_DEVICE);
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_PROGRESS, AudioBookContract.AudioBookEntry.PROGRESS_NOT_STARTED);
                copyData.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_FILETYPE, AudioBookContract.AudioBookEntry.FILETYPE_UNKNOWN);
            }

            return copyData;
        }

        public void doProgress(int value){
            publishProgress(value);
        }

        protected void onProgressUpdate(Integer... progress) {
            this.dialog.setMessage("Updating audiobook database..." + progress[0] + "%");
        }

        // can use UI thread here
        protected void onPostExecute(final String result) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putInt(getString(R.string.shared_num_files), mAudioFiles);
            editor.commit();

            //Toast.makeText(getApplicationContext(), "Number of files found:" + mNumFiles + " Number of Audiobooks found: " + mAudioFiles, Toast.LENGTH_LONG).show();
        }
    }
}
