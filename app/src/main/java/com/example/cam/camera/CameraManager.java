package com.example.cam.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.cam.R;
import com.example.cam.listener.CameraOrientationListener;
import com.example.cam.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraManager {
    private static CameraManager sManager = null;
    private WeakReference<Context> mContextWeakRef = null;
    private int mCurrentCameraId = -1;
    private Camera mCamera = null;
    private Camera.Size mCurrentPictureSize = null;
    private List<Camera.Size> mPictureSizeList = null;
    private List<Camera.Size> mPreviewSizeList = null;
    private CameraSurfaceView mSurfaceView = null;
    private OnSaveImgDoneListener mSaveImgDoneListener = null;
    private Camera.Size mPrePictureSize = null;
    private CameraOrientationListener mOrientationListener = null;
    private int mDisplayOrientation = 0;
    private int mLayoutOrientation = 0;

    public static interface OnSaveImgDoneListener {
        public void onDone(String imgPath);
    }

    private CameraManager() {

    }

    public static synchronized CameraManager getInstance() {
        if (sManager == null) {
            sManager = new CameraManager();
        }
        return sManager;
    }

    public void setSaveImgDoneListener(OnSaveImgDoneListener listener) {
        this.mSaveImgDoneListener = listener;
    }

    public void setContext(Context ctx) {
        mContextWeakRef = new WeakReference<Context>(ctx);
    }

    public void setOrientationListener(CameraOrientationListener listener) {
        this.mOrientationListener = listener;
    }

    public void setSurfaceView(CameraSurfaceView view) {
        this.mSurfaceView = view;
    }

    public int getCameraCount() {
        return Camera.getNumberOfCameras();
    }

    public void openCamera() {
        if (getCameraCount() > 0) {
            try {
                if (mCurrentCameraId == -1) {
                    mCamera = Camera.open(0);
                    mCurrentCameraId = 0;
                } else {
                    mCamera = Camera.open(mCurrentCameraId);
                }
                initPicturePreviewSizeList();
                setUpParamByPictureSize(mPrePictureSize);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mContextWeakRef.get(), mContextWeakRef.get().getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initPicturePreviewSizeList() {
        mPictureSizeList = mCamera.getParameters().getSupportedPictureSizes();
        mPreviewSizeList = mCamera.getParameters().getSupportedPreviewSizes();
        Collections.sort(mPictureSizeList, new CameraSizeComparator());
        Collections.sort(mPreviewSizeList, new CameraSizeComparator());
    }

    public void switchCamera() {

    }


    public boolean hasInitDone() {
        return mCamera != null;
    }

    public Camera.CameraInfo getCameraInfo() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, info);
        return info;
    }


    public void setUpParamByPictureSize(Camera.Size pictureSize) {
        mPrePictureSize = pictureSize;
        mCamera.stopPreview();
        if (pictureSize == null) {
            mCurrentPictureSize = mPictureSizeList.get(0);
        } else {
            mCurrentPictureSize = pictureSize;
        }
        setCameraDisplayOrientation();
        Camera.Parameters params = mCamera.getParameters();
        //set focus model
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        //set picture size
        params.setPictureSize(mCurrentPictureSize.width, mCurrentPictureSize.height);
        //set preview size
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(mCurrentPictureSize);
        params.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
        mCamera.setParameters(params);
        //update surface view layout
        Activity ac = (Activity) mContextWeakRef.get();
        DisplayMetrics metrics = new DisplayMetrics();
        ac.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //because our app is portrait
        adjustSurfaceLayoutByPreviewSize(optimalPreviewSize, metrics.widthPixels, metrics.heightPixels);
        mCamera.startPreview();
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, info);
        Activity ac = (Activity) mContextWeakRef.get();
        int rotation = ac.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mDisplayOrientation = result;
        mLayoutOrientation = degrees;
        mCamera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimalPreviewSize(Camera.Size pictureSize) {
        Camera.Size retSize = null;
        for (Camera.Size size : mPreviewSizeList) {
            if (size.equals(pictureSize)) {
                return size;
            }
        }

        float pictureRatio = ((float) pictureSize.width) / pictureSize.height;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        for (Camera.Size size : mPreviewSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(pictureRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    private void adjustSurfaceLayoutByPreviewSize(Camera.Size previewSize, int maxSurfaceWidth, int maxSurfaceHeight) {
        float tmpLayoutHeight = previewSize.width;
        float tmpLayoutWidth = previewSize.height;

        float ratioH, ratioW, ratio;
        ratioH = maxSurfaceHeight / tmpLayoutHeight;
        ratioW = maxSurfaceWidth / tmpLayoutWidth;
        if (ratioH < ratioW) {
            ratio = ratioH;
        } else {
            ratio = ratioW;
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mSurfaceView.getLayoutParams();
        int layoutHeight = (int) (tmpLayoutHeight * ratio);
        int layoutWidth = (int) (tmpLayoutWidth * ratio);

        if ((layoutWidth != mSurfaceView.getWidth()) || (layoutHeight != mSurfaceView.getHeight())) {
            layoutParams.height = layoutHeight;
            layoutParams.width = layoutWidth;
            mSurfaceView.setLayoutParams(layoutParams);
        }
    }

    public void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Camera.Size> getPictureSizeList() {
        return mPictureSizeList;
    }

    public List<String> getSupportWbList() {
        return mCamera.getParameters().getSupportedWhiteBalance();
    }

    public String getWhiteBalance() {
        return mCamera.getParameters().getWhiteBalance();
    }

    public void setWhiteBalance(String wb) {
        Camera.Parameters param = mCamera.getParameters();
        param.setWhiteBalance(wb);
        mCamera.setParameters(param);
    }

    public int getCurrentExposure() {
        return mCamera.getParameters().getExposureCompensation();
    }

    public int getMaxExposure() {
        int max = mCamera.getParameters().getMaxExposureCompensation();
        return max;
    }

    public int getMinExposure() {
        int min = mCamera.getParameters().getMinExposureCompensation();
        return min;
    }

    public void setExposure(int ev) {
        Camera.Parameters param = mCamera.getParameters();
        param.setExposureCompensation(ev);
        mCamera.setParameters(param);
    }

    public void takePicture() {
        mOrientationListener.rememberOrientationCameraInfo();
        mCamera.stopPreview();
        mCamera.takePicture(mShutterCallback, mRawCallback, mJpegCallback);
    }

    public boolean autoFocus() {
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mCamera.getParameters().getFocusMode())) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera arg1) {
                }
            });
        }
            return false;
    }

    public void setZoom(int zoom) {
        if (mCamera != null) {
            Camera.Parameters param = mCamera.getParameters();
            param.setZoom(zoom);
            mCamera.setParameters(param);
        }
    }

    public int getCurrentZoom() {
        return mCamera.getParameters().getZoom();
    }

    public int getMaxZoom() {
        return mCamera.getParameters().getMaxZoom();
    }

    public boolean isZoomSupport() {
        return mCamera.getParameters().isZoomSupported();
    }

    Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
        }
    };

    Camera.PictureCallback mRawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };

    Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            mCamera.startPreview();
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, String> {

        @Override
        protected String doInBackground(byte[]... data) {
            String imagesDir = Util.getPicDirPath();
            if (TextUtils.isEmpty(imagesDir)) {
                Toast.makeText(mContextWeakRef.get(), R.string.no_external_storage, Toast.LENGTH_LONG).show();
                return null;
            }
            Bitmap bitmap = getBitmap(data[0]);

            FileOutputStream outStream = null;
            // Write to SD Card
            try {
                File dir = new File(imagesDir);
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);
                outStream = new FileOutputStream(outFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
//              outStream.write(data[0]);
                Util.logD("onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
                return outFile.getAbsolutePath();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (outStream != null) {
                    try {
                        outStream.flush();
                        outStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        private Bitmap getBitmap(byte[] data) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            Camera.CameraInfo cameraInfo = mOrientationListener.getCameraInfo();
            int rotation = 0;
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (360 - (
                        mDisplayOrientation
                                + mOrientationListener.getRememberedOrientation()
                                + mLayoutOrientation
                )) % 360;
            } else {
                rotation = (
                        mDisplayOrientation
                                + mOrientationListener.getRememberedOrientation()
                                + mLayoutOrientation
                ) % 360;
            }

            if (rotation != 0) {
                Bitmap oldBitmap = bitmap;

                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);

                bitmap = Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        false
                );
                oldBitmap.recycle();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(String path) {
            if(hasInitDone()) {
                mCamera.startPreview();
                if (!TextUtils.isEmpty(path)) {
                    mSaveImgDoneListener.onDone(path);
                }
            }
        }
    }

    public class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if (lhs.width == rhs.width) {
                return 0;
            } else if (lhs.width < rhs.width) {
                return 1;
            } else {
                return -1;
            }
        }

    }
}
