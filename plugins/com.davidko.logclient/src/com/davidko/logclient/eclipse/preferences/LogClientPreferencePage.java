package com.davidko.logclient.eclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.davidko.logclient.PortFieldEditor;
import com.davidko.logclient.eclipse.LogClientPlugin;

public class LogClientPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public LogClientPreferencePage() {
		super(GRID);
		setPreferenceStore(LogClientPlugin.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		StringFieldEditor sfe = new StringFieldEditor(
				PreferenceInitializer.ATTR_LOG_HOST, "LogServer Host:",
				getFieldEditorParent());
		addField(sfe);

		IntegerFieldEditor ife = new PortFieldEditor(
				PreferenceInitializer.ATTR_LOG_PORT, "LogServer Port:",
				getFieldEditorParent());
		addField(ife);
	}

	public void init(IWorkbench workbench) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
	}
}
