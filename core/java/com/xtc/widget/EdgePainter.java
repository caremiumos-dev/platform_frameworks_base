package com.xtc.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * XTC edge-swipe painter used by {@link CameraMenuView} to draw the pull arrow
 * and to detect the back gesture.
 *
 * @hide
 */
public class EdgePainter {
    private static final String TAG = EdgePainter.class.getSimpleName();

    private int mActiveWidth;
    private Paint mArrowPaint;
    private Path mArrowPath;
    private int mArrowSize;
    private boolean mCallbackAfterAnimation;
    private Paint mCurvePaint;
    private Path mCurvePath;
    private long mDownTime;
    private float mDownX;
    private float mDownY;
    private PointF mEndP;
    private GestureDetector mGesture;
    private boolean mInterceptEvent;
    private GestureListener mListener;
    private float mMaxMoveSize;
    private PointF mMoveP;
    private View mParent;
    private float mRadius;
    private ValueAnimator mStartAnimator;
    private PointF mStartP;
    private int mStartWidth;
    private ValueAnimator mValueAnimator;
    private int mViewHeight;
    private int mViewWidth;
    private boolean inEdgeArea = false;

    public interface GestureListener {
        void onBack();
    }

    private final GestureDetector.OnGestureListener mGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
        private boolean mDismissed;
        private float mMaxScroll;
        private float mScroll;

        private void handleDown(MotionEvent e) {
            mDownX = e.getRawX();
            mDownY = e.getRawY();
            mScroll = 0.0f;
            mMaxScroll = 0.0f;
            mDismissed = false;
            mCallbackAfterAnimation = false;
        }

        private void handleFling(float velocityX, float velocityY, float rawX, float rawY) {
            float back = -velocityX;
            startAnimation(200L, mMoveP.x, mViewWidth);
            if (mDismissed || back <= 100.0f || Math.abs(rawX - mDownX) <= mActiveWidth) {
                return;
            }
            mDismissed = true;
            mCallbackAfterAnimation = true;
        }

        private void handleScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mScroll += distanceX;
            if (mScroll > mMaxScroll) {
                mMaxScroll = mScroll;
            }
            mDismissed = mMaxScroll - mScroll > 10.0f;
        }

        private void handleUp(MotionEvent e, boolean tapped) {
            float rawX = e.getRawX();
            if (mMoveP.x == mViewWidth) {
                mMoveP.x = mViewWidth - (mActiveWidth / 1.5f);
            }
            startAnimation(200L, mMoveP.x, mViewWidth);
            if (tapped && !mDismissed && Math.abs(rawX - mDownX) > mActiveWidth) {
                mDismissed = true;
                mCallbackAfterAnimation = true;
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
            handleFling(velocityX, velocityY, e2.getRawX(), e2.getRawY());
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

    private final ValueAnimator.AnimatorUpdateListener mUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mMoveP.x = (Float) animation.getAnimatedValue();
            invalidate();
        }
    };

    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
            resetAfterAnimation();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            resetAfterAnimation();
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }
    };

    private final Animator.AnimatorListener mCallbackListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
            mValueAnimator.removeListener(this);
            resetAfterAnimation();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mListener != null && mCallbackAfterAnimation) {
                mListener.onBack();
            }
            mCallbackAfterAnimation = false;
            mValueAnimator.removeListener(this);
            resetAfterAnimation();
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }
    };

    public EdgePainter(Context context, View view) {
        mParent = view;
        initViewParams(context);
        mGesture = new GestureDetector(context, mGestureListener);
        initInternalData(mViewWidth, mViewHeight);
        mValueAnimator = new ValueAnimator();
        mValueAnimator.addUpdateListener(mUpdateListener);
        mValueAnimator.addListener(mAnimatorListener);
        mStartAnimator = new ValueAnimator();
        mStartAnimator.addUpdateListener(mUpdateListener);
        mStartAnimator.addListener(mAnimatorListener);
    }

    private void drawArrow(Path path, Paint paint, Canvas canvas) {
        float quarter = mMaxMoveSize / 4.0f;
        path.reset();
        if (mMoveP.x <= mStartWidth + (quarter * 3.0f)) {
            float ratio = ((mViewWidth - mMoveP.x) - quarter) / (quarter * 3.0f);
            float size = ((double) ratio) < 0.5d ? mArrowSize / 2 : mArrowSize * ratio;
            float center = ((mViewWidth + mMoveP.x) + size) / 2.0f;
            paint.setAlpha((int) (ratio * 255.0f));
            path.moveTo(center, mMoveP.y - mArrowSize);
            path.lineTo(center - size, mMoveP.y);
            path.lineTo(center, mMoveP.y + mArrowSize);
        }
        canvas.drawPath(path, paint);
    }

    private void drawCurve(Path path, Paint paint, Canvas canvas) {
        path.reset();
        path.moveTo(mStartP.x, mStartP.y);
        float control = mViewWidth - ((mViewWidth - mMoveP.x) / 4.0f);
        float f = mRadius / 3.0f;
        path.cubicTo(control, mMoveP.y - (mRadius / 2.0f), mMoveP.x, mMoveP.y - f, mMoveP.x, mMoveP.y);
        path.cubicTo(mMoveP.x, mMoveP.y + f, control, mMoveP.y + (mRadius / 2.0f), mEndP.x, mEndP.y);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void initInternalData(int width, int height) {
        mRadius = height / 3;
        mStartP = new PointF(width, (height / 2) - mRadius);
        mEndP = new PointF(width, (height / 2) + mRadius);
        mMoveP = new PointF(width, height / 2);
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

    private void initViewParams(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point(320, 360);
        wm.getDefaultDisplay().getRealSize(point);
        mViewWidth = point.x;
        mViewHeight = point.y;
        mActiveWidth = mViewWidth / 7;
        mStartWidth = mViewWidth - mActiveWidth;
        mArrowSize = mActiveWidth / 3;
        mMaxMoveSize = mActiveWidth;
    }

    private void invalidate() {
        mParent.invalidate();
    }

    private void resetPoint(MotionEvent event) {
        float center = mViewHeight / 2;
        mStartP.y = center - mRadius;
        mEndP.y = mRadius + center;
        mMoveP.x = mViewWidth;
        mMoveP.y = center;
    }

    private void setInEdgeArea(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            inEdgeArea = false;
        }
    }

    private void setMovePoint(MotionEvent event, float distanceX, float distanceY) {
        float x = mMoveP.x - (distanceX / 4.0f);
        if (x > mViewWidth - mMaxMoveSize && x < mViewWidth) {
            mMoveP.x = x;
        }
        invalidate();
    }

    private void startAnimation(long duration, float... values) {
        mValueAnimator.setRepeatCount(0);
        if (mValueAnimator.isRunning()) {
            mValueAnimator.cancel();
        }
        mValueAnimator.setFloatValues(values);
        mValueAnimator.setDuration(duration);
        mValueAnimator.addListener(mCallbackListener);
        mValueAnimator.start();
    }

    protected boolean canScroll(View v, boolean checkV, float dx, float x, float y) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            int scrollX = v.getScrollX();
            int scrollY = v.getScrollY();
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (scrollX + x >= child.getLeft() && scrollX + x < child.getRight()
                        && scrollY + y >= child.getTop() && scrollY + y < child.getBottom()
                        && canScroll(child, true, dx, (scrollX + x) - child.getLeft(),
                                (scrollY + y) - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && v.canScrollHorizontally((int) (-dx));
    }

    public int getActiveWidth() {
        return mActiveWidth;
    }

    public boolean inEdgeArea(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getRawX() > mStartWidth) {
            if (mStartAnimator.isStarted()) {
                mStartAnimator.cancel();
            }
            inEdgeArea = true;
        }
        return inEdgeArea;
    }

    public boolean interceptTouchEvent(MotionEvent event) {
        if (!inEdgeArea(event)) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mGesture.onTouchEvent(event);
                mInterceptEvent = false;
                mDownTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mInterceptEvent) {
                    float dx = event.getRawX() - mDownX;
                    float dy = event.getRawY() - mDownY;
                    float absDx = Math.abs(dx);
                    boolean quick = System.currentTimeMillis() - mDownTime < 250;
                    if (absDx > Math.abs(dy) && absDx > mActiveWidth / 2 && quick
                            && !canScroll(mParent, false, dx, event.getX(), event.getY())) {
                        mInterceptEvent = true;
                    }
                }
                break;
            default:
                break;
        }
        setInEdgeArea(event);
        return mInterceptEvent;
    }

    public void onDetachedFromWindow() {
        mStartAnimator.cancel();
        mValueAnimator.cancel();
    }

    public void onDraw(Canvas canvas) {
        if (mStartP == null) {
            return;
        }
        drawCurve(mCurvePath, mCurvePaint, canvas);
        drawArrow(mArrowPath, mArrowPaint, canvas);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!inEdgeArea(event)) {
            return false;
        }
        boolean handled = mGesture.onTouchEvent(event);
        mGestureListener.onLongPress(event);
        setInEdgeArea(event);
        return handled;
    }

    void resetAfterAnimation() {
        mMoveP.x = mViewWidth;
        invalidate();
    }

    public void setGestureListener(GestureListener listener) {
        mListener = listener;
    }

    public void showTipAnimation() {
        mStartAnimator.setDuration(3000L);
        mStartAnimator.setFloatValues(mViewWidth, mStartWidth, mViewWidth,
                mViewWidth - (mActiveWidth / 2), mViewWidth);
        mStartAnimator.setStartDelay(800L);
        mStartAnimator.start();
    }
}
