package com.davidko.logclient.log;

import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.davidko.logclient.Log;
import com.davidko.logclient.Log.LogLevel;
import com.davidko.logclient.annotation.UiThread;
import com.davidko.logclient.log.LogPanel.LogMessage;

public class LogFilter {
	
	public final static int MODE_IP = 0x01;
    public final static int MODE_TAG = 0x02;
    public final static int MODE_LEVEL = 0x04;

    private String mName;

    private int mMode = 0;
    private String mClientIP;
    private int mLogLevel;

    private String mTag;

    private Table mTable;
    private TabItem mTabItem;
    private boolean mIsCurrentTabItem = false;
    private int mUnreadCount = 0;

    private String[] mTempKeywordFilters;

    private String mTempTag;

    private int mTempLogLevel = -1;

    private LogColors mColors;

    private boolean mTempFilteringStatus = false;
    
    private final ArrayList<LogMessage> mMessages = new ArrayList<LogMessage>();
    private final ArrayList<LogMessage> mNewMessages = new ArrayList<LogMessage>();

    private boolean mSupportsDelete = true;
    private boolean mSupportsEdit = true;
    private int mRemovedMessageCount = 0;

    public LogFilter(String name) {
        mName = name;
    }

    public LogFilter() {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(mName);

        sb.append(':');
        sb.append(mMode);
        if ((mMode & MODE_LEVEL) == MODE_LEVEL) {
            sb.append(':');
            sb.append(mLogLevel);
        }

        if ((mMode & MODE_TAG) == MODE_TAG) {
            sb.append(':');
            sb.append(mTag);
        }

        return sb.toString();
    }

    public boolean loadFromString(String string) {
        String[] segments = string.split(":");
        int index = 0;

        mName = segments[index++];

        mMode = Integer.parseInt(segments[index++]);

        if ((mMode & MODE_LEVEL) == MODE_LEVEL) {
            mLogLevel = Integer.parseInt(segments[index++]);
        }

        if ((mMode & MODE_TAG) == MODE_TAG) {
            mTag = segments[index++];
        }

        return true;
    }

    void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setWidgets(TabItem tabItem, Table table) {
        mTable = table;
        mTabItem = tabItem;
    }

    public boolean uiReady() {
        return (mTable != null && mTabItem != null);
    }

    public Table getTable() {
        return mTable;
    }

    public void dispose() {
        mTable.dispose();
        mTabItem.dispose();
        mTable = null;
        mTabItem = null;
    }

    public void resetFilteringMode() {
        mMode = 0;
    }

    public int getFilteringMode() {
        return mMode;
    }
    
    public void setIPMode(String ip) {
        if (ip != null) {
            mMode |= MODE_IP;
        } else {
            mMode &= ~MODE_IP;
        }
        mClientIP = ip;
    }
    
    public String getIPFilter() {
        if ((mMode & MODE_IP) == MODE_IP)
            return mClientIP;
        return null;
    }

    public void setTagMode(String tag) {
        if (tag != null && tag.length() > 0) {
            mMode |= MODE_TAG;
        } else {
            mMode &= ~MODE_TAG;
        }
        mTag = tag;
    }

    public String getTagFilter() {
        if ((mMode & MODE_TAG) == MODE_TAG)
            return mTag;
        return null;
    }

    public void setLogLevel(int level) {
        if (level == -1) {
            mMode &= ~MODE_LEVEL;
        } else {
            mMode |= MODE_LEVEL;
            mLogLevel = level;
        }

    }

    public int getLogLevel() {
        if ((mMode & MODE_LEVEL) == MODE_LEVEL) {
            return mLogLevel;
        }

        return -1;
    }


    public boolean supportsDelete() {
        return mSupportsDelete ;
    }

    public boolean supportsEdit() {
        return mSupportsEdit;
    }

    public void setSelectedState(boolean selected) {
        if (selected) {
            if (mTabItem != null) {
                mTabItem.setText(mName);
            }
            mUnreadCount = 0;
        }
        mIsCurrentTabItem = selected;
    }
    
    public boolean addMessage(LogMessage newMessage, LogMessage oldMessage) {
        synchronized (mMessages) {
            if (oldMessage != null) {
                int index = mMessages.indexOf(oldMessage);
                if (index != -1) {
                    mMessages.remove(index);
                    mRemovedMessageCount++;
                }
                
                index = mNewMessages.indexOf(oldMessage);
                if (index != -1) {
                    mNewMessages.remove(index);
                }
            }

            boolean filter = accept(newMessage);

            if (filter) {
                mMessages.add(newMessage);
                mNewMessages.add(newMessage);
            }

            return filter;
        }
    }
    
    public void clear() {
        mRemovedMessageCount = 0;
        mNewMessages.clear();
        mMessages.clear();
        mTable.removeAll();
    }
    
    boolean accept(LogMessage logMessage) {
        if ((mMode & MODE_TAG) == MODE_TAG && (
                logMessage.data.tag == null ||
                logMessage.data.tag.equals(mTag) == false)) {
            return false;
        }

        int msgLogLevel = logMessage.data.logLevel.getPriority();

        if (mTempLogLevel != -1) {
            if (mTempLogLevel > msgLogLevel) {
                return false;
            }
        } else if ((mMode & MODE_LEVEL) == MODE_LEVEL &&
                mLogLevel > msgLogLevel) {
            return false;
        }

        if (mTempKeywordFilters != null) {
            String msg = logMessage.msg;

            for (String kw : mTempKeywordFilters) {
                try {
                    if (msg.contains(kw) == false && msg.matches(kw) == false) {
                        return false;
                    }
                } catch (PatternSyntaxException e) {
                    return false;
                }
            }
        }

        if (mTempTag != null && mTempTag.length() > 0) {
            if (mTempTag.equals(logMessage.data.tag) == false) {
                return false;
            }
        }

        return true;
    }

    @UiThread
    public void flush() {
        ScrollBar bar = mTable.getVerticalBar();
        boolean scroll = bar.getMaximum() == bar.getSelection() + bar.getThumb();
        int topIndex = mTable.getTopIndex();

        mTable.setRedraw(false);
        
        int totalCount = mNewMessages.size();

        try {
            for (int i = 0 ; i < mRemovedMessageCount && mTable.getItemCount() > 0 ; i++) {
                mTable.remove(0);
            }
    
            if (mUnreadCount > mTable.getItemCount()) {
                mUnreadCount = mTable.getItemCount();
            }
    
            for (int i = 0  ; i < totalCount ; i++) {
                LogMessage msg = mNewMessages.get(i);
                addTableItem(msg);
            }
        } catch (SWTException e) {
            Log.e("localhost", "", "LogFilter", e);
        }
        
        mTable.setRedraw(true);

        if (scroll) {
            totalCount = mTable.getItemCount();
            if (totalCount > 0) {
                mTable.showItem(mTable.getItem(totalCount-1));
            }
        } else if (mRemovedMessageCount > 0) {

            topIndex -= mRemovedMessageCount;
            if (topIndex < 0) {
                mTable.showItem(mTable.getItem(0));
            } else {
                mTable.showItem(mTable.getItem(topIndex));
            }
        }

        if (mIsCurrentTabItem == false) {
            mUnreadCount += mNewMessages.size();
            totalCount = mTable.getItemCount();
            if (mUnreadCount > 0) {
                mTabItem.setText(mName + " (" 
                        + (mUnreadCount > totalCount ? totalCount : mUnreadCount)
                        + ")");
            } else {
                mTabItem.setText(mName);
            }
        }
        
        mNewMessages.clear();
    }

    void setColors(LogColors colors) {
        mColors = colors;
    }

    int getUnreadCount() {
        return mUnreadCount;
    }

    void setUnreadCount(int unreadCount) {
        mUnreadCount = unreadCount;
    }

    void setSupportsDelete(boolean support) {
        mSupportsDelete = support;
    }

    void setSupportsEdit(boolean support) {
        mSupportsEdit = support;
    }

    void setTempKeywordFiltering(String[] segments) {
        mTempKeywordFilters = segments;
        mTempFilteringStatus = true;
    }

    void setTempTagFiltering(String tag) {
        mTempTag = tag;
        mTempFilteringStatus = true;
    }

    void resetTempFiltering() {
        if (mTempTag != null || mTempKeywordFilters != null) {
            mTempFilteringStatus = true;
        }
        mTempTag = null;
        mTempKeywordFilters = null;
    }

    void resetTempFilteringStatus() {
        mTempFilteringStatus = false;
    }

    boolean getTempFilterStatus() {
        return mTempFilteringStatus;
    }

    private void addTableItem(LogMessage msg) {
        TableItem item = new TableItem(mTable, SWT.NONE);
        item.setText(0, msg.data.timestamp);
        item.setText(1, new String(new char[] { msg.data.logLevel.getPriorityLetter() }));
        item.setText(2, msg.data.clientIP);
        item.setText(3, msg.data.tag);
        item.setText(4, msg.msg);

        item.setData(msg);

        if (msg.data.logLevel == LogLevel.INFO) {
            item.setForeground(mColors.infoColor);
        } else if (msg.data.logLevel == LogLevel.DEBUG) {
            item.setForeground(mColors.debugColor);
        } else if (msg.data.logLevel == LogLevel.ERROR) {
            item.setForeground(mColors.errorColor);
        } else if (msg.data.logLevel == LogLevel.WARN) {
            item.setForeground(mColors.warningColor);
        } else {
            item.setForeground(mColors.verboseColor);
        }
    }
}
