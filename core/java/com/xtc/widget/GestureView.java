package com.xtc.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * XTC gesture view that draws the back-swipe arrow/curve and reports the back
 * gesture through {@link GestureListener}.
 *
 * @hide
 */
public class GestureView extends View {
    private static final String TAG = GestureView.class.getSimpleName();

    private static final int LEFT = android.view.Gravity.LEFT;
    private static final int RIGHT = android.view.Gravity.RIGHT;
    private static final int TOP = android.view.Gravity.TOP;
    private static final int BOTTOM = android.view.Gravity.BOTTOM;

    private int mActiveWidth;
    private Animator.AnimatorListener mAnimatorListener;
    private Paint mArrowPaint;
    private Path mArrowPath;
    private int mArrowSize;
    private Paint mCurvePaint;
    private Path mCurvePath;
    private PointF mEndP;
    private GestureDetector mGesture;
    private final GestureDetector.OnGestureListener mGestureListener;
    private final String[] mIgnoreList;
    private int mLayoutGravity;
    private GestureListener mListener;
    private float mMaxMoveSize;
    private PointF mMoveP;
    private boolean mNeedIgnoreTouch;
    private boolean mNeedZoom;
    private float mRadius;
    private boolean mShowTip;
    private ValueAnimator mStartAnimator;
    private PointF mStartP;
    private final ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private ValueAnimator mValueAnimator;

    public interface GestureListener {
        void onBack();
    }

    public GestureView(Context context) {
        this(context, null);
    }

    public GestureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLayoutGravity = LEFT;
        mShowTip = false;
        mNeedZoom = false;
        mIgnoreList = new String[]{
                "com.xtc.camera.app.module.takephoto.view.TakePhotoMainActivity",
                "com.xtc.camera.app.module.video.view.CameraRecordVideoActivity",
                "com.xtc.intelligentidentify.module.main.view.activity.MainActivity",
                "com.xtc.camera.app.module.takephoto.view.TakePhotoMaskActivity",
                "com.xtc.camera.app.module.clipphoto.view.ClipPhotoActivity"};
        mGestureListener = new GestureDetector.SimpleOnGestureListener() {
            private boolean mDismissed;
            private float mDown;
            private float mMaxScroll;
            private float mScroll;

            private void handleDown(MotionEvent e) {
                switch (mLayoutGravity) {
                    case RIGHT:
                        mDown = e.getX();
                        break;
                    case BOTTOM:
                        mDown = e.getY();
                        break;
                    default:
                        mDown = e.getX();
                        break;
                }
                mScroll = 0.0f;
                mMaxScroll = 0.0f;
                mDismissed = false;
            }

            private void handleFling(float velocityX, float velocityY, float x, float y) {
                switch (mLayoutGravity) {
                    case RIGHT:
                        velocityX = -velocityX;
                        startAnimation(200L, mMoveP.x, getMeasuredWidth());
                        break;
                    case BOTTOM:
                        velocityX = -velocityY;
                        startAnimation(200L, mMoveP.y, getMeasuredHeight());
                        break;
                    default:
                        startAnimation(200L, mMoveP.x, 0.0f);
                        break;
                }
                if (mDismissed || velocityX <= 100.0f || Math.abs(x - mDown) <= mActiveWidth) {
                    return;
                }
                mDismissed = true;
                if (mListener != null) {
                    mListener.onBack();
                }
            }

            private void handleScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                switch (mLayoutGravity) {
                    case RIGHT:
                        mScroll += distanceX;
                        break;
                    case BOTTOM:
                        mScroll += distanceY;
                        break;
                    default:
                        mScroll -= distanceX;
                        break;
                }
                if (mScroll > mMaxScroll) {
                    mMaxScroll = mScroll;
                }
                mDismissed = mMaxScroll - mScroll > 10.0f;
            }

            private void handleUp(MotionEvent e, boolean tapped) {
                float position;
                switch (mLayoutGravity) {
                    case RIGHT:
                        position = e.getX();
                        startAnimation(200L, mMoveP.x, getMeasuredWidth());
                        break;
                    case BOTTOM:
                        position = e.getY();
                        startAnimation(200L, mMoveP.y, getMeasuredHeight());
                        break;
                    default:
                        position = e.getX();
                        startAnimation(200L, mMoveP.x, 0.0f);
                        break;
                }
                if (tapped && !mDismissed && Math.abs(position - mDown) > mActiveWidth) {
                    mDismissed = true;
                    if (mListener != null) {
                        mListener.onBack();
                    }
                }
            }

            @Override
            public boolean onDown(MotionEvent e) {
                resetPoint(e);
                handleDown(e);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                handleFling(velocityX, velocityY, e2.getX(), e2.getY());
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    handleUp(e, true);
                } else if (e.getAction() == MotionEvent.ACTION_CANCEL) {
                    handleUp(e, false);
                }
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                setMovePoint(e2, distanceX, distanceY);
                handleScroll(e1, e2, distanceX, distanceY);
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        };
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mLayoutGravity == LEFT || mLayoutGravity == RIGHT) {
                    mMoveP.x = (Float) animation.getAnimatedValue();
                } else {
                    mMoveP.y = (Float) animation.getAnimatedValue();
                }
                invalidate();
            }
        };
        mAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mMoveP.x = getMeasuredWidth();
                invalidate();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mMoveP.x = getMeasuredWidth();
                invalidate();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        };
        initViewParams();
        mGesture = new GestureDetector(context, mGestureListener);
        setGestureGravity(RIGHT);
        initInternalData(mActiveWidth, 360);
        mValueAnimator = new ValueAnimator();
        mValueAnimator.addUpdateListener(mUpdateListener);
        mValueAnimator.addListener(mAnimatorListener);
        mStartAnimator = new ValueAnimator();
        mStartAnimator.addUpdateListener(mUpdateListener);
        mStartAnimator.addListener(mAnimatorListener);
        Intent intent = ((android.app.Activity) context).getIntent();
        mNeedIgnoreTouch = ignoreBottomTouch(intent);
        mNeedZoom = mIgnoreList[0].equals(intent.getComponent().getClassName())
                ? true : mIgnoreList[1].equals(intent.getComponent().getClassName());
        mShowTip = intent.getBooleanExtra(CameraMenuView.XTC_SHOW_TIP, false);
    }

    private void drawArrow(Path path, Paint paint, Canvas canvas) {
        float quarter = mMaxMoveSize / 4.0f;
        path.reset();
        switch (mLayoutGravity) {
            case RIGHT:
                if (mMoveP.x <= quarter * 3.0f) {
                    float ratio = ((getWidth() - mMoveP.x) - quarter) / (quarter * 3.0f);
                    float size = ((double) ratio) < 0.5d ? mArrowSize / 2 : mArrowSize * ratio;
                    float center = ((getWidth() + mMoveP.x) + size) / 2.0f;
                    paint.setAlpha((int) (ratio * 255.0f));
                    path.moveTo(center, mMoveP.y - mArrowSize);
                    path.lineTo(center - size, mMoveP.y);
                    path.lineTo(center, mMoveP.y + mArrowSize);
                }
                break;
            case BOTTOM:
                if (mMoveP.y < quarter) {
                    int alpha = (int) ((((getHeight() - mMoveP.y) - quarter) / quarter) * 255.0f);
                    float center = ((getHeight() + mMoveP.y) + mArrowSize) / 2.0f;
                    paint.setAlpha(alpha);
                    path.moveTo(mMoveP.x - mArrowSize, center);
                    path.lineTo(mMoveP.x, center - mArrowSize);
                    path.lineTo(mMoveP.x + mArrowSize, center);
                }
                break;
            default:
                if (mMoveP.x > quarter) {
                    int alpha = (int) (((mMoveP.x - quarter) / quarter) * 255.0f);
                    float center = (mMoveP.x - mArrowSize) / 2.0f;
                    paint.setAlpha(alpha);
                    path.moveTo(center, mMoveP.y - mArrowSize);
                    path.lineTo(mArrowSize + center, mMoveP.y);
                    path.lineTo(center, mMoveP.y + mArrowSize);
                }
                break;
        }
        canvas.drawPath(path, paint);
    }

    private void drawCurve(Path path, Paint paint, Canvas canvas) {
        path.reset();
        path.moveTo(mStartP.x, mStartP.y);
        switch (mLayoutGravity) {
            case RIGHT:
                float controlX = getMeasuredWidth() - ((getMeasuredWidth() - mMoveP.x) / 4.0f);
                float f = mRadius / 3.0f;
                path.cubicTo(controlX, mMoveP.y - (mRadius / 2.0f), mMoveP.x, mMoveP.y - f,
                        mMoveP.x, mMoveP.y);
                path.cubicTo(mMoveP.x, mMoveP.y + f, controlX, mMoveP.y + (mRadius / 2.0f),
                        mEndP.x, mEndP.y);
                break;
            case BOTTOM:
                float controlY = getMeasuredHeight() - ((getMeasuredHeight() - mMoveP.y) / 4.0f);
                float f2 = mRadius / 3.0f;
                path.cubicTo(mMoveP.x - (mRadius / 2.0f), controlY, mMoveP.x - f2, mMoveP.y,
                        mMoveP.x, mMoveP.y);
                path.cubicTo(mMoveP.x + f2, mMoveP.y, mMoveP.x + (mRadius / 2.0f), controlY,
                        mEndP.x, mEndP.y);
                break;
            default:
                float f3 = mRadius / 3.0f;
                path.cubicTo(mMoveP.x / 4.0f, mMoveP.y - (mRadius / 2.0f), mMoveP.x, mMoveP.y - f3,
                        mMoveP.x, mMoveP.y);
                path.cubicTo(mMoveP.x, mMoveP.y + f3, mMoveP.x / 4.0f,
                        (mRadius / 2.0f) + mMoveP.y, mEndP.x, mEndP.y);
                break;
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    private boolean ignoreBottomTouch(Intent intent) {
        for (String cls : mIgnoreList) {
            if (cls.equals(intent.getComponent().getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void initInternalData(int width, int height) {
        switch (mLayoutGravity) {
            case RIGHT:
                mRadius = height / 3;
                mStartP = new PointF(width, (height / 2) - mRadius);
                mEndP = new PointF(width, (height / 2) + mRadius);
                mMoveP = new PointF(width, height / 2);
                break;
            case BOTTOM:
                mStartP = new PointF(width / 4, height);
                mEndP = new PointF(width - (width / 4), height);
                mMoveP = new PointF(width / 2, height);
                mRadius = width / 3;
                break;
            default:
                mStartP = new PointF(0.0f, height / 4);
                mEndP = new PointF(0.0f, height - (height / 4));
                mMoveP = new PointF(0.0f, height / 2);
                mRadius = height / 3;
                break;
        }
        mCurvePaint = new Paint();
        mCurvePaint.setAntiAlias(true);
        mCurvePaint.setColor(0x800000A2);
        mCurvePaint.setStyle(Paint.Style.FILL);
        mCurvePath = new Path();
        mArrowPaint = new Paint();
        mArrowPaint.setAntiAlias(true);
        mArrowPaint.setStrokeWidth(4.0f);
        mArrowPaint.setStyle(Paint.Style.STROKE);
        mArrowPaint.setColor(-1);
        mArrowPath = new Path();
    }

    private void initViewParams() {
        mActiveWidth = 45;
        mArrowSize = mActiveWidth / 3;
        mMaxMoveSize = mActiveWidth;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(mActiveWidth,
                FrameLayout.LayoutParams.MATCH_PARENT);
        if (mLayoutGravity == TOP || mLayoutGravity == BOTTOM) {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.height = mActiveWidth;
        }
        lp.gravity = mLayoutGravity | android.view.Gravity.CENTER;
        setLayoutParams(lp);
    }

    private void resetPoint(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (mLayoutGravity) {
            case RIGHT:
                float center = getHeight() / 2;
                mStartP.y = center - mRadius;
                mEndP.y = mRadius + center;
                mMoveP.x = getMeasuredWidth();
                mMoveP.y = center;
                break;
            case BOTTOM:
                mStartP.x = x - mRadius;
                mEndP.x = mRadius + x;
                mMoveP.y = getMeasuredHeight();
                mMoveP.x = x;
                break;
            default:
                mStartP.y = y - mRadius;
                mEndP.y = mRadius + y;
                mMoveP.x = 0.0f;
                mMoveP.y = y;
                break;
        }
    }

    private void setMovePoint(MotionEvent event, float distanceX, float distanceY) {
        switch (mLayoutGravity) {
            case RIGHT:
                float x = mMoveP.x - (distanceX / 4.0f);
                if (x > getMeasuredWidth() - mMaxMoveSize && x < getMeasuredWidth()) {
                    mMoveP.x = x;
                }
                break;
            case BOTTOM:
                float y = mMoveP.y - (distanceY / 4.0f);
                if (y > getMeasuredHeight() - mMaxMoveSize && y < getMeasuredHeight()) {
                    mMoveP.y = y;
                }
                break;
            default:
                float x2 = mMoveP.x - (distanceX / 4.0f);
                if (x2 > 0.0f && x2 < mMaxMoveSize) {
                    mMoveP.x = x2;
                }
                break;
        }
        invalidate();
    }

    private void startAnimation(long duration, float... values) {
        mValueAnimator.setRepeatCount(0);
        mValueAnimator.cancel();
        mValueAnimator.setFloatValues(values);
        mValueAnimator.setDuration(duration);
        mValueAnimator.start();
    }

    public int getActiveWidth() {
        return mActiveWidth;
    }

    public boolean ignoreBottomTouch(MotionEvent event) {
        return mNeedIgnoreTouch && event.getAction() == MotionEvent.ACTION_DOWN
                && event.getRawY() > ((float) ((getHeight() / 3) * 2));
    }

    public boolean ignoreTopTouch(MotionEvent event) {
        return mNeedZoom && event.getAction() == MotionEvent.ACTION_DOWN
                && event.getRawY() < ((float) (getHeight() / 3));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mStartAnimator.cancel();
        mValueAnimator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mStartP == null) {
            return;
        }
        drawCurve(mCurvePath, mCurvePaint, canvas);
        drawArrow(mArrowPath, mArrowPaint, canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (ignoreBottomTouch(event) || ignoreTopTouch(event)) {
            return false;
        }
        if (mStartAnimator.isStarted()) {
            mStartAnimator.cancel();
        }
        boolean handled = mGesture.onTouchEvent(event);
        mGestureListener.onLongPress(event);
        return handled;
    }

    public void setGestureGravity(int gravity) {
        mLayoutGravity = gravity;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (gravity == BOTTOM || gravity == TOP) {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            lp.height = mActiveWidth;
        }
        lp.gravity = android.view.Gravity.CENTER;
        setLayoutParams(lp);
    }

    public void setGestureListener(GestureListener listener) {
        mListener = listener;
    }

    public void showTipAnimation() {
        if (mShowTip) {
            mShowTip = false;
            mStartAnimator.setDuration(3000L);
            mStartAnimator.setFloatValues(getWidth(), 0.0f, getWidth(), getWidth() / 2,
                    getWidth());
            mStartAnimator.setStartDelay(800L);
            mStartAnimator.start();
        }
    }
}
