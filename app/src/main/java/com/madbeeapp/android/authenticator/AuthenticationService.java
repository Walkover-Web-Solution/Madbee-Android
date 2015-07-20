package com.madbeeapp.android.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticationService extends Service {

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
