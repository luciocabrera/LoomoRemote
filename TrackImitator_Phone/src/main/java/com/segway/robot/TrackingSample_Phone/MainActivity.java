package com.segway.robot.TrackingSample_Phone;

//Basics Android
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

//Segway Robot
import com.segway.robot.mobile.sdk.connectivity.BufferMessage;
import com.segway.robot.mobile.sdk.connectivity.MobileException;
import com.segway.robot.mobile.sdk.connectivity.MobileMessageRouter;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.baseconnectivity.Message;
import com.segway.robot.sdk.baseconnectivity.MessageConnection;
import com.segway.robot.sdk.baseconnectivity.MessageRouter;

//Others
import java.nio.ByteBuffer;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import java.util.List;

//-------------------------------------------------------------------

public class MainActivity extends Activity {

    private static final String TAG = "TrackingActivity_Phone";

    private EditText mEditText;
    private EditText mEditTextMessage;
    private Spinner cmbName;
    private ImageView imgPlace;

    private MobileMessageRouter mMobileMessageRouter = null;
    private MessageConnection mMessageConnection = null;

    float SENSITIVITY = 2;

    private enum bodyPartRobot {
        BASE ,
        HEAD
    }

    // called when service bind success or failed, register MessageConnectionListener in onBind
    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind: ");
            try {
                mMobileMessageRouter.register(mMessageConnectionListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnbind(String reason) {
            Log.e(TAG, "onUnbind: " + reason);
        }
    };

    // called when connection created, set ConnectionStateListener and MessageListener in onConnectionCreated
    private MessageRouter.MessageConnectionListener mMessageConnectionListener = new MessageRouter.MessageConnectionListener() {
        @Override
        public void onConnectionCreated(final MessageConnection connection) {
            Log.d(TAG, "onConnectionCreated: " + connection.getName());
            //get the MessageConnection instance
            mMessageConnection = connection;
            try {
                mMessageConnection.setListeners(mConnectionStateListener, mMessageListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    // called when connection state change
    private MessageConnection.ConnectionStateListener mConnectionStateListener = new MessageConnection.ConnectionStateListener() {
        @Override
        public void onOpened() {
            //connection between mobile application and robot application is opened.
            //Now can send messages to each other.
            Log.d(TAG, "onOpened: " + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //enableButtons();
                    Toast.makeText(getApplicationContext(), "connected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onClosed(String error) {
            //connection closed with error
            Log.e(TAG, "onClosed: " + error + ";name=" + mMessageConnection.getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //disableButtons();
                    Toast.makeText(getApplicationContext(), "disconnected to: " + mMessageConnection.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    // called when message received/sent/sentError
    private MessageConnection.MessageListener mMessageListener = new MessageConnection.MessageListener() {
        @Override
        public void onMessageReceived(final Message message) {
            byte[] bytes = (byte[]) message.getContent();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            boolean dataIgnored = buffer.getInt()==1? true:false;
            Log.d(TAG, "onMessageReceived: data ignored=" + dataIgnored + ";timestamp=" + message.getTimestamp());
            if(dataIgnored) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Robot Ignore Data", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Robot Start Tracking", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public void onMessageSentError(Message message, String error) {
            //the message  that is sent failed
            Log.d(TAG, "Message send error");
        }

        @Override
        public void onMessageSent(Message message) {
            //the message  that is sent successfully
            Log.d(TAG, "onMessageSent: id=" + message.getId() + ";timestamp=" + message.getTimestamp());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI components
        mEditText = (EditText) findViewById(R.id.etIP);
        mEditTextMessage = (EditText) findViewById(R.id.etMesssage);
        cmbName = (Spinner) findViewById(R.id.cmbName);
        imgPlace = (ImageView) findViewById(R.id.imgPlace);

        initSeekBar();
        initJoysticks();
        initSpinners();
    }

    private void initSpinners(){

        // Define final variables since they have to be accessed from inner class
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        // Open the database
        databaseAccess.open();

        // Read all the names
        final List<String> names = databaseAccess.getNames();

        // Close the database
        databaseAccess.close();

        // Create adapter and set to the Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cmbName.setAdapter(adapter);
        cmbName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // Get the selected name
                String name = names.get(position);

                // Get the Key
                mEditTextMessage.setText(String.valueOf(position + 1));

                // Open the database
                databaseAccess.open();

                // Retrieve the selected image as byte[]
                byte[] data = databaseAccess.getImage(name);

                // Convert to Bitmap
                Bitmap image =  toBitmap(data);

                // Set to the imgPlace
                imgPlace.setImageBitmap(image);

                // Close the database
                databaseAccess.close();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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

    private void initJoysticks(){

        // init Joystick to control the Base
        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                double linearVelocity = 0;
                double angularVelocity = 0;
                if(strength != 0) {
                    double variableStrength = strength * SENSITIVITY;
                    double scaledStrength = variableStrength / 100;
                    linearVelocity = Math.sin(Math.toRadians(angle)) * scaledStrength;
                    angularVelocity = Math.cos(Math.toRadians(angle)) * scaledStrength;
                    angularVelocity = -angularVelocity;
                }

                sendMove(bodyPartRobot.BASE.ordinal(), linearVelocity, angularVelocity);
            }
        });

        // init Joystick to control the Head
        JoystickView joystickHead = (JoystickView) findViewById(R.id.joystickHeadView);
        joystickHead.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                double linearVelocity = 0;
                double angularVelocity = 0;
                if(strength != 0) {
                    double variableStrength = strength * SENSITIVITY;
                    double scaledStrength = variableStrength / 100;
                    linearVelocity = Math.sin(Math.toRadians(angle)) * scaledStrength;
                    angularVelocity = Math.cos(Math.toRadians(angle)) * scaledStrength;
                    angularVelocity = -angularVelocity;
                }

                sendMove(bodyPartRobot.HEAD.ordinal(), linearVelocity, angularVelocity);
            }
        });

    }

    // init the seekbar
    private void initSeekBar(){
        SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setMax(400);
        seekbar.setProgress(200);
        seekbar.incrementProgressBy(1);

        seekbar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {
                        SENSITIVITY = (float) progress / 100;
                    }
                }
        );
    }

    // handle the click  event on the Main page
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.btnBind:
                // init connection to Robot
                initConnection();
                break;

            case R.id.btnSendMessage:
                int messageId = Integer.parseInt(mEditTextMessage.getText().toString());
                sendMessage(1, messageId);
                break;

            case R.id.btnSendContactKey:
                Log.e(TAG, "GETTING MESSAGE..."  );
                int messageId_ = Integer.parseInt(mEditTextMessage.getText().toString());
                sendMessage(2, messageId_);
                break;

            case R.id.btnResetOrientation:
                resetHeadOrientation();
                break;
        }
    }

    // init connection to Robot
    private void initConnection() {
        // get the MobileMessageRouter instance
        mMobileMessageRouter = MobileMessageRouter.getInstance();

        // you can read the IP from the robot app.
        try {
            mMobileMessageRouter.setConnectionIp(mEditText.getText().toString());

            // bind the connection service in robot
            mMobileMessageRouter.bindService(this, mBindStateListener);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Connection init FAILED", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Connection init FAILED", e);
        }
    }

    // reset Head's Robot
    private void resetHeadOrientation() {

        if (mMessageConnection != null) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(3);
            byte[] messageByte = buffer.array();

            try {
                Log.e(TAG, "RESETTING HEAD ORIENTATION..."  );
                mMessageConnection.sendMessage(new BufferMessage(messageByte));
            } catch (MobileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
    }

    // handle the  Text to Speak (TTS)
    private void sendMessage(int typeMessage, int messageId){
        if (mMessageConnection != null) {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4);
            buffer.putInt(typeMessage);
            buffer.putInt(messageId);
            byte[] messageByte = buffer.array();

            try {
                Log.e(TAG, "SENDING MESSAGE..."  );
                mMessageConnection.sendMessage(new BufferMessage(messageByte));
            } catch (MobileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
    }

    // handle the robot movements
    private void sendMove(int partRobot, double linearVelocity, double angularVelocity){
        if (mMessageConnection != null) {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 8);
            buffer.putInt(0);
            buffer.putInt(partRobot);
            buffer.putDouble(linearVelocity);
            buffer.putDouble(angularVelocity);
            byte[] messageByte = buffer.array();

            try {
                Log.e(TAG, "SENDING MOVE..." + linearVelocity + ".." + angularVelocity);
                mMessageConnection.sendMessage(new BufferMessage(messageByte));
            } catch (MobileException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Toast.makeText(this, "No Connection", Toast.LENGTH_SHORT).show();
    }
}
