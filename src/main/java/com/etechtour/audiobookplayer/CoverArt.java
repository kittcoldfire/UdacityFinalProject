package com.etechtour.audiobookplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.etechtour.audiobookplayer.adapters.AlbumArtAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Phil on 5/21/2016.
 */
public class CoverArt extends AppCompatActivity {

    EditText txtText;
    ListView lvCoverArt;
    Button btnSearch;
    ProgressBar progressBar;
    int albumID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_coverart);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
        }

        txtText = (EditText) findViewById(R.id.coverart_textview);
        lvCoverArt = (ListView) findViewById(R.id.cover_list);
        btnSearch = (Button) findViewById(R.id.coverart_btn_search);
        progressBar = (ProgressBar) findViewById(R.id.coverart_progress);

        Intent i = getIntent();
        albumID = i.getExtras().getInt(getString(R.string.cover_albumid));
        String albumTitle = i.getExtras().getString(getString(R.string.cover_title));

        txtText.setText(albumTitle);
        txtText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    btnSearch.callOnClick();
                    handled = true;
                }
                return handled;
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = String.valueOf(txtText.getText());
                if(input != null && !input.equals("")) {
                    progressBar.setVisibility(View.VISIBLE);
                    lvCoverArt.setVisibility(View.GONE);
                    new DownloadBooks().execute(input);
                } else {
                    txtText.setError("Please enter a title to search for.");
                }
            }
        });

        new DownloadBooks().execute(albumTitle);
    }

    @SuppressWarnings("ResourceType")
    private static Bitmap getDefaultArtwork(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(
                context.getResources().openRawResource(R.drawable.placeholder   ), null, opts);
    }

    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    static {
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptions.inDither = false;
    }

    // copied from MediaProvider
    private static boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            // we will not attempt to create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            try {
                return file.createNewFile();
            } catch(IOException ioe) {
                Log.e(CoverArt.class.getSimpleName(), "File creation failed", ioe);
            }
            return false;
        }
    }

    //heavily used http://www.netmite.com/android/mydroid/packages/apps/Music/src/com/android/music/MusicUtils.java
    //for help here on updating the MediaStore album art

    private void updateCoverArtInDB(final int albumID, String url) {
        final ContentResolver res = getApplicationContext().getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, albumID);
        if (uri != null) {
            // The album art thumbnail does not actually exist. Maybe the user deleted it, or
            // maybe it never existed to begin with.

            Glide.with(getApplicationContext())
                    .load(url)
                    .asBitmap()
                    .into(new SimpleTarget<Bitmap>(200, 200) {
                        @Override
                        public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                            Bitmap bm = bitmap;
                            if (bm != null) {
                                // Put the newly found artwork in the database.
                                // Note that this shouldn't be done for the "unknown" album,
                                // but if this method is called correctly, that won't happen.

                                // first write it somewhere
                                String file = Environment.getExternalStorageDirectory()
                                        + "/albumthumbs/" + String.valueOf(System.currentTimeMillis());
                                if (ensureFileExists(file)) {
                                    try {
                                        OutputStream outstream = new FileOutputStream(file);
                                        if (bm.getConfig() == null) {
                                            bm = bm.copy(Bitmap.Config.RGB_565, false);
                                            if (bm == null) {
                                                //return getDefaultArtwork(context);
                                            }
                                        }
                                        boolean success = bm.compress(Bitmap.CompressFormat.JPEG, 75, outstream);
                                        outstream.close();
                                        if (success) {
                                            ContentValues values = new ContentValues();
                                            values.put("album_id", albumID);
                                            values.put("_data", file);
                                            Uri newuri = res.insert(sArtworkUri, values);
                                            if (newuri == null) {
                                                // Failed to insert in to the database. The most likely
                                                // cause of this is that the item already existed in the
                                                // database, and the most likely cause of that is that
                                                // the album was scanned before, but the user deleted the
                                                // album art from the sd card.
                                                // We can ignore that case here, since the media provider
                                                // will regenerate the album art for those entries when
                                                // it detects this.
                                                success = false;
                                            }
                                        }
                                        if (!success) {
                                            File f = new File(file);
                                            f.delete();
                                        }
                                    } catch (FileNotFoundException e) {
                                        Log.e(CoverArt.class.getSimpleName(), "error creating file", e);
                                    } catch (IOException e) {
                                        Log.e(CoverArt.class.getSimpleName(), "error creating file", e);
                                    }
                                }
                            } else {
                                bm = getDefaultArtwork(getApplicationContext());
                            }
                        }
                    });
        }
    }

    private class DownloadBooks extends AsyncTask<String, Integer, ArrayList<AlbumArt>> {

        @Override
        protected ArrayList<AlbumArt> doInBackground(String... params) {

            String albumTitle = params[0];

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String bookJsonString = null;

            try {
                final String FORECAST_BASE_URL = "https://www.googleapis.com/books/v1/volumes?";
                final String QUERY_PARAM = "q";

                final String ISBN_PARAM = "title:" + albumTitle;

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, ISBN_PARAM)
                        .build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                    buffer.append("\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                bookJsonString = buffer.toString();
            } catch (Exception e) {
                Log.e(CoverArt.class.getSimpleName(), "Error ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(CoverArt.class.getSimpleName(), "Error closing stream", e);
                    }
                }

            }

            final String ITEMS = "items";

            final String VOLUME_INFO = "volumeInfo";

            final String TITLE = "title";
            final String SUBTITLE = "subtitle";
            final String AUTHORS = "authors";
            final String DESC = "description";
            final String CATEGORIES = "categories";
            final String IMG_URL_PATH = "imageLinks";
            final String IMG_URL = "thumbnail";

            ArrayList<AlbumArt> arrayOfUsers = new ArrayList<AlbumArt>();
            if(bookJsonString != null) {
                try {
                    JSONObject bookJson = new JSONObject(bookJsonString);
                    JSONArray bookArray = null;
                    if (bookJson.has(ITEMS)) {
                        bookArray = bookJson.getJSONArray(ITEMS);
                    }

                    if (bookArray != null && bookArray.length() > 0) {
                        for (int x = 0; x < bookArray.length(); x++) {
                            JSONObject bookInfo = ((JSONObject) bookArray.get(x)).getJSONObject(VOLUME_INFO);

                            String title = bookInfo.getString(TITLE);

                            String subtitle = "";
                            if (bookInfo.has(SUBTITLE)) {
                                subtitle = bookInfo.getString(SUBTITLE);
                            }

                            String desc = "";
                            if (bookInfo.has(DESC)) {
                                desc = bookInfo.getString(DESC);
                            }

                            String imgUrl = "";
                            if (bookInfo.has(IMG_URL_PATH) && bookInfo.getJSONObject(IMG_URL_PATH).has(IMG_URL)) {
                                imgUrl = bookInfo.getJSONObject(IMG_URL_PATH).getString(IMG_URL);

                                arrayOfUsers.add(new AlbumArt(albumID, title, imgUrl));
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(CoverArt.class.getSimpleName(), "Error ", e);
                }
            }

            return arrayOfUsers;
        }

        @Override
        protected void onPostExecute(ArrayList<AlbumArt> albumArts) {
            super.onPostExecute(albumArts);

            final AlbumArtAdapter adapter = new AlbumArtAdapter(getApplicationContext(), albumArts);
            if(lvCoverArt != null && adapter.getCount() > 0) {
                progressBar.setVisibility(View.GONE);
                lvCoverArt.setVisibility(View.VISIBLE);
                lvCoverArt.setAdapter(adapter);

                lvCoverArt.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        AlbumArt art = adapter.getItem(position);

                        int albumId = art.getID();
                        String url = art.getUrl();

                        updateCoverArtInDB(albumId, url);

                        Intent in = new Intent(getApplicationContext(), CoverArt.class);
                        in.putExtra("albumId", albumId);
                        in.putExtra("url", url);
                        setResult(200, in);
                        finish();
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "No results found, please try another search!", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        }
    }
}
