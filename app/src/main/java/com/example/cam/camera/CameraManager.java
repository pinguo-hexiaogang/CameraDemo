package com.example.cam.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.cam.R;
import com.example.cam.util.Util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    public static interface OnSaveImgDoneListener{
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

    public void setSaveImgDoneListener(OnSaveImgDoneListener listener){
        this.mSaveImgDoneListener = listener;
    }
    public void setContext(Context ctx) {
        mContextWeakRef = new WeakReference<Context>(ctx);
    }

    public void setSurfaceView(CameraSurfaceView view){
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
                } else {
                    mCamera = Camera.open(mCurrentCameraId);
                }
                initPicturePreviewSizeList();
                setUpParamByPictureSize(null);
            }catch(Exception e){
                e.printStackTrace();
                Toast.makeText(mContextWeakRef.get(), mContextWeakRef.get().getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }
    private void initPicturePreviewSizeList(){
        mPictureSizeList = mCamera.getParameters().getSupportedPictureSizes();
        mPreviewSizeList = mCamera.getParameters().getSupportedPreviewSizes();
        Collections.sort(mPictureSizeList,new CameraSizeComparator());
        Collections.sort(mPreviewSizeList,new CameraSizeComparator());
    }

    public void switchCamera() {

    }


    public void setUpParamByPictureSize(Camera.Size pictureSize){
        mCamera.stopPreview();
        if(pictureSize == null){
            mCurrentPictureSize = mPictureSizeList.get(0);
        }else{
            mCurrentPictureSize = pictureSize;
        }
        mCamera.setDisplayOrientation(90);
        Camera.Parameters params = mCamera.getParameters();
        //set focus model
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        //set picture size
        params.setPictureSize(mCurrentPictureSize.width,mCurrentPictureSize.height);
        //set preview size
        Camera.Size optimalPreviewSize = getOptimalPreviewSize(mCurrentPictureSize);
        params.setPreviewSize(optimalPreviewSize.width,optimalPreviewSize.height);
        mCamera.setParameters(params);
        //update surface view layout
        Activity ac = (Activity)mContextWeakRef.get();
        DisplayMetrics metrics = new DisplayMetrics();
        ac.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        //because our app is portrait
        adjustSurfaceLayoutByPreviewSize(optimalPreviewSize,metrics.widthPixels,metrics.heightPixels);
        mCamera.startPreview();
    }
    private Camera.Size getOptimalPreviewSize(Camera.Size pictureSize){
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

    private void adjustSurfaceLayoutByPreviewSize(Camera.Size previewSize,int maxSurfaceWidth,int maxSurfaceHeight){
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
    public void stopCamera(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void stopPreivew(){
        if(mCamera != null){
            mCamera.stopPreview();
        }
    }
    public void setPreviewDisplay(SurfaceHolder holder){
        if(mCamera != null){
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public List<Camera.Size> getPictureSizeList(){
        return mPictureSizeList;
    }

    public void takePicture(){
        mCamera.stopPreview();
        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public boolean autoFocus(){
        if (Camera.Parameters.FOCUS_MODE_AUTO.equals(mCamera.getParameters().getFocusMode())) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera arg1) {
                    if (success) {
                        mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }
    public void setZoom(int zoom){
        if(mCamera != null){
            Camera.Parameters param = mCamera.getParameters();
            param.setZoom(zoom);
            mCamera.setParameters(param);
        }
    }
    public int getCurrentZoom(){
        return mCamera.getParameters().getZoom();
    }
    public int getMaxZoom(){
        return mCamera.getParameters().getMaxZoom();
    }
    public boolean isZoomSupport(){
        return mCamera.getParameters().isZoomSupported();
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
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
            FileOutputStream outStream = null;
            // Write to SD Card
            try {
                File dir = new File(imagesDir);
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);
                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                Util.logD("onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());
                //refreshGallery(outFile);
                return outFile.getAbsolutePath();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
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

        @Override
        protected void onPostExecute(String path) {
            if (!TextUtils.isEmpty(path)) {
                mSaveImgDoneListener.onDone(path);
            }
        }
    }

    public  class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            if(lhs.width == rhs.width){
                return 0;
            }
            else if(lhs.width < rhs.width){
                return 1;
            }
            else{
                return -1;
            }
        }

    }
}
