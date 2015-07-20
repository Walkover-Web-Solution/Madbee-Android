package com.madbeeapp.android.syncadapter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Utils.CSVWriter;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Prefs;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    public final Context mContext;
    Common mApp;
    private boolean csv_status = false;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.mContext = context;
        mApp = (Common) mContext.getApplicationContext();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        mApp.getDBAccessHelper().deleteAllOldSoundcloudFiles();
        mApp.getDBAccessHelper().deleteAllNonPlayedSoundcloudFiles();
        mApp.getDBAccessHelper().deleteAllOldTrashFiles();

        if (Common.isNetworkAvailable(mContext)) {
            if (Prefs.getMobileNumber(mContext) != null && !Prefs.getMobileNumber(mContext).equals("")) {
                createCSV();
                if (csv_status)
                    exportCSV();
                uploadPlaylist();
                saveFriendsPlaylist();
                getAllLiked();
                getTopTrending();
            }
        }
    }

    public void uploadPlaylist() {
        Cursor cursor = mApp.getDBAccessHelper().getTop30PlayedTracks();
        if (cursor.moveToFirst()) {
            do {
                String return_res;
                String songId = "";
                String artUrl = "";
                String source = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_SOURCE));
                if (source != null) {
                    if (source.equals(DBAccessHelper.SOUNDCLOUD)) {
                        songId = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_ID));
                        artUrl = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH));
                        if (artUrl == null || artUrl.equals("")) {
                            artUrl = "";
                        }
                    }
                }
                long lastModified = Long.parseLong(cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_LAST_MODIFIED)));
                long lastPlayed = Long.parseLong(cursor.getString(cursor.getColumnIndex(DBAccessHelper.LAST_PLAYED_TIMESTAMP)));
                Date lastModifiedDate = new Date(lastModified);
                Date lastPlayedDate = new Date(lastPlayed);
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                String title = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_TITLE)).replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", "");
                String album = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_ALBUM)).replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", "");
                String artist = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_ARTIST)).replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", "");
                String genre = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_GENRE)).replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", "");
                String duration = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_DURATION));

                //printing value of Date
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("api.madbeeapp.com")
                        .appendPath("api")
                        .appendPath("saveplaylist")
                        .appendQueryParameter("countryCode", Prefs.getCountryCode(mContext))
                        .appendQueryParameter("numbers", Prefs.getMobileNumber(mContext))
                        .appendQueryParameter("cloud_id", songId)
                        .appendQueryParameter("title", title)
                        .appendQueryParameter("album", album)
                        .appendQueryParameter("artist", artist)
                        .appendQueryParameter("genre", genre)
                        .appendQueryParameter("play_count", cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_PLAY_COUNT)))
                        .appendQueryParameter("duration", duration)
                        .appendQueryParameter("last_played", df.format(lastPlayedDate))
                        .appendQueryParameter("last_modified", df.format(lastModifiedDate));

                if (artUrl != null && !artUrl.equals("null")) {
                    builder.appendQueryParameter("art_url", artUrl);
                } else {
                    builder.appendQueryParameter("art_url", "");
                }

                String url = builder.build().toString();
                return_res = getResponse(url);
                if (return_res != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(return_res);
                        if (jsonObject.getJSONObject("response").getInt("type") == 0) {
                            Log.e("URL: ", "" + url);
                            Log.e("Response", "" + return_res);
                        }
                    } catch (JSONException e) {
                        Log.e("URL: ", "" + url);
                        e.printStackTrace();
                    }
                }
            } while (cursor.moveToNext());
        }
    }

    private void createCSV() {
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/contacts.csv"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            HashMap<String, String> list = mApp.getNumberList();
            if (list != null) {
                for (String number : list.keySet()) {
                    String name = list.get(number);
                    writer.writeNext(new String[]{name, number});
                }
                csv_status = true;
            } else {
                csv_status = false;
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exportCSV() {
        File CSVFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/contacts.csv");
        String response = postFile(CSVFile);
        postFilemadbee(CSVFile);
        if (response != null) {
            fileUploadResponse(response);
        }
    }

    private void postFilemadbee(File file) {
        try {
            String postReceiverUrl = "http://api.madbeeapp.com/api/insertcontact?number=" + Prefs.getMobileNumber(mContext);
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(postReceiverUrl);
            FileBody fileBody = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("file", fileBody);
            httpPost.setEntity(reqEntity);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                String responseStr = EntityUtils.toString(resEntity).trim();
                Log.d("", "madbee Upload Response: " + responseStr);
            }
            if (file.exists())
                file.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String postFile(File file) {
        String fileName = null;
        try {
            String postReceiverUrl = "http://control.msg91.com/api/fileUpload.php?authkey=87555AyeqlYXqFO1558d0ba5&id=file2&type=3&name=file2&filename=contacts.csv";
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(postReceiverUrl);
            FileBody fileBody = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("file2", fileBody);
            httpPost.setEntity(reqEntity);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                String responseStr = EntityUtils.toString(resEntity).trim();
                if (responseStr != null) {
                    JSONObject jsonObject = new JSONObject(responseStr);
                    if (jsonObject.has("msg"))
                        fileName = jsonObject.getString("msg");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public String fileUploadResponse(String fileName) {
        String return_res;
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("control.msg91.com")
                .appendPath("api")
                .appendPath("importContact.php")
                .appendQueryParameter("authkey", "87555AyeqlYXqFO1558d0ba5")
                .appendQueryParameter("group[]", "558d0b9a09cf49851e8b4568")
                .appendQueryParameter("imp_exField[0]", "name")
                .appendQueryParameter("imp_exField[1]", "number")
                .appendQueryParameter("fieldType", "{\"0\":\"1\",\"1\":\"3\"}")
                .appendQueryParameter("file_name", fileName);
        return_res = getResponse(builder.build().toString());
        return return_res;
    }

    String getResponse(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            return client.newCall(request).execute().body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void saveFriendsPlaylist() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("api.madbeeapp.com")
                .appendPath("api")
                .appendPath("getlibrarylist")
                .appendQueryParameter("number", Prefs.getMobileNumber(mContext));

        String return_res = getResponse(builder.build().toString());
        if (return_res != null) {
            try {
                JSONObject responseJsonObject = new JSONObject(return_res);
                if (responseJsonObject.getJSONObject("response").getInt("type") == 1) {
                    mApp.getDBAccessHelper().deleteAllFriends();
                    JSONArray jsonArray = responseJsonObject.getJSONArray("data");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String songId = jsonObject.getString("sound_cloud_id");
                            String phoneNumber = jsonObject.getString("number");
                            String art_url = jsonObject.getString("album_art_url");

                            String songFilePath = mContext.getFilesDir().getPath() + "/music/" + songId + ".mp3";
                            String songAlbumArtPath = "file:" + mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg";

                            ContentValues values = new ContentValues();
                            values.put(DBAccessHelper.CONTACT_NUMBER, phoneNumber);
                            values.put(DBAccessHelper.SONG_ID, songId);
                            values.put(DBAccessHelper.SONG_TITLE, jsonObject.getString("song_title"));
                            values.put(DBAccessHelper.SONG_ALBUM, jsonObject.getString("song_album"));
                            values.put(DBAccessHelper.SONG_ARTIST, jsonObject.getString("song_artist"));
                            values.put(DBAccessHelper.SONG_DURATION, jsonObject.getString("song_duration"));
                            values.put(DBAccessHelper.SONG_GENRE, jsonObject.getString("song_genre"));
                            values.put(DBAccessHelper.SONG_YEAR, "2015");
                            values.put(DBAccessHelper.SONG_PLAY_COUNT, jsonObject.getString("play_count"));
                            values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, art_url);
                            values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                            values.put(DBAccessHelper.SONG_TRACK_NUMBER, jsonObject.getString("id"));
                            values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                            values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);

                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                            Date lastModifiedDate = df.parse(jsonObject.getString("last_modified"));
                            Date lastPlayedDate = df.parse(jsonObject.getString("last_played"));

                            long lastModified = lastModifiedDate.getTime();
                            long lastPlayed = lastPlayedDate.getTime();

                            values.put(DBAccessHelper.ADDED_TIMESTAMP, lastModified);
                            values.put(DBAccessHelper.SONG_LAST_MODIFIED, lastModified);
                            values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, lastPlayed);

                            //Add all the entries to the database to build the songs library.
                            mApp.getDBAccessHelper().getWritableDatabase().insertWithOnConflict(DBAccessHelper.FRIENDS_LIBRARY_TABLE,
                                    null, values, SQLiteDatabase.CONFLICT_REPLACE);

                            downloadAlbumArt(art_url, songId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void downloadAlbumArt(String url_string, String songId) {
        if (Common.isNetworkAvailable(mContext)) {
            if (!new File(mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg").exists()) {
                try {
                    if (url_string != null && Patterns.WEB_URL.matcher(url_string).matches()) {
                        URL url = new URL(url_string);
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
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void getAllLiked() {
        if (Common.isNetworkAvailable(mContext)) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("api.madbeeapp.com")
                    .appendPath("api")
                    .appendPath("getlikedsongs")
                    .appendQueryParameter("number", Prefs.getMobileNumber(mContext));

            String return_res = getResponse(builder.build().toString());
            if (return_res != null) {
                try {
                    JSONObject responseJsonObject = new JSONObject(return_res);
                    if (responseJsonObject.getJSONObject("response").getInt("type") == 1) {
                        mApp.getDBAccessHelper().deleteFriendsSongs("likes");
                        JSONArray jsonArray = responseJsonObject.getJSONArray("data");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String songId = jsonObject.getString("sound_cloud_id");
                                String phoneNumber = "likes";
                                String art_url = jsonObject.getString("album_art_url");

                                String songFilePath = mContext.getFilesDir().getPath() + "/music/" + songId + ".mp3";
                                String songAlbumArtPath = "file:" + mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg";

                                ContentValues values = new ContentValues();
                                values.put(DBAccessHelper.CONTACT_NUMBER, phoneNumber);
                                values.put(DBAccessHelper.SONG_ID, songId);
                                values.put(DBAccessHelper.SONG_TITLE, jsonObject.getString("song_title"));
                                values.put(DBAccessHelper.SONG_ALBUM, jsonObject.getString("song_album"));
                                values.put(DBAccessHelper.SONG_ARTIST, jsonObject.getString("song_artist"));
                                values.put(DBAccessHelper.SONG_DURATION, jsonObject.getString("song_duration"));
                                values.put(DBAccessHelper.SONG_GENRE, jsonObject.getString("song_genre"));
                                values.put(DBAccessHelper.SONG_YEAR, "2015");
                                values.put(DBAccessHelper.SONG_PLAY_COUNT, jsonObject.getString("play_count"));
                                values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, art_url);
                                values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                                values.put(DBAccessHelper.SONG_TRACK_NUMBER, jsonObject.getString("id"));
                                values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                                values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);

                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                Date lastModifiedDate = df.parse(jsonObject.getString("last_modified"));
                                Date lastPlayedDate = df.parse(jsonObject.getString("last_played"));

                                long lastModified = lastModifiedDate.getTime();
                                long lastPlayed = lastPlayedDate.getTime();

                                values.put(DBAccessHelper.ADDED_TIMESTAMP, lastModified);
                                values.put(DBAccessHelper.SONG_LAST_MODIFIED, lastModified);
                                values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, lastPlayed);

                                //Add all the entries to the database to build the songs library.
                                mApp.getDBAccessHelper().getWritableDatabase().insertWithOnConflict(DBAccessHelper.FRIENDS_LIBRARY_TABLE,
                                        null, values, SQLiteDatabase.CONFLICT_REPLACE);

                                downloadAlbumArt(art_url, songId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void getTopTrending() {
        if (Common.isNetworkAvailable(mContext)) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("api.madbeeapp.com")
                    .appendPath("api")
                    .appendPath("gettoptrending")
                    .appendQueryParameter("number", Prefs.getMobileNumber(mContext));

            String return_res = getResponse(builder.build().toString());
            if (return_res != null) {
                try {
                    JSONObject responseJsonObject = new JSONObject(return_res);
                    if (responseJsonObject.getJSONObject("response").getInt("type") == 1) {
                        mApp.getDBAccessHelper().deleteFriendsSongs("toptrending");
                        JSONArray jsonArray = responseJsonObject.getJSONArray("data");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                String songId = jsonObject.getString("sound_cloud_id");
                                String phoneNumber = "toptrending";
                                String art_url = jsonObject.getString("album_art_url");

                                String songFilePath = mContext.getFilesDir().getPath() + "/music/" + songId + ".mp3";
                                String songAlbumArtPath = "file:" + mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg";

                                ContentValues values = new ContentValues();
                                values.put(DBAccessHelper.CONTACT_NUMBER, phoneNumber);
                                values.put(DBAccessHelper.SONG_ID, songId);
                                values.put(DBAccessHelper.SONG_TITLE, jsonObject.getString("song_title"));
                                values.put(DBAccessHelper.SONG_ALBUM, jsonObject.getString("song_album"));
                                values.put(DBAccessHelper.SONG_ARTIST, jsonObject.getString("song_artist"));
                                values.put(DBAccessHelper.SONG_DURATION, jsonObject.getString("song_duration"));
                                values.put(DBAccessHelper.SONG_GENRE, jsonObject.getString("song_genre"));
                                values.put(DBAccessHelper.SONG_YEAR, "2015");
                                values.put(DBAccessHelper.SONG_PLAY_COUNT, jsonObject.getString("play_count"));
                                values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, art_url);
                                values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                                values.put(DBAccessHelper.SONG_TRACK_NUMBER, jsonObject.getString("id"));
                                values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                                values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);

                                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                                Date lastModifiedDate = df.parse(jsonObject.getString("last_modified"));
                                Date lastPlayedDate = df.parse(jsonObject.getString("last_played"));

                                long lastModified = lastModifiedDate.getTime();
                                long lastPlayed = lastPlayedDate.getTime();

                                values.put(DBAccessHelper.ADDED_TIMESTAMP, lastModified);
                                values.put(DBAccessHelper.SONG_LAST_MODIFIED, lastModified);
                                values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, lastPlayed);

                                //Add all the entries to the database to build the songs library.
                                mApp.getDBAccessHelper().getWritableDatabase().insertWithOnConflict(DBAccessHelper.FRIENDS_LIBRARY_TABLE,
                                        null, values, SQLiteDatabase.CONFLICT_REPLACE);

                                downloadAlbumArt(art_url, songId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

