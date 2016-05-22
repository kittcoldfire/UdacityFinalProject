package com.etechtour.audiobookplayer.database;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by Phil on 5/2/2016.
 */
public class AudioBookProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private MySQLiteHelper mOpenHelper;

    static final int AUDIO = 100;
    static final int AUDIO_WITH_ID = 101;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher =  new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AudioBookContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, AudioBookContract.PATH_AUDIO, AUDIO);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MySQLiteHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case AUDIO:
                return AudioBookContract.AudioBookEntry.CONTENT_TYPE;
            case AUDIO_WITH_ID:
                return AudioBookContract.AudioBookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case AUDIO: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case AUDIO: {
                normalizeDate(values);
                long _id = db.insert(AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS, null, values);
                if ( _id > 0 )
                    returnUri = AudioBookContract.AudioBookEntry.buildAudioBookUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        if(null == selection) selection = "1";

        switch (match) {
            case AUDIO: {
                rowsDeleted = db.delete(AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if(rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED) || values.containsKey(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED)) {
            if(values.containsKey(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED)) {
                long dateAdded = values.getAsLong(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED);
                values.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_ADDED, AudioBookContract.normalizeDate(dateAdded));
            }
            if(values.containsKey(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED)) {
                long dateModified = values.getAsLong(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED);
                values.put(AudioBookContract.AudioBookEntry.TAC_COLUMN_DATE_MODIFIED, AudioBookContract.normalizeDate(dateModified));
            }
        }
    }

    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        int rowsUpdated;
        if(null == selection) selection = "1";

        switch (match) {
            case AUDIO: {
                normalizeDate(values);
                rowsUpdated = db.update(AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS, values, selection, selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if(rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case AUDIO:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(AudioBookContract.AudioBookEntry.TABLE_AUDIO_COLUMNS, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
