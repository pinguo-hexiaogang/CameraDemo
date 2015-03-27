package com.example.cam.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Square layout whose width is equal to height
 */
public class SquareFrameLayout extends FrameLayout{

    public SquareFrameLayout(Context context) {
        super(context);
    }

    public SquareFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //both pass widthMeasureSpec
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
