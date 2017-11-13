package com.segway.robot.TrackingSample_Robot;

import android.content.Context;
import android.util.Log;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;


public class BaseControlManager {
    private static final String TAG = "BaseControlManager";
    private Base mBase;
    private boolean mIsBindSuccess = false;

    public BaseControlManager(Context context) {
        Log.d(TAG, "BaseControlManager() called");
        mBase = Base.getInstance();
        mBase.bindService(context.getApplicationContext(), mBindStateListener);
    }

    // bindService, if not, all Base api will not work.
    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "Base bind success");
            mBase.setControlMode(Base.CONTROL_MODE_RAW);
            mIsBindSuccess = true;
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "Base bind failed");
            mIsBindSuccess = false;
        }
    };

    public void setAngularVelocity(float angularVelocity) {
        mBase.setAngularVelocity(angularVelocity);
    }

    public void setLinearVelocity(float linearVelocity) {
        mBase.setLinearVelocity(linearVelocity);
    }

}
