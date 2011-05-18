package com.github.picologger.eclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.github.picologger.eclipse.SyslogPlugin;

/**
 * Preference page.
 * 
 * @author kyan
 */
public class SyslogViewerPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    /**
     * Remote syslog server ip address.
     */
    private static final String PREF_SERVER_ADDR = SyslogPlugin.PLUGIN_ID
            + ".server.address";

    /**
     * Remote syslog server port number.
     */
    private static final String PREF_SERVER_PORT = SyslogPlugin.PLUGIN_ID
            + ".server.port";

    public void init(IWorkbench workbench) {

        // TODO Auto-generated method stub

    }

    @Override
    protected void createFieldEditors() {

        StringFieldEditor sfe = new StringFieldEditor(PREF_SERVER_ADDR,
                "LogServer Host:", getFieldEditorParent());
        addField(sfe);

        IntegerFieldEditor ife = new IntegerFieldEditor(PREF_SERVER_PORT,
                "LogServer Port:", getFieldEditorParent());
        addField(ife);
    }
}
