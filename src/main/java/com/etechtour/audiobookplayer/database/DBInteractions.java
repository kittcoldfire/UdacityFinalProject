package com.etechtour.audiobookplayer.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Created by Phil on 4/15/2016.
 */
public class DBInteractions {

    private static SQLiteDatabase database;
    private MySQLiteHelper dbHelper;

    public DBInteractions (Context context) {
        dbHelper = new MySQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void openRead() throws SQLException {
        database = dbHelper.getReadableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * Returns the number of Tracks for a specific audio book
     * @param id Audiobook id
     * @return number of tracks for the audiobook
     */
    public int getNumTracksForAlbum(int id) {

        open();
        Cursor select = database.rawQuery("SELECT * FROM " + AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS
                        + " WHERE " + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + " = '" + id + "'"
                        + " AND " + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC + " = 1"
                , null);

        return select.getCount();
    }

    /**
     * Returns the number of Tracks for a specific author
     * @param id Author id
     * @return Number of books for an author
     */
    public int getNumTracksForArtist(int id) {

        open();
        Cursor select = database.rawQuery("SELECT * FROM " + AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS
                        + " WHERE " + AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST_ID + " = '" + id + "'"
                        + " AND " + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC + " = 1"
                , null);

        return select.getCount();
    }

    /**
     * Returns a cursor with all the books for a specific author
     * @param id Author id
     * @return Cursor to results of books from author
     */
    public Cursor getAlbumsForArtist(int id) {
        open();
        Cursor select = database.rawQuery("SELECT *"
                        + " FROM " + AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS + " tac"
                        + " WHERE " + AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST_ID + " = '" + id + "'"
                        + " AND " + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC + " = 1"
                        + " GROUP BY " + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID
                        + " ORDER BY " + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM + ", "
                        + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID
                , null);
        return select;
    }

    /**
     * Return how many books for a specific Author
     * @param id Author id
     * @return Number of books for that author
     */
    public int getNumAlbumsForArtist(int id) {

        return getAlbumsForArtist(id).getCount();
    }



    public void updateEvoOnSongCompletion(int id, boolean timesCompleted, boolean timesSkipped, int bookmark,
                                          boolean timesRepeat, boolean timesPlayed) {
        /*open();
        Cursor select = database.rawQuery("SELECT * FROM " + MySQLiteHelper.TABLE_AUDIOBOOKS
                + " WHERE " + MySQLiteHelper.EVO_COLUMN_ID + " = '" + id + "'", null);

        long insert = 5, update = 5;
        if(select.getCount() > 0 ) {
            select.moveToFirst();
            //Update the row with the values that we've passed to it
            ContentValues cv = new ContentValues();
            if(timesCompleted) {
                int tC = select.getInt(select.getColumnIndex(MySQLiteHelper.EVO_COLUMN_TIMES_COMPLETED)) + 1;

                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_COMPLETED, tC);
            }
            if(timesSkipped) {
                int tS = select.getInt(select.getColumnIndex(MySQLiteHelper.EVO_COLUMN_TIMES_SKIPPED)) + 1;

                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_COMPLETED, tS);
            }
            if(timesRepeat) {
                int tR = select.getInt(select.getColumnIndex(MySQLiteHelper.EVO_COLUMN_TIMES_REPEAT)) + 1;

                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_REPEAT, tR);
            }
            if(timesPlayed) {
                int tP = select.getInt(select.getColumnIndex(MySQLiteHelper.EVO_COLUMN_TIMES_PLAYED)) + 1;

                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_PLAYED, tP);
            }
            if(bookmark != -1) {
                cv.put(MySQLiteHelper.EVO_COLUMN_COMPLETION, bookmark);
            }

            update = database.update(MySQLiteHelper.TABLE_EVOMUSIC, cv,
                    MySQLiteHelper.EVO_COLUMN_ID + " = " + id,
                    null);
        } else {
            //Start a new record
            ContentValues cv = new ContentValues();
            cv.put(MySQLiteHelper.EVO_COLUMN_ID, id);
            if(timesCompleted) {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_COMPLETED, 1);
            } else {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_COMPLETED, 0);
            }
            if(timesSkipped) {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_SKIPPED, 1);
            } else {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_SKIPPED, 0);
            }
            if(bookmark != -1) {
                cv.put(MySQLiteHelper.EVO_COLUMN_COMPLETION, bookmark);
            } else {
                cv.put(MySQLiteHelper.EVO_COLUMN_COMPLETION, 0);
            }
            if(timesRepeat) {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_REPEAT, 1);
            } else {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_REPEAT, 0);
            }
            if(timesPlayed) {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_PLAYED, 1);
            } else {
                cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_PLAYED, 0);
            }
            insert = database.insert(MySQLiteHelper.TABLE_EVOMUSIC, null, cv);
        }

        select.close();

        if(insert == -1 || update == 0) {
            Log.e(EvoPlaylistMainActivity.class.getName(), "Error updating or inserting completion record.");
        }*/
    }

    public void updateEvoBookmark(int id, long milliseconds) {
        /*open();
        Cursor select = database.rawQuery("SELECT * FROM " + MySQLiteHelper.TABLE_EVOMUSIC
                + " WHERE " + MySQLiteHelper.EVO_COLUMN_ID + " = '" + id + "'", null);
        long insert = 5, update = 5;
        if(select.getCount() > 0 ) {
            //Update the row with the value
            ContentValues cv = new ContentValues();
            cv.put(MySQLiteHelper.EVO_COLUMN_COMPLETION, milliseconds);

            update = database.update(MySQLiteHelper.TABLE_EVOMUSIC, cv,
                    MySQLiteHelper.EVO_COLUMN_ID + " = " + id,
                    null);
        } else {
            //Start a new record
            ContentValues cv = new ContentValues();
            cv.put(MySQLiteHelper.EVO_COLUMN_ID, id);
            cv.put(MySQLiteHelper.EVO_COLUMN_COMPLETION, milliseconds);
            cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_COMPLETED, 0);
            cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_PLAYED, 0);
            cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_REPEAT, 0);
            cv.put(MySQLiteHelper.EVO_COLUMN_TIMES_SKIPPED, 0);
            insert = database.insert(MySQLiteHelper.TABLE_EVOMUSIC, null, cv);
        }

        if(insert == -1 || update == 0) {
            Log.e(EvoPlaylistMainActivity.class.getName(), "Error updating or inserting bookmark record.");
        }

        select.close();*/
    }

    public void createTempMusicTable() {
        Cursor mCursor;
        open();
        //SELECT name FROM sqlite_master WHERE type='table' AND name='table_name';
        mCursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS + "'", null);
        if(mCursor != null) {
            if(mCursor.getCount() > 0) {
                Log.d("CreateTempMusic", "Table already exists.");
            } else {
                //Error if table doesn't exist
                database.execSQL(MySQLiteHelper.T_AUDIO_CREATE);
                Log.d("CreateTempMusic", "Creating table.");
            }
        } else {
            //Error if table doesn't exist
            database.execSQL(MySQLiteHelper.T_AUDIO_CREATE);
            Log.d("CreateTempMusic", "Creating table.");
        }
        mCursor.close();
    }

    public Cursor getAllMusicData() {
        open();
        try {
            Cursor mCursor = database.rawQuery("SELECT * FROM " + AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS, null);
			/*for(int x = 0; x < mCursor.getCount(); x++) {
				mCursor.moveToPosition(x);
				Log.d("MusicTable",
						mCursor.getString(mCursor.getColumnIndex(MySQLiteHelper.TAC_COLUMN_TITLE)) + ", " +
						mCursor.getString(mCursor.getColumnIndex(MySQLiteHelper.TAC_COLUMN_ARTIST)) + ", " +
						mCursor.getString(mCursor.getColumnIndex(MySQLiteHelper.TAC_COLUMN_ALBUM))) ;
			}*/
            Log.d("MusicTable", mCursor.getCount() + "");
            return mCursor;
        } catch (Exception e) {
            Log.d("GetAllMusic", "Something happened to crash here " + e.toString());
        }
        return null;
    }

    public void copyAllMusicData(Cursor mCursor) {
        try {
            open();
            String sql = "INSERT OR REPLACE INTO " + AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS
                    + " ("
                    + MySQLiteHelper.COLUMN_ID + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST_ID + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_TRACK + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_BOOKMARK + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_ALARM + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_NOTIFICATION + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_PODCAST + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_RINGTONE + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_YEAR + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED + ", "
                    + AudioBookContract.AudioBookEntry.TAC_COLUMN_SIZE
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            database.beginTransactionNonExclusive();

            SQLiteStatement stmt = database.compileStatement(sql);

            for(int x = 0; x < mCursor.getCount(); x++) {
                mCursor.moveToPosition(x);
                stmt.bindLong(1, mCursor.getInt(mCursor.getColumnIndex(MySQLiteHelper.COLUMN_ID)));
                stmt.bindString(2, mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_TITLE)));
                stmt.bindString(3, mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST)));
                stmt.bindLong(4, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ARTIST_ID)));
                stmt.bindString(5, mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM)));
                stmt.bindLong(6, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID)));
                stmt.bindLong(7, mCursor.getLong(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION)));
                stmt.bindLong(8, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_TRACK)));
                stmt.bindLong(9, mCursor.getLong(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_BOOKMARK)));
                stmt.bindLong(10, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_ALARM)));
                stmt.bindLong(11, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_MUSIC)));
                stmt.bindLong(12, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_NOTIFICATION)));
                stmt.bindLong(13, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_PODCAST)));
                stmt.bindLong(14, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_IS_RINGTONE)));
                stmt.bindLong(15, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_YEAR)));
                stmt.bindString(16, mCursor.getString(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATA)));
                stmt.bindLong(17, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED)));
                stmt.bindLong(18, mCursor.getInt(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED)));
                stmt.bindLong(19, mCursor.getLong(mCursor.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_SIZE)));

                stmt.execute();
                stmt.clearBindings();
            }

            database.setTransactionSuccessful();
            database.endTransaction();

            Log.d("CopyAllMusic", "All records recorded properly");
            //database.close();
        } catch (Exception e) {
            Log.d("CopyAllMusic", "Something happened to crash here " + e.toString());
        }
    }
}
