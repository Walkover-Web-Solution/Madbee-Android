package com.madbeeapp.android.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.madbeeapp.android.Utils.Common;

public class StopServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //Stop the service.
        Common app = (Common) context.getApplicationContext();
        if (app.isServiceRunning())
            app.getService().stopSelf();
    }
}
