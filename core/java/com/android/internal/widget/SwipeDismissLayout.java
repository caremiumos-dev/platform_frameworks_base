/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.res.TypedArray;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

/**
 * A layout which can be dismissed by swiping it to the right.
 *
 * <p>XTC device addition: the vendor repurposes this as the "swipe right to
 * exit" gesture, which also drives a back-window translucency/scale animation
 * through {@link Activity#setBackActivityAnim(float)} and
 * {@link Activity#setBackActivityBackground(boolean)}.</p>
 */
public class SwipeDismissLayout extends FrameLayout {
    private static final String TAG = "SwipeDismissLayout";

    private static final float ALPHA_DECREASE = 0.7f;
    private static final int MAXIMUM_FLING_VELOCITY = 2000;
    private static final float MAX_DIST_THRESHOLD = 0.33f;
    private static final float MIN_DIST_THRESHOLD = 0.25f;
    private static final float VELOCITY_MIN_ANIMVECTOR = 0.5f;

    public interface OnDismissedListener {
        void onDismissed(SwipeDismissLayout layout);
    }

    public interface OnSwipeProgressChangedListener {
        void onSwipeCancelled(SwipeDismissLayout layout);

        void onSwipeProgressChanged(SwipeDismissLayout layout, float alpha, float translate);
    }

    private boolean isBackWindowTranslucencyConverted;
    private int mActiveTouchId;
    private boolean mActivityTranslucencyConverted;
    private boolean mBlockGesture;
    private boolean mDiscardIntercept;
    private final DismissAnimator mDismissAnimator;
    private boolean mDismissable;
    private boolean mDismissed;
    private OnDismissedListener mDismissedListener;
    private float mDownX;
    private float mDownY;
    private boolean mIsWindowNativelyTranslucent;
    private float mLastX;
    private int mMinFlingVelocity;
    private OnSwipeProgressChangedListener mProgressListener;
    private IntentFilter mScreenOffFilter;
    private BroadcastReceiver mScreenOffReceiver;
    private int mSlop;
    private boolean mSwiping;
    private VelocityTracker mVelocityTracker;

    public SwipeDismissLayout(Context context) {
        super(context);
        mBlockGesture = false;
        mActivityTranslucencyConverted = false;
        isBackWindowTranslucencyConverted = false;
        mDismissAnimator = new DismissAnimator();
        mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mDismissable = true;
        init(context);
    }

    public SwipeDismissLayout(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        mBlockGesture = false;
        mActivityTranslucencyConverted = false;
        isBackWindowTranslucencyConverted = false;
        mDismissAnimator = new DismissAnimator();
        mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mDismissable = true;
        init(context);
    }

    public SwipeDismissLayout(Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mBlockGesture = false;
        mActivityTranslucencyConverted = false;
        isBackWindowTranslucencyConverted = false;
        mDismissAnimator = new DismissAnimator();
        mScreenOffFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mDismissable = true;
        init(context);
    }

    public void setOnDismissedListener(OnDismissedListener listener) {
        mDismissedListener = listener;
    }

    public void setOnSwipeProgressChangedListener(OnSwipeProgressChangedListener listener) {
        mProgressListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            mScreenOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            if (mDismissed) {
                                dismiss();
                            } else {
                                cancel();
                            }
                            resetMembers();
                        }
                    });
                }
            };
            getContext().registerReceiver(mScreenOffReceiver, mScreenOffFilter);
        } catch (ReceiverCallNotAllowedException e) {
            mScreenOffReceiver = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mScreenOffReceiver != null) {
            try {
                getContext().unregisterReceiver(mScreenOffReceiver);
            } catch (Exception e) {
                Log.d(TAG, "unregister failed:" + e.getMessage());
            }
            mScreenOffReceiver = null;
        }
        super.onDetachedFromWindow();
    }

    private void checkGesture(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mBlockGesture = mDismissAnimator.isAnimating();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        checkGesture(ev);
        if (mBlockGesture) {
            return true;
        }
        if (!mDismissable) {
            return super.onInterceptTouchEvent(ev);
        }
        ev.offsetLocation(ev.getRawX() - ev.getX(), 0.0f);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                resetMembers();
                mDownX = ev.getRawX();
                mDownY = ev.getRawY();
                mActiveTouchId = ev.getPointerId(0);
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetMembers();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mVelocityTracker != null && !mDiscardIntercept) {
                    int activePointerIndex = ev.findPointerIndex(mActiveTouchId);
                    if (activePointerIndex != -1) {
                        float deltaX = ev.getRawX() - mDownX;
                        float x = ev.getX(activePointerIndex);
                        float y = ev.getY(activePointerIndex);
                        if (deltaX != 0.0f && canScroll(this, false, deltaX, x, y)) {
                            mDiscardIntercept = true;
                        } else {
                            updateSwiping(ev);
                        }
                    } else {
                        Log.e(TAG, "Invalid pointer index: ignoring.");
                        mDiscardIntercept = true;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mActiveTouchId = ev.getPointerId(ev.getActionIndex());
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int actionIndex = ev.getActionIndex();
                if (ev.getPointerId(actionIndex) == mActiveTouchId) {
                    mActiveTouchId = ev.getPointerId(actionIndex == 0 ? 1 : 0);
                }
                break;
        }
        if (mDiscardIntercept) {
            return false;
        }
        return mSwiping;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        checkGesture(ev);
        if (mBlockGesture) {
            return true;
        }
        if (mVelocityTracker == null || !mDismissable) {
            return super.onTouchEvent(ev);
        }
        ev.offsetLocation(ev.getRawX() - ev.getX(), 0.0f);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                float velocity = updateDismiss(ev);
                if (mDismissed) {
                    if (velocity == -1.0f) {
                        velocity = 1.0f - ((ev.getRawX() - mDownX) / getWidth());
                    }
                    mDismissAnimator.animateDismissal(ev.getRawX() - mDownX, velocity);
                } else if (mSwiping && mLastX != -Float.MAX_VALUE) {
                    mDismissAnimator.animateRecovery(ev.getRawX() - mDownX);
                }
                resetMembers();
                return true;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(ev);
                mLastX = ev.getRawX();
                updateSwiping(ev);
                if (mSwiping) {
                    setProgress(ev.getRawX() - mDownX);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancel();
                resetMembers();
                return true;
            default:
                return true;
        }
    }

    public void setDismissable(boolean dismissable) {
        if (!dismissable && mDismissable) {
            cancel();
            resetMembers();
        }
        mDismissable = dismissable;
    }

    private void dismiss() {
        if (mDismissedListener != null) {
            mDismissedListener.onDismissed(this);
        }
        Activity activity = findActivity();
        Log.d(TAG, "dismiss: activity=" + activity
                + ", isBackWindowTranslucencyConverted=" + isBackWindowTranslucencyConverted);
        if (activity == null || !isBackWindowTranslucencyConverted) {
            return;
        }
        activity.setBackActivityBackground(true);
        isBackWindowTranslucencyConverted = false;
    }

    private void cancel() {
        Activity activity = findActivity();
        if (!mIsWindowNativelyTranslucent && activity != null && mActivityTranslucencyConverted) {
            activity.convertFromTranslucent();
            mActivityTranslucencyConverted = false;
        }
        Log.d(TAG, "cancel: activity=" + activity
                + ", isBackWindowTranslucencyConverted=" + isBackWindowTranslucencyConverted);
        if (activity != null && isBackWindowTranslucencyConverted) {
            activity.setBackActivityBackground(true);
            isBackWindowTranslucencyConverted = false;
        }
        if (mProgressListener != null) {
            mProgressListener.onSwipeCancelled(this);
        }
    }

    private void resetMembers() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = null;
        mDownX = 0.0f;
        mLastX = -Float.MAX_VALUE;
        mDownY = 0.0f;
        mSwiping = false;
        mDismissed = false;
        mDiscardIntercept = false;
    }

    private void updateSwiping(MotionEvent ev) {
        boolean wasSwiping = mSwiping;
        if (!mSwiping) {
            float deltaX = ev.getRawX() - mDownX;
            float deltaY = ev.getRawY() - mDownY;
            if ((deltaX * deltaX) + (deltaY * deltaY) > mSlop * mSlop) {
                mSwiping = deltaX > (mSlop * 2) && Math.abs(deltaY) < Math.abs(deltaX);
            } else {
                mSwiping = false;
            }
        }
        if (!mSwiping || wasSwiping || mIsWindowNativelyTranslucent) {
            return;
        }
        Activity activity = findActivity();
        if (activity == null) {
            return;
        }
        if (!isBackWindowTranslucencyConverted && activity.setBackActivityBackground(false)) {
            isBackWindowTranslucencyConverted = true;
        }
        mActivityTranslucencyConverted = activity.convertToTranslucent(null, null);
    }

    private float updateDismiss(MotionEvent ev) {
        float velocity = VELOCITY_MIN_ANIMVECTOR;
        float deltaX = ev.getRawX() - mDownX;
        float result = -1.0f;
        mVelocityTracker.computeCurrentVelocity(1000, MAXIMUM_FLING_VELOCITY);
        float xVelocity = mVelocityTracker.getXVelocity();
        if (mLastX == -Float.MAX_VALUE) {
            xVelocity = deltaX / ((ev.getEventTime() - ev.getDownTime()) / 1000);
        }
        if (!mDismissed) {
            getWidth();
            Math.max(Math.min(((-0.08000001f) * xVelocity) / mMinFlingVelocity
                    + MAX_DIST_THRESHOLD, MAX_DIST_THRESHOLD), MIN_DIST_THRESHOLD);
            if ((deltaX > getWidth() * MIN_DIST_THRESHOLD && ev.getRawX() + 2.0f >= mLastX)
                    || (xVelocity >= mMinFlingVelocity && xVelocity < Float.MAX_VALUE)) {
                mDismissed = true;
            }
        }
        if (xVelocity > 0.0f) {
            result = 1.0f - (xVelocity / MAXIMUM_FLING_VELOCITY);
            if (result > VELOCITY_MIN_ANIMVECTOR) {
                velocity = result;
            }
        } else {
            velocity = result;
        }
        if (mDismissed && mSwiping && xVelocity < (-mMinFlingVelocity)) {
            mDismissed = false;
        }
        return velocity;
    }

    private void setProgress(float deltaX) {
        if (mProgressListener == null || deltaX < 0.0f) {
            return;
        }
        mProgressListener.onSwipeProgressChanged(this,
                progressToAlpha(deltaX / getWidth(), setBackWindowAnim(deltaX)), deltaX);
    }

    private boolean setBackWindowAnim(float deltaX) {
        if (!isBackWindowTranslucencyConverted) {
            return false;
        }
        Activity activity = findActivity();
        if (activity == null) {
            return false;
        }
        return activity.setBackActivityAnim(deltaX / getWidth());
    }

    private float progressToAlpha(float progress, boolean backWindow) {
        float alpha = 1.0f - ((2.0f * progress) * ALPHA_DECREASE);
        alpha = alpha > 0.15f ? alpha : 0.15f;
        if (backWindow) {
            return 1.0f;
        }
        return alpha;
    }

    private Activity findActivity() {
        for (Context context = getContext(); context instanceof ContextWrapper;
                context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
        }
        return null;
    }

    private void init(Context context) {
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        TypedArray a = context.getTheme().obtainStyledAttributes(
                com.android.internal.R.styleable.Theme);
        mIsWindowNativelyTranslucent = a.getBoolean(
                com.android.internal.R.styleable.Theme_windowIsTranslucent, false);
        a.recycle();
    }

    protected boolean canScroll(View v, boolean checkV, float dx, float x, float y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScroll(child, true, dx, x + scrollX - child.getLeft(),
                                y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }
        return checkV && v.canScrollHorizontally((int) -dx);
    }

    private class DismissAnimator implements ValueAnimator.AnimatorUpdateListener,
            Animator.AnimatorListener {
        private final PathInterpolator DISMISS_INTERPOLATOR =
                new PathInterpolator(0.4f, 0.1f, 0.66f, 0.96f);
        private final long DISMISS_DURATION_DISMISS = 150;
        private final long DISMISS_DURATION_RECOVERY = 150;
        private final ValueAnimator mDismissAnimator = new ValueAnimator();
        private boolean mWasCanceled = false;
        private boolean mDismissOnComplete = false;

        DismissAnimator() {
            mDismissAnimator.addUpdateListener(this);
            mDismissAnimator.addListener(this);
        }

        private void animate(float from, float to, long duration,
                PathInterpolator interpolator, boolean dismissOnComplete) {
            mDismissAnimator.cancel();
            mDismissOnComplete = dismissOnComplete;
            mDismissAnimator.setFloatValues(from, to);
            mDismissAnimator.setDuration(duration);
            mDismissAnimator.setInterpolator(interpolator);
            mDismissAnimator.start();
        }

        void animateDismissal(float currentTranslation, float velocity) {
            animate(currentTranslation / getWidth(), 1.0f,
                    (long) (DISMISS_DURATION_DISMISS * velocity), DISMISS_INTERPOLATOR, true);
        }

        void animateRecovery(float currentTranslation) {
            animate(currentTranslation / getWidth(), 0.0f,
                    DISMISS_DURATION_RECOVERY, DISMISS_INTERPOLATOR, false);
        }

        boolean isAnimating() {
            return mDismissAnimator.isStarted();
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            mWasCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (mWasCanceled) {
                return;
            }
            if (mDismissOnComplete) {
                dismiss();
            } else {
                cancel();
            }
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
        }

        @Override
        public void onAnimationStart(Animator animator) {
            mWasCanceled = false;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
            setProgress(((Float) animator.getAnimatedValue()).floatValue() * getWidth());
        }
    }
}