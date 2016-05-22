package com.etechtour.audiobookplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import com.etechtour.audiobookplayer.adapters.AudioBookListAdapter;
import com.etechtour.audiobookplayer.database.AudioBookContract;

/**
 * Created by Phil on 4/30/2016.
 */
public class LibraryActivity extends AppCompatActivity {

    private AudioBookListAdapter abListAdapter;

    final String[] CURSOR_COLS = new String[] {
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
    int mNumFiles;

    ListView lvLibrary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }

        mBaseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String prefAudioBookLocation = sharedPrefs.getString(getString(R.string.pref_audiobook_location), "0");

        ListView lvLibrary = (ListView) findViewById(R.id.playlist_list);

        if(prefAudioBookLocation.equals("0")) {
            selectionClause = AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA + " LIKE '%' GROUP BY (" + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM + ")";
            selectionArgs = new String[] {};
        } else {
            selectionClause = AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA + " LIKE '%" + prefAudioBookLocation + "%' GROUP BY (" + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM + ")";
            selectionArgs = new String[] { };
        }

        sortOrder = AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE;
        mCursor = this.getContentResolver().query(
                AudioBookContract.AudioBookEntry.CONTENT_URI,  			// The content URI of the music table
                CURSOR_COLS,        // The columns to return for each row
                selectionClause,	// Either null, or the word the user entered
                selectionArgs,      // Either empty, or the string the user entered
                sortOrder			// The sort order for the returned rows
        );

        if(mCursor != null) {
            abListAdapter = new AudioBookListAdapter(this, mCursor);
            lvLibrary.setAdapter(abListAdapter);

            lvLibrary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mCursor.moveToPosition(position);

                    int albumId = mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID));
                    Intent in = new Intent(getApplicationContext(), LibraryActivity.class);
                    in.putExtra("albumId", albumId);
                    setResult(100, in);
                    finish();
                }
            });
        }
    }
}
