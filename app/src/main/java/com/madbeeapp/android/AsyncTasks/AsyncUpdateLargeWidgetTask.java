package com.madbeeapp.android.AsyncTasks;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.widget.RemoteViews;

import com.madbeeapp.android.LauncherActivity.LauncherActivity;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Services.LargeWidgetAdapterService;
import com.madbeeapp.android.Utils.Common;

public class AsyncUpdateLargeWidgetTask extends AsyncTask<String, Integer, Boolean> {

    public static final String PREVIOUS_ACTION = "com.madbeeapp.android.PREVIOUS_ACTION";
    public static final String PLAY_PAUSE_ACTION = "com.madbeeapp.android.PLAY_PAUSE_ACTION";
    public static final String NEXT_ACTION = "com.madbeeapp.android.NEXT_ACTION";
    private Context mContext;
    private Common mApp;
    private int mNumWidgets;
    private int mAppWidgetIds[];
    private AppWidgetManager mAppWidgetManager;
    private int currentAppWidgetId;
    private RemoteViews views;
    private String songTitle = "";
    private String albumName = "";
    private String artistName = "";

    public AsyncUpdateLargeWidgetTask(Context context, int numWidgets, int appWidgetIds[], AppWidgetManager appWidgetManager) {
        mContext = context;
        mApp = (Common) mContext.getApplicationContext();
        mAppWidgetIds = appWidgetIds;
        mAppWidgetManager = appWidgetManager;
        mNumWidgets = numWidgets;
    }

    @SuppressLint("NewApi")
    @Override
    protected Boolean doInBackground(String... params) {
        //Perform this loop procedure for each App Widget that belongs to this mApp
        for (int i = 0; i < mNumWidgets; i++) {
            currentAppWidgetId = mAppWidgetIds[i];
            String widgetColor = mApp.getSharedPreferences().getString("" + currentAppWidgetId, "LIGHT");

            //Initialize the RemoteView object to gain access to the widget's UI elements.
            views = new RemoteViews(mContext.getPackageName(), R.layout.large_widget_layout);

            /* Create a pendingIntent that will serve as a general template for the clickListener.
             * We'll create a fillInIntent in LargeWidgetAdapterService.java that will provide the 
             * index of the listview item that's been clicked. */
            Intent intent = new Intent();
            intent.setAction("com.madbeeapp.android.WIDGET_CHANGE_TRACK");
            PendingIntent pendingIntentTemplate = PendingIntent.getBroadcast(mContext, 0, intent, 0);
            views.setPendingIntentTemplate(R.id.widget_listview, pendingIntentTemplate);

            //Create the intent to fire up the service that will back the adapter of the listview.
            Intent serviceIntent = new Intent(mContext, LargeWidgetAdapterService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetIds[i]);
            serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
            serviceIntent.putExtra("WIDGET_COLOR", widgetColor);

            views.setRemoteAdapter(R.id.widget_listview, serviceIntent);
            mAppWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetIds, R.id.widget_listview);

            //Check if the service is running and update the widget elements.
            if (mApp.isServiceRunning()) {
                //Set the album art.
                views.setViewVisibility(R.id.widget_listview, View.VISIBLE);
                views.setImageViewBitmap(R.id.widget_album_art, getAlbumArt());
                songTitle = mApp.getService().getCurrentSong().getTitle();
                albumName = mApp.getService().getCurrentSong().getAlbum();
                artistName = mApp.getService().getCurrentSong().getArtist();
                final Intent notificationIntent = new Intent(mContext, MainActivity.class);
                notificationIntent.putExtra("CALLED_FROM_FOOTER", true);
                notificationIntent.putExtra("CALLED_FROM_NOTIF", true);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
                views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent);
            } else {
                songTitle = mContext.getResources().getString(R.string.no_music_playing);

                //Set the default album art.
                views.setImageViewResource(R.id.widget_album_art, R.drawable.empty_art_padding);
                views.setViewVisibility(R.id.widget_listview, View.INVISIBLE);
                views.setImageViewResource(R.id.app_widget_small_play, R.drawable.play_light);
                final Intent notificationIntent = new Intent(mContext, LauncherActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
                views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent);

            }

            //Set the song title, artist title, and album title.
            views.setTextViewText(R.id.widget_song_title_text, songTitle);
            views.setTextViewText(R.id.widget_album_text, albumName);
            views.setTextViewText(R.id.widget_artist_text, artistName);

            //Attach PendingIntents to the widget controls.
            Intent previousTrackIntent = new Intent();
            previousTrackIntent.setAction(PREVIOUS_ACTION);
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(mContext, 0, previousTrackIntent, 0);

            Intent playPauseTrackIntent = new Intent();
            playPauseTrackIntent.setAction(PLAY_PAUSE_ACTION);
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(mContext, 0, playPauseTrackIntent, 0);

            Intent nextTrackIntent = new Intent();
            nextTrackIntent.setAction(NEXT_ACTION);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mContext, 0, nextTrackIntent, 0);

            //Set the pending intents on the buttons.
            views.setOnClickPendingIntent(R.id.widget_play, playPausePendingIntent);
            views.setOnClickPendingIntent(R.id.widget_previous_track, previousPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_next_track, nextPendingIntent);

            if (mApp.isServiceRunning()) {
                try {
                    if (mApp.getService().getCurrentMediaPlayer().isPlaying()) {
                        views.setImageViewResource(R.id.widget_play, R.drawable.pause_light);
                    } else {
                        views.setImageViewResource(R.id.widget_play, R.drawable.play_light);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Tell the AppWidgetManager to perform an update on the current app widget.
            try {
                mAppWidgetManager.updateAppWidget(currentAppWidgetId, views);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return true;
    }

    private Bitmap getAlbumArt() {
        mApp = (Common) mContext.getApplicationContext();
        //Check if the album art has been cached for this song.
        Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(),
                R.drawable.empty_art_padding);
        if (mApp.isServiceRunning())
            if (mApp.getService().getCurrentSong().getAlbumArt() != null)
                bm = mApp.getService().getCurrentSong().getAlbumArt();
        return bm;
    }

    @Override
    public void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        switch (values[0]) {
            case 0:
                mAppWidgetManager.updateAppWidget(currentAppWidgetId, views);
                break;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    }
}
