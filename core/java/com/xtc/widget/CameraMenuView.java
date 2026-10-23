package com.xtc.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Point;
import android.media.SoundPool;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.xtc.moduleswitch.FunSwitchUtil;
import com.xtc.utils.HallUtil;

import java.util.List;

/**
 * XTC camera menu that slides in from the right edge. See {@code MenuHolder} for
 * the vendor menu definition.
 *
 * @hide
 */
public class CameraMenuView extends FrameLayout
        implements MenuClickListener, EdgePainter.GestureListener {
    private static final String TAG = CameraMenuView.class.getSimpleName();

    public static final String XTC_ADD_MENU = "xtc_show_menu";
    public static final String XTC_FROM_PHOTO = "xtc_from_photo";
    public static final String XTC_SHOW_TIP = "xtc_show_gesture_tip";

    private static final int MSG_INIT = 1;
    private static final int MSG_FUN_SWITCH_CHANGED = 2;
    private static final int MSG_ORDER_CHANGED = 3;
    private static final int MSG_PERIOD_CHANGED = 4;
    private static final int MSG_ATTACHED_TO_WINDOW = 5;
    private static final int MSG_DETACHED_TO_WINDOW = 6;

    private static final long INIT_DELAY = HallUtil.IS_SDW2700 ? 500 : 1000;
    private static final long TOUCH_DELAY = 200L;
    private static final float MIN_FLING_VELOCITY = 3;

    private boolean mAlwaysInTapRegion;
    private ValueAnimator mAnimator;
    private final Animator.AnimatorListener mAnimatorListener;
    private final String mClassName;
    private List<MenuItem> mDataList;
    private float mDefaultTranslate;
    private Point mDisplayPoint;
    private long mDownTime;
    private float mDownX;
    private float mDownY;
    private boolean mEdgeActive;
    private EdgePainter mEdgePainter;
    private final ContentObserver mFunctionObserver;
    private boolean mHandleTouch;
    private HandlerThread mHandlerThread;
    private boolean mInterceptEvent;
    private long mLastShowTime;
    private float mLastX;
    private final View.OnTouchListener mListTouchListener;
    private boolean mNeedScrollSound;
    private final ContentObserver mPeriodObserver;
    private SortRecyclerView mRecyclerView;
    private SoundPool mScrollPool;
    private boolean mScrollSoundEnabled;
    private int mScrollSoundId;
    private boolean mShowTip;
    private int mTouchSlopSquare;
    private final ValueAnimator.AnimatorUpdateListener mUpdateListener;
    private WorkHandler mWorkHandler;

    private class WorkHandler extends Handler {
        WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_INIT:
                    initSwitchValue();
                    break;
                case MSG_FUN_SWITCH_CHANGED:
                    updateFunctionSwitch();
                    break;
                case MSG_ORDER_CHANGED:
                    MenuHolder.instance().saveMenuOrder();
                    break;
                case MSG_PERIOD_CHANGED:
                    updateFunctionSwitch();
                    break;
                case MSG_ATTACHED_TO_WINDOW:
                    callWhenAttached();
                    break;
                case MSG_DETACHED_TO_WINDOW:
                    callWhenDetached();
                    break;
                default:
                    Log.w(TAG, "invalid msg what:" + message.what);
                    break;
            }
        }
    }

    public CameraMenuView(Context context) {
        this(context, null);
    }

    public CameraMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mNeedScrollSound = true;
        mScrollSoundEnabled = true;
        mHandleTouch = false;
        mEdgeActive = true;
        mFunctionObserver = new ContentObserver(null) {
            @Override
            public boolean deliverSelfNotifications() {
                return false;
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (mWorkHandler != null) {
                    mWorkHandler.sendEmptyMessage(MSG_FUN_SWITCH_CHANGED);
                }
            }
        };
        mPeriodObserver = new ContentObserver(null) {
            @Override
            public boolean deliverSelfNotifications() {
                return false;
            }

            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                if (mWorkHandler != null) {
                    mWorkHandler.sendEmptyMessage(MSG_PERIOD_CHANGED);
                }
            }
        };
        mListTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (!mScrollSoundEnabled || mScrollPool == null) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE && mNeedScrollSound) {
                    mNeedScrollSound = false;
                    mScrollPool.play(mScrollSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mNeedScrollSound = true;
                }
                return false;
            }
        };
        mAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (getTranslationX() == mDisplayPoint.x) {
                    setTranslationX(mDefaultTranslate);
                    setVisibility(View.INVISIBLE);
                    notifyActivityOnKeyDown(1001);
                    mHandleTouch = false;
                } else if (getTranslationX() == 0.0f) {
                    setVisibility(View.VISIBLE);
                    notifyActivityOnKeyDown(1000);
                    mHandleTouch = true;
                }
                getEdgePainter().resetAfterAnimation();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
        };
        mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setTranslationX((Float) animation.getAnimatedValue());
            }
        };
        long start = SystemClock.uptimeMillis();
        Intent intent = getIntent(context);
        mClassName = intent.getComponent().getClassName();
        mShowTip = intent.getBooleanExtra(XTC_SHOW_TIP, false);
        initInternalData();
        initViewParameters();
        setVisibility(View.INVISIBLE);
        Log.d(TAG, "camera menu view init cost:" + (SystemClock.uptimeMillis() - start) + "ms");
    }

    private void addRecyclerView() {
        if (mRecyclerView != null) {
            return;
        }
        long start = SystemClock.uptimeMillis();
        mRecyclerView = new SortRecyclerView(getContext());
        mRecyclerView.setAdapter(mDataList);
        mRecyclerView.getAdapter().setClickListener(this);
        mRecyclerView.setOnTouchListener(mListTouchListener);
        addView(mRecyclerView);
        Log.d(TAG, "add recycler view cost:" + (SystemClock.uptimeMillis() - start) + "ms");
    }

    private void callWhenAttached() {
        if (mScrollPool == null) {
            mScrollPool = new SoundPool(1, 5, 0);
            mScrollSoundId = mScrollPool.load(getContext(),
                    com.android.internal.R.raw.scroll_sound, 1);
        }
        getContext().getContentResolver().registerContentObserver(
                FunSwitchUtil.FUN_URI, true, mFunctionObserver);
        getContext().getContentResolver().registerContentObserver(
                FunSwitchUtil.EXTRA_URI, true, mPeriodObserver);
    }

    private void callWhenDetached() {
        if (mScrollPool != null) {
            mScrollPool.release();
            mScrollPool = null;
        }
        getContext().getContentResolver().unregisterContentObserver(mFunctionObserver);
        getContext().getContentResolver().unregisterContentObserver(mPeriodObserver);
    }

    private int getActiveIndex(String className) {
        int index = -1;
        for (int i = 0; i < mDataList.size(); i++) {
            MenuItem item = mDataList.get(i);
            if (item.clsName.equals(className)) {
                item.selected = true;
                index = i;
            } else {
                item.selected = false;
            }
        }
        return index;
    }

    private Intent getIntent(Context context) {
        return ((Activity) getContext()).getIntent();
    }

    private void handleFling(float velocityX, float velocityY) {
        float translationX = getTranslationX();
        if (velocityX > 0.0f) {
            startAnimation(translationX, mDisplayPoint.x, 200L);
        } else {
            startAnimation(translationX, 0.0f, 200L);
        }
    }

    private void handleUp(MotionEvent event) {
        float translationX = getTranslationX();
        if (translationX < mDisplayPoint.x / 2) {
            startAnimation(translationX, 0.0f, 200L);
        } else {
            startAnimation(translationX, mDisplayPoint.x, 200L);
        }
    }

    private boolean ignoreEvent() {
        return !(mEdgeActive && getTranslationX() == ((float) mDisplayPoint.x)
                && mRecyclerView != null);
    }

    private void initInternalData() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplayPoint = new Point();
        wm.getDefaultDisplay().getSize(mDisplayPoint);
        mAnimator = new ValueAnimator();
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addListener(mAnimatorListener);
        mAnimator.addUpdateListener(mUpdateListener);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlopSquare = configuration.getScaledTouchSlop() * configuration.getScaledTouchSlop();
    }

    private void initSwitchValue() {
        Context context = getContext();
        MenuHolder.instance().init(context, true);
        mDataList = MenuHolder.instance().queryListByModuleSwitch(context);
        MenuHolder.instance().updateFunSwitchValue(context);
        post(new Runnable() {
            @Override
            public void run() {
                addRecyclerView();
            }
        });
    }

    private void initViewParameters() {
        mDefaultTranslate = mDisplayPoint.x;
        setTranslationX(mDefaultTranslate);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        lp.gravity = android.view.Gravity.CENTER;
        setLayoutParams(lp);
    }

    private boolean itemDisabled(MenuItem item) {
        if (!item.funEnable) {
            showDisableToast(com.android.internal.R.string.menu_function_disable);
            return true;
        }
        if (item.periodDisabled()) {
            showDisableToast(com.android.internal.R.string.menu_period_disable);
            return true;
        }
        return false;
    }

    private void notifyActivityOnKeyDown(int keyCode) {
        if (getContext() instanceof Activity) {
            ((Activity) getContext()).onKeyDown(keyCode, new android.view.KeyEvent(0, keyCode));
        } else {
            Log.w(TAG, "context not instance of Activity, notify onKeyDown failed.");
        }
    }

    private void showDisableToast(int resId) {
        if (System.currentTimeMillis() - mLastShowTime < 4000) {
            return;
        }
        mLastShowTime = System.currentTimeMillis();
        Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show();
    }

    private void startAnimation(float from, float to, long duration) {
        mAnimator.cancel();
        mAnimator.setDuration(duration);
        mAnimator.setFloatValues(from, to);
        mAnimator.start();
    }

    private void startHandlerThread() {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("MenuWorkThread");
            mHandlerThread.start();
            mWorkHandler = new WorkHandler(mHandlerThread.getLooper());
            mWorkHandler.sendEmptyMessageDelayed(MSG_INIT, INIT_DELAY);
        }
    }

    private void stopHandlerThread() {
        if (mHandlerThread != null) {
            mWorkHandler.removeMessages(MSG_FUN_SWITCH_CHANGED);
            mWorkHandler.removeMessages(MSG_PERIOD_CHANGED);
            mHandlerThread.quitSafely();
            mHandlerThread = null;
            mWorkHandler = null;
            Log.d(TAG, "release handler thread");
        }
    }

    private void updateFunctionSwitch() {
        MenuHolder.instance().updateFunSwitchValue(getContext());
        post(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView != null) {
                    mRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        });
    }

    public EdgePainter getEdgePainter() {
        return mEdgePainter;
    }

    public void hideCameraMenu(boolean animate) {
        if (animate) {
            if (mAnimator.isRunning() || getTranslationX() == mDefaultTranslate) {
                return;
            }
            startAnimation(getTranslationX(), mDisplayPoint.x, 500L);
            return;
        }
        setVisibility(View.INVISIBLE);
        setTranslationX(mDefaultTranslate);
        notifyActivityOnKeyDown(1001);
        mHandleTouch = false;
    }

    public boolean interceptEdgeEvent(MotionEvent event) {
        if (ignoreEvent() || mEdgePainter == null) {
            return false;
        }
        return mEdgePainter.interceptTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startHandlerThread();
        mWorkHandler.sendEmptyMessage(MSG_ATTACHED_TO_WINDOW);
    }

    @Override
    public void onBack() {
        showCameraMenu();
    }

    @Override
    public void onClick(int position, Intent intent) {
        MenuItem item = mDataList.get(position);
        if (itemDisabled(item)) {
            return;
        }
        intent.putExtra(XTC_ADD_MENU, item.showMenu
                ? getIntent(getContext()).getBooleanExtra(XTC_ADD_MENU, false) : false);
        intent.putExtra(XTC_FROM_PHOTO, item.showMenu
                ? getIntent(getContext()).getBooleanExtra(XTC_FROM_PHOTO, false) : false);
        Activity activity = (Activity) getContext();
        if (item.clsName.equals(mClassName)) {
            hideCameraMenu(true);
            return;
        }
        if (activity.getPackageManager().resolveActivity(intent, 65536) == null) {
            Log.e(TAG, "no activity found:" + intent.getComponent().getClassName());
            return;
        }
        try {
            notifyActivityOnKeyDown(1002);
            hideCameraMenu(false);
            activity.startActivity(intent);
            if (item.showMenu) {
                activity.finish();
            }
        } catch (Exception e) {
            Log.e(TAG, " start activity failed", e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWorkHandler.sendEmptyMessage(MSG_DETACHED_TO_WINDOW);
        stopHandlerThread();
        if (mEdgePainter != null) {
            mEdgePainter.onDetachedFromWindow();
        }
    }

    public void onEdgeEvent(MotionEvent event) {
        if (ignoreEvent() || mEdgePainter == null) {
            return;
        }
        mEdgePainter.onTouchEvent(event);
    }

    public void onHideOpenAnimation() {
        if (mEdgePainter == null || !mShowTip) {
            return;
        }
        mShowTip = false;
        post(new Runnable() {
            @Override
            public void run() {
                mEdgePainter.showTipAnimation();
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getRawX();
                mDownY = event.getRawY();
                mLastX = mDownX;
                mAlwaysInTapRegion = true;
                mDownTime = System.currentTimeMillis();
                mInterceptEvent = false;
                break;
            case MotionEvent.ACTION_MOVE:
                long elapsed = System.currentTimeMillis() - mDownTime;
                if (!mInterceptEvent && elapsed <= TOUCH_DELAY) {
                    float dx = event.getRawX() - mDownX;
                    float dy = event.getRawY() - mDownY;
                    float distance = (dx * dx) + (dy * dy);
                    if (Math.abs(dx) > Math.abs(dy) && distance > mTouchSlopSquare) {
                        mInterceptEvent = true;
                    }
                }
                break;
            default:
                break;
        }
        return mInterceptEvent;
    }

    @Override
    public void onOrderChanged() {
        if (mWorkHandler != null) {
            mWorkHandler.sendEmptyMessage(MSG_ORDER_CHANGED);
        } else {
            MenuHolder.instance().saveMenuOrder();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mHandleTouch || mAnimator.isRunning()) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                long elapsed = System.currentTimeMillis() - mDownTime;
                float velocity = ((event.getRawX() - mDownX) / (elapsed * elapsed)) * 1000.0f;
                if (!mAlwaysInTapRegion) {
                    if (Math.abs(velocity) > MIN_FLING_VELOCITY) {
                        handleFling(velocity, 0.0f);
                    } else {
                        handleUp(event);
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) (event.getRawX() - mDownX);
                int dy = (int) (event.getRawY() - mDownY);
                if ((dx * dx) + (dy * dy) > mTouchSlopSquare) {
                    mAlwaysInTapRegion = false;
                    float delta = event.getRawX() - mLastX;
                    mLastX = event.getRawX();
                    float translationX = delta + getTranslationX();
                    setTranslationX(translationX >= 0.0f ? translationX : 0.0f);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                hideCameraMenu(false);
                return true;
            case MotionEvent.ACTION_DOWN:
            default:
                return true;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            mScrollSoundEnabled = android.os.SystemProperties.getBoolean(
                    "persist.sys.switch.scrolltone", true);
        }
    }

    public void setEdgePainter(EdgePainter edgePainter) {
        mEdgePainter = edgePainter;
        mEdgePainter.setGestureListener(this);
    }

    public void setMenuVisibility(final int visibility) {
        post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "set menu visibility:" + visibility);
                mEdgeActive = visibility == View.VISIBLE;
                setVisibility(visibility);
            }
        });
    }

    public void showCameraMenu() {
        if (mAnimator.isRunning() || getTranslationX() != mDefaultTranslate) {
            return;
        }
        int activeIndex = getActiveIndex(mClassName);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        if (activeIndex != -1) {
            mRecyclerView.scrollToCenter(activeIndex);
        }
        setVisibility(View.VISIBLE);
        startAnimation(getTranslationX(), 0.0f, 250L);
    }
}
