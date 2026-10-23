package android.view;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.UserHandle;
import android.util.Log;

/**
 * XTC two-finger screenshot gesture detector (XTC addition).
 *
 * @hide
 */
public class XtcScreenCutUtils {
    private static final int FINGER_COUNT = 2;
    private static final String TAG = "XtcScreenCutUtils";
    private static final int TAKE_SCREENCUT_TIME_OUT = 1000;
    private static final int TAKE_SCREENCUT_TIME_RANGE = 600;
    private static final String XTC_SCREENCUT_DOUBLECLICK_BROADCAST =
            "com.xtc.screencut.doubleclick.broadcast";

    private View mAttachView;
    private Context mContext;
    private long mDownTime;
    private float mDownY;
    private Intent mScreenCutIntent;
    private long xtc_diff_time;
    private boolean mScreenCutFireable = false;
    private boolean mIntercept = false;
    private boolean mIsTimeReach = false;
    private boolean mIsTimeReachOut = false;
    private boolean mIsFingerOut = false;
    private boolean isShieldScreenCut = false;
    private Handler mHandler = new Handler();

    private Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mScreenCutFireable || !mIntercept) {
                return;
            }
            Log.d(TAG, "ScreenCutUtil  Long press sendBroadCast");
            sendXtcScreenCut_doubleclick_brocast();
            mScreenCutFireable = false;
        }
    };

    private void sendXtcScreenCut_doubleclick_brocast() {
        Log.d(TAG, "sendXtcScreenCut_doubleclick_brocast xtc_diff_time=" + xtc_diff_time);
        if (mScreenCutIntent == null) {
            mScreenCutIntent = new Intent(XTC_SCREENCUT_DOUBLECLICK_BROADCAST);
        }
        mContext.sendBroadcastAsUser(mScreenCutIntent, UserHandle.CURRENT_OR_SELF);
    }

    public boolean dealPausedPointerEvent(MotionEvent motionEvent) {
        if (!ActivityManagerNative.isSystemReady()) {
            Log.d(TAG, "System is not Ready");
            return false;
        }
        if (mAttachView != null && mContext != null) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mScreenCutFireable = true;
                    mIntercept = false;
                    mIsTimeReach = false;
                    mIsTimeReachOut = false;
                    mIsFingerOut = false;
                    mDownY = motionEvent.getY();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIntercept = false;
                    break;
            }
            if (mIsFingerOut) {
                return false;
            }
            int pointerCount = motionEvent.getPointerCount();
            if ((pointerCount >= FINGER_COUNT && mScreenCutFireable) || mIntercept) {
                int action = motionEvent.getAction() & MotionEvent.ACTION_MASK;
                if (!mIntercept) {
                    motionEvent.setAction(MotionEvent.ACTION_CANCEL);
                    mAttachView.dispatchTouchEvent(motionEvent);
                }
                mIntercept = true;
                switch (action) {
                    case MotionEvent.ACTION_MOVE:
                        if (pointerCount > FINGER_COUNT) {
                            mIsFingerOut = true;
                        } else if (pointerCount == FINGER_COUNT) {
                            xtc_diff_time = System.currentTimeMillis() - mDownTime;
                            if (xtc_diff_time >= TAKE_SCREENCUT_TIME_RANGE) {
                                mIsTimeReach = true;
                            }
                            if (xtc_diff_time >= TAKE_SCREENCUT_TIME_OUT) {
                                mIsTimeReachOut = true;
                                if (mScreenCutFireable) {
                                    Log.d(TAG, "ScreenCutUtil  pointMove sendBroadCast");
                                    sendXtcScreenCut_doubleclick_brocast();
                                    mScreenCutFireable = false;
                                }
                            }
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_POINTER_UP:
                        Log.d(TAG, "ScreenCutUtil  pointUp mScreenCutFireable="
                                + mScreenCutFireable);
                        mScreenCutFireable = false;
                        mHandler.removeCallbacks(mLongPressRunnable);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mDownTime = System.currentTimeMillis();
                        mHandler.postDelayed(mLongPressRunnable, TAKE_SCREENCUT_TIME_OUT);
                        break;
                }
                return mIntercept;
            }
        }
        return mIntercept;
    }

    public String getRunningApp() {
        String name = ((ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE))
                .getRunningAppProcesses().get(0).processName.toString();
        Log.e(TAG, "APP_NAME=" + name);
        return name;
    }

    public boolean isIntercept() {
        return mIntercept;
    }

    public boolean isShieldScreenCut() {
        return isShieldScreenCut;
    }

    public void setAttachView(View view) {
        mAttachView = view;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setShieldScreenCut() {
        isShieldScreenCut = true;
    }
}