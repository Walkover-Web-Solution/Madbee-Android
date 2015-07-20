package com.madbeeapp.android.AsyncTasks;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.DBHelpers.MediaStoreAccessHelper;
import com.madbeeapp.android.FoldersFragment.FileExtensionFilter;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.CSVWriter;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Constants;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * The Mother of all AsyncTasks in this app.
 *
 * @author Arpit Gandhi
 */
public class AsyncBuildLibraryTask extends AsyncTask<String, String, Void> {

    public ArrayList<OnBuildLibraryProgressUpdate> mBuildLibraryProgressUpdate;
    private Context mContext;
    private Common mApp;
    private String mCurrentTask = "";
    private int mOverallProgress = 0;
    private Date date = new Date();
    private String mMediaStoreSelection = null;
    private HashMap<String, String> mGenresHashMap = new HashMap<>();
    private HashMap<String, Uri> mMediaStoreAlbumArtMap = new HashMap<>();
    private HashMap<String, String> mFolderArtHashMap = new HashMap<>();
    private MediaMetadataRetriever mMMDR = new MediaMetadataRetriever();
    private boolean csv_status = false;
    private PowerManager.WakeLock wakeLock;

    public AsyncBuildLibraryTask(Context context) {
        mContext = context;
        mApp = (Common) mContext;
        mBuildLibraryProgressUpdate = new ArrayList<>();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mApp.setIsBuildingLibrary(true);
        mApp.setIsScanFinished(false);

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onStartBuildingLibrary();

        // Acquire a wakelock to prevent the CPU from sleeping while the process is running.
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "AsyncBuildLibraryTask");
        wakeLock.acquire();
    }

    @Override
    protected Void doInBackground(String... params) {

		/*
         * Get a cursor of songs from MediaStore. The cursor
		 * is limited by the folders that have been selected
		 * by the user.
		 */
        mCurrentTask = mContext.getResources().getString(R.string.building_music_library);
        Cursor mediaStoreCursor = getSongsFromMediaStore();

		/*
         * Transfer the content in mediaStoreCursor over to
		 * madbee' private database.
		 */
        if (mediaStoreCursor != null) {
            saveMediaStoreDataToDB(mediaStoreCursor);
            mediaStoreCursor.close();
        }
        publishProgress("MEDIASTORE_TRANSFER_COMPLETE");

        if (Common.isNetworkAvailable(mContext)) {
            createCSV();
            if (csv_status)
                exportCSV();
            saveFriendsPlaylist();
            getAllLiked();
            getTopTrending();
            publishProgress("DATA_TRANSFER_COMPLETE");
        } else {
            publishProgress("DATA_TRANSFER_COMPLETE");
        }

        //Save album art paths for each song to the database.
        getAlbumArt();
        return null;
    }

    /**
     * Retrieves a cursor of songs from MediaStore. The cursor
     * is limited to songs that are within the folders that the user
     * selected.
     */
    private Cursor getSongsFromMediaStore() {
        //Get a cursor of all active music folders.
        Cursor musicFoldersCursor = mApp.getDBAccessHelper().getAllMusicFolderPaths();

        //Build the appropriate selection statement.
        Cursor mediaStoreCursor;
        String projection[] = {MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media._ID,
                MediaStoreAccessHelper.ALBUM_ARTIST};

        //Grab the cursor of MediaStore entries.
        if (musicFoldersCursor == null || musicFoldersCursor.getCount() < 1) {
            //No folders were selected by the user. Grab all songs in MediaStore.
            mediaStoreCursor = MediaStoreAccessHelper.getAllSongs(mContext, projection, null);
        } else {
            //Build a selection statement for querying MediaStore.
            mMediaStoreSelection = buildMusicFoldersSelection(musicFoldersCursor);
            mediaStoreCursor = MediaStoreAccessHelper.getAllSongsWithSelection(mContext,
                    mMediaStoreSelection,
                    projection,
                    null);

            //Close the music folders cursor.
            musicFoldersCursor.close();
        }
        return mediaStoreCursor;
    }

    /**
     * Iterates through mediaStoreCursor and transfers its data
     * over to madbee' private database.
     */
    private void saveMediaStoreDataToDB(Cursor mediaStoreCursor) {
        try {
            //Initialize the database transaction manually (improves performance).
            mApp.getDBAccessHelper().getWritableDatabase().beginTransaction();

            //Clear out the table.
            mApp.getDBAccessHelper()
                    .getWritableDatabase()
                    .delete(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                            null,
                            null);

            //Tracks the progress of this method.
            int subProgress;
            if (mediaStoreCursor.getCount() != 0) {
                subProgress = 250000 / (mediaStoreCursor.getCount());
            } else {
                subProgress = 250000;
            }

            //Populate a hash of all albums and their album art path.
            buildMediaStoreAlbumArtHash();

            //Prefetch each column's index.
            final int titleColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            final int artistColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            final int albumColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            final int albumIdColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            final int durationColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            final int trackColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            final int yearColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            final int dateModifiedColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
            final int filePathColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            final int idColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int albumArtistColIndex = mediaStoreCursor.getColumnIndex(MediaStoreAccessHelper.ALBUM_ARTIST);

    		/* The album artist field is hidden by default and we've explictly exposed it.
             * The field may cease to exist at any time and if it does, use the artists
    		 * field instead.
    		 */
            if (albumArtistColIndex == -1) {
                albumArtistColIndex = artistColIndex;
            }
            //Iterate through MediaStore's cursor and save the fields to madbee' DB.
            for (int i = 0; i < mediaStoreCursor.getCount(); i++) {
                mediaStoreCursor.moveToPosition(i);
                mOverallProgress += subProgress;
                publishProgress();

                String songTitle = mediaStoreCursor.getString(titleColIndex);
                String songArtist = mediaStoreCursor.getString(artistColIndex);
                String songAlbum = mediaStoreCursor.getString(albumColIndex);
                String songAlbumId = mediaStoreCursor.getString(albumIdColIndex);
                String songAlbumArtist = mediaStoreCursor.getString(albumArtistColIndex);
                String songFilePath = mediaStoreCursor.getString(filePathColIndex);
                String songGenre = getSongGenre(songFilePath);
                String songDuration = mediaStoreCursor.getString(durationColIndex);
                String songTrackNumber = mediaStoreCursor.getString(trackColIndex);
                String songYear = mediaStoreCursor.getString(yearColIndex);
                String songDateModified = mediaStoreCursor.getString(dateModifiedColIndex);
                String songId = mediaStoreCursor.getString(idColIndex);
                String songSource = DBAccessHelper.LOCAL;
                String songAlbumArtPath = "";
                songDateModified = String.valueOf(Long.parseLong(songDateModified) * 1000);
                if (mMediaStoreAlbumArtMap.get(songAlbumId) != null)
                    songAlbumArtPath = mMediaStoreAlbumArtMap.get(songAlbumId).toString();

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

                //Filter out track numbers and remove any bogus values.
                if (songTrackNumber != null) {
                    if (songTrackNumber.contains("/")) {
                        int index = songTrackNumber.lastIndexOf("/");
                        songTrackNumber = songTrackNumber.substring(0, index);
                    }

                    try {
                        if (Integer.parseInt(songTrackNumber) <= 0) {
                            songTrackNumber = "";
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        songTrackNumber = "";
                    }

                }

                long durationLong = 0;
                try {
                    durationLong = Long.parseLong(songDuration);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!songFilePath.contains("call_rec") && !songFilePath.contains("recording")) {
                    if (durationLong > 120000) {
                        ContentValues values = new ContentValues();
                        values.put(DBAccessHelper.SONG_TITLE, songTitle);
                        values.put(DBAccessHelper.SONG_ARTIST, songArtist);
                        values.put(DBAccessHelper.SONG_ALBUM, songAlbum);
                        values.put(DBAccessHelper.SONG_ALBUM_ARTIST, songAlbumArtist);
                        values.put(DBAccessHelper.SONG_DURATION, mApp.convertMillisToMinsSecs(durationLong));
                        values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                        values.put(DBAccessHelper.SONG_TRACK_NUMBER, songTrackNumber);
                        values.put(DBAccessHelper.SONG_GENRE, songGenre);
                        values.put(DBAccessHelper.SONG_YEAR, songYear);
                        values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                        values.put(DBAccessHelper.SONG_LAST_MODIFIED, songDateModified);
                        values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                        values.put(DBAccessHelper.ADDED_TIMESTAMP, date.getTime());
                        values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, songDateModified);
                        values.put(DBAccessHelper.SONG_SOURCE, songSource);
                        values.put(DBAccessHelper.SONG_ID, songId);
                        values.put(DBAccessHelper.SONG_PLAY_COUNT, "0");

                        //Add all the entries to the database to build the songs library.
                        mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                                null,
                                values);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            //Close the transaction.
            mApp.getDBAccessHelper().getWritableDatabase().setTransactionSuccessful();
            mApp.getDBAccessHelper().getWritableDatabase().endTransaction();
        }
    }

    /**
     * Constructs the selection string for limiting the MediaStore
     * query to specific music folders.
     */
    private String buildMusicFoldersSelection(Cursor musicFoldersCursor) {
        String mediaStoreSelection = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND (";
        int folderPathColIndex = musicFoldersCursor.getColumnIndex(DBAccessHelper.FOLDER_PATH);
        int includeColIndex = musicFoldersCursor.getColumnIndex(DBAccessHelper.INCLUDE);

        for (int i = 0; i < musicFoldersCursor.getCount(); i++) {
            musicFoldersCursor.moveToPosition(i);
            boolean include = musicFoldersCursor.getInt(includeColIndex) > 0;

            //Set the correct LIKE clause.
            String likeClause;
            if (include)
                likeClause = " LIKE ";
            else
                likeClause = " NOT LIKE ";

            //The first " AND " clause was already appended to mediaStoreSelection.
            if (i != 0 && !include)
                mediaStoreSelection += " AND ";
            else if (i != 0)
                mediaStoreSelection += " OR ";

            mediaStoreSelection += MediaStore.Audio.Media.DATA + likeClause
                    + "'%" + musicFoldersCursor.getString(folderPathColIndex)
                    + "/%'";
        }
        //Append the closing parentheses.
        mediaStoreSelection += ")";
        return mediaStoreSelection;
    }

    /**
     * Builds a HashMap of all albums and their album art path.
     */
    private void buildMediaStoreAlbumArtHash() {
        Cursor albumsCursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media.ALBUM_ID},
                MediaStore.Audio.Media.IS_MUSIC + "=1",
                null,
                null);

        final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");
        if (albumsCursor == null)
            return;

        for (int i = 0; i < albumsCursor.getCount(); i++) {
            albumsCursor.moveToPosition(i);
            Uri albumArtUri = ContentUris.withAppendedId(ART_CONTENT_URI, albumsCursor.getLong(0));
            mMediaStoreAlbumArtMap.put(albumsCursor.getString(0), albumArtUri);
        }

        albumsCursor.close();
    }

    /**
     * Returns the genre of the song at the specified file path.
     */
    private String getSongGenre(String filePath) {
        if (mGenresHashMap != null)
            return mGenresHashMap.get(filePath);
        else
            return mContext.getResources().getString(R.string.unknown_genre);
    }

    /**
     * Loops through a cursor of all local songs in
     * the library and searches for their album art.
     */
    private void getAlbumArt() {

        //Get a cursor with a list of all local music files on the device.
        Cursor cursor = mApp.getDBAccessHelper().getAllLocalSongs();
        mCurrentTask = mContext.getResources().getString(R.string.building_album_art);

        if (cursor == null || cursor.getCount() < 1)
            return;

        //Tracks the progress of this method.
        int subProgress;
        if (cursor.getCount() != 0) {
            subProgress = 750000 / (cursor.getCount());
        } else {
            subProgress = 750000;
        }

        try {
            mApp.getDBAccessHelper().getWritableDatabase().beginTransactionNonExclusive();

            //Loop through the cursor and retrieve album art.
            for (int i = 0; i < cursor.getCount(); i++) {
                try {
                    cursor.moveToPosition(i);
                    mOverallProgress += subProgress;
                    publishProgress();

                    String filePath = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                    String artworkPath;
                    artworkPath = getEmbeddedArtwork(filePath);

                    String normalizedFilePath = filePath.replace("'", "''");

                    //Store the artwork file path into the DB.
                    ContentValues values = new ContentValues();
                    values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, artworkPath);
                    String where = DBAccessHelper.SONG_FILE_PATH + "='" + normalizedFilePath + "'";

                    mApp.getDBAccessHelper().getWritableDatabase().update(DBAccessHelper.MUSIC_LIBRARY_TABLE, values, where, null);
                    mApp.getDBAccessHelper().getWritableDatabase().yieldIfContendedSafely();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mApp.getDBAccessHelper().getWritableDatabase().setTransactionSuccessful();
            mApp.getDBAccessHelper().getWritableDatabase().endTransaction();
            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Searchs for folder art within the specified file's
     * parent folder. Returns a path string to the artwork
     * image file if it exists. Returns an empty string
     * otherwise.
     */
    public String getArtworkFromFolder(String filePath) {

        File file = new File(filePath);
        if (!file.exists()) {
            return "";

        } else {
            //Create a File that points to the parent directory of the album.
            File directoryFile = file.getParentFile();
            String directoryPath = "";
            String albumArtPath = "";
            try {
                directoryPath = directoryFile.getCanonicalPath();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            //Check if album art was already found in this directory.
            if (mFolderArtHashMap.containsKey(directoryPath))
                return mFolderArtHashMap.get(directoryPath);

            //Get a list of images in the album's folder.
            FileExtensionFilter IMAGES_FILTER = new FileExtensionFilter(new String[]{".jpg", ".jpeg",
                    ".png", ".gif"});
            File[] folderList = directoryFile.listFiles(IMAGES_FILTER);

            //Check if any image files were found in the folder.
            if (folderList.length == 0) {
                //No images found.
                return "";

            } else {
                //Loop through the list of image files. Use the first jpeg file if it's found.
                for (File aFolderList1 : folderList) {

                    try {
                        albumArtPath = aFolderList1.getCanonicalPath();
                        if (albumArtPath.endsWith("jpg") ||
                                albumArtPath.endsWith("jpeg")) {

                            //Add the folder's album art file to the hash.
                            mFolderArtHashMap.put(directoryPath, albumArtPath);
                            return albumArtPath;
                        }
                    } catch (Exception e) {
                        //Skip the file if it's corrupted or unreadable.
                    }
                }

                //If an image was not found, check for gif or png files (lower priority).
                for (File aFolderList : folderList) {
                    try {
                        albumArtPath = aFolderList.getCanonicalPath();
                        if (albumArtPath.endsWith("png") ||
                                albumArtPath.endsWith("gif")) {

                            //Add the folder's album art file to the hash.
                            mFolderArtHashMap.put(directoryPath, albumArtPath);
                            return albumArtPath;
                        }
                    } catch (Exception e) {
                        //Skip the file if it's corrupted or unreadable.
                    }
                }
            }
            //Add the folder's album art file to the hash.
            mFolderArtHashMap.put(directoryPath, albumArtPath);
            return "";
        }
    }

    /**
     * Searchs for embedded art within the specified file.
     * Returns a path string to the artwork if it exists.
     * Returns an empty string otherwise.
     */
    public String getEmbeddedArtwork(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return getArtworkFromFolder(filePath);
        } else {
            mMMDR.setDataSource(filePath);
            byte[] embeddedArt = mMMDR.getEmbeddedPicture();
            if (embeddedArt != null) {
                return "byte://" + filePath;
            } else {
                return getArtworkFromFolder(filePath);
            }
        }
    }

    @Override
    protected void onProgressUpdate(String... progressParams) {
        super.onProgressUpdate(progressParams);

        if (progressParams.length > 0 && progressParams[0].equals("MEDIASTORE_TRANSFER_COMPLETE")) {
            mBuildLibraryProgressUpdate.get(0).onProgressUpdate(this, mCurrentTask, mOverallProgress,
                    1000000, true, false);
            return;
        }
        if (progressParams.length > 0 && progressParams[0].equals("DATA_TRANSFER_COMPLETE")) {
            mBuildLibraryProgressUpdate.get(0).onProgressUpdate(this, mCurrentTask, mOverallProgress,
                    1000000, true, true);
            return;
        }
        if (mBuildLibraryProgressUpdate != null)
            mBuildLibraryProgressUpdate.get(0).onProgressUpdate(this, mCurrentTask, mOverallProgress, 1000000, false, false);

    }

    @Override
    protected void onPostExecute(Void arg0) {
        //Release the wakelock.
        ContentResolver.setMasterSyncAutomatically(true);
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        Account account = new Account("madbee", Constants.ACCOUNT_TYPE);
        ContentResolver.requestSync(account, Constants.AUTHORITY, settingsBundle);

        wakeLock.release();
        mApp.setIsBuildingLibrary(false);
        mApp.setIsScanFinished(true);

        Toast.makeText(mContext, R.string.finished_scanning_album_art, Toast.LENGTH_LONG).show();

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onFinishBuildingLibrary(this);
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

    /**
     * Setter methods.
     */
    public void setOnBuildLibraryProgressUpdate(OnBuildLibraryProgressUpdate
                                                        buildLibraryProgressUpdate) {
        if (buildLibraryProgressUpdate != null)
            mBuildLibraryProgressUpdate.add(buildLibraryProgressUpdate);
    }

    /**
     * Provides callback methods that expose this
     * AsyncTask's progress.
     *
     * @author Arpit Gandhi
     */
    public interface OnBuildLibraryProgressUpdate {
        /**
         * Called when this AsyncTask begins executing
         * its doInBackground() method.
         */
        void onStartBuildingLibrary();

        /**
         * Called whenever mOverall Progress has been updated.
         */
        void onProgressUpdate(AsyncBuildLibraryTask task, String mCurrentTask,
                              int overallProgress, int maxProgress,
                              boolean mediaStoreTransferDone, boolean syncData);

        /**
         * Called when this AsyncTask finishes executing
         * its onPostExecute() method.
         */
        void onFinishBuildingLibrary(AsyncBuildLibraryTask task);
    }
}
