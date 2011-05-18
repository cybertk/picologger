package com.github.picologger.eclipse;

import java.util.Calendar;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class SyslogPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.davidko.logclient.eclipse";

    private static SyslogPlugin sPlugin;

    private MessageConsole mConsole;

    private Color mRed;

    public SyslogPlugin() {

        sPlugin = this;
        // what should do?
    }

    @Override
    public void start(BundleContext context) throws Exception {

        super.start(context);

        final Display display = getDisplay();

        mConsole = new MessageConsole("SyslogViewer", null);
        ConsolePlugin.getDefault().getConsoleManager()
                .addConsoles(new IConsole[] { mConsole });

        final MessageConsoleStream consoleStream = mConsole
                .newMessageStream();
        final MessageConsoleStream errorConsoleStream = mConsole
                .newMessageStream();

        display.asyncExec(new Runnable() {

            public void run() {

                errorConsoleStream
                        .setColor(new Color(display, 0xFF, 0x00, 0x00));
            }
        });

//        Log.setLogOutput(new ILogOutput() {
//
//            public void printLog(LogLevel logLevel, String tag, String message) {
//
//                if (logLevel.getPriority() >= LogLevel.ERROR.getPriority()) {
//                    printToStream(errorConsoleStream, tag, message);
//                    ConsolePlugin.getDefault().getConsoleManager()
//                            .showConsoleView(mConsole);
//                } else {
//                    printToStream(consoleStream, tag, message);
//                }
//            }
//
//            public void printAndPromptLog(final LogLevel logLevel,
//                    final String tag, final String message) {
//
//                printLog(logLevel, tag, message);
//
//                display.asyncExec(new Runnable() {
//
//                    public void run() {
//
//                        Shell shell = display.getActiveShell();
//                        if (logLevel == LogLevel.ERROR) {
//                            MessageDialog.openError(shell, tag, message);
//                        } else {
//                            MessageDialog.openWarning(shell, tag, message);
//                        }
//                    }
//                });
//            }
//
//        });

        // Init preference store.
        final IPreferenceStore eclipseStore = getPreferenceStore();
        // SyslogViewerPreferencePage.setStore(eclipseStore);
        eclipseStore.addPropertyChangeListener(new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                String property = event.getProperty();
                //
                // if (PreferenceInitializer.ATTR_LOG_HOST.equals(property)) {
                // LogClientPreferences.setDefaultLogHost(eclipseStore
                // .getString(PreferenceInitializer.ATTR_LOG_HOST));
                // } else if
                // (PreferenceInitializer.ATTR_LOG_PORT.equals(property)) {
                // LogClientPreferences.setDefaultLogPort(eclipseStore
                // .getInt(PreferenceInitializer.ATTR_LOG_PORT));
                // }
            }
        });

        // PreferenceInitializer.setupPreferences();

    }

    public static Display getDisplay() {

        IWorkbench bench = sPlugin.getWorkbench();
        if (bench != null) {
            return bench.getDisplay();
        }
        return null;
    }

    @Override
    public void stop(BundleContext context) throws Exception {

        sPlugin = null;
        super.stop(context);
    }

    public static SyslogPlugin getDefault() {

        return sPlugin;
    }

    private static synchronized void printToStream(MessageConsoleStream stream,
            String tag, String message) {

        String dateTag = getMessageTag(tag);

        stream.print(dateTag);
        stream.println(message);
    }

    private static String getMessageTag(String tag) {

        Calendar c = Calendar.getInstance();

        if (tag == null) {
            return String.format("[%1$tF %1$tT]", c);
        }

        return String.format("[%1$tF %1$tT - %2$s]", c, tag);
    }
}
