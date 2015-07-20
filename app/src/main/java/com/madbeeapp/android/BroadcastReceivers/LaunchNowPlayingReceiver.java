package com.madbeeapp.android.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.Utils.Common;

public class LaunchNowPlayingReceiver extends BroadcastReceiver {

    private Common mApp;

    @Override
    public void onReceive(Context context, Intent intent) {
        mApp = (Common) context.getApplicationContext();

        if (mApp.isServiceRunning()) {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.putExtra(Common.SHOW_EXPANDEDVIEW, true);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        }
    }
}
