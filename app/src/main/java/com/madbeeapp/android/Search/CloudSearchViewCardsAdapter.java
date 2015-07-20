package com.madbeeapp.android.Search;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.JSONArray;
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

public class CloudSearchViewCardsAdapter extends BaseAdapter {

    public static ListViewHolder mHolder = null;
    public ImageLoader imageLoader;
    ArrayList<SearchResponse> searchResponses;
    int mPosition;
    private Context mContext;
    private Common mApp;

    private View.OnClickListener onSearchItemLongClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
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
                        SearchResponse dto = searchResponses.get(mPosition);

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
                    }.execute();

                }
            });
            // Showing Alert Message
            alertDialog.show();
        }
    };

    public CloudSearchViewCardsAdapter(Context context, ArrayList<SearchResponse> searchResponses) {
        mContext = context;
        mApp = (Common) mContext.getApplicationContext();
        this.searchResponses = searchResponses;
        imageLoader = new ImageLoader(context);
    }

    @Override
    public int getCount() {
        return searchResponses.size();
    }

    @Override
    public Object getItem(int i) {
        return searchResponses.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    /**
     * Returns the individual row/child in the list/grid.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SearchResponse c = searchResponses.get(position);
        mPosition = position;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.cloud_list_view_item, parent, false);

            mHolder = new ListViewHolder();
            mHolder.leftImage = (ImageView) convertView.findViewById(R.id.listViewLeftIcon);
            mHolder.titleText = (TextView) convertView.findViewById(R.id.listViewTitleText);
            mHolder.subText = (TextView) convertView.findViewById(R.id.listViewSubText);
            mHolder.rightSubText = (TextView) convertView.findViewById(R.id.listViewRightSubText);
            mHolder.subTextParent = (RelativeLayout) convertView.findViewById(R.id.listViewSubTextParent);

            mHolder.titleText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.subText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.rightSubText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            convertView.setTag(mHolder);
        } else {
            mHolder = (ListViewHolder) convertView.getTag();
        }

        //Retrieve data from the cursor.
        String titleText = c.getSONG_TITLE();
        String artist = c.getSONG_ARTIST();
        String album = c.getSONG_ALBUM();
        String albumArtist = c.getSONG_ALBUM_ARTIST();
        String filePath = c.getSONG_ID();
        String genre = c.getSONG_GENRE();
        String year = c.getSONG_YEAR();
        String artworkPath = c.getSONG_ALBUM_ART_PATH();

        //Set the tags for this grid item.
        convertView.setTag(R.string.title_text, titleText);
        convertView.setTag(R.string.album, album);
        convertView.setTag(R.string.artist, artist);
        convertView.setTag(R.string.album_artist, albumArtist);
        convertView.setTag(R.string.file_path, filePath);
        convertView.setTag(R.string.genre, genre);
        convertView.setTag(R.string.year, year);
        convertView.setTag(R.string.artist_art_path, artworkPath);

        //Set the name text in the ListView.
        mHolder.titleText.setText(titleText);
        mHolder.subText.setText(artist);
        int seconds = (int) (Long.parseLong(c.getSONG_DURATION()) / 1000) % 60;
        int minutes = (int) (Long.parseLong(c.getSONG_DURATION()) / (1000 * 60) % 60);
        int hours = (int) (Long.parseLong(c.getSONG_DURATION()) / (1000 * 60 * 60) % 24);
        if (hours > 0) {
            mHolder.rightSubText.setText(hours + ":" + minutes + ":" + seconds);

        } else {
            mHolder.rightSubText.setText(minutes + ":" + seconds);
        }
        //Load the album art.
        if (mHolder.leftImage != null) {
            imageLoader.DisplayImage(artworkPath, mHolder.leftImage);
        }
        mHolder.leftImage.setOnClickListener(onSearchItemLongClickListener);
        return convertView;
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
     * Holder subclass for CloudListViewCardsAdapter.
     *
     * @author Arpit Gandhi
     */
    static class ListViewHolder {
        public ImageView leftImage;
        public TextView titleText;
        public TextView subText;
        public TextView rightSubText;
        public RelativeLayout subTextParent;
    }
}
