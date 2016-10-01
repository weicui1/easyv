/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen.elvis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.Handler;
import android.util.Log;

import com.example.testvoicescreen.AppConstants;
import com.nuance.dragon.toolkit.audio.AudioChunk;
import com.nuance.dragon.toolkit.audio.AudioType;
import com.nuance.dragon.toolkit.audio.sources.RecorderSource;
 

public class BufferedAudioSource extends RecorderSource {
    private static final String TAG = AppConstants.TAG + "."
            + BufferedAudioSource.class.getSimpleName();
    private static final boolean DEBUG = AppConstants.DEBUG;
    private final Object mLock = new Object();
    private boolean mStarted = false;

    public BufferedAudioSource(AudioType audioType, Handler workerHandler) {
        super(audioType, workerHandler);
    }

    @Override
    protected boolean isCodecSupported(AudioType type) {
        // Based on acoustic included in the apk.
        return type == AudioType.PCM_16k;
    }

    @Override
    protected boolean startRecordingInternal(AudioType arg0) {
        if (DEBUG) {
            Log.d(TAG, "startRecordingInternal");
        }
        synchronized(mLock) {
            mStarted = true;
            mLock.notifyAll();
        }
        return true;
    }

    @Override
    protected void stopRecordingInternal() {
        if (DEBUG) {
            Log.d(TAG, "stopRecordingInternal");
        }
        synchronized(mLock) {
            mStarted = false;
            mLock.notify();
        }
    }

    @Override
    public void startRecording() {
        synchronized(mLock) {
            if (mStarted) {
                // already started
                return;
            }
            super.startRecording();
            try {
                mLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void stopRecording() {
        synchronized(mLock) {
            if (!mStarted) {
                return;
            }
            super.stopRecording();
            try {
                mLock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    private synchronized short[] getShortArray(ByteBuffer buffer) {
        buffer.rewind();
        int length = buffer.capacity() / 2;
        short[] shortArray = new short[length];
        // make sure we copy in short array with LITTLE ENDIAN format
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < length; i++) {
            shortArray[i] = buffer.getShort();
        }
        return shortArray;
    }

    public boolean bufferAvailable(ByteBuffer buffer, int numSamples) {
        AudioChunk chunk = new AudioChunk(getAudioType(), getShortArray(buffer));
        handleNewAudio(chunk);
        return true;
    }
}
