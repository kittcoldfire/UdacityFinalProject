package com.etechtour.audiobookplayer;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.ArrayMap;

import com.etechtour.audiobookplayer.database.AudioBookContract;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Phil on 5/17/2016.
 */
public class Utilities {

    public static Bitmap getAlbumArtwork(Context c, int albumId, int width, int height) {
        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId);

        //Logger.debug(albumArtUri.toString());
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(
                    c.getContentResolver(), albumArtUri);
            bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
            bitmap = BitmapFactory.decodeResource(c.getResources(),
                    R.drawable.placeholder);
        } catch (IOException e) {

            e.printStackTrace();
        }

        return bitmap;
    }

    public static ArrayMap getTotalBookDuration(Context context, Cursor c) {
        long duration = 0;
        long id;
        ArrayMap durations = new ArrayMap();

        c.moveToFirst();

        do {
            id = c.getLong(c.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID));

            String selectionClause = AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID + " = ?";
            String[] selectionArgs = new String[] { id + "" };

            String sortOrder = MediaStore.Audio.Media.TITLE;

            Cursor album = context.getContentResolver().query(
                    AudioBookContract.AudioBookEntry.CONTENT_URI,
                    null,
                    selectionClause,
                    selectionArgs,
                    sortOrder
            );

            if(album.moveToFirst()) {
                do {
                    duration += album.getLong(album.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION));
                } while (album.moveToNext());
            }

            durations.put(id, duration);
            duration = 0;
        } while (c.moveToNext());

        return durations;
    }

    public static int[] getProgressColors(long progress, long duration) {

        int color = 0xFFbedcd9;
        int white = 0xFFFFFF;
        double p = getPercentage(progress, duration);

        if(p < 1) {
            return new int[] {white,white,white,white,white,white,white,white,white,white};
        } else if(p <= 10) {
            return new int[] {color,white,white,white,white,white,white,white,white,white};
        } else if(p > 10 && p <= 20) {
            return new int[] {color,color,white,white,white,white,white,white,white,white};
        } else if(p > 20 && p <= 30) {
            return new int[] {color,color,color,white,white,white,white,white,white,white};
        } else if(p > 30 && p <= 40) {
            return new int[] {color,color,color,color,white,white,white,white,white,white};
        } else if(p > 40 && p <= 50) {
            return new int[] {color,color,color,color,color,white,white,white,white,white};
        } else if(p > 50 && p <= 60) {
            return new int[] {color,color,color,color,color,color,white,white,white,white};
        } else if(p > 60 && p <= 70) {
            return new int[] {color,color,color,color,color,color,color,white,white,white};
        } else if(p > 70 && p <= 80) {
            return new int[] {color,color,color,color,color,color,color,color,white,white};
        } else if(p > 80 && p <= 90) {
            return new int[] {color,color,color,color,color,color,color,color,color,white};
        } else if(p > 90 && p <= 100) {
            return new int[] {color,color,color,color,color,color,color,color,color,color};
        }

        return null;
    }

    public static double getPercentage(double num1, double num2) {
        return (num1 * 100.0f) / num2;
    }

    public static String getPercentageString(double num) {
        return String.format("%.2f%%", num);
    }

    public static long getTotalBookDuration(Cursor c, int id) {
        long duration = 0;

        c.moveToFirst();

        do {
            if(c.getInt(c.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_ALBUM_ID)) == id) {
                duration += c.getLong(c.getColumnIndex(AudioBookContract.AudioBookEntry.TAC_COLUMN_DURATION));
            }
        } while (c.moveToNext());

        return duration;
    }

    /**
     * Function to convert milliseconds time to Timer format
     * Hours:Minutes:Seconds
     */
    public static String milliSecondsToTime(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";
        String minutesString = "";

        //Convert total duration into time
        int hours = (int)(milliseconds / (1000*60*60));
        int minutes = (int)(milliseconds % (1000*60*60)) / (1000*60);
        int seconds = (int)((milliseconds % (1000*60*60)) % (1000*60) / 1000);
        //Add hours if there
        if(hours > 0) {
            finalTimerString = hours + ":";
        }

        //Prepending 0 to seconds if its one digit
        if(minutes < 10) {
            minutesString = "0" + minutes;
        } else {
            minutesString = "" + minutes;
        }

        //Prepending 0 to seconds if its one digit
        if(seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutesString + ":" + secondsString;

        //return timer string
        return finalTimerString;
    }
}
