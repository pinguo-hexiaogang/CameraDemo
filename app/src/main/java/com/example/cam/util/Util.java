package com.example.cam.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class Util {

    private static final String TAG = "CameraDemo";
    private static boolean sIsLogAble = true;
    private Util(){}

    public static void logE(String msg){
        if(sIsLogAble){
            Log.e(TAG,msg);
        }
    }
    public static void logD(String msg){
        if(sIsLogAble){
            Log.d(TAG,msg);
        }
    }

    /**
     * @return the path of the picture to store
     */
    public static String getPicDirPath(){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/CameDemo");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir.getAbsolutePath();
        }else{
            return null;
        }
    }

}
