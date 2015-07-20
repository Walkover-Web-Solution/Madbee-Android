package com.madbeeapp.android.BroadcastReceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Prefs;

public class CallReceiver extends BroadcastReceiver {
    TelephonyManager telManager;
    Context context;
    Common mApp;

    private final PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            try {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING: {
                        if (mApp.isServiceRunning()) {
                            if (mApp.getService().isPlayingMusic()) {
                                mApp.getService().pausePlayback();
                                Prefs.setIsOnCall(context, true);
                            }
                        }
                        break;
                    }

                    case TelephonyManager.CALL_STATE_OFFHOOK: {
                        if (mApp.isServiceRunning()) {
                            if (mApp.getService().isPlayingMusic()) {
                                mApp.getService().pausePlayback();
                                Prefs.setIsOnCall(context, true);
                            }
                        }
                        break;
                    }

                    case TelephonyManager.CALL_STATE_IDLE: {
                        Prefs.setIsOnCall(context, false);
                        if (mApp.isServiceRunning()) {
                            if (Prefs.getIsOnCall(context))
                                mApp.getService().startPlayback();
                        }
                        break;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        mApp = (Common) context.getApplicationContext();
        telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
}