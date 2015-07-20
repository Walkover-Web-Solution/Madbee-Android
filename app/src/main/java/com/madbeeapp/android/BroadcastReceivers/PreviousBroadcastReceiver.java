package com.madbeeapp.android.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.madbeeapp.android.Utils.Common;

public class PreviousBroadcastReceiver extends BroadcastReceiver {

    private Common mApp;

    @Override
    public void onReceive(Context context, Intent intent) {
        mApp = (Common) context.getApplicationContext();

        if (mApp.isServiceRunning())
            mApp.getService().skipToPreviousTrack();

    }

}
