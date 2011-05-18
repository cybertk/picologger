package com.github.picologger.eclipse.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.github.picologger.eclipse.syslog.Syslog.LogLevel;

public class SyslogViewerPreference {

    public final static String DEFAULT_LOG_HOST = "127.0.0.1";// 0.nj.kyan.im

    public final static int DEFAULT_LOG_PORT = 20504;

    public final static LogLevel DEFAULT_LOG_LEVEL = LogLevel.ERROR;

    public final static String DEFAULT_LOG_COMMAND = "FLTR";

    public static final int DEFAULT_TIMEOUT = 5000; // ms

    private static String sLogServerHost = DEFAULT_LOG_HOST;

    private static int sLogServerPort = DEFAULT_LOG_PORT;

    private static LogLevel sLogLevel = DEFAULT_LOG_LEVEL;

    private static int sTimeOut = DEFAULT_TIMEOUT;

    public static int getsTimeOut() {

        return sTimeOut;
    }

    public static void setsTimeOut(int timeOut) {

        sTimeOut = timeOut;
    }

    private static IPreferenceStore mStore;

    public static void setStore(IPreferenceStore store) {

        mStore = store;
    }

    public static IPreferenceStore getStore() {

        return mStore;
    }

    private SyslogViewerPreference() {

        // TODO Auto-generated constructor stub
    }

    public static void setDefaultLogHost(String host) {

        sLogServerHost = host;
    }

    public static void setDefaultLogPort(int port) {

        sLogServerPort = port;
    }

    public static String getsLogServerHost() {

        return sLogServerHost;
    }

    public static int getsLogServerPort() {

        return sLogServerPort;
    }

    public static LogLevel getLogLevel() {

        return sLogLevel;
    }
}
