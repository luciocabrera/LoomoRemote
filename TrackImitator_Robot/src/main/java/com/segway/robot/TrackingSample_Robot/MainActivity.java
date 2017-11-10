package com.segway.robot.TrackingSample_Robot;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;
import com.segway.robot.sdk.connectivity.BufferMessage;
import com.segway.robot.sdk.connectivity.RobotException;
import com.segway.robot.sdk.connectivity.RobotMessageRouter;
import com.segway.robot.sdk.emoji.Emoji;
import com.segway.robot.sdk.emoji.EmojiPlayListener;
import com.segway.robot.sdk.emoji.EmojiView;
import com.segway.robot.sdk.emoji.configure.BehaviorList;
import com.segway.robot.sdk.emoji.exception.EmojiException;
import com.segway.robot.sdk.emoji.player.RobotAnimator;
import com.segway.robot.sdk.emoji.player.RobotAnimatorFactory;
import com.segway.robot.sdk.locomotion.head.Head;


import com.segway.robot.sdk.voice.Speaker;
import com.segway.robot.sdk.voice.VoiceException;
import com.segway.robot.sdk.voice.recognition.RecognitionListener;
import com.segway.robot.sdk.voice.tts.TtsListener;


import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;


public class MainActivity extends Activity {
    private final String TAG = "TrackingActivity_Robot";

    private static final int ACTION_SHOW_MSG = 1;
    private static final int ACTION_BEHAVE = 2;
    private static final int ACTION_DOWNLOAD_AND_TRACK = 3;
    private static final int ACTION_START_RECOGNITION = 2;
    private static final int ACTION_STOP_RECOGNITION = 3;

    private Context mContext;
    private DatabaseAccess databaseAccess;
    private TextView mTextView;
    private ImageView imgPlace;
    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private BaseControlManager mBase;;
    private Head mHead;
    private Speaker mSpeaker;
    private boolean mSpeakerReady;

    private ServiceBinder.BindStateListener mRecognitionBindStateListener;
    private ServiceBinder.BindStateListener mSpeakerBindStateListener;

    private boolean mRecognitionReady;
    private int mSpeakerLanguage;
    private int mRecognitionLanguage;
    private RecognitionListener mRecognitionListener;
    private TtsListener mTtsListener;
    private EmojiView mEmojiView;
    private Emoji mEmoji;
    MusicPlayer mMusicPlayer;

    private static final int  BASE = 0;
    private static final int HEAD = 1;


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ACTION_SHOW_MSG:
                    mTextView.setText(msg.obj.toString());
                    break;
                case ACTION_BEHAVE:
                    try {
                        mEmoji.startAnimation(RobotAnimatorFactory.getReadyRobotAnimator((Integer)msg.obj), new EmojiPlayListener() {
                            @Override
                            public void onAnimationStart(RobotAnimator animator) {
                                Log.d(TAG, "onAnimationStart: " + animator);
                            }

                            @Override
                            public void onAnimationEnd(RobotAnimator animator) {
                                Log.d(TAG, "onAnimationEnd: " + animator);
                            }

                            @Override
                            public void onAnimationCancel(RobotAnimator animator) {
                                Log.d(TAG, "onAnimationCancel: " + animator);
                            }
                        });
                    } catch (EmojiException e) {
                        Log.e(TAG, "onCreate: ", e);
                    }
                    break;
                case ACTION_DOWNLOAD_AND_TRACK:
                    Message message = (Message)msg.obj;
                    byte[] bytes = (byte[]) message.getContent();
                    actOnData(bytes);

                    break;
            }
        }
    };

    // called when service bind or unBind, register MessageConnectionListener
    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind");
            Toast.makeText(mContext, "Service bind success", Toast.LENGTH_SHORT).show();
            try {
                //register MessageConnectionListener in the RobotMessageRouter
                mRobotMessageRouter.register(mMessageConnectionListener);
            } catch (RobotException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
            Toast.makeText(mContext, "Service bind FAILED", Toast.LENGTH_SHORT).show();
        }
    };

    // called when connection created, set ConnectionStateListener and MessageListener in onConnectionCreated
    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new RobotMessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    // called when connection state changed
    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            Log.d(TAG, "onOpened: ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            Log.e(TAG, "onClosed: " + error);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    // called when message received/sent/sentError, download data in onMessageReceived
    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageSentError(Message message, String error) {
            Log.d(TAG, "Message send error");
        }

        @Override
        public void onMessageSent(Message message) {
            Log.d(TAG, "Message sent");
        }

        @Override
        public void onMessageReceived(final Message message) {
            Log.d(TAG, "onMessageReceived: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
            if (message instanceof BufferMessage) {
                // don't do too much work here to avoid blockage of next message
                // download data and start tracking in UIThread
                android.os.Message msg = mHandler.obtainMessage(ACTION_DOWNLOAD_AND_TRACK, message);
                mHandler.sendMessage(msg);
            } else {
                Log.e(TAG, "Received StringMessage. " + "It's not gonna happen");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        databaseAccess = DatabaseAccess.getInstance(this);
        mEmojiView = (EmojiView) findViewById(R.id.face);
        initView();
        initConnection();
        mBase = new BaseControlManager(this);
        initHead();
        initSpeaker();
        initEmoji();
        mMusicPlayer = MusicPlayer.getInstance();
        mMusicPlayer.initialize(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind base service and connection service
        mHead.unbindService();
        mSpeaker.unbindService();
        mRobotMessageRouter.unbindService();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.tvHint);
        imgPlace = (ImageView) findViewById(R.id.imgPlace);
        mTextView.setText(getDeviceIp());
    }

    private void initConnection() {
        // get RobotMessageRouter
        mRobotMessageRouter = RobotMessageRouter.getInstance();
        // bind to connection service in robot
        mRobotMessageRouter.bindService(this, mBindStateListener);
    }

    private void initHead(){
        // get Head Instance
        mHead = Head.getInstance();
        // bindService, if not, all Head API will not work.
        mHead.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Log.d(TAG, "Head bind success");
                mHead.setMode(Head.MODE_ORIENTATION_LOCK);
            }

            @Override
            public void onUnbind(String reason) {
                Log.d(TAG, "Head bind failed");
            }
        });
    }

    private void initSpeaker(){
        // get Speaker Instance
        mSpeaker = Speaker.getInstance();
        bindSpeakersListeners();

        //bind the speaker service.
        mSpeaker.bindService(MainActivity.this, mSpeakerBindStateListener);
        // get Language
        //mSpeakerLanguage = mSpeaker.getLanguage();
    }

    private void bindSpeakersListeners(){


        mSpeakerBindStateListener = new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                Log.d(TAG, "Speaker onBind");
                try {
                    // make toast to indicate bind success.
                    //Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "Speaker service bind success");
                    //mHandler.sendMessage(msg);

                    //get speaker service language.
                    mSpeakerLanguage = mSpeaker.getLanguage();
                    if (mSpeakerLanguage != mRecognitionLanguage) {
                        Log.e(TAG, "Speakerlanguage dosen't match Recognitionlanguage!!!");
                    }

                    // Speak welcome words
                 //   try {
                 //       mSpeaker.speak("Hello, my name is Loomo.", mTtsListener);
                 //   } catch (VoiceException e) {
                 //       Log.e(TAG, "Speaker speak failed", e);
                 //   }

                    // if both ready, start recognition
                    mSpeakerReady = true;
                    if(mSpeakerReady && mRecognitionReady) {
                        android.os.Message msg = mHandler.obtainMessage(ACTION_START_RECOGNITION);
                        mHandler.sendMessage(msg);
                    }
                } catch (VoiceException e) {
                    Log.e(TAG, "Exception: ", e);
                }
            }

            @Override
            public void onUnbind(String s) {
                Log.d(TAG, "Speaker onUnbind");
                // make toast to indicate unbind success.
                android.os.Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "Speaker service unbind success");
                mHandler.sendMessage(msg);

                // stop recognition
                mSpeakerReady = false;
                msg = mHandler.obtainMessage(ACTION_STOP_RECOGNITION);
                mHandler.sendMessage(msg);
            }
        };


        mTtsListener = new TtsListener() {
            @Override
            public void onSpeechStarted(String s) {
                //s is speech content, callback this method when speech is starting.
                Log.d(TAG, "onSpeechStarted() called with: s = [" + s + "]");
                //Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "speech start");
                //mHandler.sendMessage(msg);
            }

            @Override
            public void onSpeechFinished(String s) {
                //s is speech content, callback this method when speech is finish.
                Log.d(TAG, "onSpeechFinished() called with: s = [" + s + "]");
                //Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "speech end");
                //mHandler.sendMessage(msg);
            }

            @Override
            public void onSpeechError(String s, String s1) {
                //s is speech content, callback this method when speech occurs error.
                Log.d(TAG, "onSpeechError() called with: s = [" + s + "], s1 = [" + s1 + "]");
                android.os.Message msg = mHandler.obtainMessage(ACTION_SHOW_MSG, "speech error: " + s1);
                mHandler.sendMessage(msg);
            }
        };

    }

    private String getDeviceIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = (ipAddress & 0xFF) + "." +
                ((ipAddress >> 8) & 0xFF) + "." +
                ((ipAddress >> 16) & 0xFF) + "." +
                (ipAddress >> 24 & 0xFF);
        return ip;
    }

    private int getpartRobot(int partRobot){

        int part = BASE;
        switch (partRobot) {
            case 0:
                part = BASE;
                break;
            case 1:
                part = HEAD;
               break;
       }
       return part;
    }

    private void actOnData(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int callRobot = buffer.getInt();

        while(buffer.hasRemaining()) {
            try {
                switch (callRobot){
                    case 0:
                        int partRobot = buffer.getInt();
                        partRobot = getpartRobot(partRobot);
                        float linearVelocity = (float) buffer.getDouble();
                        float angularVelocity = (float) buffer.getDouble();
                        android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_COMFORT);
                        mHandler.sendMessage(msg);
                        switch (partRobot) {
                            case BASE:
                                Log.e(TAG, "Moving Base Linear: " + linearVelocity + "< >Angular:" + angularVelocity);
                                mBase.setLinearVelocity(linearVelocity);
                                mBase.setAngularVelocity(angularVelocity);
                                break;
                            case HEAD:
                                Log.e(TAG, "Moving Head Linear: " + linearVelocity + "< >Angular:" + angularVelocity);
                                mHead.setYawAngularVelocity(angularVelocity);
                                mHead.setPitchAngularVelocity(linearVelocity);
                                break;
                        }
                        break;
                    case 1:
                        try {
                            int speechRobot = buffer.getInt();
                            android.os.Message msg_ = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_CURIOUS);
                            mHandler.sendMessage(msg_);

                            //get speaker service language.
                            //mSpeakerLanguage = mSpeaker.getLanguage();
                            Log.d(TAG, "start speak");
                            switch (speechRobot){
                                case 1:
                                    //mSpeaker.speak("Hi everybody", mTtsListener);
                                    loomoSpeaks("Hi everybody");
                                    break;
                                case 2:
                                    //mSpeaker.speak("Hi body, How is going?", mTtsListener);
                                    loomoSpeaks("Hi body, How is going?");
                                    break;
                                case 3:
                                    //mSpeaker.speak("I am very well", mTtsListener);
                                    loomoSpeaks("I am very well");
                                    break;
                                case 4:
                                    //mSpeaker.speak("would you like something to drink?", mTtsListener);
                                    loomoSpeaks("would you like something to drink?");
                                    break;
                                case 5:
                                    //mSpeaker.speak("very good, you can go to the kitchen and serve yourself", mTtsListener);
                                    loomoSpeaks("very good, you can go to the kitchen and serve yourself");
                                    break;
                                case 6:
                                    //mSpeaker.speak("I am sorry, I didn't get it, could you please repeat again?", mTtsListener);
                                    loomoSpeaks("I am sorry, I didn't get it, could you please repeat again?");
                                    break;
                                case 7:
                                    mSpeaker.speak("hello sweetheart", mTtsListener);
                                    break;
                                case 8:
                                    mSpeaker.speak("I love you so much, I am in love with you, and i would like to marry you", mTtsListener);
                                    break;
                            }

                        } catch (VoiceException e) {
                            Log.w(TAG, "Exception: ", e);
                        }
                        break;
                    case 2:
                        try{
                            int speechRobot = buffer.getInt();
                            String strKey =  String.valueOf(speechRobot);
                            getMessageContact(String.valueOf(strKey));
                            //mSpeaker.speak("I will try to get an image from the database", mTtsListener);
                            //loomoSpeaks("I will try to get an image from the database");
                            //getMessageContact(String.valueOf(speechRobot));

                            android.os.Message msg_ = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_CURIOUS);
                            mHandler.sendMessage(msg_);

                            //get speaker service language.
                            mSpeakerLanguage = mSpeaker.getLanguage();

                        } catch (VoiceException e) {
                            Log.w(TAG, "Exception: ", e);
                        }
                        break;
                }

            } catch(BufferUnderflowException ignored) {
                break;
            }
        }
    }


    private void loomoSpeaks(String TextToSpeak){
        try{
            android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_CURIOUS);
            mHandler.sendMessage(msg);

            //get speaker service language.
            mSpeakerLanguage = mSpeaker.getLanguage();

            mSpeaker.speak(TextToSpeak, mTtsListener);

        } catch (VoiceException e) {
            Log.w(TAG, "Exception: ", e);
        }

    }
    // init EmojiView.
    private void initEmoji() {
        mEmoji = Emoji.getInstance();
        mEmoji.init(this);
        mEmoji.setEmojiView((EmojiView) findViewById(R.id.face));
    }

    private void getMessageContact(String ContactId){


        //loomoSpeaks("OPENING DATABASE");
        // Define final variables since they have to be accessed from inner class
        //DatabaseAccess databaseAccess = DatabaseAccess.getInstance(mContext);

        // Open the database
        databaseAccess.open();


        //String data = databaseAccess.getDataContact(ContactId);
        String speech =  databaseAccess.getDataContact(ContactId);
        mTextView.setText(speech);
        loomoSpeaks(speech);
        //mTextView.setText(ContactId + " -- " + data );
        //loomoSpeaks("now getting image");
        // Retrieve the selected image as byte[]
     //   byte[] data = databaseAccess.getImage(ContactId);

        //loomoSpeaks("converting to bitmap");
        // Convert to Bitmap
     //   Bitmap image =  toBitmap(data);

     //   loomoSpeaks("setting the image");
        // Set to the imgPlace
     //  imgPlace.setImageBitmap(image);



        // Close the database
        databaseAccess.close();
    }
    /**
     * Convert byte[] to Bitmap
     *
     * @param image
     * @return
     */
    public static Bitmap toBitmap(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }
}
