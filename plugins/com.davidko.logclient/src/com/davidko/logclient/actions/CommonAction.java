package com.davidko.logclient.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import com.davidko.logclient.actions.ICommonAction;

public class CommonAction extends Action implements ICommonAction {
    
    private Runnable mRunnable;

    public CommonAction() {
        super();
    }

    public CommonAction(String text) {
        super(text);
    }

    public CommonAction(String text, ImageDescriptor image) {
        super(text, image);
    }

    public CommonAction(String text, int style) {
        super(text, style);
    }
    
    @Override
    public void run() {
        if (mRunnable != null) {
            mRunnable.run();
        }
    }
    
    public void setRunnable(Runnable runnable) {
        mRunnable = runnable;
    }
}
