package com.davidko.logclient.log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.davidko.logclient.ImageLoader;

public class EditFilterDialog extends Dialog {

    private static final int DLG_WIDTH = 400;
    private static final int DLG_HEIGHT = 250;

    private Shell mParent;

    private Shell mShell;

    private boolean mOk = false;

    private LogFilter mFilter;

    private String mName;
    private String mTag;
    private String mClientIP;

    private int mLogLevel;

    private Button mOkButton;

    private Label mClientIPWarning;

    public EditFilterDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
    }

    public EditFilterDialog(Shell shell, LogFilter filter) {
        this(shell);
        mFilter = filter;
    }

    public boolean open() {
        createUI();

        if (mParent == null || mShell == null) {
            return false;
        }

        mShell.setMinimumSize(DLG_WIDTH, DLG_HEIGHT);
        Rectangle r = mParent.getBounds();
        int cx = r.x + r.width/2;
        int x = cx - DLG_WIDTH / 2;
        int cy = r.y + r.height/2;
        int y = cy - DLG_HEIGHT / 2;
        mShell.setBounds(x, y, DLG_WIDTH, DLG_HEIGHT);

        mShell.open();

        Display display = mParent.getDisplay();
        while (!mShell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        if (mOk) {
            if (mFilter == null) {
                mFilter = new LogFilter(mName);
            }

            mFilter.setTagMode(mTag);

            if (mClientIP != null && mClientIP.length() > 0) {
                mFilter.setIPMode(mClientIP);
            } else {
                mFilter.setIPMode(null);
            }

            mFilter.setLogLevel(getLogLevel(mLogLevel));
        }

        return mOk;
    }

    public LogFilter getFilter() {
        return mFilter;
    }

    private void createUI() {
        mParent = getParent();
        mShell = new Shell(mParent, getStyle());
        mShell.setText("Log Filter");

        mShell.setLayout(new GridLayout(1, false));

        mShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
            }
        });

        Composite nameComposite = new Composite(mShell, SWT.NONE);
        nameComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        nameComposite.setLayout(new GridLayout(2, false));

        Label l = new Label(nameComposite, SWT.NONE);
        l.setText("Filter Name:");

        final Text filterNameText = new Text(nameComposite,
                SWT.SINGLE | SWT.BORDER);
        if (mFilter != null) {
            mName = mFilter.getName();
            if (mName != null) {
                filterNameText.setText(mName);
            }
        }
        filterNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mName = filterNameText.getText().trim();
                validate();
            }
        });

        l = new Label(mShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite main = new Composite(mShell, SWT.NONE);
        main.setLayoutData(new GridData(GridData.FILL_BOTH));
        main.setLayout(new GridLayout(3, false));

        l = new Label(main, SWT.NONE);
        l.setText("by Log Tag:");

        final Text tagText = new Text(main, SWT.SINGLE | SWT.BORDER);
        if (mFilter != null) {
            mTag = mFilter.getTagFilter();
            if (mTag != null) {
                tagText.setText(mTag);
            }
        }
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        tagText.setLayoutData(gd);
        tagText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                mTag = tagText.getText().trim();
                validate();
            }
        });

        l = new Label(main, SWT.NONE);
        l.setText("by ClientIP:");

        final Text pidText = new Text(main, SWT.SINGLE | SWT.BORDER);
        if (mFilter != null) {
            if (mFilter.getIPFilter() != null) {
                mClientIP = mFilter.getIPFilter();
            } else {
            	mClientIP = "";
            }
            pidText.setText(mClientIP);
        }
        pidText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        pidText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
            	mClientIP = pidText.getText().trim();
                validate();
            }
        });

        mClientIPWarning = new Label(main, SWT.NONE);
        mClientIPWarning.setImage(ImageLoader.getDdmUiLibLoader().loadImage("empty.png",
                mShell.getDisplay()));

        l = new Label(main, SWT.NONE);
        l.setText("by Log level:");

        final Combo logCombo = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        logCombo.setLayoutData(gd);

        logCombo.add("<none>");
        logCombo.add("Error");
        logCombo.add("Warning");
        logCombo.add("Info");
        logCombo.add("Debug");
        logCombo.add("Verbose");

        if (mFilter != null) {
            mLogLevel = getComboIndex(mFilter.getLogLevel());
            logCombo.select(mLogLevel);
        } else {
            logCombo.select(0);
        }

        logCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mLogLevel = logCombo.getSelectionIndex();
                validate();
            }
        });

        l = new Label(mShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite bottomComp = new Composite(mShell, SWT.NONE);
        bottomComp
                .setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        bottomComp.setLayout(new GridLayout(2, true));

        mOkButton = new Button(bottomComp, SWT.NONE);
        mOkButton.setText("OK");
        mOkButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mOk = true;
                mShell.close();
            }
        });
        mOkButton.setEnabled(false);
        mShell.setDefaultButton(mOkButton);

        Button cancelButton = new Button(bottomComp, SWT.NONE);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mShell.close();
            }
        });

        validate();
    }

    protected int getLogLevel(int index) {
        if (index == 0) {
            return -1;
        }

        return 7 - index;
    }

    private int getComboIndex(int logLevel) {
        if (logLevel == -1) {
            return 0;
        }

        return 7 - logLevel;
    }

    private void validate() {

        if (mClientIP != null) {
            if (mClientIP.matches("^$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$") == false) {
                mOkButton.setEnabled(false);
                mClientIPWarning.setImage(ImageLoader.getDdmUiLibLoader().loadImage(
                        "warning.png", 
                        mShell.getDisplay()));
                return;
            } else {
                mClientIPWarning.setImage(ImageLoader.getDdmUiLibLoader().loadImage(
                        "empty.png",
                        mShell.getDisplay()));
            }
        }

        if (mName == null || mName.length() == 0) {
            mOkButton.setEnabled(false);
            return;
        }

        mOkButton.setEnabled(true);
    }
}
