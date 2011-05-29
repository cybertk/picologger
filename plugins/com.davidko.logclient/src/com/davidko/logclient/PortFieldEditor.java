package com.davidko.logclient;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class PortFieldEditor extends IntegerFieldEditor {

    public boolean mRecursiveCheck = false;

    public PortFieldEditor(String name, String label, Composite parent) {
        super(name, label, parent);
        setValidateStrategy(VALIDATE_ON_KEY_STROKE);
    }

    public int getCurrentValue() {
        int val;
        try {
            val = Integer.parseInt(getStringValue());
        }
        catch (NumberFormatException nfe) {
            val = -1;
        }
        return val;
    }

    @Override
    protected boolean checkState() {
        if (super.checkState() == false) {
            return false;
        }
        boolean err = false;
        int val = getCurrentValue();
        if (val < 1024 || val > 32767) {
            setErrorMessage("Port must be between 1024 and 32767");
            err = true;
        } else {
            setErrorMessage(null);
            err = false;
        }
        showErrorMessage();
        return !err;
    }

    protected void updateCheckState(PortFieldEditor pfe) {
        pfe.refreshValidState();
    }
}
