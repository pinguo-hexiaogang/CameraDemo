package com.example.cam.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.example.cam.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.ArrayList;

public class GridImageFragment extends Fragment {
    DisplayImageOptions options;
    GridView mGridView = null;
    private String mImagesPath = null;
    private ArrayList<String> mImagesList = new ArrayList<String>();
    public static String KEY_CURRENT_POSITION = "current_position";
    public static String KEY_FILE_LIST = "file_list";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_grid_image, container, false);
        mGridView = (GridView) rootView.findViewById(R.id.grid);
        mImagesPath = getArguments().getString(CamDemoActivity.IMAGE_PATH_EXTRA_KEY);
        initImageList();
        mGridView.setAdapter(new ImageAdapter());
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getActivity(),PagerImageAc.class);
                i.putStringArrayListExtra(KEY_FILE_LIST,mImagesList);
                i.putExtra(KEY_CURRENT_POSITION,position);
                startActivity(i);
            }
        });
        return rootView;
    }

    private void initImageList() {
        File imagesFile = new File(mImagesPath);
        File[] fileArray = imagesFile.listFiles();
        for (int i = fileArray.length - 1; i >= 0; i--) {
            mImagesList.add(fileArray[i].getAbsolutePath());
        }
    }

    public class ImageAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        ImageAdapter() {
            inflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            return mImagesList.size();
        }

        @Override
        public Object getItem(int position) {
            return mImagesList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_grid_image, parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            ImageLoader.getInstance().displayImage("file://" + mImagesList.get(position),holder.imageView, options);
            return view;
        }
    }

    static class ViewHolder {
        ImageView imageView;
        ProgressBar progressBar;
    }
}
