package com.example.cam.ui;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.example.cam.R;
import com.example.cam.camera.CameraManager;
import com.example.cam.camera.CameraSurfaceView;
import com.example.cam.listener.CameraOrientationListener;
import com.example.cam.util.Util;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class CamDemoActivity extends Activity {
    public static final String IMAGE_PATH_EXTRA_KEY = "image_path";
    private static final String TAG = "CamTestActivity";
    private static final int SEEK_BAR_SHOW_TIME = 2000;
    private CameraManager mCameraManager = null;
    private Button mBtnCam;
    private CameraSurfaceView mSurfaceView = null;
    private Camera.Size mPreviewSize = null;
    private String mImagePath = null;
    private ImageView mThumbImv = null;
    private Spinner mPicSizeSpinner = null;
    private SeekBar mSeekBar = null;
    private Handler mHandler;
    private CameraOrientationListener mOrientationListener = null;
    private boolean mHasUpdatePicSizeList = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initPath();
        mOrientationListener = new CameraOrientationListener(this);
        mCameraManager = CameraManager.getInstance();
        mCameraManager.setOrientationListener(mOrientationListener);
        mCameraManager.setContext(this);
        mHandler = new Handler();
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        mCameraManager.setSurfaceView(mSurfaceView);
        initViews();
    }

    private void initViews() {
        mThumbImv = (ImageView) findViewById(R.id.imv_thumb);
        mPicSizeSpinner = (Spinner) findViewById(R.id.spinner);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
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
                if (mCameraManager.isZoomSupport()) {
                    mSeekBar.setVisibility(View.VISIBLE);
                }
                mHandler.removeCallbacks(mHideSeekBarRunnable);
                mHandler.postDelayed(mHideSeekBarRunnable, SEEK_BAR_SHOW_TIME);
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

    private void setUpSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCameraManager.setZoom(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeCallbacks(mHideSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.postDelayed(mHideSeekBarRunnable, SEEK_BAR_SHOW_TIME);
            }
        });
        mHandler.postDelayed(mHideSeekBarRunnable, SEEK_BAR_SHOW_TIME);
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
        mCameraManager.setSaveImgDoneListener(new CameraManager.OnSaveImgDoneListener() {
            @Override
            public void onDone(String imgPath) {
                refreshGallery(new File(imgPath));
                ImageLoader.getInstance().displayImage("file://" + imgPath, mThumbImv);
                mBtnCam.setEnabled(true);
            }
        });
    }



    private void setBtnCam() {
        mBtnCam = (Button) findViewById(R.id.btn_capture);

        mBtnCam.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mCameraManager.takePicture();
                mBtnCam.setEnabled(false);
            }
        });

        mBtnCam.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                return mCameraManager.autoFocus();
            }
        });
    }

    private void initPath() {
        mImagePath = Util.getPicDirPath();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationListener.enable();
        mCameraManager.openCamera();
        if(!mHasUpdatePicSizeList) {
            updatePicSizeSpinner();
            mHasUpdatePicSizeList = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationListener.disable();
        mCameraManager.stopCamera();
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    private void updateZoom() {
        if (mCameraManager.isZoomSupport()) {
            mSeekBar.setMax(mCameraManager.getMaxZoom());
        } else {
            mSeekBar.setVisibility(View.GONE);
        }
    }

    private void updatePicSizeSpinner() {
        List<Camera.Size> picSizeList = mCameraManager.getPictureSizeList();
        String[] sizeStrArray = new String[picSizeList.size()];
        for (int i = 0; i < picSizeList.size(); i++) {
            Camera.Size size = picSizeList.get(i);
            String s = size.width + "*" + size.height;
            sizeStrArray[i] = s;
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sizeStrArray);
        mPicSizeSpinner.setAdapter(arrayAdapter);
        mPicSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCameraManager.setUpParamByPictureSize(mCameraManager.getPictureSizeList().get(position));
                updateZoom();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mCameraManager.isZoomSupport()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    int zoom = mCameraManager.getCurrentZoom() + 1;
                    if (zoom <= mCameraManager.getMaxZoom()) {
                        mCameraManager.setZoom(zoom);
                    }
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    int zoomDown = mCameraManager.getCurrentZoom() - 1;
                    if (zoomDown >= 0) {
                        mCameraManager.setZoom(zoomDown);
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}


