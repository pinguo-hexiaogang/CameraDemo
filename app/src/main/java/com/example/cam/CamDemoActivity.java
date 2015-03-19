package com.example.cam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class CamDemoActivity extends Activity implements SurfaceHolder.Callback {
    public static final String IMAGE_PATH_EXTRA_KEY = "image_path";
    private static final String TAG = "CamTestActivity";
    private static final int SEEK_BAR_SHOW_TIME = 2000;
    private Button mBtnCam;
    private Camera mCamera;
    private SurfaceHolder mHolder = null;
    private SurfaceView mSurfaceView = null;
    private Camera.Size mPreviewSize = null;
    private String mImagePath = null;
    private ImageView mThumbImv = null;
    private Spinner mPicSizeSpinner = null;
    private List<Camera.Size> mPicSizeList = null;
    private SeekBar mSeekBar = null;
    private Handler mHandler;
    private boolean mIsSupportZoom = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPath();
        initImageLoader();
        setContentView(R.layout.main);
        mHandler = new Handler();
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setKeepScreenOn(true);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mThumbImv = (ImageView) findViewById(R.id.imv_thumb);
        mPicSizeSpinner = (Spinner) findViewById(R.id.spinner);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        setUpSeekBar();
        setThumbImv();
        mThumbImv.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(CamDemoActivity.this, GridImageAc.class);
                i.putExtra(IMAGE_PATH_EXTRA_KEY, mImagePath);
                startActivity(i);
            }
        });

        mSurfaceView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(mIsSupportZoom){
                    mSeekBar.setVisibility(View.VISIBLE);
                }
                mHandler.removeCallbacks(mHideSeekBarRunnable);
                mHandler.postDelayed(mHideSeekBarRunnable,SEEK_BAR_SHOW_TIME);
            }
        });
        setBtnCam();
    }
    private Runnable mHideSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            mSeekBar.setVisibility(View.GONE);
        }
    };

    private void setUpSeekBar(){
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mCamera != null) {
                    Camera.Parameters param = mCamera.getParameters();
                    param.setZoom(progress);
                    mCamera.setParameters(param);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.postDelayed(mHideSeekBarRunnable,SEEK_BAR_SHOW_TIME);
            }
        });
        mHandler.postDelayed(mHideSeekBarRunnable,SEEK_BAR_SHOW_TIME);
    }

    private void setThumbImv() {
        if (TextUtils.isEmpty(mImagePath)) {
            return;
        }
        File[] imagesArray = new File(mImagePath).listFiles();
        if (imagesArray.length > 1) {
            File lastImageFile = imagesArray[imagesArray.length - 1];
            ImageLoader.getInstance().displayImage("file://" + lastImageFile.getAbsolutePath(), mThumbImv);
        }
    }

    private void initImageLoader() {

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this.getApplication()).defaultDisplayImageOptions(options).build();

        ImageLoader.getInstance().init(config);
    }

    private void setBtnCam() {
        mBtnCam = (Button) findViewById(R.id.btn_capture);

        mBtnCam.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        });

        mBtnCam.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
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
        });
    }

    private void initPath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/CameDemo");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            mImagePath = dir.getAbsolutePath();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int numCams = Camera.getNumberOfCameras();
        if (numCams > 0) {
            try {
                mCamera = Camera.open(0);
                setCameraParam(mCamera);
                mCamera.startPreview();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    private void resetCam() {
        mCamera.startPreview();
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };

    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, String> {

        @Override
        protected String doInBackground(byte[]... data) {
            if (TextUtils.isEmpty(mImagePath)) {
                Toast.makeText(CamDemoActivity.this, R.string.no_external_storage, Toast.LENGTH_LONG).show();
                return null;
            }
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File dir = new File(mImagePath);
                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);
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
        protected void onPostExecute(String s) {
            if (!TextUtils.isEmpty(s)) {
                //int size = getResources().getDimensionPixelSize(R.dimen.thub_imv_width_height);
                //mThumbImv.setImageBitmap(Util.getImageThumbnail(s,size,size));
                ImageLoader.getInstance().displayImage("file://" + s, mThumbImv);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public void setCameraParam(Camera camera) {
        if (camera != null) {
            //camera.setDisplayOrientation(90);
            setCameraDisplayOrientation(this, 0, camera);

            List<Camera.Size> pictureSize = camera.getParameters().getSupportedPictureSizes();
            List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
            mPicSizeList = pictureSize;
            if (previewSizes != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                mPreviewSize = getOptimalPreviewSize(previewSizes, metrics.widthPixels, metrics.heightPixels);
                //mPreviewSize = getOptimalPreviewSize(previewSizes, 700, 600);
            }

            // get Camera parameters
            Camera.Parameters params = camera.getParameters();
            updateZoom();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                camera.setParameters(params);
            }
            updatePicSizeSpinner();
        }
    }

    private void updateZoom() {
        Camera.Parameters params = mCamera.getParameters();
        mIsSupportZoom = params.isZoomSupported();
        if(params.isZoomSupported()){
            mSeekBar.setMax(params.getMaxZoom());
        }else{
            mSeekBar.setVisibility(View.GONE);
        }
        mCamera.setParameters(params);
    }

    private void updatePicSizeSpinner() {
        Collections.reverse(mPicSizeList);
        String[] sizeStrArray = new String[mPicSizeList.size()];
        for (int i = 0; i < mPicSizeList.size(); i++) {
            Camera.Size size = mPicSizeList.get(i);
            String s = size.width + "*" + size.height;
            sizeStrArray[i] = s;
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sizeStrArray);
        mPicSizeSpinner.setAdapter(arrayAdapter);
        mPicSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Camera.Parameters param = mCamera.getParameters();
                Camera.Size selectedSize = mPicSizeList.get(position);
                param.setPictureSize(selectedSize.width, selectedSize.height);
                mCamera.setParameters(param);
                updateZoom();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setCameraDisplayOrientation(Activity activity,
                                             int cameraId, Camera camera) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
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
        camera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mIsSupportZoom) {
            Camera.Parameters parameters = mCamera.getParameters();
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    int zoom = parameters.getZoom() + 1;
                    if(zoom <= parameters.getMaxZoom()){
                        parameters.setZoom(zoom);
                        mCamera.setParameters(parameters);
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    int zoomDown = parameters.getZoom() - 1;
                    if(zoomDown >= 0){
                        parameters.setZoom(zoomDown);
                        mCamera.setParameters(parameters);
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}


