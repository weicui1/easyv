
package com.example.testvoicescreen;

import com.example.testvoicescreen.RecogExecutor.ExecutorCallback;
import com.example.testvoicescreen.utils.PackageControl;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;

public class RecogController extends HandlerThread
        implements IRecogController{
    private final static String TAG = RecogController.class.getSimpleName();
    private Context mContext;

    private final Handler mLooperHandler;
    private final Handler mMainHandler = new Handler();

    HandlerTimer mHandlerTimer;

    private OverlayDisplayWindow mOverlay = null;
    private QuickLaunchWindow mQuickLaunchOverlay = null;

    private void addMicView() {
        mOverlay = OverlayDisplayWindow.create(mContext, "", 400, 400, Gravity.RIGHT|Gravity.TOP);
        mOverlay.show();
        
    }
    
    private void addLaunchView() {

        mQuickLaunchOverlay = QuickLaunchWindow.create(mContext, "", 1000, 400, Gravity.CENTER);
        mQuickLaunchOverlay.show();
    }

    ExecutorCallback mRecogExecutorCB = new RecogExecutor.ExecutorCallback() {
        @Override
        public void onStartRecording() {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        @Override
        public void onRecogResult(final String spot) {
            if (mHandlerTimer == null ) {
                return;
            }
            
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (spot != null) {
                        Log.d(TAG, "get result is " + spot);
                        if (spot.equals("capture") || spot.equals("take picture")) {

                            Thread task = new Thread(new Runnable() {

                                private Handler handler;

                                @Override
                                public void run() {
                                    Instrumentation m_Instrumentation = new Instrumentation();
                                    m_Instrumentation
                                            .sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
                                  /*  Looper.prepare();
                                    handler = new Handler();
                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            // TODO Auto-generated method stub
                                            Instrumentation m_Instrumentation = new Instrumentation();
                                            m_Instrumentation
                                                    .sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER);
                                        }
                                    });
                                    Looper.loop();*/
                                }
                            });
                            task.start();

                        } else if(PackageControl.isPackageName(spot)) {
                            Log.d(TAG, "got packagename");
                            PackageControl.findTargetPackages(spot);
                            addLaunchView();
                            
                        } else if (spot.equalsIgnoreCase("stop")){
                       
                            Thread task = new Thread(new Runnable() {

                                private Handler handler;

                                @Override
                                public void run() {

                                    Looper.prepare();
                                    handler = new Handler();
                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            // TODO Auto-generated method stub
                                            Instrumentation m_Instrumentation = new Instrumentation();
                                            m_Instrumentation
                                                    .sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PAUSE);
                                        }
                                    });
                                    Looper.loop();
                                }
                            });
                            task.start();
                        }  else if (spot.equalsIgnoreCase("start")){
                       
                            Thread task = new Thread(new Runnable() {

                                private Handler handler;

                                @Override
                                public void run() {

                                    Looper.prepare();
                                    handler = new Handler();
                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            // TODO Auto-generated method stub
                                            Instrumentation m_Instrumentation = new Instrumentation();
                                            m_Instrumentation
                                                    .sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY);
                                        }
                                    });
                                    Looper.loop();
                                }
                            });
                            task.start();
                        }
                    }
                }
            });
            mLooperHandler.post(new Runnable() {
                @Override
                public void run() {

                    Log.d(TAG, "doRecognize again");

                    doRecognize();
                }
            });
        }
    };

    private void doRecognize() {

        RecogExecutor.instance().startExecution(mContext, mRecogExecutorCB);
    }

    private enum State {
        IDLE, SPEECH_DETECT
    };

    private State mState = State.IDLE;

    public RecogController(Context context) {
        super(RecogController.class.getSimpleName());
        mContext = context;
        // Redundant code here since mLooperHandler is final & needs to be
        // initialized in constructor.
        start();
      mLooperHandler = new Handler(getLooper());
        //mLooperHandler = new Handler();
    }

    @Override
    public void startRecog() {
        addMicView();
        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                onStart();
            }
        });
    }

    private void onStart() {

        mState = State.SPEECH_DETECT;
        doRecognize();
        initHandlerTimers();
    }

    private void onStop() {

        Log.d(TAG, "onStop of RecogController"); 
        if (mHandlerTimer != null) {
            mHandlerTimer.stop();
            mHandlerTimer = null;
        }
        if (mRecogExecutorCB != null) {
            mRecogExecutorCB = null;
        }
        if (mOverlay != null) {
            mOverlay.dismiss();
        }
        if (mQuickLaunchOverlay != null) {
            mQuickLaunchOverlay.dismiss();
        }

    }

    @Override
    public void cleanUp() {
        mLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                onStop();
                mLooperHandler.removeCallbacksAndMessages(null);
                quit();
            }
        });
        try {
            join();
        } catch (InterruptedException e) {
        }

    }

    private void goToSpeechDetect(long dropDuration, boolean flush) {
        mState = State.SPEECH_DETECT;

        if (dropDuration > 0) {

            Log.d(TAG, "drop audio buffer duration= " + dropDuration);

        }

        // mHandlerTimer.start(AppConstants.actionDetectTimeout + dropDuration);
    }

    private void transitionTo(State newState) {
        switch (mState) {
            case IDLE:
                if (newState == State.SPEECH_DETECT) {
                    // get drop audio buffer duration
                    // Need to check before audio focus

                    // resume recording and process audio for triggered by DSP
                    // ignore audio buffer for headset trigger case

                }
                break;
            case SPEECH_DETECT:
                if (newState == State.IDLE) {
                    mHandlerTimer.stop();

                }
                break;
        }

        Log.d(TAG, "Current: " + mState);

    }

    private void initHandlerTimers() {
        mHandlerTimer = new HandlerTimer(mLooperHandler, new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "handler timeout");

                transitionTo(State.SPEECH_DETECT);
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalize");
    }

};
