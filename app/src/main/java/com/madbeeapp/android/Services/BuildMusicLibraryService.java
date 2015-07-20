package com.madbeeapp.android.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.madbeeapp.android.AsyncTasks.AsyncBuildLibraryTask;
import com.madbeeapp.android.R;
import com.madbeeapp.android.WelcomeActivity.WelcomeActivity;

public class BuildMusicLibraryService extends Service implements AsyncBuildLibraryTask.OnBuildLibraryProgressUpdate {

    public static int mNotificationId = 92713;
    private Context mContext;
    private NotificationCompat.Builder mBuilder;
    private Notification mNotification;
    private NotificationManager mNotifyManager;

    @Override
    public void onCreate() {
        mContext = this.getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int startId, int flags) {

        //Create a persistent notification that keeps this service running and displays the scan progress.
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(R.drawable.ic_action_music_raag);
        mBuilder.setContentTitle(getResources().getString(R.string.building_music_library));
        mBuilder.setTicker(getResources().getString(R.string.building_music_library));
        mBuilder.setContentText("");
        mBuilder.setProgress(0, 0, true);

        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = mBuilder.build();
        mNotification.flags |= Notification.FLAG_INSISTENT | Notification.FLAG_NO_CLEAR;

        startForeground(mNotificationId, mNotification);

        //Go crazy with a full-on scan.
        AsyncBuildLibraryTask task = new AsyncBuildLibraryTask(mContext);
        task.setOnBuildLibraryProgressUpdate(WelcomeActivity.mBuildingLibraryProgressFragment);
        task.setOnBuildLibraryProgressUpdate(this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStartBuildingLibrary() {

    }

    @Override
    public void onProgressUpdate(AsyncBuildLibraryTask task, String mCurrentTask, int overallProgress,
                                 int maxProgress, boolean mediaStoreTransferDone, boolean syncData) {
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(R.drawable.ic_action_music_raag);
        mBuilder.setContentTitle(mCurrentTask);
        mBuilder.setTicker(mCurrentTask);
        mBuilder.setContentText("");
        mBuilder.setProgress(maxProgress, overallProgress, false);

        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = mBuilder.build();
        mNotification.flags |= Notification.FLAG_INSISTENT | Notification.FLAG_NO_CLEAR;
        mNotifyManager.notify(mNotificationId, mNotification);

    }

    @Override
    public void onFinishBuildingLibrary(AsyncBuildLibraryTask task) {
        mNotifyManager.cancel(mNotificationId);
        stopSelf();
        Toast.makeText(mContext, R.string.finished_scanning_album_art, Toast.LENGTH_LONG).show();
    }
}
