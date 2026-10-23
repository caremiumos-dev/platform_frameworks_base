package com.xtc.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;

/**
 * XTC camera-menu tile with a press/release zoom animation. Used as the root
 * view of {@code camera_menu_item_layout}.
 *
 * @hide
 */
public class AppLinearLayout extends LinearLayout {
    public static final int TYPE_ZOOM_IN = 0;
    public static final int TYPE_ZOOM_OUT = 1;
    public static final int TYPE_RESET_ZOOM = 2;

    private boolean mDragging;
    ObjectAnimator zoomAlpha;
    ObjectAnimator zoomScaleX;
    ObjectAnimator zoomScaleY;
    private AnimatorSet zoomSet;

    public AppLinearLayout(Context context) {
        this(context, null);
    }

    public AppLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAnimation();
    }

    private void initAnimation() {
        zoomAlpha = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.6f);
        zoomScaleX = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 0.95f);
        zoomScaleY = ObjectAnimator.ofFloat(this, "scaleY", 1.0f, 0.95f);
        zoomSet = new AnimatorSet();
        zoomSet.setDuration(100L);
        zoomSet.setInterpolator(new AccelerateDecelerateInterpolator());
        zoomSet.playTogether(zoomAlpha, zoomScaleX, zoomScaleY);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startZoom(TYPE_ZOOM_OUT);
        } else if (event.getAction() != MotionEvent.ACTION_MOVE
                && (event.getAction() == MotionEvent.ACTION_UP
                        || event.getAction() == MotionEvent.ACTION_CANCEL)
                && !mDragging) {
            startZoom(TYPE_RESET_ZOOM);
        }
        return super.dispatchTouchEvent(event);
    }

    public void setDragging(boolean dragging) {
        mDragging = dragging;
    }

    public void startZoom(int type) {
        float alpha = getAlpha();
        float scaleX = getScaleX();
        float scaleY = getScaleY();
        zoomSet.cancel();
        zoomScaleX.cancel();
        zoomScaleY.cancel();
        zoomAlpha.cancel();
        switch (type) {
            case TYPE_ZOOM_IN:
                zoomAlpha.setFloatValues(alpha, 1.0f);
                zoomScaleX.setFloatValues(scaleX, 1.05f);
                zoomScaleY.setFloatValues(scaleY, 1.05f);
                break;
            case TYPE_ZOOM_OUT:
                zoomAlpha.setFloatValues(alpha, 0.6f);
                zoomScaleX.setFloatValues(scaleX, 0.95f);
                zoomScaleY.setFloatValues(scaleY, 0.95f);
                break;
            default:
                zoomAlpha.setFloatValues(alpha, 1.0f);
                zoomScaleX.setFloatValues(scaleX, 1.0f);
                zoomScaleY.setFloatValues(scaleY, 1.0f);
                break;
        }
        zoomSet.start();
    }
}
