package com.xtc.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * XTC camera-menu container that draws a selection stroke around its content.
 *
 * @hide
 */
public class StrokeLayout extends LinearLayout {
    private Paint mPaint;
    private float mRadius;
    private boolean mShowStroke;
    private float mStrokeWidth;

    public StrokeLayout(Context context) {
        this(context, null);
    }

    public StrokeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StrokeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initSelectedAnimation() {
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(com.android.internal.R.color.colorMenuSelected));
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mRadius = getResources().getDimension(com.android.internal.R.dimen.menuItemRadius);
        mStrokeWidth = getResources().getDimension(
                com.android.internal.R.dimen.menuItemSelectedWidth);
        mPaint.setStrokeWidth(mStrokeWidth);
    }

    public void disableStroke() {
        if (mShowStroke) {
            mShowStroke = false;
            invalidate();
        }
    }

    public void enableStroke() {
        if (mShowStroke) {
            return;
        }
        mShowStroke = true;
        initSelectedAnimation();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mShowStroke) {
            float inset = mStrokeWidth / 2.0f;
            canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset,
                    mRadius, mRadius, mPaint);
        }
    }

    public void setStrokeAlpha(int alpha) {
        if (mShowStroke) {
            mPaint.setAlpha(alpha);
            invalidate();
        }
    }

    public boolean strokeEnable() {
        return mShowStroke;
    }
}
