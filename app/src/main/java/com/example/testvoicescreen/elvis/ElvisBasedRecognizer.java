/*
 * Copyright (C) 2013 Motorola Mobility LLC.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 */

package com.example.testvoicescreen.elvis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.example.testvoicescreen.AppConstants;
import com.nuance.dragon.toolkit.audio.AudioType;
import com.nuance.dragon.toolkit.audio.SpeechDetectionListener;
import com.nuance.dragon.toolkit.elvis.Constraint;
import com.nuance.dragon.toolkit.elvis.ElvisConfig;
import com.nuance.dragon.toolkit.elvis.ElvisError;
import com.nuance.dragon.toolkit.elvis.ElvisRecognizer;
import com.nuance.dragon.toolkit.elvis.ElvisRecognizer.RebuildListener.SkippedWord;
import com.nuance.dragon.toolkit.elvis.ElvisResult;
import com.nuance.dragon.toolkit.elvis.Grammar;
import com.nuance.dragon.toolkit.file.FileManager; 

public class ElvisBasedRecognizer extends HandlerThread {
    private static final String TAG = AppConstants.TAG + "."
            + ElvisBasedRecognizer.class.getSimpleName();
    private static final boolean DEBUG = AppConstants.DEBUG;
    private static final boolean VERBOSE = false;
    private ElvisRecognizer mElvisRecognizer;
    private BufferedAudioSource mSource;
    private final Handler mLooperHandler;

    private final HandlerThread mAudioWorkerThread;
    private final Handler mAudioWorkerHandler;

    private final ElvisRecognizer.InitializeListener mInitListener;
    private final ElvisRecognizer.ResultListener mResultListener;
    private final ElvisRecognizer.RebuildListener mRebuildListener;
    private final ElvisRecognizer.ReleaseListener mReleaseListener;
    private final SpeechDetectionListener mSpeechListener;

    private final DragonRecognizerCallback mCallback;

    private boolean mReadyToRecognize = false;

    // Each ElvisRecognizer instance (with different name) will create different
    // files and it's not cleaned up even if recognizer is destroyed. Try to
    // limit file leaks by limiting the number of instance name available.
    private static final List<String> sInstanceNames = new ArrayList<String>();

    private final String mInstanceName;
    private static int sInstanceMaxNum = 0;

    // It looks Nuance engine doesn't like multiple FileManager exists in a
    // process.
    private static FileManager sFileManager = null;

    public static void initFileManagerAndInstanceNames(Context applicationContext) {
        if (sFileManager == null) {
            sFileManager = new FileManager(applicationContext, ".jpg", "elvis", "elvis");
        }
    }

    public interface DragonRecognizerCallback {
        public void onInitComplete(ElvisBasedRecognizer recog, boolean success);

        public void onError(String errorCode);

        public void onStartOfSpeech();

        public void onEndOfSpeech();

        public void onComplete(int confidence, JSONObject json);
    }

    public ElvisBasedRecognizer(DragonRecognizerCallback callback,
            final Grammar grammar) {
        super(ElvisBasedRecognizer.class.getSimpleName());

        // make sure sFileManager is set.
        if (sFileManager == null) {
            throw new RuntimeException("FileManager not initialized");
        }
        synchronized (sInstanceNames) {
            if (sInstanceNames.isEmpty()) {
                mInstanceName = "default-" + Integer.toString(++sInstanceMaxNum);
            } else {
                mInstanceName = sInstanceNames.remove(0);
            }
        }
        mCallback = callback;
        start();
        mLooperHandler = new Handler(getLooper());

        mAudioWorkerThread = new HandlerThread("ElvisAudioWorker");
        mAudioWorkerThread.start();
        mAudioWorkerHandler = new Handler(mAudioWorkerThread.getLooper());

        mInitListener = new ElvisRecognizer.InitializeListener() {
            @Override
            public void onLoaded(ElvisRecognizer recog, boolean success) {
                if (!success) {
                    Log.e(TAG, "Init failure " + mInstanceName);
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "Init Success " + mInstanceName);
                    }
                    if (mElvisRecognizer != null) {
                        mElvisRecognizer.loadGrammar(
                                grammar,
                                mRebuildListener);
                    }
                }
            }
        };

        mRebuildListener = new ElvisRecognizer.RebuildListener() {
            @Override
            public void onComplete(Grammar grammar, List<SkippedWord> skippedWords) {
                List<Constraint> constraints = grammar.getConstraints();
                if (constraints != null && mElvisRecognizer != null) {
                    mElvisRecognizer.setActiveConstraints(constraints);

                    if (DEBUG) {
                        Log.d(TAG, "Rebuild completed");
                        String skipped = getSkippedWordList(skippedWords);
                        Log.d(TAG, "skipped words : " + skipped);
                    }
                    mReadyToRecognize = true;
                    mCallback.onInitComplete(ElvisBasedRecognizer.this, true);
                } else {
                    Log.e(TAG, "Init not completed");
                }
            }

            @Override
            public void onError(ElvisError error) {
                Log.e(TAG, "Failed to build the grammar: " + error.getReason());
                mCallback.onInitComplete(ElvisBasedRecognizer.this, false);
            }
        };
        mResultListener = new ElvisRecognizer.ResultListener() {
            @Override
            public void onResult(ElvisResult result) {
                if (mCallback != null) {
                    mCallback.onComplete(result.getConfidence(), result.toJSON());
                }
            }

            @Override
            public void onError(ElvisError error) {
                Log.e(TAG, "Recognition result failure : " + error.getReason());
                if (mCallback != null) {
                    mCallback.onError(error.getReason());
                }
            }
        };

        mReleaseListener = new ElvisRecognizer.ReleaseListener() {
            @Override
            public void onReleased(ElvisRecognizer arg0) {
                // stop handler thread
                quit();
            }
        };

        mSpeechListener = new SpeechDetectionListener() {
            @Override
            public void onStartOfSpeech() {
                if (DEBUG) {
                    Log.d(TAG, "Start of speech detected.");
                }
                if (mCallback != null) {
                    mCallback.onStartOfSpeech();
                }
            }

            @Override
            public void onEndOfSpeech() {
                if (DEBUG) {
                    Log.d(TAG, "End of speech detected.");
                }
                if (mCallback != null) {
                    mCallback.onEndOfSpeech();
                }
            }
        };

        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                mElvisRecognizer = ElvisRecognizer.createElvisRecognizer(sFileManager);

                mElvisRecognizer.enableVerboseAndroidLogging(VERBOSE);

                mElvisRecognizer.initialize(
                        new ElvisConfig(ElvisSettings.getLanguage(),
                                getElvisFrequencies(AppConstants.sampleRate)),
                        mInstanceName, mInitListener);

            }
        });
    }

    private int getElvisFrequencies(int sampleRate) {
        // making an assumption that rates we support are only 16k and 8K
        return sampleRate == 16000 ? ElvisRecognizer.Frequencies.FREQ_16KHZ :
                ElvisRecognizer.Frequencies.FREQ_8KHZ;
    }

    private AudioType sampleRateToAudioType(int sampleRate) {
        // making an assumption that rates we support are only 16k and 8K
        return sampleRate == 16000 ? AudioType.PCM_16k : AudioType.PCM_8k;
    }

    public boolean isReady() {
        return mReadyToRecognize;
    }

    public void recognize(final List<ByteBuffer> audio) {
        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                startRecognition();
                for (ByteBuffer buffer : audio) {
                    bufferAvailable(buffer, buffer.capacity() / 2);
                }
                stopRecognition();
            }
        });
    }

    private synchronized void startRecognition() {
        if (!mReadyToRecognize) {
            Log.e(TAG, "Recognizer not ready");
            return;
        }
        if (mSource != null) {
            mSource.stopRecording();
        }
        mSource = new BufferedAudioSource(sampleRateToAudioType(AppConstants.sampleRate),
                mAudioWorkerHandler);
        mSource.startRecording();
        if (DEBUG) {
            Log.d(TAG, "StartRecognition");
        }
        mElvisRecognizer.startRecognition(mSource, mSpeechListener, mResultListener);
    }

    private synchronized void stopRecognition() {
        if (mSource != null) {
            mSource.stopRecording();
            mSource = null;
        }
    }

    public synchronized boolean bufferAvailable(ByteBuffer buffer, int numSamples) {
        if (mSource != null) {
            mSource.bufferAvailable(buffer, numSamples);
        }
        return true;
    }

    public void startSpotting() {
        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                startRecognition();
            }
        });
    }

    public void stopSpotting() {
        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                stopRecognition();
            }
        });
    }

    public void cleanUp() {
        stopRecognition();
        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                mReadyToRecognize = false;
                if (mElvisRecognizer != null) {
                    mElvisRecognizer.release(mReleaseListener);
                    mElvisRecognizer = null;
                }
            }
        });
        while (true) {
            try {
                join(3000);
                break;
            } catch (InterruptedException e) {
            }
        }
        mAudioWorkerThread.quit();
        while (true) {
            try {
                mAudioWorkerThread.join(3000);
                break;
            } catch (InterruptedException e) {
            }
        }

        synchronized (sInstanceNames) {
            sInstanceNames.add(mInstanceName);
        }
        if (DEBUG) {
            Log.d(TAG, "Cleaned up " + mInstanceName);
        }
    }

    private String getSkippedWordList(List<SkippedWord> skippedWords) {
        StringBuilder sb = new StringBuilder();
        for (SkippedWord skippedWord : skippedWords) {
            sb.append("Slot: ").append(skippedWord.getSlotId());
            sb.append("(").append(skippedWord.getWord().getSurfaceForm()).append(",");
            sb.append(skippedWord.getWord().getSpokenForm()).append(")\n");
        }
        return sb.toString();
    }
}
