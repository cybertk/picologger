package com.davidko.logclient.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.davidko.logclient.LogClientPreferences;
import com.davidko.logclient.eclipse.LogClientPlugin;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    public final static String ATTR_LOG_HOST =
        LogClientPlugin.PLUGIN_ID + ".logHost";

    public final static String ATTR_LOG_PORT =
    	LogClientPlugin.PLUGIN_ID + ".logPort";
    
    public final static String ATTR_LOG_V =
    	LogClientPlugin.PLUGIN_ID + ".logV";
    public final static String ATTR_LOG_D =
    	LogClientPlugin.PLUGIN_ID + ".logD";
    public final static String ATTR_LOG_I =
    	LogClientPlugin.PLUGIN_ID + ".logI";
    public final static String ATTR_LOG_W =
    	LogClientPlugin.PLUGIN_ID + ".logW";
    public final static String ATTR_LOG_E =
    	LogClientPlugin.PLUGIN_ID + ".logE";

    public void initializeDefaultPreferences() {
        IPreferenceStore store = LogClientPlugin.getDefault().getPreferenceStore();
        store.setDefault(ATTR_LOG_HOST, LogClientPreferences.DEFAULT_LOG_HOST);
        store.setDefault(ATTR_LOG_PORT, LogClientPreferences.DEFAULT_LOG_PORT);
    }

    public synchronized static void setupPreferences() {
        IPreferenceStore store = LogClientPlugin.getDefault().getPreferenceStore();
        LogClientPreferences.setDefaultLogHost(store.getString(ATTR_LOG_HOST));
        LogClientPreferences.setDefaultLogPort(store.getInt(ATTR_LOG_PORT));
    }
}
