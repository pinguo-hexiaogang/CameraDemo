package com.example.cam;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;

/**
 * Created by ws-zhangxiaoming on 15-3-18.
 */
public class Util {
//    public static Bitmap getThumbByPath(Context ctx,String path){
//        ContentResolver resolver = ctx.getContentResolver();
//        if(!TextUtils.isEmpty(path) && (new File(path).exists())) {
//            String[] projection = {MediaStore.Images.Thumbnails.}
//            Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnails(resolver, MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, MediaStore.Images.Thumbnails.MICRO_KIND, null);
//            if(cursor != null){
//                do {
//                    cursor.moveToLast();
//                    long imageId = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID));
//                    Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, imageId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
//                    return bitmap;
//                }while(cursor.moveToPrevious());
//            }
//        }
//        return null;
//    }

    public static Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false;
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }
}
