package com.etechtour.audiobookplayer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.etechtour.audiobookplayer.database.AudioBookContract.*;

public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String OPENB = " (";
    public static final String CLOSEB = ")";
    public static final String CLOSEBS = ");";
    public static final String PRIMARY = " integer primary key ";
    public static final String COMMA = ", ";

    public static final String COLUMN_ID = "_id";

    //Table to store information in the Media Tables from default Android
    //We will also store our extra information in here

    public static final String DATABASE_NAME = "etech_audiobooks.db";
    private static final int DATABASE_VERSION = 4;

    public static final String T_AUDIO_CREATE = "CREATE TABLE "
            + AudioBookEntry.TABLE_AUDIO_COLUMNS + OPENB
            + COLUMN_ID + PRIMARY + COMMA
            + AudioBookEntry.TAC_COLUMN_TITLE + " text not null, "
            + AudioBookEntry.TAC_COLUMN_ARTIST + " text, "
            + AudioBookEntry.TAC_COLUMN_ARTIST_ID + " integer, "
            + AudioBookEntry.TAC_COLUMN_ALBUM + " text, "
            + AudioBookEntry.TAC_COLUMN_ALBUM_ID + " integer, "
            + AudioBookEntry.TAC_COLUMN_DURATION + " integer, "
            + AudioBookEntry.TAC_COLUMN_TRACK + " integer, "
            + AudioBookEntry.TAC_COLUMN_BOOKMARK + " integer, "
            + AudioBookEntry.TAC_COLUMN_IS_ALARM + " integer, "
            + AudioBookEntry.TAC_COLUMN_IS_MUSIC + " integer, "
            + AudioBookEntry.TAC_COLUMN_IS_NOTIFICATION + " integer, "
            + AudioBookEntry.TAC_COLUMN_IS_PODCAST + " integer, "
            + AudioBookEntry.TAC_COLUMN_IS_RINGTONE + " integer, "
            + AudioBookEntry.TAC_COLUMN_YEAR + " integer, "
            + AudioBookEntry.TAC_COLUMN_DATA + " text, "
            + AudioBookEntry.TAC_COLUMN_DATE_ADDED + " integer, "
            + AudioBookEntry.TAC_COLUMN_DATE_MODIFIED + " integer, "
            + AudioBookEntry.TAC_COLUMN_SIZE + " integer, "

            + AudioBookEntry.TAC_COLUMN_TIMES_COMPLETED + " integer, "
            + AudioBookEntry.TAC_COLUMN_RATING + " integer, "
            + AudioBookEntry.TAC_COLUMN_REVIEW + " text, "
            + AudioBookEntry.TAC_COLUMN_ALBUM_ART + " text, "
            + AudioBookEntry.TAC_COLUMN_ON_DEVICE + " integer, "
            + AudioBookEntry.TAC_COLUMN_PROGRESS + " integer, "
            + AudioBookEntry.TAC_COLUMN_FILETYPE + " integer"
            + CLOSEBS
            ;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.w(MySQLiteHelper.class.getName(),
                "Creating tables");
        db.execSQL(T_AUDIO_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");

        db.execSQL("DROP TABLE IF EXISTS " + "audio_book_table");
        db.execSQL("DROP TABLE IF EXISTS " + AudioBookEntry.TABLE_AUDIO_COLUMNS);

        onCreate(db);
    }

}
