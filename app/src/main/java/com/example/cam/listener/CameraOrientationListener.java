package com.example.cam.listener;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

import com.example.cam.camera.CameraManager;
import com.example.cam.util.Util;


public class CameraOrientationListener extends OrientationEventListener {
    private int currentNormalizedOrientation;
    private int rememberedNormalizedOrientation;
    private Camera.CameraInfo mCameraInfo = null;

    public CameraOrientationListener(Context context) {
        super(context, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation != ORIENTATION_UNKNOWN) {
            currentNormalizedOrientation = normalize(orientation);
        }
    }

    private int normalize(int degrees) {
        if (degrees > 315 || degrees <= 45) {
            return 0;
        }

        if (degrees > 45 && degrees <= 135) {
            return 90;
        }

        if (degrees > 135 && degrees <= 225) {
            return 180;
        }

        if (degrees > 225 && degrees <= 315) {
            return 270;
        }

        throw new RuntimeException("The physics as we know them are no more. Watch out for anomalies.");
    }

    public void rememberOrientationCameraInfo() {
        rememberedNormalizedOrientation = currentNormalizedOrientation;
        mCameraInfo = CameraManager.getInstance().getCameraInfo();
        Util.logD("rememberedNormalizedOrientation:"+rememberedNormalizedOrientation);
    }

    public int getRememberedOrientation() {
        return rememberedNormalizedOrientation;
    }
    public Camera.CameraInfo getCameraInfo(){
        return mCameraInfo;
    }
}
