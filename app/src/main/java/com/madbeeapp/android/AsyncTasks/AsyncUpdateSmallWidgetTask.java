package com.madbeeapp.android.AsyncTasks;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.RemoteViews;

import com.madbeeapp.android.LauncherActivity.LauncherActivity;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

public class AsyncUpdateSmallWidgetTask extends AsyncTask<String, Integer, Boolean> {
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

    public AsyncUpdateSmallWidgetTask(Context context, int numWidgets, int appWidgetIds[], AppWidgetManager appWidgetManager) {
        mContext = context;
        mApp = (Common) mContext.getApplicationContext();
        mAppWidgetIds = appWidgetIds;
        mAppWidgetManager = appWidgetManager;
        mNumWidgets = numWidgets;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        //Perform this loop procedure for each App Widget that belongs to this mApp
        for (int i = 0; i < mNumWidgets; i++) {
            currentAppWidgetId = mAppWidgetIds[i];
            views = new RemoteViews(mContext.getPackageName(), R.layout.small_widget_layout);

            Intent playPauseIntent = new Intent();
            playPauseIntent.setAction(PLAY_PAUSE_ACTION);
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, playPauseIntent, 0);

            Intent nextIntent = new Intent();
            nextIntent.setAction(NEXT_ACTION);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, nextIntent, 0);

            Intent previousIntent = new Intent();
            previousIntent.setAction(PREVIOUS_ACTION);
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, previousIntent, 0);

            //Get the layout of the widget and attach a click listener to each element.
            views.setOnClickPendingIntent(R.id.app_widget_small_play, playPausePendingIntent);
            views.setOnClickPendingIntent(R.id.app_widget_small_previous, previousPendingIntent);
            views.setOnClickPendingIntent(R.id.app_widget_small_next, nextPendingIntent);

            if (mApp.isServiceRunning()) {
                final Intent notificationIntent = new Intent(mContext, MainActivity.class);
                notificationIntent.putExtra("CALLED_FROM_FOOTER", true);
                notificationIntent.putExtra("CALLED_FROM_NOTIF", true);

                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
                views.setOnClickPendingIntent(R.id.app_widget_small_image, pendingIntent);
            } else {
                views.setImageViewResource(R.id.app_widget_small_image, R.drawable.empty_art_padding);
                views.setImageViewResource(R.id.app_widget_small_play, R.drawable.play_light);
                final Intent intent = new Intent(mContext, LauncherActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
                views.setOnClickPendingIntent(R.id.app_widget_small_image, pendingIntent);
            }

            if (mApp.isServiceRunning()) {
                try {
                    //Get the downsampled image of the current song's album art.
                    views.setImageViewBitmap(R.id.app_widget_small_image, getAlbumArt());
                    views.setTextViewText(R.id.app_widget_small_line_one, mApp.getService().getCurrentSong().getTitle());
                    views.setTextViewText(R.id.app_widget_small_line_two, mApp.getService().getCurrentSong().getAlbum() +
                            mApp.getService().getCurrentSong().getArtist());

                    if (mApp.getService().getCurrentMediaPlayer().isPlaying()) {
                        views.setImageViewResource(R.id.app_widget_small_play, R.drawable.pause_light);
                    } else {
                        views.setImageViewResource(R.id.app_widget_small_play, R.drawable.play_light);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //Tell the AppWidgetManager to perform an update on the current app widget\
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
                try {
                    mAppWidgetManager.updateAppWidget(currentAppWidgetId, views);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
    }
}
