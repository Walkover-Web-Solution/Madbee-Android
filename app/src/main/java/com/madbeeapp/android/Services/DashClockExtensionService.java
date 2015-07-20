package com.madbeeapp.android.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

public class DashClockExtensionService extends DashClockExtension {

    private Common mApp;
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = (Common) this.getApplicationContext();

        //Register a broadcast listener to listen for track updates.
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                updateExtensionData();

            }

        };

        LocalBroadcastManager.getInstance(this)
                .registerReceiver((receiver),
                        new IntentFilter("com.madbeeapp.android.NEW_SONG_UPDATE_UI"));

    }

    @Override
    protected void onUpdateData(int reason) {
        //Nope.
    }

    private void updateExtensionData() {
        ExtensionData data = new ExtensionData();

        //Publish the extension data update.
        if (mApp.isServiceRunning()) {
            //Show the extension with updated data.
            try {

                String status = "Playing";
                String expandedTitle = mApp.getService().getCurrentSong().getTitle();
                String expandedBody = mApp.getService().getCurrentSong().getAlbum()
                        + " - "
                        + mApp.getService().getCurrentSong().getArtist();

                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.putExtra(Common.SHOW_EXPANDEDVIEW, true);
                notificationIntent.putExtra("CALLED_FROM_FOOTER", true);
                notificationIntent.putExtra("CALLED_FROM_NOTIF", true);

                //Publish the extension data update.
                publishUpdate(data.visible(true)
                        .icon(R.drawable.ic_action_music_raag)
                        .status(status)
                        .expandedTitle(expandedTitle)
                        .expandedBody(expandedBody)
                        .clickIntent(notificationIntent));

            } catch (Exception e) {
                e.printStackTrace();
                //Hide the extension.
                publishUpdate(data.visible(false));
            }
        } else {
            //Hide the extension.
            publishUpdate(data.visible(false));
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

    }

}
