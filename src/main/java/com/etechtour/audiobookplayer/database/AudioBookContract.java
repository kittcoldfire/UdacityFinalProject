package com.etechtour.audiobookplayer.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by Phil on 5/2/2016.
 */
public class AudioBookContract {

    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.etechtour.audiobookplayer";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_AUDIO = "audiobook";

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long normalizeDate(long startDate) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    /* Inner class that defines the table contents of the weather table */
    public static final class AudioBookEntry implements BaseColumns {

        public static final String TABLE_AUDIO_COLUMNS = "audio_columns_table";
        public static final String COLUMN_ID = "_id";
        public static final String TAC_COLUMN_TITLE = "title";
        public static final String TAC_COLUMN_TRACK = "track";
        public static final String TAC_COLUMN_ARTIST = "artist";
        public static final String TAC_COLUMN_ARTIST_ID = "artist_id";
        public static final String TAC_COLUMN_ALBUM = "album";
        public static final String TAC_COLUMN_ALBUM_ID = "album_id";
        public static final String TAC_COLUMN_BOOKMARK = "bookmark";
        public static final String TAC_COLUMN_DURATION = "duration";
        public static final String TAC_COLUMN_IS_ALARM = "is_alarm";
        public static final String TAC_COLUMN_IS_MUSIC = "is_music";
        public static final String TAC_COLUMN_IS_NOTIFICATION = "is_notification";
        public static final String TAC_COLUMN_IS_PODCAST = "is_podcast";
        public static final String TAC_COLUMN_IS_RINGTONE = "is_ringtone";
        public static final String TAC_COLUMN_YEAR = "year";
        public static final String TAC_COLUMN_DATA = "_data";
        public static final String TAC_COLUMN_DATE_ADDED = "date_added";
        public static final String TAC_COLUMN_DATE_MODIFIED = "date_modified";
        public static final String TAC_COLUMN_SIZE = "_size";

        //Columns that store the information we need for our purposes
        public static final String TAC_COLUMN_PROGRESS = "progress"; //-1 not started
        public static final String TAC_COLUMN_TIMES_COMPLETED = "times_completed";
        public static final String TAC_COLUMN_RATING = "rating";
        public static final String TAC_COLUMN_REVIEW = "review";
        public static final String TAC_COLUMN_ALBUM_ART = "album_art";
        public static final String TAC_COLUMN_ON_DEVICE = "on_device"; //0 not on device, 1 on device and available
        public static final String TAC_COLUMN_FILETYPE = "filetype"; //0 newly added not known, 1 audiobook, 2 not audiobook file

        public static final int NOT_ON_DEVICE = 0;
        public static final int ON_DEVICE = 1;

        public static final int FILETYPE_UNKNOWN = 0;
        public static final int FILETYPE_AUDIOBOOK = 1;
        public static final int FILETYPE_NOT_AUDIOBOOK = 2;

        public static final int PROGRESS_NOT_STARTED = -1;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_AUDIO).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_AUDIO;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_AUDIO;


        public static Uri buildAudioBookUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
