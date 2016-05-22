package com.etechtour.audiobookplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;

/**
 * Created by Phil on 4/22/2016.
 */
public class Settings extends PreferenceActivity {

    Preference prefAudiobook;
    String prefAudioBookLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefAudiobook = (Preference) findPreference(getString(R.string.pref_audiobook));
        prefAudioBookLocation = sharedPrefs.getString(getString(R.string.pref_audiobook_location), "0");
        if(prefAudioBookLocation.equals("0")) {
            prefAudiobook.setSummary(getString(R.string.pref_audiobook_location_missing));
        } else {
            prefAudiobook.setSummary(prefAudioBookLocation);
        }

        if(getIntent() != null) {
            if(getIntent().getExtras() != null) {
                int firstLaunch = getIntent().getExtras().getInt(getString(R.string.pref_firstlaunch));

                if(firstLaunch == 0) {
                    launchFileActivityChooser(300);
                }
            }
        }

        prefAudiobook.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                launchFileActivityChooser(0);
                return true;
            }
        });
    }

    private void launchFileActivityChooser(int resultCode) {
        // This always works
        Intent i = new Intent(getApplicationContext(), FilePickerActivity.class);
        // This works if you defined the intent filter
        // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

        // Set these depending on your use case. These are the defaults.
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        if(prefAudioBookLocation.equals("0")) {
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
        } else {
            i.putExtra(FilePickerActivity.EXTRA_START_PATH, prefAudioBookLocation);
        }

        Toast.makeText(getApplicationContext(), "Choose the folder containing your audiobooks.", Toast.LENGTH_LONG).show();
        startActivityForResult(i, resultCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String folder = "0";
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();

            folder = uri.getPath();

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(getString(R.string.pref_audiobook_location), folder);
            editor.commit();

            prefAudiobook.setSummary(folder);
        } else if (requestCode == 300 && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            folder = uri.getPath();

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(getString(R.string.pref_audiobook_location), folder);
            editor.commit();

            Intent in = new Intent(getApplicationContext(), Settings.class);
            setResult(300, in);
            finish();

        } else {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefAudioBookLocation = sharedPrefs.getString(getString(R.string.pref_audiobook_location), "0");
            if(prefAudioBookLocation.equals("0")) {
                prefAudiobook.setSummary(getString(R.string.pref_audiobook_location_missing));
            } else {
                prefAudiobook.setSummary(prefAudioBookLocation);
            }
        }
    }
}
