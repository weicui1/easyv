/*
 * Copyright (C) 2012 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen;

import android.os.Handler;
import android.util.Log;
 

public class HandlerTimer {
    private final static String TAG = AppConstants.TAG + "."
            + HandlerTimer.class.getSimpleName();

    private final Handler mHandler;
    private final Runnable mRunnable;

    public HandlerTimer(final Handler handler, final Runnable runnable) {
        mHandler = handler;
        mRunnable = runnable;
    }

    public synchronized void start(long timeout) {
       
            Log.d(TAG, "start: timeout=" + timeout);
        

        // If the runnable is already scheduled, cancel it.
        if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        boolean success = mHandler.postDelayed(mRunnable, timeout);
        if (!success) {
            Log.w(TAG, "Failed to start");
        }
    }

    public synchronized void stop() {
 
            Log.d(TAG, "stop");
   

        if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }
}
