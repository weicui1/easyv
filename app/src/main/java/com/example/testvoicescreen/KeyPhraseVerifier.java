/*
 * Copyright (C) 2012-2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.example.testvoicescreen.elvis.ElvisAudioSpotter;
import com.example.testvoicescreen.elvis.ElvisAudioSpotter.AudioSpotCallback;
import com.example.testvoicescreen.utils.PackageControl;

public class KeyPhraseVerifier {
    private final static String TAG = AppConstants.TAG + "."
            + KeyPhraseVerifier.class.getSimpleName();
    private final static boolean DEBUG = AppConstants.DEBUG;

    private final ElvisAudioSpotter mSpotter;
    List<String> mSpotCommands = new ArrayList<String>();
    private final RecogExecutor mExecutor;

    private final AudioSpotCallback mCallback = new AudioSpotCallback() {

        @Override
        public void onSpotted(String word, int confidence) {
            Log.d(TAG, "on spot: " + word);

            mExecutor.doneRecog(word);
        }
    };

    KeyPhraseVerifier(RecogExecutor executor) {
        mExecutor = executor;
        mSpotCommands.add("capture");
        mSpotCommands.add("take picture");
        mSpotCommands.add("stop");
        mSpotCommands.add("start");

        for (String str :PackageControl.sKeyWords){
            mSpotCommands.add(str);
        }
        mSpotter = new ElvisAudioSpotter(mCallback, mSpotCommands);
    }

    public void reset() {
    }

    public void startRecognition() {
        if (DEBUG) {
            Log.d(TAG, "Starting Key Phrase Spotting.");
        }
        mSpotter.startSpotting();
    }

    public boolean bufferAvailable(ByteBuffer buffer, final int numSamples) {
        mSpotter.bufferAvailable(buffer, numSamples);
        return true;
    }

    public void stopRecognition() {
        mSpotter.stopSpotting();
        if (DEBUG) {
            Log.d(TAG, "Key Phrase Spotting stopped.");
        }
    }

    public void cleanUp() {
        mSpotter.cleanUp();
    }
}
