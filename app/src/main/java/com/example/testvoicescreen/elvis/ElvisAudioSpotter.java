/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen.elvis;

import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.example.testvoicescreen.elvis.ElvisBasedRecognizer.DragonRecognizerCallback;
import com.nuance.dragon.toolkit.elvis.Grammar;
import com.example.testvoicescreen.AppConstants;

public class ElvisAudioSpotter {
    private static final String TAG = AppConstants.TAG + "."
            + ElvisAudioSpotter.class.getSimpleName();
    private static final boolean DEBUG = AppConstants.DEBUG;

    private final ElvisBasedRecognizer mRecog;

    public interface AudioSpotCallback {
        void onSpotted(String word, int confidence);
    }

    private final DragonRecognizerCallback mElvisCallback = new DragonRecognizerCallback() {

        @Override
        public void onInitComplete(final ElvisBasedRecognizer recog, final boolean success) {
            if (DEBUG) {
                Log.d(TAG, "onInitComplete");
            }
        }

        @Override
        public void onError(String errorCode) {
            Log.e(TAG, "Error during audio parsing: " + errorCode);
        }

        @Override
        public void onStartOfSpeech() {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onComplete(int confidence, JSONObject json) {
            if (DEBUG) {
                Log.d(TAG, "onComplete");
            }
            parseJsonResult(confidence, json);
        }
    };

    private final AudioSpotCallback mSpotCallback;

    public ElvisAudioSpotter(AudioSpotCallback callback, List<String> spotCommands) {
        mSpotCallback = callback;
        Grammar grammar = ElvisSpottingGrammar.getGrammar(spotCommands);
        mRecog =  new ElvisBasedRecognizer(mElvisCallback, grammar);
    }

    public void startSpotting() {
        mRecog.startSpotting();
    }

    public void stopSpotting() {
        mRecog.stopSpotting();

    }

    public boolean bufferAvailable(ByteBuffer buffer, int numSamples) {
        return mRecog.bufferAvailable(buffer, numSamples);
    }

    public void cleanUp() {
        mRecog.cleanUp();

        if (DEBUG) {
            Log.d(TAG, "cleaned up");
        }
    }

    private void parseJsonResult(int confidence, JSONObject json) {
        String spotWord = null;
        try {
            JSONObject nbest = json.getJSONObject("nbest");
            int gateConfidence = json.getInt("gate_confidence");
            JSONArray entries = nbest.getJSONArray("entries");
            int count = entries.length();
            if (DEBUG) {
                Log.d(TAG, "nbest = " + count
                        + " gate_confidence = " + gateConfidence
                        + " confidence = " + confidence);
            }
            for (int currCount = 0; currCount < count; currCount++) {
                JSONObject item = entries.getJSONObject(currCount);
                String constraint = item.getString("constraint");
                int score = item.getInt("score");
                JSONArray words = item.getJSONArray("words");
                int wordCount = words.length();

                if ("spotCommand".equals(constraint)) {
                    String command = null;
                    boolean extraWord = false;
                    for (int j = 0; j < wordCount; j++) {
                        JSONObject obj = words.getJSONObject(j);
                        if ("spotCommands".equals(obj.get("slot"))) {
                            command = obj.getString("phrase");
                        } else if ("garbage".equals(obj.get("slot"))) {
                            extraWord = true;
                        }
                    }
                    if (DEBUG) {
                        Log.d(TAG, "[" + score + "] " + command + (extraWord ? " + extra" : ""));
                    }
                    if (currCount == 0 && !extraWord) {
                        spotWord = command;
                    }
                } else if ("garbageConst".equals(constraint)) {
                    if (DEBUG) {
                        Log.d(TAG, "[" + score + "] garbage");
                    }
                }
            }
        } catch (JSONException e) {
        }
        if (mSpotCallback != null) {
            mSpotCallback.onSpotted(spotWord, confidence);
        }
    }
}
