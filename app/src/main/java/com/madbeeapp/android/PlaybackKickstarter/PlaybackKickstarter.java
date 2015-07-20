package com.madbeeapp.android.PlaybackKickstarter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.MainActivity.MainActivity.NowPlayingActivityListener;
import com.madbeeapp.android.Services.AudioPlaybackService;
import com.madbeeapp.android.Services.AudioPlaybackService.PrepareServiceListener;
import com.madbeeapp.android.Utils.Common;

/**
 * Initiates the playback sequence and
 * starts AudioPlaybackService.
 *
 * @author Arpit Gandhi
 */
public class PlaybackKickstarter implements NowPlayingActivityListener, PrepareServiceListener {

    private Context mContext;
    private Common mApp;
    private String mQuerySelection;
    private int mPlaybackRouteId;
    private int mCurrentSongIndex;
    private boolean mPlayAll, mHasClicked;
    private BuildCursorListener mBuildCursorListener;

    public PlaybackKickstarter(Context context) {
        mContext = context;
    }

    /**
     * Helper method that calls all the required method(s)
     * that initialize music playback. This method should
     * always be called when the cursor for the service
     * needs to be changed.
     */
    public void initPlayback(String querySelection,
                             int playbackRouteId,
                             int currentSongIndex,
                             boolean hasClicked,
                             boolean playAll) {

        mApp = (Common) mContext.getApplicationContext();
        mQuerySelection = querySelection;
        mPlaybackRouteId = playbackRouteId;
        mCurrentSongIndex = currentSongIndex;
        mPlayAll = playAll;
        mHasClicked = hasClicked;

        //Start the playback service if it isn't running.
        if (!mApp.isServiceRunning()) {
            startService();
        } else {
            //Call the callback method that will start building the new cursor.
            mApp.getService()
                    .getPrepareServiceListener()
                    .onServiceRunning(mApp.getService());
        }
    }

    /**
     * Starts AudioPlaybackService. Once the service is running, we get a
     * callback toto onServiceRunning() (see below). That's where the method to
     * build the cursor is called.
     */
    private void startService() {
        Intent intent = new Intent(mContext, AudioPlaybackService.class);
        mContext.startService(intent);
    }

    @Override
    public void onServiceRunning(AudioPlaybackService service) {
        //Build the cursor and pass it on to the service.
        mApp = (Common) mContext.getApplicationContext();
        mApp.setIsServiceRunning(true);
        mApp.setService(service);
        mApp.getService().setPrepareServiceListener(this);
        mApp.getService().setCurrentSongIndex(mCurrentSongIndex);
        new AsyncBuildCursorTask().execute();
    }

    @Override
    public void onNowPlayingActivityReady() {
        //Start the playback service if it isn't running.
        if (!mApp.isServiceRunning()) {
            startService();
        } else {
            mApp.getService()
                    .getPrepareServiceListener()
                    .onServiceRunning(mApp.getService());
        }
    }

    public BuildCursorListener getBuildCursorListener() {
        return mBuildCursorListener;
    }

    public void setBuildCursorListener(BuildCursorListener listener) {
        mBuildCursorListener = listener;
    }

    /**
     * Public interface that provides access to
     * major events during the cursor building
     * process.
     *
     * @author Arpit Gandhi
     */
    public interface BuildCursorListener {
        void onServiceCursorReady(Cursor cursor, int currentSongIndex, boolean playAll);

        void onServiceCursorFailed(String exceptionMessage);
    }

    /**
     * Builds the cursor that will be used for playback. Once the cursor
     * is built, AudioPlaybackService receives a callback via
     * onServiceCursorReady() (see below). The service then takes over
     * the rest of the process.
     */
    class AsyncBuildCursorTask extends AsyncTask<Boolean, String, Cursor> {

        @Override
        protected Cursor doInBackground(Boolean... params) {
            return mApp.getDBAccessHelper().getPlaybackCursor(mQuerySelection, mPlaybackRouteId);
        }

        @Override
        public void onProgressUpdate(String... params) {
            getBuildCursorListener().onServiceCursorFailed(params[0]);
        }

        @Override
        public void onPostExecute(Cursor mCursor) {
            super.onPostExecute(mCursor);
            if (mCursor != null) {
                getBuildCursorListener().onServiceCursorReady(mCursor, mCurrentSongIndex, mPlayAll);
                if (mHasClicked) {
                    if (mCursor.moveToFirst()) {
                        if (mCursor.move(mCurrentSongIndex)) {
                            String filePath = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                            if (filePath != null) {
                                mApp.getDBAccessHelper().updateSongPlayCount(filePath);
                                mApp.getDBAccessHelper().updateSongTimestamp(filePath);
                            }
                        }
                    }
                }
            } else {
                getBuildCursorListener().onServiceCursorFailed("Playback cursor null.");
            }
        }
    }
}
