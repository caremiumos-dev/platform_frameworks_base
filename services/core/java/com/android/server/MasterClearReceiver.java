/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.server;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.android.internal.R;
import com.android.server.job.controllers.JobStatus;

import java.io.IOException;

public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";

    // Vendor "app data only" packages cleared instead of a full wipe.
    private static final String LAUNCHER_PACKAGE = "com.xtc.i3launcher";
    private static final String SETTINGS_PACKAGE = "com.xtc.setting";

    private boolean mWipeExternalStorage;
    private boolean mWipeEsims;

    private class ClearAppDataTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private final Object mLock = new Object();
        private final IPackageDataObserver mPackageDataObserver = new IPackageDataObserver.Stub() {
            @Override
            public void onRemoveCompleted(String pkgName, boolean succeeded) throws RemoteException {
                Slog.d(TAG, "onRemoveCompleted pkgName = " + pkgName + ", succeeded = " + succeeded);
                synchronized (mLock) {
                    mLock.notify();
                }
            }
        };

        public ClearAppDataTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            PackageManager packageManager = mContext.getPackageManager();
            for (String pkg : new String[] { LAUNCHER_PACKAGE, SETTINGS_PACKAGE }) {
                Slog.d(TAG, "pkg = " + pkg);
                packageManager.clearApplicationUserData(pkg, mPackageDataObserver);
                synchronized (mLock) {
                    try {
                        mLock.wait(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                    } catch (InterruptedException e) {
                        Slog.w(TAG, "clearApplicationUserData timeout");
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Slog.d(TAG, "onPostExecute");
            ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                    .reboot("ClearAppData");
        }
    }

    private class WipeDataTask extends AsyncTask<Void, Void, Void> {
        private final Thread mChainedTask;
        private final Context mContext;
        private final ProgressDialog mProgressDialog;

        public WipeDataTask(Context context, Thread chainedTask) {
            mContext = context;
            mChainedTask = chainedTask;
            mProgressDialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mProgressDialog.setMessage(mContext.getText(R.string.progress_erasing));
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Slog.w(TAG, "Wiping adoptable disks");
            if (mWipeExternalStorage) {
                StorageManager sm = (StorageManager) mContext.getSystemService(
                        Context.STORAGE_SERVICE);
                sm.wipeAdoptableDisks();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mProgressDialog.dismiss();
            mChainedTask.start();
        }
    }

    /**
     * Vendor addition: full-screen "restoring" dialog shown while the factory
     * reset is being prepared. Uses the device framework-res layout/style
     * (restore_dialog / restoreingDialogStyle, recovered from resource ids
     * 0x010900dd / 0x010304c8).
     */
    public static Dialog createLoadingDialog(Context context) {
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(context)
                .inflate(R.layout.restore_dialog, (ViewGroup) null)
                .findViewById(R.id.dialog_view);
        Dialog dialog = new Dialog(context, R.style.restoreingDialogStyle);
        dialog.setCancelable(false);
        dialog.setContentView(linearLayout, new LinearLayout.LayoutParams(-1, -1));
        return dialog;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_REMOTE_INTENT)) {
            if (!"google.com".equals(intent.getStringExtra("from"))) {
                Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
                return;
            }
        }
        if (Intent.ACTION_MASTER_CLEAR.equals(intent.getAction())) {
            Slog.w(TAG, "The request uses the deprecated Intent#ACTION_MASTER_CLEAR, "
                    + "Intent#ACTION_FACTORY_RESET should be used instead.");
        }
        if (intent.hasExtra(Intent.EXTRA_FORCE_MASTER_CLEAR)) {
            Slog.w(TAG, "The request uses the deprecated Intent#EXTRA_FORCE_MASTER_CLEAR, "
                    + "Intent#EXTRA_FORCE_FACTORY_RESET should be used instead.");
        }

        // Vendor: block the screen with the "restoring" dialog for the whole
        // reset. 0x7d9 == TYPE_KEYGUARD_DIALOG, flag 0x1 == ALLOW_LOCK_WHILE_SCREEN_ON.
        Dialog loadingDialog = createLoadingDialog(context);
        loadingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        loadingDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        loadingDialog.show();

        final boolean shutdown = intent.getBooleanExtra("shutdown", false);
        final String reason = intent.getStringExtra(Intent.EXTRA_REASON);
        mWipeExternalStorage = intent.getBooleanExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, false);
        mWipeEsims = intent.getBooleanExtra(Intent.EXTRA_WIPE_ESIMS, false);
        final boolean forceWipe = intent.getBooleanExtra(Intent.EXTRA_FORCE_MASTER_CLEAR, false)
                || intent.getBooleanExtra(Intent.EXTRA_FORCE_FACTORY_RESET, false);
        final boolean clearAppDataOnly = intent.getBooleanExtra("clearAppDataOnly", false);

        Slog.w(TAG, "!!! FACTORY RESET !!!");
        // The reboot call is blocking, so we need to do it on another thread.
        Thread thr = new Thread("Reboot") {
            @Override
            public void run() {
                try {
                    RecoverySystem
                            .rebootWipeUserData(context, shutdown, reason, forceWipe, mWipeEsims);
                    Log.wtf(TAG, "Still running after master clear?!");
                } catch (IOException e) {
                    Slog.e(TAG, "Can't perform master clear/factory reset", e);
                } catch (SecurityException e) {
                    Slog.e(TAG, "Can't perform master clear/factory reset", e);
                }
            }
        };

        if (clearAppDataOnly) {
            Slog.d(TAG, "clearAppDataOnly");
            new ClearAppDataTask(context).execute();
        } else if (mWipeExternalStorage || mWipeEsims) {
            // thr will be started at the end of this task.
            new WipeDataTask(context, thr).execute();
        } else {
            thr.start();
        }
    }
}
