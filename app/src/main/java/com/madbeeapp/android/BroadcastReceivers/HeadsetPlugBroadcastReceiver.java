package com.madbeeapp.android.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.madbeeapp.android.Utils.Common;

/**
 * BroadcastReceiver that handles and processes all headset
 * unplug/plug actions and events.
 *
 * @author Arpit Gandhi
 */
public class HeadsetPlugBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Common mApp = (Common) context.getApplicationContext();
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    //Headset unplug event.
                    if (mApp.isServiceRunning())
                        if (mApp.getService().isPlayingMusic())
                            mApp.getService().pausePlayback();
                    break;
                case 1:
                    //Headset plug-in event.
                    if (mApp.isServiceRunning())
                        mApp.getService().startPlayback();
                    break;
            }
        }
    }
}
