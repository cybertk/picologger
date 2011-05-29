package com.davidko.logclient.eclipse.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;

import com.davidko.logclient.ImageLoader;
import com.davidko.logclient.Log.LogLevel;
import com.davidko.logclient.LogClientPreferences;
import com.davidko.logclient.LogServer;
import com.davidko.logclient.actions.CommonAction;
import com.davidko.logclient.eclipse.LogClientPlugin;
import com.davidko.logclient.log.LogColors;
import com.davidko.logclient.log.LogFilter;
import com.davidko.logclient.log.LogPanel;
import com.davidko.logclient.log.LogPanel.ILogFilterStorageManager;

public final class LogClientView extends ViewPart {

	public static final String ID = "com.davidko.logclient.eclipse.views.LogClientView";

	private static final String PREFS_COL_CLIENTIP = LogClientPlugin.PLUGIN_ID
			+ ".logcat.clientip";
	private static final String PREFS_COL_TIME = LogClientPlugin.PLUGIN_ID
			+ ".logcat.time";
	private static final String PREFS_COL_LEVEL = LogClientPlugin.PLUGIN_ID
			+ ".logcat.level";
	private static final String PREFS_COL_TAG = LogClientPlugin.PLUGIN_ID
			+ ".logcat.tag";
	private static final String PREFS_COL_MESSAGE = LogClientPlugin.PLUGIN_ID
			+ ".logcat.message";

	private static final String PREFS_FILTERS = LogClientPlugin.PLUGIN_ID
			+ ".logcat.filters";

	private static LogClientView sThis;
	private LogPanel mLogPanel;

	private CommonAction mCreateFilterAction;
	private CommonAction mDeleteFilterAction;
	private CommonAction mEditFilterAction;
	private CommonAction mExportAction;

	private CommonAction[] mLogLevelActions;
	private String[] mLogLevelIcons = { "v.png", "d.png", "i.png", "w.png",
			"e.png", };

	private Action mClearAction;

	private Clipboard mClipboard;

	private LogServer logServer;

	private final class FilterStorage implements ILogFilterStorageManager {

		public LogFilter[] getFilterFromStore() {
			String filterPrefs = LogClientPlugin.getDefault()
					.getPreferenceStore().getString(PREFS_FILTERS);

			String[] filters = filterPrefs.split("\\|");

			ArrayList<LogFilter> list = new ArrayList<LogFilter>(filters.length);

			for (String f : filters) {
				if (f.length() > 0) {
					LogFilter logFilter = new LogFilter();
					if (logFilter.loadFromString(f)) {
						list.add(logFilter);
					}
				}
			}

			return list.toArray(new LogFilter[list.size()]);
		}

		public void saveFilters(LogFilter[] filters) {
			StringBuilder sb = new StringBuilder();
			for (LogFilter f : filters) {
				String filterString = f.toString();
				sb.append(filterString);
				sb.append('|');
			}

			LogClientPlugin.getDefault().getPreferenceStore()
					.setValue(PREFS_FILTERS, sb.toString());
		}

		public boolean requiresDefaultFilter() {
			return true;
		}
	}

	public LogClientView() {
		sThis = this;
		LogPanel.PREFS_CLIENTIP = PREFS_COL_CLIENTIP;
		LogPanel.PREFS_TIME = PREFS_COL_TIME;
		LogPanel.PREFS_LEVEL = PREFS_COL_LEVEL;
		LogPanel.PREFS_TAG = PREFS_COL_TAG;
		LogPanel.PREFS_MESSAGE = PREFS_COL_MESSAGE;
	}

	public static LogClientView getInstance() {
		return sThis;
	}

	@Override
	public void createPartControl(Composite parent) {
		Display d = parent.getDisplay();
		LogColors colors = new LogColors();

		ImageLoader loader = ImageLoader.getDdmUiLibLoader();

		colors.infoColor = new Color(d, 0, 127, 0);
		colors.debugColor = new Color(d, 0, 0, 127);
		colors.errorColor = new Color(d, 255, 0, 0);
		colors.warningColor = new Color(d, 255, 127, 0);
		colors.verboseColor = new Color(d, 0, 0, 0);

		mCreateFilterAction = new CommonAction("Create Filter") {
			@Override
			public void run() {
				mLogPanel.addFilter();
			}
		};
		mCreateFilterAction.setToolTipText("Create Filter");
		mCreateFilterAction
				.setImageDescriptor(loader.loadDescriptor("add.png"));

		mEditFilterAction = new CommonAction("Edit Filter") {
			@Override
			public void run() {
				mLogPanel.editFilter();
			}
		};
		mEditFilterAction.setToolTipText("Edit Filter");
		mEditFilterAction.setImageDescriptor(loader.loadDescriptor("edit.png"));

		mDeleteFilterAction = new CommonAction("Delete Filter") {
			@Override
			public void run() {
				mLogPanel.deleteFilter();
			}
		};
		mDeleteFilterAction.setToolTipText("Delete Filter");
		mDeleteFilterAction.setImageDescriptor(loader
				.loadDescriptor("delete.png"));

		mExportAction = new CommonAction("Export Selection As Text...") {
			@Override
			public void run() {
				mLogPanel.save();
			}
		};
		mExportAction.setToolTipText("Export Selection As Text...");
		mExportAction.setImageDescriptor(loader.loadDescriptor("save.png"));

		LogLevel[] levels = LogLevel.values();
		mLogLevelActions = new CommonAction[mLogLevelIcons.length];
		for (int i = 0; i < mLogLevelActions.length; i++) {
			String name = levels[i].getStringValue();
			mLogLevelActions[i] = new CommonAction(name, IAction.AS_CHECK_BOX) {
				@Override
				public void run() {
					for (int i = 0; i < mLogLevelActions.length; i++) {
						Action a = mLogLevelActions[i];
						if (a == this) {
							a.setChecked(true);
							mLogPanel.setCurrentFilterLogLevel(i + 2);
						} else {
							a.setChecked(false);
						}
					}
				}
			};

			mLogLevelActions[i].setToolTipText(name);
			mLogLevelActions[i].setImageDescriptor(loader
					.loadDescriptor(mLogLevelIcons[i]));
		}

		mClearAction = new Action("Clear Log") {
			@Override
			public void run() {
				mLogPanel.clear();
			}
		};
		mClearAction.setImageDescriptor(loader.loadDescriptor("clear.png"));

		mLogPanel = new LogPanel(colors, new FilterStorage(),
				LogPanel.FILTER_MANUAL);
		mLogPanel.setActions(mDeleteFilterAction, mEditFilterAction,
				mLogLevelActions);

		mLogPanel.createPanel(parent);

		placeActions();

		mClipboard = new Clipboard(d);
		IActionBars actionBars = getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
				new Action("Copy") {
					@Override
					public void run() {
						mLogPanel.copy(mClipboard);
					}
				});

		actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
				new Action("Select All") {
					@Override
					public void run() {
						mLogPanel.selectAll();
					}
				});

		logServer = new LogServer();
		mLogPanel.startLogCat(logServer, LogClientPreferences.DEFAULT_LOG_COMMAND);
	}

	@Override
	public void dispose() {
		mLogPanel.stopLogCat(true);
		mClipboard.dispose();
		logServer.closeConnection();
	}

	@Override
	public void setFocus() {
		mLogPanel.setFocus();
	}

	private void placeActions() {
		IActionBars actionBars = getViewSite().getActionBars();

		IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(mCreateFilterAction);
		menuManager.add(mEditFilterAction);
		menuManager.add(mDeleteFilterAction);
		menuManager.add(new Separator());
		menuManager.add(mClearAction);
		menuManager.add(new Separator());
		menuManager.add(mExportAction);

		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		for (CommonAction a : mLogLevelActions) {
			toolBarManager.add(a);
		}
		toolBarManager.add(new Separator());
		toolBarManager.add(mCreateFilterAction);
		toolBarManager.add(mEditFilterAction);
		toolBarManager.add(mDeleteFilterAction);
		toolBarManager.add(new Separator());
		toolBarManager.add(mClearAction);
	}
}
