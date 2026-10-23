package com.xtc.snap;

import android.app.job.JobInfo;
import android.util.Log;

import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XTC system exception snapshot / reporting helper.
 *
 * <p><b>Condemnation.</b> This class silently collects full thread stack traces
 * (including other applications' frames) on ANR/WTF events and hands them to a
 * vendor native library for out-of-band reporting. Capturing and transmitting
 * unrelated processes' stacks without their consent is an unacceptable privacy
 * and trust model; it is reproduced here only for source fidelity.
 */
public class SnapException {
    private static final boolean DEBUG = false;
    private static final String TAG = "Snap_Exp_SVC";

    private long mLastSysSnapTime = 1000;
    private final long SYS_SNAP_TIME_THRESHOLD = JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS;
    private final String SEND_NON_PROTECTED_BROADCAST = "Sending non-protected broadcast";
    private final String FILE_OBSERVER_NULL_PATH =
            "Unhandled exception in FileObserver com.android.server.BootReceiver";
    private final String TERRIBLE_FAILURE_INFO = "android.util.Log$TerribleFailure";
    private final String LOW_MEMORY_INFO = "Low on memory";

    static {
        Log.i(TAG, "load Exception Log jni");
        System.loadLibrary("xtc_syssnap_expmgr");
    }

    private static String getAllThreadStackTraces() {
        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        StringWriter stringWriter = new StringWriter();
        try {
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                StackTraceElement[] stackTraceElementArr = entry.getValue();
                Thread thread = entry.getKey();
                stringWriter.write("\"" + thread.getName() + "\""
                        + (thread.isDaemon() ? "daemon" : "") + " prio=" + thread.getPriority()
                        + " Thread id=" + thread.getId() + " " + thread.getState() + "\n");
                for (StackTraceElement stackTraceElement : stackTraceElementArr) {
                    stringWriter.write("\t" + stackTraceElement + "\n");
                }
                stringWriter.write("\n");
            }
            return stringWriter.toString();
        } catch (OutOfMemoryError e2) {
            return "java.lang.OutOfMemoryError";
        }
    }

    private static native boolean getNativeExceptionPidListImpl(int[] pids);

    private static String getThreadStackTrace() {
        StringWriter stringWriter = new StringWriter();
        try {
            Thread threadCurrentThread = Thread.currentThread();
            StackTraceElement[] stackTrace = threadCurrentThread.getStackTrace();
            stringWriter.write("\"" + threadCurrentThread.getName() + "\""
                    + (threadCurrentThread.isDaemon() ? "daemon" : "")
                    + " prio=" + threadCurrentThread.getPriority()
                    + " Thread id=" + threadCurrentThread.getId()
                    + " " + threadCurrentThread.getState() + "\n");
            for (StackTraceElement stackTraceElement : stackTrace) {
                stringWriter.write("\t" + stackTraceElement + "\n");
            }
            stringWriter.write("\n");
            return stringWriter.toString();
        } catch (OutOfMemoryError e2) {
            return "java.lang.OutOfMemoryError";
        }
    }

    private boolean isSkipLowMemoryInfo(String info) {
        return info.contains(LOW_MEMORY_INFO);
    }

    private boolean isSkipReportFromNullFilePath(String info) {
        return info.contains(FILE_OBSERVER_NULL_PATH);
    }

    private boolean isSkipReportFromProtectedBroadcast(String info) {
        return info.contains(SEND_NON_PROTECTED_BROADCAST);
    }

    private boolean isSkipWTF(String info) {
        return info.contains(TERRIBLE_FAILURE_INFO);
    }

    private boolean needSkipExceptionType(String type) {
        return type.equals("system_server_wtf") || type.equals("system_server_lowmem");
    }

    private boolean needSkipSystemWtfReport(String info) {
        return isSkipReportFromProtectedBroadcast(info) || isSkipReportFromNullFilePath(info)
                || isSkipWTF(info) || isSkipLowMemoryInfo(info);
    }

    private static native void report(String group, String packages, String info, String backtrace,
            String type, long timestamp);

    private static native void systemreportImpl(byte type, String tag, String threadStack,
            String message, String extra);

    public boolean getNativeExceptionPidList(int[] pids) {
        return getNativeExceptionPidListImpl(pids);
    }

    public void handle(String type, String info, String timestamp) {
        long elapsed = System.currentTimeMillis() - mLastSysSnapTime;
        if (elapsed < JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS) {
            Log.i(TAG, "xtc syssnap preventing frequent triggers (ignore) , now time =  "
                    + elapsed + " ms. threshhold time = "
                    + JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS + " ms.");
            return;
        }
        Log.i(TAG, "xtc syssnap capture ANR, now time =  " + elapsed + " ms.");
        mLastSysSnapTime = System.currentTimeMillis();
        String group = "";
        String packages = "";
        String[] lines = info.split("\n+");
        Pattern processPattern = Pattern.compile("^Process:\\s+(.*)");
        Pattern packagePattern = Pattern.compile("^Package:\\s+(.*)");
        for (String line : lines) {
            Matcher processMatcher = processPattern.matcher(line);
            if (processMatcher.matches()) {
                group = processMatcher.group(1);
            }
            Matcher packageMatcher = packagePattern.matcher(line);
            if (packageMatcher.matches()) {
                packages = packages + packageMatcher.group(1) + "\n";
            }
        }
        long time = timestamp.equals("") ? 0L : Long.parseLong(timestamp);
        if (needSkipExceptionType(type) && needSkipSystemWtfReport(info)) {
            return;
        }
        report(group, packages, info, "Backtrace of all threads:\n\n", type, time);
    }

    public void systemreport(byte type, String tag, String message, String extra) {
        systemreportImpl(type, tag, getThreadStackTrace(), message, extra);
    }
}