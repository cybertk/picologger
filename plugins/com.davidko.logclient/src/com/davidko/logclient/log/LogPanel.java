package com.davidko.logclient.log;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.davidko.logclient.Log;
import com.davidko.logclient.Log.LogLevel;
import com.davidko.logclient.LogClientPreferences;
import com.davidko.logclient.LogServer;
import com.davidko.logclient.MultiLineReceiver;
import com.davidko.logclient.Panel;
import com.davidko.logclient.TableHelper;
import com.davidko.logclient.actions.ICommonAction;

public class LogPanel extends Panel {

	private static final long serialVersionUID = 6734609098702869791L;

	private static final int STRING_BUFFER_LENGTH = 10000;

	public static final int FILTER_NONE = 0;
	public static final int FILTER_MANUAL = 1;
	public static final int FILTER_AUTO_IP = 2;
	public static final int FILTER_AUTO_TAG = 3;
	public static final int FILTER_DEBUG = 4;
	public static final int COLUMN_MODE_MANUAL = 0;
	public static final int COLUMN_MODE_AUTO = 1;

	public static String PREFS_CLIENTIP;
	public static String PREFS_TIME;
	public static String PREFS_LEVEL;
	public static String PREFS_TAG;
	public static String PREFS_MESSAGE;

	public interface ILogFilterStorageManager {

		public LogFilter[] getFilterFromStore();

		public void saveFilters(LogFilter[] filters);

		public boolean requiresDefaultFilter();
	}

	private Composite mParent;
	private IPreferenceStore mStore;

	private TabFolder mFolders;

	private LogColors mColors;

	private ILogFilterStorageManager mFilterStorage;

	private LogCatOuputReceiver mCurrentLogCat;

	private LogMessage[] mBuffer = new LogMessage[STRING_BUFFER_LENGTH];

	private int mBufferStart = -1;

	private int mBufferEnd = -1;

	private LogFilter[] mFilters;

	private LogFilter mDefaultFilter;

	private LogFilter mCurrentFilter;

	private int mFilterMode = FILTER_NONE;

	private ICommonAction mDeleteFilterAction;
	private ICommonAction mEditFilterAction;

	private ICommonAction[] mLogLevelActions;

	private LogServer mlogserver = null;

	public static class LogMessageInfo {
		public String clientIP;
		public String timestamp;
		public LogLevel logLevel;
		public String tag;
	}

	private LogMessageInfo mLastMessageInfo = null;

	private boolean mPendingAsyncRefresh = false;

	private String mDefaultLogSave;

	private int mColumnMode = COLUMN_MODE_MANUAL;
	private Font mDisplayFont;

	public static class LogMessage {
		public LogMessageInfo data;
		public String msg;

		@Override
		public String toString() {
			return data.timestamp + ": " + data.logLevel + "/" + data.tag + "("
					+ data.clientIP + "): " + msg;
		}
	}

	private final class LogCatOuputReceiver extends MultiLineReceiver {

		public boolean isCancelled = false;

		public LogCatOuputReceiver() {
			super();
		}

		@Override
		public void processNewLines(MultiLineReceiver.LineStrut[] lines) {
			if (isCancelled == false) {
				processLogLines(lines);
			}
		}

		public boolean isCancelled() {
			return isCancelled;
		}
	}

	public LogPanel(LogColors colors, ILogFilterStorageManager filterStorage,
			int mode) {
		mColors = colors;
		mFilterMode = mode;
		mFilterStorage = filterStorage;
		mStore = LogClientPreferences.getStore();
	}

	public void setActions(ICommonAction deleteAction,
			ICommonAction editAction, ICommonAction[] logLevelActions) {
		mDeleteFilterAction = deleteAction;
		mEditFilterAction = editAction;
		mLogLevelActions = logLevelActions;
	}

	public void setColumnMode(int mode) {
		mColumnMode = mode;
	}

	public void setFont(Font font) {
		mDisplayFont = font;

		if (mFilters != null) {
			for (LogFilter f : mFilters) {
				Table table = f.getTable();
				if (table != null) {
					table.setFont(font);
				}
			}
		}

		if (mDefaultFilter != null) {
			Table table = mDefaultFilter.getTable();
			if (table != null) {
				table.setFont(font);
			}
		}
	}

	@Override
	protected Control createControl(Composite parent) {
		mParent = parent;

		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		top.setLayout(new GridLayout(1, false));

		mFolders = new TabFolder(top, SWT.NONE);
		mFolders.setLayoutData(new GridData(GridData.FILL_BOTH));
		mFolders.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (mCurrentFilter != null) {
					mCurrentFilter.setSelectedState(false);
				}
				mCurrentFilter = getCurrentFilter();
				mCurrentFilter.setSelectedState(true);
				updateColumns(mCurrentFilter.getTable());
				if (mCurrentFilter.getTempFilterStatus()) {
					initFilter(mCurrentFilter);
				}
				selectionChanged(mCurrentFilter);
			}
		});

		Composite bottom = new Composite(top, SWT.NONE);
		bottom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		bottom.setLayout(new GridLayout(3, false));

		Label label = new Label(bottom, SWT.NONE);
		label.setText("Filter:");

		final Text filterText = new Text(bottom, SWT.SINGLE | SWT.BORDER);
		filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateFilteringWith(filterText.getText());
			}
		});

		createFilters();

		int index = 0;

		if (mDefaultFilter != null) {
			createTab(mDefaultFilter, index++, false);
		}

		if (mFilters != null) {
			for (LogFilter f : mFilters) {
				createTab(f, index++, false);
			}
		}

		return top;
	}

	@Override
	protected void postCreation() {
	}

	@Override
	public void setFocus() {
		mFolders.setFocus();
	}
	
	public void startLogCat(final LogServer logserver, final String command) {

		if (mlogserver != null) {
			stopLogCat(false);
			mlogserver = null;
		}
		resetUI(false);

		mCurrentLogCat = new LogCatOuputReceiver();

		new Thread("LogClient") {
			@Override
			public void run() {

				while (logserver.isOnLine() == false && mCurrentLogCat != null
						&& mCurrentLogCat.isCancelled == false) {
					try {
						sleep(2000);
					} catch (InterruptedException e) {
						return;
					}
				}

				if (mCurrentLogCat == null || mCurrentLogCat.isCancelled) {
					return;
				}

				try {
					mlogserver = logserver;
					logserver.executeShellCommand(command, mCurrentLogCat, 0);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("localhost", "", "LogClient", e);
				} finally {
					mCurrentLogCat = null;
					mlogserver = null;
				}
			}
		}.start();
	}

	public void stopLogCat(boolean inUiThread) {
		if (mCurrentLogCat != null) {
			mCurrentLogCat.isCancelled = true;

			mCurrentLogCat = null;

			for (int i = 0; i < STRING_BUFFER_LENGTH; i++) {
				mBuffer[i] = null;
			}

			mBufferStart = -1;
			mBufferEnd = -1;

			resetFilters();
			resetUI(inUiThread);
		}
	}

	public void addFilter() {
		EditFilterDialog dlg = new EditFilterDialog(mFolders.getShell());
		if (dlg.open()) {
			synchronized (mBuffer) {
				LogFilter filter = dlg.getFilter();
				addFilterToArray(filter);

				int index = mFilters.length - 1;
				if (mDefaultFilter != null) {
					index++;
				}

				createTab(filter, index, true);

				if (mDefaultFilter != null) {
					initDefaultFilter();
				}

				if (mCurrentFilter != null) {
					mCurrentFilter.setSelectedState(false);
				}
				mFolders.setSelection(index);
				filter.setSelectedState(true);
				mCurrentFilter = filter;

				selectionChanged(filter);

				if (mFilterMode == FILTER_NONE) {
					mFilterMode = FILTER_MANUAL;
				}

				mFilterStorage.saveFilters(mFilters);
			}
		}
	}

	public void editFilter() {
		if (mCurrentFilter != null && mCurrentFilter != mDefaultFilter) {
			EditFilterDialog dlg = new EditFilterDialog(mFolders.getShell(),
					mCurrentFilter);
			if (dlg.open()) {
				synchronized (mBuffer) {
					initFilter(mCurrentFilter);
					if (mDefaultFilter != null) {
						initDefaultFilter();
					}

					mFilterStorage.saveFilters(mFilters);
				}
			}
		}
	}

	public void deleteFilter() {
		synchronized (mBuffer) {
			if (mCurrentFilter != null && mCurrentFilter != mDefaultFilter) {
				removeFilterFromArray(mCurrentFilter);
				mCurrentFilter.dispose();

				mFolders.setSelection(0);
				if (mFilters.length > 0) {
					mCurrentFilter = mFilters[0];
				} else {
					mCurrentFilter = mDefaultFilter;
				}

				selectionChanged(mCurrentFilter);
				if (mDefaultFilter != null) {
					initDefaultFilter();
				}

				mFilterStorage.saveFilters(mFilters);
			}
		}
	}

	public boolean save() {
		synchronized (mBuffer) {
			FileDialog dlg = new FileDialog(mParent.getShell(), SWT.SAVE);
			String fileName;

			dlg.setText("Save log...");
			dlg.setFileName("log.txt");
			String defaultPath = mDefaultLogSave;
			if (defaultPath == null) {
				defaultPath = System.getProperty("user.home"); //$NON-NLS-1$
			}
			dlg.setFilterPath(defaultPath);
			dlg.setFilterNames(new String[] { "Text Files (*.txt)" });
			dlg.setFilterExtensions(new String[] { "*.txt" });

			fileName = dlg.open();
			if (fileName != null) {
				mDefaultLogSave = dlg.getFilterPath();

				Table currentTable = mCurrentFilter.getTable();

				int[] selection = currentTable.getSelectionIndices();

				Arrays.sort(selection);

				try {
					FileWriter writer = new FileWriter(fileName);

					for (int i : selection) {
						TableItem item = currentTable.getItem(i);
						LogMessage msg = (LogMessage) item.getData();
						String line = msg.toString();
						writer.write(line);
						writer.write('\n');
					}
					writer.flush();

				} catch (IOException e) {
					return false;
				}
			}
		}

		return true;
	}

	public void clear() {
		synchronized (mBuffer) {
			for (int i = 0; i < STRING_BUFFER_LENGTH; i++) {
				mBuffer[i] = null;
			}

			mBufferStart = -1;
			mBufferEnd = -1;

			for (LogFilter filter : mFilters) {
				filter.clear();
			}

			if (mDefaultFilter != null) {
				mDefaultFilter.clear();
			}
		}
	}

	public void copy(Clipboard clipboard) {
		Table currentTable = mCurrentFilter.getTable();
		copyTable(clipboard, currentTable);
	}

	public void selectAll() {
		Table currentTable = mCurrentFilter.getTable();
		currentTable.selectAll();
	}

	private static void copyTable(Clipboard clipboard, Table table) {
		int[] selection = table.getSelectionIndices();

		Arrays.sort(selection);

		StringBuilder sb = new StringBuilder();

		for (int i : selection) {
			TableItem item = table.getItem(i);
			LogMessage msg = (LogMessage) item.getData();
			String line = msg.toString();
			sb.append(line);
			sb.append('\n');
		}

		clipboard.setContents(new Object[] { sb.toString() },
				new Transfer[] { TextTransfer.getInstance() });
	}

	public void setCurrentFilterLogLevel(int i) {
		LogFilter filter = getCurrentFilter();

		filter.setLogLevel(i);

		initFilter(filter);
	}

	private TabItem createTab(LogFilter filter, int index, boolean fillTable) {
		synchronized (mBuffer) {
			TabItem item = null;
			if (index != -1) {
				item = new TabItem(mFolders, SWT.NONE, index);
			} else {
				item = new TabItem(mFolders, SWT.NONE);
			}
			item.setText(filter.getName());

			Composite top = new Composite(mFolders, SWT.NONE);
			item.setControl(top);

			top.setLayout(new FillLayout());

			final Table t = new Table(top, SWT.MULTI | SWT.FULL_SELECTION);
			t.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

			if (mDisplayFont != null) {
				t.setFont(mDisplayFont);
			}

			filter.setWidgets(item, t);

			t.setHeaderVisible(true);
			t.setLinesVisible(false);

			ControlListener listener = null;
			if (mColumnMode == COLUMN_MODE_AUTO) {
				listener = new ControlListener() {
					public void controlMoved(ControlEvent e) {
					}

					public void controlResized(ControlEvent e) {
						Rectangle r = t.getClientArea();

						int total = t.getColumn(0).getWidth();
						total += t.getColumn(1).getWidth();
						total += t.getColumn(2).getWidth();
						total += t.getColumn(3).getWidth();

						if (r.width > total) {
							t.getColumn(4).setWidth(r.width - total);
						}
					}
				};

				t.addControlListener(listener);
			}

			TableColumn col = TableHelper.createTableColumn(t, "Timestamp",
					SWT.LEFT, "00000000", PREFS_TIME, mStore);
			if (mColumnMode == COLUMN_MODE_AUTO) {
				col.addControlListener(listener);
			}

			col = TableHelper.createTableColumn(t, "Priority", SWT.CENTER, "D",
					PREFS_LEVEL, mStore);
			if (mColumnMode == COLUMN_MODE_AUTO) {
				col.addControlListener(listener);
			}

			col = TableHelper.createTableColumn(t, "ClientIP", SWT.LEFT,
					"9999", PREFS_CLIENTIP, mStore);
			if (mColumnMode == COLUMN_MODE_AUTO) {
				col.addControlListener(listener);
			}

			col = TableHelper.createTableColumn(t, "Tag", SWT.LEFT, "abcdefgh",
					PREFS_TAG, mStore);
			if (mColumnMode == COLUMN_MODE_AUTO) {
				col.addControlListener(listener);
			}

			col = TableHelper.createTableColumn(t, "Message", SWT.LEFT,
					"abcdefghijklmnopqrstuvwxyz0123456789", PREFS_MESSAGE,
					mStore);
			if (mColumnMode == COLUMN_MODE_AUTO) {
				col.setResizable(false);
			}

			if (fillTable) {
				initFilter(filter);
			}
			return item;
		}
	}

	protected void updateColumns(Table table) {
		if (table != null) {
			int index = 0;
			TableColumn col;

			col = table.getColumn(index++);
			col.setWidth(mStore.getInt(PREFS_TIME));

			col = table.getColumn(index++);
			col.setWidth(mStore.getInt(PREFS_LEVEL));

			col = table.getColumn(index++);
			col.setWidth(mStore.getInt(PREFS_CLIENTIP));

			col = table.getColumn(index++);
			col.setWidth(mStore.getInt(PREFS_TAG));

			col = table.getColumn(index++);
			col.setWidth(mStore.getInt(PREFS_MESSAGE));
		}
	}

	public void resetUI(boolean inUiThread) {
		if (mFilterMode == FILTER_AUTO_IP || mFilterMode == FILTER_AUTO_TAG) {
			if (inUiThread) {
				mFolders.dispose();
				mParent.pack(true);
				createControl(mParent);
			} else {
				Display d = mFolders.getDisplay();

				d.syncExec(new Runnable() {
					public void run() {
						mFolders.dispose();
						mParent.pack(true);
						createControl(mParent);
					}
				});
			}
		} else {
			if (mFolders.isDisposed() == false) {
				if (inUiThread) {
					emptyTables();
				} else {
					Display d = mFolders.getDisplay();

					d.syncExec(new Runnable() {
						public void run() {
							if (mFolders.isDisposed() == false) {
								emptyTables();
							}
						}
					});
				}
			}
		}
	}

	private boolean checkDataComplete(byte[] data) {
		boolean ok = false;
		try {
			int size = Integer.parseInt(getSize(data));
			if(data.length == 20 + size || data.length - 1 == 20 + size) {
				ok = true;
			}
		} catch (Exception e) {
			return false;
		}
		return ok;
	}
	
	private String genIP(byte[] data) {
		return data[0]+"."+data[1]+"."+data[2]+"."+data[3];
	}
	
	private String genTime(byte[] data) {
		String time = "";
		for(int i=4;i<12;i++) {
			if(data[i] != '\0' && i<=7) {
				time+=data[i];
			}else if(i==8) {
				time+="."+data[i];
			}else{
				time+=data[i];
			}
		}
		return time;
	}
	
	private String getSize(byte[] data) {
		String size="";
		for (int i = 16; i < 20; i++) {
			if(size.equals("") && data[i] != '\0') {
				size+=data[i];
			} else if(!size.equals("")){
				size+=data[i];
			}
		}
		return size;
	}
	
	private String getPriority(byte[] data) {
		String level = "";
		for (int i = 12; i < 16; i++) {
			if(level.equals("") && data[i] != '\0') {
			    level+=data[i];
			} else if(!level.equals("")){
				level+=data[i];
			}
		}
		return level;
	}
	
	private String genTag(byte[] data, int length) {
		String tag = "";
		for (int i = 20; i < length; i++) {
			tag+=(char)data[i];
		}
		return tag;
	}
	
	protected void processLogLines(MultiLineReceiver.LineStrut[] lines) {

		if (lines.length > STRING_BUFFER_LENGTH) {
			Log.e("localhost", "", "LogClient",
					"Receiving more lines than STRING_BUFFER_LENGTH");
		}

		final ArrayList<LogMessage> newMessages = new ArrayList<LogMessage>();

		synchronized (mBuffer) {
			for(MultiLineReceiver.LineStrut line : lines) {
				if (line.getLine().length > 0) {
					boolean ok = false;
					byte[] data = line.getLine();
					ok = checkDataComplete(data);
					LogMessage mc = new LogMessage();
					if (ok) {
						mLastMessageInfo = new LogMessageInfo();
						mLastMessageInfo.clientIP = genIP(data);
						mLastMessageInfo.timestamp = genTime(data);
						mLastMessageInfo.logLevel = LogLevel.getbyPriority(Integer.parseInt(getPriority(data)));
						mLastMessageInfo.tag = genTag(data, line.getTagEndIndex());
					} else {
						if (mLastMessageInfo == null) {
							mLastMessageInfo = new LogMessageInfo();
							mLastMessageInfo.timestamp = "????.????";
							mLastMessageInfo.clientIP = "<unknown>";
							mLastMessageInfo.logLevel = LogLevel.INFO;
							mLastMessageInfo.tag = "<unknown>";
						}
					}
					mc.data = mLastMessageInfo;
					mc.msg = line.getMsg().replaceAll("\t", "    ");
					System.out.println("will process new message:"+mc.data.tag+";"+this);
					processNewMessage(mc);
					newMessages.add(mc);
				}
			}

			if (mPendingAsyncRefresh == false) {
				mPendingAsyncRefresh = true;

				try {
					Display display = mFolders.getDisplay();

					display.asyncExec(new Runnable() {
						public void run() {
							asyncRefresh();
						}
					});
				} catch (SWTException e) {
					stopLogCat(false);
				}
			}
		}
	}

	private void asyncRefresh() {
		if (mFolders.isDisposed() == false) {
			synchronized (mBuffer) {
				try {
					if (mFilters != null) {
						for (LogFilter f : mFilters) {
							f.flush();
						}
					}

					if (mDefaultFilter != null) {
						mDefaultFilter.flush();
					}
				} finally {
					// the pending refresh is done.
					mPendingAsyncRefresh = false;
				}
			}
		} else {
			stopLogCat(true);
		}
	}

	private void processNewMessage(LogMessage newMessage) {
		if (mFilterMode == FILTER_AUTO_IP || mFilterMode == FILTER_AUTO_TAG) {
			checkFilter(newMessage.data);
		}

		int messageIndex = -1;
		if (mBufferStart == -1) {
			messageIndex = mBufferStart = 0;
			mBufferEnd = 1;
		} else {
			messageIndex = mBufferEnd;

			if (mBufferEnd == mBufferStart) {
				mBufferStart = (mBufferStart + 1) % STRING_BUFFER_LENGTH;
			}

			mBufferEnd = (mBufferEnd + 1) % STRING_BUFFER_LENGTH;
		}

		LogMessage oldMessage = null;

		if (mBuffer[messageIndex] != null) {
			oldMessage = mBuffer[messageIndex];
		}

		mBuffer[messageIndex] = newMessage;

		boolean filtered = false;
		if (mFilters != null) {
			for (LogFilter f : mFilters) {
				filtered |= f.addMessage(newMessage, oldMessage);
			}
		}
		if (filtered == false && mDefaultFilter != null) {
			mDefaultFilter.addMessage(newMessage, oldMessage);
		}
	}

	private void createFilters() {
		if (mFilterMode == FILTER_DEBUG || mFilterMode == FILTER_MANUAL) {
			mFilters = mFilterStorage.getFilterFromStore();

			if (mFilters != null) {
				for (LogFilter f : mFilters) {
					f.setColors(mColors);
				}
			}

			if (mFilterStorage.requiresDefaultFilter()) {
				mDefaultFilter = new LogFilter("Log");
				mDefaultFilter.setColors(mColors);
				mDefaultFilter.setSupportsDelete(false);
				mDefaultFilter.setSupportsEdit(false);
			}
		} else if (mFilterMode == FILTER_NONE) {
			mDefaultFilter = new LogFilter("Log");
			mDefaultFilter.setColors(mColors);
			mDefaultFilter.setSupportsDelete(false);
			mDefaultFilter.setSupportsEdit(false);
		}
	}

	private boolean checkFilter(final LogMessageInfo md) {
		return true;
	}

	private void addFilterToArray(LogFilter newFilter) {
		newFilter.setColors(mColors);

		if (mFilters != null && mFilters.length > 0) {
			LogFilter[] newFilters = new LogFilter[mFilters.length + 1];
			System.arraycopy(mFilters, 0, newFilters, 0, mFilters.length);
			newFilters[mFilters.length] = newFilter;
			mFilters = newFilters;
		} else {
			mFilters = new LogFilter[1];
			mFilters[0] = newFilter;
		}
	}

	private void removeFilterFromArray(LogFilter oldFilter) {
		int index = -1;
		for (int i = 0; i < mFilters.length; i++) {
			if (mFilters[i] == oldFilter) {
				index = i;
				break;
			}
		}

		if (index != -1) {
			LogFilter[] newFilters = new LogFilter[mFilters.length - 1];
			System.arraycopy(mFilters, 0, newFilters, 0, index);
			System.arraycopy(mFilters, index + 1, newFilters, index,
					newFilters.length - index);
			mFilters = newFilters;
		}
	}

	private void initFilter(LogFilter filter) {
		if (filter.uiReady() == false) {
			return;
		}

		if (filter == mDefaultFilter) {
			initDefaultFilter();
			return;
		}

		filter.clear();

		if (mBufferStart != -1) {
			int max = mBufferEnd;
			if (mBufferEnd < mBufferStart) {
				max += STRING_BUFFER_LENGTH;
			}

			for (int i = mBufferStart; i < max; i++) {
				int realItemIndex = i % STRING_BUFFER_LENGTH;

				filter.addMessage(mBuffer[realItemIndex], null /* old message */);
			}
		}

		filter.flush();
		filter.resetTempFilteringStatus();
	}

	private void initDefaultFilter() {
		mDefaultFilter.clear();

		if (mBufferStart != -1) {
			int max = mBufferEnd;
			if (mBufferEnd < mBufferStart) {
				max += STRING_BUFFER_LENGTH;
			}

			for (int i = mBufferStart; i < max; i++) {
				int realItemIndex = i % STRING_BUFFER_LENGTH;
				LogMessage msg = mBuffer[realItemIndex];

				boolean filtered = false;
				for (LogFilter f : mFilters) {
					filtered |= f.accept(msg);
				}

				if (filtered == false) {
					mDefaultFilter.addMessage(msg, null /* old message */);
				}
			}
		}

		mDefaultFilter.flush();
		mDefaultFilter.resetTempFilteringStatus();
	}

	private void resetFilters() {
		if (mFilterMode == FILTER_AUTO_IP || mFilterMode == FILTER_AUTO_TAG) {
			mFilters = null;

			createFilters();
		}
	}

	private LogFilter getCurrentFilter() {
		int index = mFolders.getSelectionIndex();

		if (index == 0 || mFilters == null) {
			return mDefaultFilter;
		}

		return mFilters[index - 1];
	}

	private void emptyTables() {
		for (LogFilter f : mFilters) {
			f.getTable().removeAll();
		}

		if (mDefaultFilter != null) {
			mDefaultFilter.getTable().removeAll();
		}
	}

	protected void updateFilteringWith(String text) {
		synchronized (mBuffer) {
			for (LogFilter f : mFilters) {
				f.resetTempFiltering();
			}
			if (mDefaultFilter != null) {
				mDefaultFilter.resetTempFiltering();
			}

			String[] segments = text.split(" ");

			ArrayList<String> keywords = new ArrayList<String>(segments.length);

			int tempPid = -1;
			String tempTag = null;
			for (int i = 0; i < segments.length; i++) {
				String s = segments[i];
				if (tempPid == -1 && s.startsWith("pid:")) {
					// get the pid
					String[] seg = s.split(":");
					if (seg.length == 2) {
						if (seg[1].matches("^[0-9]*$")) {
							tempPid = Integer.valueOf(seg[1]);
						}
					}
				} else if (tempTag == null && s.startsWith("tag:")) {
					String seg[] = segments[i].split(":");
					if (seg.length == 2) {
						tempTag = seg[1];
					}
				} else {
					keywords.add(s);
				}
			}

			if (tempPid != -1 || tempTag != null || keywords.size() > 0) {
				String[] keywordsArray = keywords.toArray(new String[keywords
						.size()]);

				for (LogFilter f : mFilters) {
					if (tempTag != null) {
						f.setTempTagFiltering(tempTag);
					}
					f.setTempKeywordFiltering(keywordsArray);
				}

				if (mDefaultFilter != null) {
					if (tempTag != null) {
						mDefaultFilter.setTempTagFiltering(tempTag);
					}
					mDefaultFilter.setTempKeywordFiltering(keywordsArray);

				}
			}

			initFilter(mCurrentFilter);
		}
	}

	private void selectionChanged(LogFilter selectedFilter) {
		if (mLogLevelActions != null) {
			int level = selectedFilter.getLogLevel();
			for (int i = 0; i < mLogLevelActions.length; i++) {
				ICommonAction a = mLogLevelActions[i];
				if (i == level - 2) {
					a.setChecked(true);
				} else {
					a.setChecked(false);
				}
			}
		}

		if (mDeleteFilterAction != null) {
			mDeleteFilterAction.setEnabled(selectedFilter.supportsDelete());
		}
		if (mEditFilterAction != null) {
			mEditFilterAction.setEnabled(selectedFilter.supportsEdit());
		}
	}

	public String getSelectedErrorLineMessage() {
		Table table = mCurrentFilter.getTable();
		int[] selection = table.getSelectionIndices();

		if (selection.length == 1) {
			TableItem item = table.getItem(selection[0]);
			LogMessage msg = (LogMessage) item.getData();
			if (msg.data.logLevel == LogLevel.ERROR
					|| msg.data.logLevel == LogLevel.WARN)
				return msg.msg;
		}
		return null;
	}

}
