package com.example.myapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.RemoteException;

public class MyService extends Service {

    private final RemoteCallbackList<IMyCallback> mCallbacks = new RemoteCallbackList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IMyService.Stub mBinder = new IMyService.Stub() {
        @Override
        public void registerCallback(IMyCallback callback) throws RemoteException {
            mCallbacks.register(callback);
        }

        @Override
        public void unregisterCallback(IMyCallback callback) throws RemoteException {
            mCallbacks.unregister(callback);
        }
    };

    private void notifyCallbacks(String message) {
        int n = mCallbacks.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                mCallbacks.getBroadcastItem(i).onCallback(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void simulateConcurrentAccess() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                notifyCallbacks("Message from thread 1");
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                notifyCallbacks("Message from thread 2");
            }
        }).start();
    }
}