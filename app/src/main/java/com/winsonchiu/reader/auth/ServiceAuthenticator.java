package com.winsonchiu.reader.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ServiceAuthenticator extends Service {
    public ServiceAuthenticator() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Authenticator authenticator = new Authenticator(this);
        return authenticator.getIBinder();
    }
}
