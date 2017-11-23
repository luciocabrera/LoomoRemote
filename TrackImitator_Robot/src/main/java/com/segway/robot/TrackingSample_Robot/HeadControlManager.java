package com.segway.robot.TrackingSample_Robot;

import android.content.Context;
import android.util.Log;

import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.emoji.HeadControlHandler;
import com.segway.robot.sdk.locomotion.head.Head;

public class HeadControlManager implements HeadControlHandler{
    private static final String TAG = "HeadControlManager";
    private Head mHead;
    private boolean mIsBindSuccess = false;

    public HeadControlManager(Context context) {
        Log.d(TAG, "HeadControlHandler() called");
        mHead = Head.getInstance();
        mHead.bindService(context.getApplicationContext(), mBindStateListener);
    }

    private ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind() called");
            mHead.setMode(Head.MODE_EMOJI);
            mIsBindSuccess = true;
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
            mIsBindSuccess = false;
        }
    };

    @Override
    public int getMode() {
        if (mIsBindSuccess) {
            return mHead.getMode();
        }
        return 0;
    }

    @Override
    public void setMode(int mode) {
        if (mIsBindSuccess) {
            mHead.setMode(mode);
        }
    }

    @Override
    public void setWorldPitch(float angle) {
        if (mIsBindSuccess) {
            mHead.setWorldPitch(angle);
        }
    }

    @Override
    public void setWorldYaw(float angle) {
        if (mIsBindSuccess) {
            mHead.setWorldYaw(angle);
        }
    }

    @Override
    public float getWorldPitch() {
        if (mIsBindSuccess) {
            return mHead.getWorldPitch().getAngle();
        }
        return 0;
    }

    @Override
    public float getWorldYaw() {
        if (mIsBindSuccess) {
            return mHead.getWorldYaw().getAngle();
        }
        return 0;
    }

    public void setYawAngularVelocity(float angularVelocity) {
        mHead.setYawAngularVelocity(angularVelocity);
    }

    public void setPitchAngularVelocity(float angularVelocity) {
        mHead.setPitchAngularVelocity(angularVelocity);
    }

}
