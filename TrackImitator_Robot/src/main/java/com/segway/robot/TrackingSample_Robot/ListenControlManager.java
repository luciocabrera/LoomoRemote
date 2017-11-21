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
    private String[][] dialogs;

    public ListenControlManager(Context context, Handler mHandler_, SpeechControlManager mSpeaker_) {
        Log.d(TAG, "ListenControlManager() called");
        mRecognizer = Recognizer.getInstance();
        mSpeaker = mSpeaker_;
        initListeners();
        this.mHandler = mHandler_;
        //bind the recognition service.
        mRecognizer.bindService(context.getApplicationContext(), mRecognitionBindStateListener);
        this.initDialogs(context);
    }

    private void initDialogs(Context context){
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context.getApplicationContext());

        databaseAccess.open();

        dialogs = databaseAccess.getDialogs();

        databaseAccess.close();
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
                android.os.Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "Loomo awake, you can say \"OK Loomo\" \n or touch screen");
                mHandler.sendMessage(msg);
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
            }
        };

        mRecognitionListener = new RecognitionListener() {
            @Override
            public void onRecognitionStart() {
                Log.d(TAG, "onRecognitionStart");

                String messageString = "";
                for( int i = 0; i <= dialogs.length - 1; i++) {
                    messageString += ", " + dialogs[i][0];
                }
                android.os.Message statusMsg = mHandler.obtainMessage(ACTION_SHOW_MSG, "Loomo begin recognition, say:\n " +
                        messageString);
                mHandler.sendMessage(statusMsg);
            }

            @Override
            public boolean onRecognitionResult(RecognitionResult recognitionResult) {
                //show the recognition result and recognition result confidence.
                String result = recognitionResult.getRecognitionResult();
                Log.d(TAG, "recognition result: " + result +", confidence:" + recognitionResult.getConfidence());
                android.os.Message resultMsg = mHandler.obtainMessage(ACTION_SHOW_MSG, "recognition result: " + result + ", confidence:" + recognitionResult.getConfidence());
                mHandler.sendMessage(resultMsg);

                if (result.contains("hello") || result.contains("hi")) {
                    try {
                        mRecognizer.removeGrammarConstraint(mMoveSlotGrammar);
                    } catch (VoiceException e) {
                        Log.e(TAG, "Exception: ", e);
                    }
                    //true means continuing to recognition, false means wakeup.
                    return true;
                }else{
                    Log.e(TAG, "I should recognize an expression " +  result);
                    for( int i = 0; i <= dialogs.length - 1; i++) {
                        if (result.contains(dialogs[i][0].trim())){
                            String speechString = "";
                            speechString = dialogs[i][1];
                            mSpeaker.loomoSpeaks(speechString);
                        }
                    }
                    return false;
                }
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
            Slot dialogSlot = new Slot("dialogs");
            dialogSlot.setOptional(false);

            for( int i = 0; i <= dialogs.length - 1; i++) {
                dialogSlot.addWord(dialogs[i][0].trim());
            }
            mMoveSlotGrammar.addSlot(dialogSlot);
        } else {
            // Recognition language dosen't support
            Log.e(TAG, "Speakerlanguage dosen't support " + mRecognitionLanguage);
            android.os.Message msg = this.mHandler.obtainMessage(ACTION_SHOW_MSG, "Speakerlanguage dosen't support " + mRecognitionLanguage);
            this.mHandler.sendMessage(msg);
        }
    }

}
