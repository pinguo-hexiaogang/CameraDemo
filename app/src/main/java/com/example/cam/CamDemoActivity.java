package com.example.cam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class CamDemoActivity extends Activity implements SurfaceHolder.Callback {
    public static final String IMAGE_PATH_EXTRA_KEY = "image_path";
    private static final String TAG = "CamTestActivity";
    private Button mBtnCam;
    private Camera mCamera;
    private SurfaceHolder mHolder = null;
    private SurfaceView mSurfaceView = null;
    private Camera.Size mPreviewSize = null;
    private String mImagePath = null;
    private ImageView mThumbImv = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initPath();
        initImageLoader();

        setContentView(R.layout.main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setKeepScreenOn(true);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mThumbImv = (ImageView) findViewById(R.id.imv_thumb);
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
                //mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
            }
        });
        setBtnCam();
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
                        public void onAutoFocus(boolean arg0, Camera arg1) {
                            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                        }
                    });
                } else {
                    mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
                }
                return true;
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
                mCamera.startPreview();
                setCameraParam(mCamera);
            } catch (RuntimeException ex) {
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
            List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
            if (previewSizes != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                mPreviewSize = getOptimalPreviewSize(previewSizes, metrics.widthPixels, metrics.heightPixels);
            }

            // get Camera parameters
            Camera.Parameters params = camera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
                camera.setParameters(params);
            }
        }
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
}


