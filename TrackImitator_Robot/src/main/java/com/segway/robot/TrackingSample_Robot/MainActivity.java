package com.segway.robot.TrackingSample_Robot;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;


public class MainActivity extends Activity {
    private final String TAG = "TrackingActivity_Robot";

    private static final int ACTION_SHOW_MSG = 1;
    private static final int ACTION_BEHAVE = 2;
    private static final int ACTION_DOWNLOAD_AND_TRACK = 3;
    private static final int ACTION_START_RECOGNITION = 4;
    private static final int ACTION_STOP_RECOGNITION = 5;

    private Context mContext;
    private DatabaseAccess databaseAccess;
    private TextView mTextView;
    private RobotMessageRouter mRobotMessageRouter = null;
    private MessageConnection mMessageConnection = null;
    private BaseControlManager mBase;;
    private HeadControlManager mHead;
    private SpeechControlManager mSpeaker;
    private ListenControlManager mRecognizer;

    private EmojiView mEmojiView;
    private Emoji mEmoji;
    private static final int BASE = 0;
    private static final int HEAD = 1;
    private static final int MOVE = 0;
    private static final int DIALOG = 1;
    private static final int CONTACT = 2;


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ACTION_SHOW_MSG:
                    mTextView.setText(msg.obj.toString());
                    break;
                case ACTION_START_RECOGNITION:
                     mRecognizer.startRecognition();
                     break;
                case ACTION_STOP_RECOGNITION:
                    mRecognizer.stopRecognition();
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
        mBase = new BaseControlManager(this);
        mSpeaker = new SpeechControlManager(this);
        mHead = new HeadControlManager(this);
        mRecognizer = new ListenControlManager(this, mHandler, mSpeaker);

        initView();
        initConnection();
        initEmoji();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind base service and connection service
        mRobotMessageRouter.unbindService();
    }

    private void initView() {
        mTextView = (TextView) findViewById(R.id.tvHint);
        mTextView.setText(getDeviceIp());
    }

    private void initConnection() {
        // get RobotMessageRouter
        mRobotMessageRouter = RobotMessageRouter.getInstance();
        // bind to connection service in robot
        mRobotMessageRouter.bindService(this, mBindStateListener);
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

    private int getCalled(int idCalled){

        int called = MOVE;
        switch (idCalled) {
            case 0:
                called = MOVE;
                break;
            case 1:
                called = DIALOG;
                break;
            case 2:
                called = CONTACT;
                break;
        }
        return called;
    }

    private void actOnData(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int callRobot = buffer.getInt();
        callRobot = getCalled(callRobot);

        while(buffer.hasRemaining()) {
            try {
                switch (callRobot){
                    case MOVE:
                        int partRobot = buffer.getInt();
                        partRobot = getpartRobot(partRobot);
                        float linearVelocity = (float) buffer.getDouble();
                        float angularVelocity = (float) buffer.getDouble();
                        moveRobot(partRobot, linearVelocity, angularVelocity);
                        break;
                    case DIALOG:
                        int idSpeech = buffer.getInt();
                        speakRobot(idSpeech);
                        break;
                    case CONTACT:
                        int idContact = buffer.getInt();
                        String strIdContact=  String.valueOf(idContact);
                        getMessageContact(String.valueOf(strIdContact));
                        break;
                }

            } catch(BufferUnderflowException ignored) {
                break;
            }
        }
    }

    private void moveRobot(int partRobot, float linearVelocity, float angularVelocity) {

        android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_COMFORT);
        mHandler.sendMessage(msg);

        switch (partRobot) {
            case BASE:
                mBase.setLinearVelocity(linearVelocity);
                mBase.setAngularVelocity(angularVelocity);
                break;
            case HEAD:
                mHead.setYawAngularVelocity(angularVelocity);
                mHead.setPitchAngularVelocity(linearVelocity);
                break;
        }
    }

    private void speakRobot(int IdSpeech){

        android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_CURIOUS);
        mHandler.sendMessage(msg);

        switch (IdSpeech){
            case 1:
                mSpeaker.loomoSpeaks("Hi everybody");
                break;
            case 2:
                mSpeaker.loomoSpeaks("Hi body, How is going?");
                break;
            case 3:
                mSpeaker.loomoSpeaks("I am very well");
                break;
            case 4:
                mSpeaker.loomoSpeaks("would you like something to drink?");
                break;
            case 5:
                mSpeaker.loomoSpeaks("very good, you can go to the kitchen and serve yourself");
                break;
            case 6:
                mSpeaker.loomoSpeaks("I am sorry, I didn't get it, could you please repeat again?");
                break;
        }
    }
    // init EmojiView.
    private void initEmoji() {

        mEmoji = Emoji.getInstance();
        mEmoji.init(this);
        mEmoji.setEmojiView((EmojiView) findViewById(R.id.face));
    }

    private void getMessageContact(String ContactId){

        android.os.Message msg = mHandler.obtainMessage(ACTION_BEHAVE, BehaviorList.LOOK_CURIOUS);
        mHandler.sendMessage(msg);

        // Open the database
        databaseAccess.open();

        String speech =  databaseAccess.getDataContact(ContactId);
        mTextView.setText(speech);
        mSpeaker.loomoSpeaks(speech);

        // Close the database
        databaseAccess.close();
    }
}

