
package com.example.testvoicescreen;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;

public class UIService extends Service {

    private static final String TAG = "UIService";
    private IRecogController mRecogCtrl;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
       
        if (mRecogCtrl != null) {
            mRecogCtrl.cleanUp();
        }
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
 

        mRecogCtrl = new RecogController(this);
        mRecogCtrl.startRecog();

        return START_STICKY;
    }

  
    
   
}
