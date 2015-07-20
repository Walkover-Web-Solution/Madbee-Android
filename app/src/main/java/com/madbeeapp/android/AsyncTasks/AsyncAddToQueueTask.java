package com.madbeeapp.android.AsyncTasks;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Services.AudioPlaybackService;
import com.madbeeapp.android.Utils.Common;

public class AsyncAddToQueueTask extends AsyncTask<Boolean, Integer, Boolean> {

    private Context mContext;
    private Common mApp;

    private String mArtistName;
    private String mAlbumName;
    private String mSongTitle;
    private String mGenreName;
    private String mAlbumArtistName;

    private Cursor mCursor;
    private int mEnqueueType;
    private boolean mPlayNext = false;
    private String mQuerySelection;

    public AsyncAddToQueueTask(Context context,
                               int enqueueType,
                               String artistName,
                               String albumName,
                               String songTitle,
                               String genreName,
                               String albumArtistName) {

        mContext = context;
        mApp = (Common) mContext.getApplicationContext();

        mEnqueueType = enqueueType;
        mArtistName = artistName;
        mAlbumName = albumName;
        mSongTitle = songTitle;
        mGenreName = genreName;
        mAlbumArtistName = albumArtistName;
    }

    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Boolean... params) {

        //Specifies if the user is trying to add song(s) to play next.
        if (params.length > 0) {
            mPlayNext = params[0];
        }

        //Escape any rogue apostrophes.
        if (mArtistName != null && mArtistName.contains("'")) {
            mArtistName = mArtistName.replace("'", "''");
        }

        if (mAlbumName != null && mAlbumName.contains("'")) {
            mAlbumName = mAlbumName.replace("'", "''");
        }

        if (mSongTitle != null && mSongTitle.contains("'")) {
            mSongTitle = mSongTitle.replace("'", "''");
        }

        if (mGenreName != null && mGenreName.contains("''")) {
            mGenreName = mGenreName.replace("'", "''");
        }

        if (mAlbumArtistName != null && mAlbumArtistName.contains("'")) {
            mAlbumArtistName = mAlbumArtistName.replace("'", "''");
        }

        //Fetch the cursor based on the type of set of songs that are being enqueued.
        assignCursor();

        //Check if the service is currently active.
        if (mApp.isServiceRunning()) {
            if (mPlayNext) {
                int playNextIndex = mApp.getService().getCurrentSongIndex() + 1;
                for (int i = 0; i < mCursor.getCount(); i++) {
                    mApp.getService().getPlaybackIndecesList().add(playNextIndex + i,
                            mApp.getService().getCursor().getCount() + i);
                }
            } else {
                for (int i = 0; i < mCursor.getCount(); i++) {
                    try {
                        mApp.getService().getPlaybackIndecesList().add(mApp.getService().getCursor().getCount() + i);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
            mApp.getService().enqueueCursor(mCursor, mPlayNext);
            publishProgress(1);
        } else {
            Intent serviceIntent = new Intent(mContext, AudioPlaybackService.class);
            mContext.stopService(serviceIntent);
            publishProgress(0);
        }
        return true;
    }

    //Retrieves and assigns the cursor based on the set of song(s) that are being enqueued.
    private void assignCursor() {
        DBAccessHelper dbHelper = new DBAccessHelper(mContext);
        switch (mEnqueueType) {
            case Common.SONGS_FRAGMENT: {
                String selection = DBAccessHelper.SONG_TITLE + "=" + "'" + mArtistName + "'";

                mCursor = dbHelper.getReadableDatabase().query(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                        null,
                        selection,
                        null,
                        null,
                        null,
                        DBAccessHelper.SONG_TITLE + " ASC");
                break;
            }
            case Common.ARTISTS_FRAGMENT: {
                String selection = DBAccessHelper.SONG_ARTIST + "=" + "'" + mArtistName + "'";
                mCursor = dbHelper.getReadableDatabase().query(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                        null,
                        selection,
                        null,
                        null,
                        null,
                        DBAccessHelper.SONG_ALBUM + " ASC" + ", " + DBAccessHelper.SONG_TRACK_NUMBER + "*1 ASC");
                break;
            }
            case Common.ALBUMS_FRAGMENT: {
                String selection = DBAccessHelper.SONG_ALBUM + "=" + "'" + mArtistName + "'";
                mCursor = dbHelper.getReadableDatabase().query(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                        null,
                        selection,
                        null,
                        null,
                        null,
                        DBAccessHelper.SONG_TRACK_NUMBER + "*1 ASC");
                break;
            }
            case Common.ALBUM_ARTISTS_FLIPPED_SONGS_FRAGMENT: {
                String selection = DBAccessHelper.SONG_ALBUM_ARTIST + "=" + "'" + mAlbumArtistName + "'" + " AND "
                        + DBAccessHelper.SONG_ALBUM + "=" + "'" + mAlbumName + "'";
                mCursor = dbHelper.getReadableDatabase().query(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                        null,
                        selection,
                        null,
                        null,
                        null,
                        DBAccessHelper.SONG_TRACK_NUMBER + "*1 ASC");
                break;
            }
            case Common.ALBUM_ARTISTS_FLIPPED_FRAGMENT: {
                String selection = DBAccessHelper.SONG_ALBUM_ARTIST + "=" + "'" + mAlbumArtistName + "'";
                mCursor = dbHelper.getReadableDatabase().query(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                        null,
                        selection,
                        null,
                        null,
                        null,
                        DBAccessHelper.SONG_ALBUM + " ASC, " + DBAccessHelper.SONG_TRACK_NUMBER + "*1 ASC");
                break;
            }
            case Common.TOP_25_PLAYED_FRAGMENT: {
                mCursor = dbHelper.getTop25PlayedTracks();
                break;
            }
            case Common.RECENTLY_ADDED_FRAGMENT: {
                mCursor = dbHelper.getRecentlyAddedSongs();
                break;
            }
            case Common.RECENTLY_PLAYED_FRAGMENT: {
                mCursor = dbHelper.getRecentlyPlayedSongs();
                break;
            }
            case Common.GENRES_FRAGMENT: {
                String selection = DBAccessHelper.SONG_GENRE + "=" + "'" + mArtistName + "'";
                mCursor = dbHelper.getReadableDatabase().query(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                        null,
                        selection,
                        null,
                        null,
                        null,
                        DBAccessHelper.SONG_ALBUM + " ASC, " +
                                DBAccessHelper.SONG_TRACK_NUMBER + "*1 ASC");
                break;
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int value = values[0];

        switch (value) {
            case 0:
                mQuerySelection = buildQuerySelectionClause();
                mApp.getPlaybackKickstarter()
                        .initPlayback(
                                mQuerySelection,
                                Common.PLAY_ALL_SONGS,
                                0,
                                false,
                                false);
                break;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        mApp.broadcastUpdateUICommand(new String[]{Common.NEW_QUEUE_ORDER}, new String[]{""});
        //Start preparing the next song if the current song is the last track.
        if (mApp.isServiceRunning()) {
            if (mApp.getService().getCurrentSongIndex() == (mApp.getService().getPlaybackIndecesList().size() - 1)) {
                mApp.getService().prepareAlternateMediaPlayer();
            }
        }
    }

    /**
     * Builds the cursor query's selection clause based on the activity's
     * current usage case.
     */
    private String buildQuerySelectionClause() {
        switch (mEnqueueType) {
            case Common.SONGS_FRAGMENT:
                mQuerySelection = DBAccessHelper.SONG_TITLE + "=" + "'"
                        + mArtistName.replace("'", "''") + "'";
                break;
            case Common.ALBUMS_FRAGMENT:
                mQuerySelection = DBAccessHelper.SONG_ALBUM + "=" + "'"
                        + mArtistName.replace("'", "''") + "'";
                break;
            case Common.ARTISTS_FRAGMENT:
                mQuerySelection = DBAccessHelper.SONG_ARTIST + "=" + "'"
                        + mArtistName.replace("'", "''") + "'";
                break;
            case Common.GENRES_FRAGMENT:
                mQuerySelection = DBAccessHelper.SONG_GENRE + "=" + "'"
                        + mArtistName.replace("'", "''") + "'";
                break;
        }
        return mQuerySelection;
    }
}
