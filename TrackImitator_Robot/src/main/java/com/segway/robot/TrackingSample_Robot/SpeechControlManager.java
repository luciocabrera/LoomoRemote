package com.segway.robot.TrackingSample_Robot;

import android.content.Context;
import android.util.Log;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.voice.Speaker;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.tts.TtsListener;

/**
 * Created by LCabrera on 27/10/2017.
 */

public class SpeechControlManager {
    private static final String TAG = "SpeechControlManager";
    private boolean mSpeakerReady;
    private TtsListener mTtsListener;
    private Speaker mSpeaker;
    private ServiceBinder.BindStateListener mSpeakerBindStateListener;

    public SpeechControlManager(Context context) {
        Log.d(TAG, "SpeechControlManager() called");
        mSpeaker = Speaker.getInstance();
        initListeners();
        //bind the speaker service.
        mSpeaker.bindService(context.getApplicationContext(), mSpeakerBindStateListener);
    }

    // init listeners.
    private void initListeners() {

        mSpeakerBindStateListener = new ServiceBinder.BindStateListener() {

            @Override
            public void onBind() {
                Log.d(TAG, "Speaker onBind");

                // Speak welcome words
                // loomoSpeaks("Hello, my name is Loomo.");
                mSpeakerReady = true;
            }

            @Override
            public void onUnbind(String s) {
                Log.d(TAG, "Speaker onUnbind");

                // stop recognition
                mSpeakerReady = false;
            }
        };

        mTtsListener = new TtsListener() {

            @Override
            public void onSpeechStarted(String s) {
                //s is speech content, callback this method when speech is starting.
                Log.d(TAG, "onSpeechStarted() called with: s = [" + s + "]");
            }

            @Override
            public void onSpeechFinished(String s) {
                //s is speech content, callback this method when speech is finish.
                Log.d(TAG, "onSpeechFinished() called with: s = [" + s + "]");
            }

            @Override
            public void onSpeechError(String s, String s1) {
                //s is speech content, callback this method when speech occurs error.
                Log.d(TAG, "onSpeechError() called with: s = [" + s + "], s1 = [" + s1 + "]");
            }
        };
    }

    public void loomoSpeaks(String TextToSpeak){
        try{

            mSpeaker.speak(TextToSpeak, mTtsListener);

        } catch (VoiceException e) {
            Log.w(TAG, "Exception: ", e);
        }
    }
}
