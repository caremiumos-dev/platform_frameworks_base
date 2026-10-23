package android.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth DUN profile proxy (XTC/QTI addition).
 *
 * @hide
 */
public final class BluetoothDun implements BluetoothProfile {
    public static final String ACTION_CONNECTION_STATE_CHANGED =
            "codeaurora.bluetooth.dun.profile.action.CONNECTION_STATE_CHANGED";

    private static final boolean DBG = false;
    private static final boolean VDBG = false;
    private static final String TAG = "BluetoothDun";

    private Context mContext;
    private IBluetoothDun mDunService;
    private BluetoothProfile.ServiceListener mServiceListener;

    private IBluetoothStateChangeCallback mStateChangeCallback =
            new IBluetoothStateChangeCallback.Stub() {
        @Override
        public void onBluetoothStateChange(boolean on) {
            Log.d(TAG, "onBluetoothStateChange on: " + on);
            if (!on) {
                synchronized (mConnection) {
                    if (mDunService != null) {
                        try {
                            mDunService = null;
                            mContext.unbindService(mConnection);
                        } catch (Exception e) {
                            Log.e(TAG, "", e);
                        }
                    }
                }
                return;
            }
            try {
                if (mDunService == null) {
                    Log.d(TAG, "onBluetoothStateChange call bindService");
                    doBind();
                }
            } catch (IllegalStateException e2) {
                Log.e(TAG, "onBluetoothStateChange: could not bind to DUN service: ", e2);
            } catch (SecurityException e3) {
                Log.e(TAG, "onBluetoothStateChange: could not bind to DUN service: ", e3);
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDunService = IBluetoothDun.Stub.asInterface(service);
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.DUN, BluetoothDun.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mDunService = null;
            if (mServiceListener != null) {
                mServiceListener.onServiceDisconnected(BluetoothProfile.DUN);
            }
        }
    };

    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    BluetoothDun(Context context, BluetoothProfile.ServiceListener listener) {
        mContext = context;
        mServiceListener = listener;
        try {
            mAdapter.getBluetoothManager().registerStateChangeCallback(mStateChangeCallback);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to register BluetoothStateChangeCallback", e);
        }
        Log.d(TAG, "BluetoothDun() call bindService");
        doBind();
    }

    private boolean isEnabled() {
        return mAdapter.getState() == BluetoothAdapter.STATE_ON;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(device.getAddress());
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    void close() {
        mServiceListener = null;
        IBluetoothManager manager = mAdapter.getBluetoothManager();
        if (manager != null) {
            try {
                manager.unregisterStateChangeCallback(mStateChangeCallback);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to unregister BluetoothStateChangeCallback", e);
            }
        }
        synchronized (mConnection) {
            if (mDunService != null) {
                try {
                    mDunService = null;
                    mContext.unbindService(mConnection);
                } catch (Exception e2) {
                    Log.e(TAG, "", e2);
                }
            }
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        if (mDunService != null && isEnabled() && isValidDevice(device)) {
            try {
                return mDunService.disconnect(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return false;
            }
        }
        if (mDunService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return false;
    }

    boolean doBind() {
        Intent intent = new Intent(IBluetoothDun.class.getName());
        ComponentName comp = intent.resolveSystemService(mContext.getPackageManager(), 0);
        intent.setComponent(comp);
        if (comp != null && mContext.bindServiceAsUser(intent, mConnection, 0,
                Process.myUserHandle())) {
            return true;
        }
        Log.e(TAG, "Could not bind to Bluetooth Dun Service with " + intent);
        return false;
    }

    @Override
    protected void finalize() {
        close();
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        if (mDunService == null || !isEnabled()) {
            if (mDunService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList<BluetoothDevice>();
        }
        try {
            return mDunService.getConnectedDevices();
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList<BluetoothDevice>();
        }
    }

    @Override
    public int getConnectionState(BluetoothDevice device) {
        if (mDunService != null && isEnabled() && isValidDevice(device)) {
            try {
                return mDunService.getConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
        if (mDunService == null) {
            Log.w(TAG, "Proxy not attached to service");
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (mDunService == null || !isEnabled()) {
            if (mDunService == null) {
                Log.w(TAG, "Proxy not attached to service");
            }
            return new ArrayList<BluetoothDevice>();
        }
        try {
            return mDunService.getDevicesMatchingConnectionStates(states);
        } catch (RemoteException e) {
            Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
            return new ArrayList<BluetoothDevice>();
        }
    }
}