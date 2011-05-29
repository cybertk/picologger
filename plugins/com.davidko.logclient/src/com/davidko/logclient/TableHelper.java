package com.davidko.logclient;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

public final class TableHelper {
    public static TableColumn createTableColumn(Table parent, String header,
            int style, String sample_text, final String pref_name,
            final IPreferenceStore prefs) {

        TableColumn col = new TableColumn(parent, style);

        if (prefs == null || prefs.contains(pref_name) == false) {
            col.setText(sample_text);
            col.pack();
            if (prefs != null) {
                prefs.setValue(pref_name, col.getWidth());
            }
        } else {
            col.setWidth(prefs.getInt(pref_name));
        }

        col.setText(header);

        if (prefs != null && pref_name != null) {
            col.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    int w = ((TableColumn)e.widget).getWidth();
                    prefs.setValue(pref_name, w);
                }
            });
        }

        return col;
    }

    public static void createTreeColumn(Tree parent, String header, int style,
            String sample_text, final String pref_name,
            final IPreferenceStore prefs) {

        TreeColumn col = new TreeColumn(parent, style);

        if (prefs == null || prefs.contains(pref_name) == false) {
            col.setText(sample_text);
            col.pack();
            if (prefs != null) {
                prefs.setValue(pref_name, col.getWidth());
            }
        } else {
            col.setWidth(prefs.getInt(pref_name));
        }

        col.setText(header);

        if (prefs != null && pref_name != null) {
            col.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    int w = ((TreeColumn)e.widget).getWidth();
                    prefs.setValue(pref_name, w);
                }
            });
        }
    }

    public static void createTreeColumn(Tree parent, String header, int style,
            int width, final String pref_name,
            final IPreferenceStore prefs) {

        TreeColumn col = new TreeColumn(parent, style);

        if (prefs == null || prefs.contains(pref_name) == false) {
            col.setWidth(width);
            if (prefs != null) {
                prefs.setValue(pref_name, width);
            }
        } else {
            col.setWidth(prefs.getInt(pref_name));
        }

        col.setText(header);

        if (prefs != null && pref_name != null) {
            col.addControlListener(new ControlListener() {
                public void controlMoved(ControlEvent e) {
                }

                public void controlResized(ControlEvent e) {
                    int w = ((TreeColumn)e.widget).getWidth();
                    prefs.setValue(pref_name, w);
                }
            });
        }
    }
}
