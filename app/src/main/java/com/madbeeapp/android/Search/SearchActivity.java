package com.madbeeapp.android.Search;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.madbeeapp.android.AsyncTasks.AsyncAddToQueueTask;
import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Prefs;
import com.splunk.mint.Mint;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Generic, multipurpose Search Activity.
 *
 * @author Arpit Gandhi
 */
public class SearchActivity extends ActionBarActivity {

    final long DELAY = 400; // in ms
    public Handler mHandler;
    AsyncTask<Void, Void, Void> asyncRunQuery;
    /**
     * Query runnable.
     */
    public Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            asyncRunQuery = new AsyncRunQuery();
            asyncRunQuery.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            ;
        }
    };
    View showMoreLayout;
    EditText searchView;
    ArrayList<SearchResponse> searchResponses = new ArrayList<>();
    boolean fromSoundCloud = false;
    Timer timer = new Timer();
    TextView soundcloudResults;
    RelativeLayout searchLayout;
    private Context mContext;
    private Common mApp;
    private String mQuery = "";
    private ListView mListView;
    private Cursor mCursor;
    private boolean loadDrawer = false;
    private String mQuerySelection = "";

    /**
     * Item click listener for the ListView.
     */
    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
            mApp.getPlaybackKickstarter()
                    .initPlayback(
                            mQuerySelection,
                            Common.PLAY_ALL_SONGS,
                            index,
                            true,
                            false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SearchActivity.this.onBackPressed();
                }
            }, 200);
        }

    };

    private AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {
            View playlistChooseDialog = View.inflate(mContext, R.layout.playlist_selection_dialog, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(
                    mContext).create();
            alertDialog.setView(playlistChooseDialog);
            alertDialog.setCancelable(true);
            final EditText newPlaylist = (EditText) playlistChooseDialog.findViewById(R.id.new_playlist);
            final ListView playlistView = (ListView) playlistChooseDialog.findViewById(R.id.list);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, R.layout.single_dialog_singlechoice, mApp.getDBAccessHelper().getPlaylistNames());
            arrayAdapter.remove("Trash");
            arrayAdapter.add("Trash");
            playlistView.setAdapter(arrayAdapter);
            playlistView.setDrawSelectorOnTop(true);

            if (arrayAdapter.getCount() == 0) {
                playlistView.setVisibility(View.GONE);
            }

            playlistChooseDialog.findViewById(R.id.create_playlist_button).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String code = newPlaylist.getText().toString();
                    if (!code.equals("")) {
                        if (!mApp.getDBAccessHelper().checkIfPlaylistExists(code)) {
                            arrayAdapter.insert(code, 0);
                            Toast.makeText(mContext, code, Toast.LENGTH_SHORT).show();
                            playlistView.setVisibility(View.VISIBLE);
                            newPlaylist.setText("");
                        }
                    }
                }
            });
            playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String selectedPlaylist = arrayAdapter.getItem(i);
                    if (mCursor.moveToFirst()) {
                        if (mCursor.move(position)) {
                            String filePath = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                            if (!mApp.getDBAccessHelper().checkIfSongIsPresentPlaylist(selectedPlaylist, filePath)) {
                                mApp.getDBAccessHelper().addPlaylistEntry(selectedPlaylist, mCursor);
                                loadDrawer = true;
                            }
                        }
                    }
                    Toast.makeText(mContext, "Added to " + selectedPlaylist, Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
            });
            // Showing Alert Message
            alertDialog.show();
            return true;
        }
    };

    /**
     * Item click listener for the ListView.
     */
    private AdapterView.OnItemClickListener onSearchItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, final int index, long id) {
            new AsyncTask<Void, Void, Void>() {
                SearchResponse dto = searchResponses.get(index);

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    Common.show_PDialog(mContext, "Just a moment..", false);
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    if (mApp.getDBAccessHelper().getCount(DBAccessHelper.SONG_ID, dto.getSONG_ID()) == 0) {
                        String songTitle = dto.getSONG_TITLE();
                        String songArtist = dto.getSONG_ARTIST();
                        String songAlbum = dto.getSONG_ALBUM();
                        String songAlbumArtist = dto.getSONG_ALBUM_ARTIST();
                        String songFilePath = "";
                        String songGenre = dto.getSONG_GENRE();
                        String songDuration = dto.getSONG_DURATION();
                        String songTrackNumber = dto.getSONG_TRACK_NUMBER();
                        String songYear = dto.getSONG_YEAR();
                        String songDateModified = dto.getSONG_LAST_MODIFIED();
                        String songId = dto.getSONG_ID();
                        String songAlbumArtPath = dto.getSONG_ALBUM_ART_PATH();
                        String soundcloudAlbumArt = dto.getSONG_ALBUM_ART_PATH();
                        songDateModified = timeinMillis(songDateModified);
                        //Check if any of the other tags were empty/null and set them to "Unknown xxx" values.
                        if (songArtist == null || songArtist.isEmpty()) {
                            songArtist = mContext.getResources().getString(R.string.unknown_artist);
                        }

                        if (songAlbumArtist == null || songAlbumArtist.isEmpty()) {
                            if (songArtist != null && !songArtist.isEmpty()) {
                                songAlbumArtist = songArtist;
                            } else {
                                songAlbumArtist = mContext.getResources().getString(R.string.unknown_album_artist);
                            }
                        }

                        if (songAlbum == null || songAlbum.isEmpty()) {
                            songAlbum = mContext.getResources().getString(R.string.unknown_album);
                        }

                        if (songGenre == null || songGenre.isEmpty()) {
                            songGenre = mContext.getResources().getString(R.string.unknown_genre);
                        }

                        if (soundcloudAlbumArt != null) {
                            soundcloudAlbumArt = soundcloudAlbumArt.replaceAll("-large", "-t500x500");
                        }

                        try {
                            Uri.Builder builder = new Uri.Builder();
                            builder.scheme("https")
                                    .authority("itunes.apple.com")
                                    .appendPath("search")
                                    .appendQueryParameter("term", songTitle.replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]+$", ""));
                            final String url = builder.build().toString();
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url(url)
                                    .build();
                            String response = client.newCall(request).execute().body().string();
                            if (response != null) {
                                JSONObject jsonObject = new JSONObject(response);
                                int count = jsonObject.getInt("resultCount");
                                if (count > 0) {
                                    JSONArray jsonArray = jsonObject.getJSONArray("results");
                                    JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                                    songArtist = jsonObject1.getString("artistName");
                                    songGenre = jsonObject1.getString("primaryGenreName");
                                    songAlbum = jsonObject1.getString("collectionName");
                                    songTitle = jsonObject1.getString("trackName");
                                    if (jsonObject1.has("artworkUrl60")) {
                                        String artworkUrl60 = jsonObject1.getString("artworkUrl60");
                                        soundcloudAlbumArt = artworkUrl60.replaceAll(".60x60-50", ".600x600-100");
                                    } else {
                                        String artworkUrl60 = jsonObject1.getString("artworkUrl100");
                                        soundcloudAlbumArt = artworkUrl60.replaceAll(".100x100-75", ".600x600-100");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            songFilePath = mContext.getFilesDir().getPath() + "/music/" + songId + ".mp3";
                            URL url = new URL(soundcloudAlbumArt);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setDoInput(true);
                            conn.connect();
                            InputStream is = conn.getInputStream();
                            Bitmap bm = BitmapFactory.decodeStream(is);
                            File myDir = new File(mContext.getFilesDir().getPath() + "/album_art/");
                            File mDir = new File(mContext.getFilesDir().getPath() + "/music/");

                            myDir.mkdirs();
                            mDir.mkdirs();
                            FileOutputStream fos = new FileOutputStream(mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg");

                            ByteArrayOutputStream outstream = new ByteArrayOutputStream();

                            bm.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                            byte[] byteArray = outstream.toByteArray();

                            fos.write(byteArray);
                            fos.close();
                            songAlbumArtPath = "file:" + mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg";
                            Log.d("", "" + songAlbumArtPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ContentValues values = new ContentValues();
                        values.put(DBAccessHelper.SONG_TITLE, songTitle);
                        values.put(DBAccessHelper.SONG_ARTIST, songArtist);
                        values.put(DBAccessHelper.SONG_ALBUM, songAlbum);
                        values.put(DBAccessHelper.SONG_ALBUM_ARTIST, songAlbumArtist);
                        values.put(DBAccessHelper.SONG_DURATION, convertMillisToMinsSecs(songDuration));
                        values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                        values.put(DBAccessHelper.SONG_TRACK_NUMBER, songTrackNumber);
                        values.put(DBAccessHelper.SONG_GENRE, songGenre);
                        values.put(DBAccessHelper.SONG_YEAR, songYear);
                        values.put(DBAccessHelper.SONG_PLAY_COUNT, "0");
                        values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                        values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, soundcloudAlbumArt);
                        values.put(DBAccessHelper.SONG_LAST_MODIFIED, songDateModified);
                        values.put(DBAccessHelper.ADDED_TIMESTAMP, dto.getADDED_TIMESTAMP());
                        values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, songDateModified);
                        values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);
                        values.put(DBAccessHelper.SONG_ID, songId);
                        //Add all the entries to the database to build the songs library.
                        mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                                null,
                                values);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    Toast.makeText(mContext, "Playback will start in a while.", Toast.LENGTH_LONG).show();
                    String selection = DBAccessHelper.SONG_ID + "=" + "'" + dto.getSONG_ID() + "'";
                    mApp.getPlaybackKickstarter()
                            .initPlayback(
                                    selection,
                                    Common.PLAY_ALL_SONGS,
                                    0,
                                    true,
                                    false);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Common.dialog.dismiss();
                            new AsyncAddToQueueTask(mContext, Common.RECENTLY_PLAYED_FRAGMENT, null, null, null, null, null).execute();
                        }
                    }, 2000);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            ;
        }
    };

    private AdapterView.OnItemLongClickListener onSearchItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {
            View playlistChooseDialog = View.inflate(mContext, R.layout.playlist_selection_dialog, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(
                    mContext).create();
            alertDialog.setView(playlistChooseDialog);
            alertDialog.setCancelable(true);
            final EditText newPlaylist = (EditText) playlistChooseDialog.findViewById(R.id.new_playlist);
            final ListView playlistView = (ListView) playlistChooseDialog.findViewById(R.id.list);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, R.layout.single_dialog_singlechoice, mApp.getDBAccessHelper().getPlaylistNames());
            arrayAdapter.remove("Trash");
            playlistView.setAdapter(arrayAdapter);
            playlistView.setDrawSelectorOnTop(true);

            if (arrayAdapter.getCount() == 0) {
                playlistView.setVisibility(View.GONE);
            }

            playlistChooseDialog.findViewById(R.id.create_playlist_button).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String code = newPlaylist.getText().toString();
                    if (!code.equals("")) {
                        if (!mApp.getDBAccessHelper().checkIfPlaylistExists(code)) {
                            arrayAdapter.insert(code, 0);
                            Toast.makeText(mContext, code, Toast.LENGTH_SHORT).show();
                            playlistView.setVisibility(View.VISIBLE);
                            newPlaylist.setText("");
                        }
                    }
                }
            });
            playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                    new AsyncTask<Void, Void, Void>() {
                        final String selectedPlaylist = arrayAdapter.getItem(i);
                        SearchResponse dto = searchResponses.get(position);

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            Common.show_PDialog(mContext, "Adding to playlist..", false);
                        }

                        @Override
                        protected Void doInBackground(Void... voids) {
                            if (mApp.getDBAccessHelper().getCount(DBAccessHelper.SONG_ID, dto.getSONG_ID()) == 0) {
                                String songTitle = dto.getSONG_TITLE();
                                String songArtist = dto.getSONG_ARTIST();
                                String songAlbum = dto.getSONG_ALBUM();
                                String songAlbumArtist = dto.getSONG_ALBUM_ARTIST();
                                String songFilePath = "";
                                String songGenre = dto.getSONG_GENRE();
                                String songDuration = dto.getSONG_DURATION();
                                String songTrackNumber = dto.getSONG_TRACK_NUMBER();
                                String songYear = dto.getSONG_YEAR();
                                String songDateModified = dto.getSONG_LAST_MODIFIED();
                                String songId = dto.getSONG_ID();
                                String songAlbumArtPath = dto.getSONG_ALBUM_ART_PATH();
                                String soundcloudAlbumArt = dto.getSONG_ALBUM_ART_PATH();
                                songDateModified = timeinMillis(songDateModified);
                                //Check if any of the other tags were empty/null and set them to "Unknown xxx" values.
                                if (songArtist == null || songArtist.isEmpty()) {
                                    songArtist = mContext.getResources().getString(R.string.unknown_artist);
                                }

                                if (songAlbumArtist == null || songAlbumArtist.isEmpty()) {
                                    if (songArtist != null && !songArtist.isEmpty()) {
                                        songAlbumArtist = songArtist;
                                    } else {
                                        songAlbumArtist = mContext.getResources().getString(R.string.unknown_album_artist);
                                    }
                                }

                                if (songAlbum == null || songAlbum.isEmpty()) {
                                    songAlbum = mContext.getResources().getString(R.string.unknown_album);
                                }

                                if (songGenre == null || songGenre.isEmpty()) {
                                    songGenre = mContext.getResources().getString(R.string.unknown_genre);
                                }

                                if (soundcloudAlbumArt != null) {
                                    soundcloudAlbumArt = soundcloudAlbumArt.replaceAll("-large", "-t500x500");
                                }

                                try {
                                    Uri.Builder builder = new Uri.Builder();
                                    builder.scheme("https")
                                            .authority("itunes.apple.com")
                                            .appendPath("search")
                                            .appendQueryParameter("term", songTitle);
                                    final String url = builder.build().toString();
                                    OkHttpClient client = new OkHttpClient();
                                    Request request = new Request.Builder()
                                            .url(url)
                                            .build();
                                    String response = client.newCall(request).execute().body().string();
                                    if (response != null) {
                                        JSONObject jsonObject = new JSONObject(response);
                                        int count = jsonObject.getInt("resultCount");
                                        if (count > 0) {
                                            JSONArray jsonArray = jsonObject.getJSONArray("results");
                                            JSONObject jsonObject1 = jsonArray.getJSONObject(0);
                                            songArtist = jsonObject1.getString("artistName");
                                            songGenre = jsonObject1.getString("primaryGenreName");

                                            if (jsonObject1.has("artworkUrl60")) {
                                                String artworkUrl60 = jsonObject1.getString("artworkUrl60");
                                                soundcloudAlbumArt = artworkUrl60.replaceAll(".60x60-50", ".600x600-100");
                                            } else if (jsonObject1.has("artworkUrl100")) {
                                                String artworkUrl60 = jsonObject1.getString("artworkUrl100");
                                                soundcloudAlbumArt = artworkUrl60.replaceAll(".100x100-75", ".600x600-100");
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                try {
                                    songFilePath = mContext.getFilesDir().getPath() + "/music/" + songId + ".mp3";
                                    URL url = new URL(soundcloudAlbumArt);
                                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                    conn.setDoInput(true);
                                    conn.connect();
                                    InputStream is = conn.getInputStream();
                                    Bitmap bm = BitmapFactory.decodeStream(is);
                                    File myDir = new File(mContext.getFilesDir().getPath() + "/album_art/");
                                    File mDir = new File(mContext.getFilesDir().getPath() + "/music/");

                                    myDir.mkdirs();
                                    mDir.mkdirs();
                                    FileOutputStream fos = new FileOutputStream(mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg");

                                    ByteArrayOutputStream outstream = new ByteArrayOutputStream();

                                    bm.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
                                    byte[] byteArray = outstream.toByteArray();

                                    fos.write(byteArray);
                                    fos.close();
                                    songAlbumArtPath = "file:" + mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg";
                                    Log.d("", "" + songAlbumArtPath);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                ContentValues values = new ContentValues();
                                values.put(DBAccessHelper.SONG_TITLE, songTitle);
                                values.put(DBAccessHelper.SONG_ARTIST, songArtist);
                                values.put(DBAccessHelper.SONG_ALBUM, songAlbum);
                                values.put(DBAccessHelper.SONG_ALBUM_ARTIST, songAlbumArtist);
                                values.put(DBAccessHelper.SONG_DURATION, convertMillisToMinsSecs(songDuration));
                                values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                                values.put(DBAccessHelper.SONG_TRACK_NUMBER, songTrackNumber);
                                values.put(DBAccessHelper.SONG_GENRE, songGenre);
                                values.put(DBAccessHelper.SONG_YEAR, songYear);
                                values.put(DBAccessHelper.SONG_PLAY_COUNT, "0");
                                values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                                values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, soundcloudAlbumArt);
                                values.put(DBAccessHelper.SONG_LAST_MODIFIED, songDateModified);
                                values.put(DBAccessHelper.ADDED_TIMESTAMP, dto.getADDED_TIMESTAMP());
                                values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, songDateModified);
                                values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);
                                values.put(DBAccessHelper.SONG_ID, songId);
                                //Add all the entries to the database to build the songs library.
                                mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                                        null,
                                        values);
                                if (!mApp.getDBAccessHelper().checkIfSongIsPresentPlaylist(selectedPlaylist, songFilePath)) {
                                    values.put(DBAccessHelper.PLAYLIST_NAME, selectedPlaylist);
                                    values.put(DBAccessHelper.PLAYLIST_PLAY_COUNT, "0");
                                    mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.PLAYLIST_TABLE,
                                            null,
                                            values);
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Common.dialog.dismiss();
                            alertDialog.dismiss();
                            Toast.makeText(mContext, "Added to " + selectedPlaylist, Toast.LENGTH_SHORT).show();
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    ;
                }
            });
            // Showing Alert Message
            alertDialog.show();
            return true;
        }
    };

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("loadDrawer", loadDrawer);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    private String timeinMillis(String givenDateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/mm/dd HH:mm:ss z", Locale.US);
        try {
            Date mDate = sdf.parse(givenDateString);
            long timeInMilliseconds = mDate.getTime();
            System.out.println("Date in milli :: " + timeInMilliseconds);
            return String.valueOf(timeInMilliseconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mContext = this;
        Mint.initAndStartSession(SearchActivity.this, "b48ec200");
        if (Prefs.getMobileNumber(mContext) != null && !Prefs.getMobileNumber(mContext).equals(""))
            Mint.setUserIdentifier(Prefs.getMobileNumber(mContext));
        handleIntent(getIntent());
        mApp = (Common) this.getApplicationContext();
        mHandler = new Handler();
        initSearch();

        showMoreLayout = View.inflate(mContext, R.layout.show_more_layout, null);
        Button showMore = (Button) showMoreLayout.findViewById(R.id.showmore);
        showMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fromSoundCloud = true;
                mListView.setVisibility(View.GONE);
                searchLayout.setVisibility(View.VISIBLE);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        searchSoundcloud();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        searchLayout = (RelativeLayout) findViewById(R.id.search_layout);
        mListView = (ListView) findViewById(R.id.generalListView);
        mListView.setVerticalScrollBarEnabled(false);

        //Apply the ListViews' dividers.
        mListView.setDivider(ContextCompat.getDrawable(mContext, R.drawable.icon_list_divider_light));
        mListView.setDividerHeight(1);

        //Create a set of options to optimize the bitmap memory usage.
        soundcloudResults = (TextView) findViewById(R.id.checkbox);
        soundcloudResults.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        searchLayout.setVisibility(View.GONE);

        findViewById(R.id.soundcloud_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://soundcloud.com"));
                startActivity(browserIntent);
            }
        });

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });
    }

    void initSearch() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.action_bar);
        setSupportActionBar(toolbar);
        searchView = (EditText) findViewById(R.id.search_bar);

        findViewById(android.R.id.home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(final CharSequence s, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        search(s.toString());
                    }
                }, DELAY);
            }
        });

        if (Intent.ACTION_SEARCH.equals(getIntent().getAction())) {
            String query = getIntent().getStringExtra(SearchManager.QUERY);
            searchView.setText(query);
        }
        searchView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            search(query);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    void search(String query) {
        mQuery = query;
        if (query != null && !query.equals("")) {
            if (asyncRunQuery != null)
                if (asyncRunQuery.getStatus() == AsyncTask.Status.RUNNING) {
                    asyncRunQuery.cancel(true);
                }
            if (mHandler != null)
                mHandler.post(queryRunnable);
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    searchLayout.setVisibility(View.GONE);
                    mListView.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * Builds the cursor query's selection clause based on the activity's
     * current usage case.
     */
    private void buildQuerySelectionClause(String filter) {
        mQuerySelection = DBAccessHelper.SONG_TITLE + " LIKE " + "'%" + filter.replaceAll("'", "''") + "%'"
                + " OR " + DBAccessHelper.SONG_ALBUM + " LIKE " + "'%" + filter.replaceAll("'", "''") + "%'"
                + " OR " + DBAccessHelper.SONG_ARTIST + " LIKE " + "'%" + filter.replaceAll("'", "''") + "%'"
                + " OR " + DBAccessHelper.SONG_ALBUM_ARTIST + " LIKE " + "'%" + filter.replaceAll("'", "''") + "%'"
                + " OR " + DBAccessHelper.SONG_YEAR + " LIKE " + "'%" + filter.replaceAll("'", "''") + "%'"
                + " OR " + DBAccessHelper.SONG_GENRE + " LIKE " + "'%" + filter.replaceAll("'", "''") + "%'";
    }

    void searchSoundcloud() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority("api.soundcloud.com")
                .appendPath("tracks")
                .appendQueryParameter("q", mQuery)
                .appendQueryParameter("client_id", "cf72ae2acdb9737fac2c9f3951958a59")
                .appendQueryParameter("format", "json")
                .appendQueryParameter("limit", "30");
        final String url = builder.build().toString();

        String response = requesUrl(url);
        if (response != null) {
            parseResponse(response);
        }
    }

    void parseResponse(String response) {
        try {
            final JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() > 0) {
                searchResponses.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (jsonObject.get("kind").equals("track")) {
                            searchResponses.add(new SearchResponse(jsonObject.get("id").toString(), jsonObject.get("title").toString(), jsonObject.getJSONObject("user").get("username").toString()
                                    , "SoundCloud", "SoundCloud", jsonObject.get("duration").toString(), null, jsonObject.get("id").toString(), jsonObject.get("genre").toString()
                                    , "0", jsonObject.get("created_at").toString().substring(0, 4), "0", "0", "0", jsonObject.get("last_modified").toString()
                                    , "0", jsonObject.get("last_modified").toString(), "0", "", DBAccessHelper.SOUNDCLOUD, jsonObject.get("artwork_url").toString()
                                    , "0", jsonObject.getJSONObject("user").get("avatar_url").toString(), "0", "", "", "", DBAccessHelper.SOUNDCLOUD, jsonObject.getString("likes_count"), jsonObject.getString("playback_count")));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mQuery != null && !mQuery.equals("")) {
                            if (searchResponses.size() > 0) {
                                mListView.setVisibility(View.VISIBLE);
                                mListView.setAdapter(new CloudSearchViewCardsAdapter(mContext, searchResponses));
                                mListView.setOnItemClickListener(onSearchItemClickListener);
                                mListView.setOnItemLongClickListener(onSearchItemLongClickListener);
                            } else {
                                Toast.makeText(mContext, "No results found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    String requesUrl(String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException ignore) {
        }
        return null;
    }

    String convertMillisToMinsSecs(String duration) {
        int seconds = (int) (Long.parseLong(duration) / 1000) % 60;
        int minutes = (int) (Long.parseLong(duration) / (1000 * 60) % 60);
        int hours = (int) (Long.parseLong(duration) / (1000 * 60 * 60) % 24);
        if (hours > 0) {
            return (hours + ":" + minutes + ":" + seconds);
        } else {
            return (minutes + ":" + seconds);
        }
    }

    /**
     * Runs the correct DB query and
     * displays in the ListView.
     *
     * @author Arpit Gandhi
     */
    public class AsyncRunQuery extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mListView.removeFooterView(showMoreLayout);
        }

        @Override
        protected Void doInBackground(Void... params) {
            buildQuerySelectionClause(mQuery);
            mCursor = mApp.getDBAccessHelper().getFragmentCursor(mQuerySelection, Common.SONGS_FRAGMENT);
            if (mCursor.getCount() == 0) {
                if (Common.isNetworkAvailable(mContext)) {
                    fromSoundCloud = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListView.removeFooterView(showMoreLayout);
                            mListView.setVisibility(View.GONE);
                            searchLayout.setVisibility(View.VISIBLE);
                        }
                    });
                    searchSoundcloud();
                }
            } else {
                fromSoundCloud = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mListView.addFooterView(showMoreLayout);
                        searchLayout.setVisibility(View.GONE);
                    }
                });
            }
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!fromSoundCloud) {
                mListView.setVisibility(View.VISIBLE);
                SearchViewCardsAdapter mListViewAdapter = new SearchViewCardsAdapter(mContext, mCursor, SearchActivity.this);
                mListView.setAdapter(mListViewAdapter);
                mListView.setOnItemClickListener(onItemClickListener);
                mListView.setOnItemLongClickListener(onItemLongClickListener);
            }
        }
    }
}
