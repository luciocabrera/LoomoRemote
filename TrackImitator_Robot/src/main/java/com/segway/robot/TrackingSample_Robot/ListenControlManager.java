package com.segway.robot.TrackingSample_Robot;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.emoji.configure.BehaviorList;
import com.segway.robot.sdk.voice.Languages;
import com.segway.robot.sdk.voice.Recognizer;
import com.segway.robot.sdk.voice.Speaker;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.grammar.GrammarConstraint;
import com.segway.robot.sdk.voice.grammar.Slot;
import com.segway.robot.sdk.voice.recognition.RecognitionListener;
import com.segway.robot.sdk.voice.recognition.RecognitionResult;
import com.segway.robot.sdk.voice.recognition.WakeupListener;
import com.segway.robot.sdk.voice.recognition.WakeupResult;
import com.segway.robot.sdk.voice.tts.TtsListener;

/**
 * Created by LCabrera on 27/10/2017.
 */

public class ListenControlManager {
    private static final String TAG = "ListenControlManager";
    private Recognizer mRecognizer;
    private boolean mRecognitionReady;
    private int mRecognitionLanguage;
    private SpeechControlManager mSpeaker;
    private WakeupListener mWakeupListener;
    private RecognitionListener mRecognitionListener;
    private GrammarConstraint mMoveSlotGrammar;
    private ServiceBinder.BindStateListener mRecognitionBindStateListener;
    private Handler mHandler;
    private static final int ACTION_SHOW_MSG = 1;
    private static final int ACTION_BEHAVE = 4;

    public ListenControlManager(Context context, Handler mHandler_, SpeechControlManager mSpeaker_) {
        Log.d(TAG, "ListenControlManager() called");
        mRecognizer = Recognizer.getInstance();
        mSpeaker = mSpeaker_;
        initListeners();
        this.mHandler = mHandler_;
        //bind the recognition service.
        mRecognizer.bindService(context.getApplicationContext(), mRecognitionBindStateListener);

    }

    // init listeners.
    private void initListeners() {

        mRecognitionBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Log.d(TAG, "Recognition onBind");
                try {
                    //get recognition language when service bind and init Constrained grammar.
                    mRecognitionLanguage = mRecognizer.getLanguage();
                    if (mRecognitionLanguage == Languages.EN_US)
                        initControlGrammar();
                    mRecognizer.addGrammarConstraint(mMoveSlotGrammar);

                    // if ready, start recognition
                    mRecognitionReady = true;
                    if(mRecognitionReady) {
                        try {
                            mRecognizer.startRecognition(mWakeupListener, mRecognitionListener);
                        } catch (VoiceException e) {
                            Log.e(TAG, "Exception: ", e);
                        }
                    }
                } catch (VoiceException e) {
                    Log.e(TAG, "Exception: ", e);
                }
            }

            @Override
            public void onUnbind(String s) {
                Log.d(TAG, "Recognition onUnbind");
                // Stop recognition
                mRecognitionReady = false;
                try {
                    mRecognizer.stopRecognition();
                } catch (VoiceException e) {
                    Log.e(TAG, "stop  recognition failed: ", e);
                }
            }
        };

        mWakeupListener = new WakeupListener() {
            @Override
            public void onStandby() {
                Log.d(TAG, "onStandby");
                //    android.os.Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "Loomo awake, you can say \"OK Loomo\" \n or touch screen");
                //    mHandler.sendMessage(msg);
            }

            @Override
            public void onWakeupResult(WakeupResult wakeupResult) {
                //show the wakeup result and wakeup angle.
                Log.d(TAG, "wakeup word:" + wakeupResult.getResult() + ", angle: " + wakeupResult.getAngle());

            }

            @Override
            public void onWakeupError(String s) {
                //show the wakeup error reason.
                Log.d(TAG, "onWakeupError:" + s);
                //  android.os.Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "wakeup error:" + s);
                // mHandler.sendMessage(msg);
            }
        };

        mRecognitionListener = new RecognitionListener() {
            @Override
            public void onRecognitionStart() {
                Log.d(TAG, "onRecognitionStart");
                android.os.Message statusMsg = mHandler.obtainMessage(ACTION_SHOW_MSG, "Loomo begin recognition, say:\n look up, look down, look left, look right," +
                        " turn left, turn right, turn around, turn full, are you happy, are you worried, are you scared");
                mHandler.sendMessage(statusMsg);
            }

            @Override
            public boolean onRecognitionResult(RecognitionResult recognitionResult) {
                //show the recognition result and recognition result confidence.
                String result = recognitionResult.getRecognitionResult();
                Log.d(TAG, "recognition result: " + result +", confidence:" + recognitionResult.getConfidence());
                android.os.Message resultMsg = mHandler.obtainMessage(ACTION_SHOW_MSG, "recognition result: " + result + ", confidence:" + recognitionResult.getConfidence());
                mHandler.sendMessage(resultMsg);

                // recognize instruction
                if (result.contains("hello") || result.contains("hi")) {
                    try {
                        mRecognizer.removeGrammarConstraint(mMoveSlotGrammar);
                    } catch (VoiceException e) {
                        Log.e(TAG, "Exception: ", e);
                    }
                    //true means continuing to recognition, false means wakeup.
                    return true;
                } else if (result.contains("look") && result.contains("left")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_LEFT);
                    mHandler.sendMessage(msg);
                } else if (result.contains("look") && result.contains("right")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_RIGHT);
                    mHandler.sendMessage(msg);
                } else if (result.contains("look") && result.contains("up")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_UP);
                    mHandler.sendMessage(msg);
                } else if (result.contains("look") && result.contains("down")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_DOWN);
                    mHandler.sendMessage(msg);
                } else if (result.contains("turn") && result.contains("left")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.TURN_LEFT);
                    mHandler.sendMessage(msg);
                } else if (result.contains("turn") && result.contains("right")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.TURN_RIGHT);
                    mHandler.sendMessage(msg);
                } else if (result.contains("turn") && result.contains("around")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.TURN_AROUND);
                    mHandler.sendMessage(msg);
                } else if (result.contains("turn") && result.contains("full")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.TURN_FULL);
                    mHandler.sendMessage(msg);
                } else if (result.contains("are") && result.contains("you")  && result.contains("happy")) {
                    android.os.Message msg4 = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_COMFORT);
                    mHandler.sendMessage(msg4);
                    mSpeaker.loomoSpeaks("Yes, I am very happy, I am the happiest robot in the world");
                } else if (result.contains("are") && result.contains("you")  && result.contains("worried")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_NO_NO);
                    mHandler.sendMessage(msg);
                    mSpeaker.loomoSpeaks("I little bit, you know I  am a baby and sometimes the humans wish me to do to much things");
                } else if (result.contains("are") && result.contains("you")  && result.contains("scared")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_NO_NO);
                    mHandler.sendMessage(msg);
                    mSpeaker.loomoSpeaks("not right now, but please let me know if you see terminator");
                } else if (result.contains("what") && result.contains("kind")  && result.contains("of") && result.contains("robot")  && result.contains("are")  && result.contains("you")) {
                    android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_NO_NO);
                    mHandler.sendMessage(msg);
                    mSpeaker.loomoSpeaks("well, I am a small robot very young");
                } else if (result.contains("are") && result.contains("you")  && result.contains("a") && result.contains("robot") ) {
                    mSpeaker.loomoSpeaks("Yes,  I am a robot");
                }
                return false;
            }

            @Override
            public boolean onRecognitionError(String s) {
                //show the recognition error reason.
                Log.d(TAG, "onRecognitionError: " + s);
                android.os.Message errorMsg = mHandler.obtainMessage(ACTION_SHOW_MSG, "recognition error: " + s);
                mHandler.sendMessage(errorMsg);
                return false; //to wakeup
            }
        };

    }

    public void startRecognition() {

        try {
            mRecognizer.startRecognition(mWakeupListener, mRecognitionListener);
        } catch (VoiceException e) {
            Log.e(TAG, "Exception: ", e);
        }
    }

    public void stopRecognition() {

        try {
            mRecognizer.stopRecognition();
        } catch (VoiceException e) {
            Log.e(TAG, "Exception: ", e);
        }
    }

    // init control grammar.
    private void initControlGrammar() {
        if (mRecognitionLanguage == Languages.EN_US) {
            mMoveSlotGrammar = new GrammarConstraint();
            mMoveSlotGrammar.setName("movement slots grammar");

            Slot moveSlot = new Slot("movement");
            moveSlot.setOptional(false);
            moveSlot.addWord("look");
            moveSlot.addWord("turn");
            mMoveSlotGrammar.addSlot(moveSlot);

            Slot orientationSlot = new Slot("orientation");
            orientationSlot.setOptional(false);
            orientationSlot.addWord("right");
            orientationSlot.addWord("left");
            orientationSlot.addWord("up");
            orientationSlot.addWord("down");
            orientationSlot.addWord("full");
            orientationSlot.addWord("around");
            orientationSlot.addWord("are you happy");
            orientationSlot.addWord("are you worried");
            orientationSlot.addWord("are you scared");
            orientationSlot.addWord("are you a robot");
            orientationSlot.addWord("what kind of robot are you");
            mMoveSlotGrammar.addSlot(orientationSlot);
        } else {
            // Recognition language dosen't support
            Log.e(TAG, "Speakerlanguage dosen't support " + mRecognitionLanguage);
            android.os.Message msg = this.mHandler.obtainMessage(ACTION_SHOW_MSG, "Speakerlanguage dosen't support " + mRecognitionLanguage);
            this.mHandler.sendMessage(msg);
        }
    }

}
