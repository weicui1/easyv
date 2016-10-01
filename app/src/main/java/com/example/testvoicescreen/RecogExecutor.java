/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen;

import java.nio.ByteBuffer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class RecogExecutor extends HandlerThread {
    private final static String TAG = AppConstants.TAG;

    private Handler mLooperHandler;
    private static RecogExecutor sInstance;

    private static final int START_TEST = 0;
    private static final int DO_PREP = 1;

    private static final long INIT_DELAY = 500; // delay to initialize all

    private final KeyPhraseVerifier mMyRecog;
    private boolean mDoRecog = false;

    public interface ExecutorCallback {
        void onStartRecording();
        void onRecogResult(String spot);
    }

    private ExecutorCallback mCallback;

    public static RecogExecutor instance() {
        if (sInstance == null) {
            sInstance = new RecogExecutor();
        }
        return sInstance;
    }

    private RecogExecutor() {
        super("TestExecutor");
        mMyRecog = new KeyPhraseVerifier(this);
        start();
        mLooperHandler = new Handler(getLooper(), mHandler);
    }

    public boolean startExecution(Context context, ExecutorCallback callback) {
        mCallback = callback;

        Log.e(TAG, "Start running one recognition");
        // give some time for other thread to start up.
        mLooperHandler.sendEmptyMessageDelayed(DO_PREP, INIT_DELAY);
        return true;
    }

    Handler.Callback mHandler = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case DO_PREP:
                    mLooperHandler.sendEmptyMessageDelayed(START_TEST, INIT_DELAY);
                    break;
                case START_TEST:
                    startOneScenario();
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    private void startOneScenario() {
        int bufferSize = AudioRecord.getMinBufferSize(16000, 1, AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord audioRecord = null;
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000, 1,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize * 8);
        } catch (IllegalStateException e) {
            Log.e(TAG, "AudioRecorder: AudioRecord init failed.");
            return;
        }

        Log.i(TAG, "Start audio recording..");
        if (mCallback != null) {
            mCallback.onStartRecording();
        }

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "DO start RECORDING.");
            mDoRecog = true;
            audioRecord.startRecording();
            mMyRecog.startRecognition();

            while (mDoRecog) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize * 2);
                int numBytes = audioRecord.read(buffer, bufferSize * 2);

                if (numBytes > 0) {
                    //Log.i(TAG, "read .. " + numBytes + " bytes..");
                    mMyRecog.bufferAvailable(buffer, buffer.capacity() / 2);
                }
            }
            mMyRecog.stopRecognition();
            audioRecord.stop();
        }
        audioRecord.release();
        Log.i(TAG, "Audio recording stopped..");
    }

    public void doneRecog(String recogWord) {
        mDoRecog = false;
        if (mCallback != null) {
            mCallback.onRecogResult(recogWord);
            Log.i(TAG, "Called callback..");
        }
    }
}
