/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com see bug 25324 Ability to know when tests are finished [junit]
 *     Vincent Massol: vmassol@octo.com 25324 Ability to know when tests are finished [junit]
 *     Sebastian Davids: sdavids@gmx.de 35762 JUnit View wasting a lot of screen space [JUnit]
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *     Andrew Eisenberg <andrew@eisenberg.as> - [JUnit] Rerun failed first does not work with JUnit4 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=140392
 *     Thirumala Reddy Mutchukota <thirumala@google.com> - [JUnit] Avoid rerun test launch on UI thread - https://bugs.eclipse.org/bugs/show_bug.cgi?id=411841
 *     Andrew Eisenberg <andrew@eisenberg.as> - [JUnit] Add a monospace font option for the junit results view - https://bugs.eclipse.org/bugs/show_bug.cgi?id=411794
 *     Andrej Zachar <andrej@chocolatejar.eu> - [JUnit] Add a filter for ignored tests - https://bugs.eclipse.org/bugs/show_bug.cgi?id=298603
 *     Sandra Lions <sandra.lions-piron@oracle.com> - [JUnit] allow to sort by name and by execution time - https://bugs.eclipse.org/bugs/show_bug.cgi?id=219466
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.junit.model.ITestElement.Result;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.PageSwitcher;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.debug.ui.DebugUITools;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestRunSessionListener;
import org.eclipse.jdt.internal.junit.model.ITestSessionListener;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import org.eclipse.jdt.internal.ui.viewsupport.ViewHistory;

/**
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart {

	public static final String NAME= "org.eclipse.jdt.junit.ResultView"; //$NON-NLS-1$

	private static final String RERUN_LAST_COMMAND= "org.eclipse.jdt.junit.junitShortcut.rerunLast"; //$NON-NLS-1$
	private static final String RERUN_FAILED_FIRST_COMMAND= "org.eclipse.jdt.junit.junitShortcut.rerunFailedFirst"; //$NON-NLS-1$

	static final int REFRESH_INTERVAL= 200;

	public static final int LAYOUT_FLAT= 0;
	public static final int LAYOUT_HIERARCHICAL= 1;

	/**
	 * Whether the output scrolls and reveals tests as they are executed.
	 */
	protected boolean fAutoScroll = true;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>, or <code>VIEW_ORIENTATION_AUTOMATIC</code>.
	 */
	private int fOrientation= VIEW_ORIENTATION_AUTOMATIC;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>.
	 */
	private int fCurrentOrientation;
	/**
	 * The current layout mode (LAYOUT_FLAT or LAYOUT_HIERARCHICAL).
	 */
	private int fLayout= LAYOUT_HIERARCHICAL;

//	private boolean fTestIsRunning= false;

	protected JUnitProgressBar fProgressBar;
	protected ProgressImages fProgressImages;
	protected Image fViewImage;
	protected CounterPanel fCounterPanel;
	protected boolean fShowOnErrorOnly= false;
	protected Clipboard fClipboard;
	protected volatile String fInfoMessage;

	private FailureTrace fFailureTrace;

	private TestViewer fTestViewer;
	/**
	 * Is the UI disposed?
	 */
	private boolean fIsDisposed= false;

	public enum SortingCriterion {
		SORT_BY_NAME, SORT_BY_EXECUTION_ORDER, SORT_BY_EXECUTION_TIME
	}
	/**
	 * The current sorting criterion.
	 */
	private SortingCriterion fSortingCriterion= SortingCriterion.SORT_BY_EXECUTION_ORDER;

	/**
	 * Actions
	 */
	private Action fNextAction;
	private Action fPreviousAction;

	private StopAction fStopAction;
	private JUnitCopyAction fCopyAction;
	private Action fPasteAction;

	private Action fRerunLastTestAction;
	private IHandlerActivation fRerunLastActivation;
	private Action fRerunFailedFirstAction;
	private IHandlerActivation fRerunFailedFirstActivation;

	private Action fFailuresOnlyFilterAction;
	private Action fIgnoredOnlyFilterAction;
	private ScrollLockAction fScrollLockAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
	private ShowTestHierarchyAction fShowTestHierarchyAction;
	private ShowTimeAction fShowTimeAction;
	private ActivateOnErrorAction fActivateOnErrorAction;
	private IMenuListener fViewMenuListener;

	private MenuManager fSortByMenu;
	private ToggleSortingAction[] fToggleSortingActions;

	private TestRunSession fTestRunSession;
	private TestSessionListener fTestSessionListener;

	private RunnerViewHistory fViewHistory;
	private TestRunSessionListener fTestRunSessionListener;

	final Image fStackViewIcon;
	final Image fTestRunOKIcon;
	final Image fTestRunFailIcon;
	final Image fTestRunOKDirtyIcon;
	final Image fTestRunFailDirtyIcon;

	final Image fTestIcon;
	final Image fTestOkIcon;
	final Image fTestErrorIcon;
	final Image fTestFailIcon;
	final Image fTestAssumptionFailureIcon;
	final Image fTestRunningIcon;
	final Image fTestIgnoredIcon;

	final ImageDescriptor fSuiteIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuite.svg"); //$NON-NLS-1$
	final ImageDescriptor fSuiteOkIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuiteok.svg"); //$NON-NLS-1$
	final ImageDescriptor fSuiteErrorIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuiteerror.svg"); //$NON-NLS-1$
	final ImageDescriptor fSuiteFailIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuitefail.svg"); //$NON-NLS-1$
	final ImageDescriptor fSuiteRunningIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuiterun.svg"); //$NON-NLS-1$

	final Image fSuiteIcon;
	final Image fSuiteOkIcon;
	final Image fSuiteErrorIcon;
	final Image fSuiteFailIcon;
	final Image fSuiteRunningIcon;

	final List<Image> fImagesToDispose;

	// Persistence tags.
	static final String TAG_PAGE= "page"; //$NON-NLS-1$
	static final String TAG_RATIO= "ratio"; //$NON-NLS-1$
	static final String TAG_TRACEFILTER= "tracefilter"; //$NON-NLS-1$
	static final String TAG_ORIENTATION= "orientation"; //$NON-NLS-1$
	static final String TAG_SCROLL= "scroll"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_LAYOUT= "layout"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_FAILURES_ONLY= "failuresOnly"; //$NON-NLS-1$

	/**
	 * @since 3.8
	 */
	static final String TAG_IGNORED_ONLY= "ignoredOnly"; //$NON-NLS-1$
	/**
	 * @since 3.4
	 */
	static final String TAG_SHOW_TIME= "time"; //$NON-NLS-1$

	static final String TAG_SORTING_CRITERION= "sortingCriterion"; //$NON-NLS-1$

	/**
	 * @since 3.5
	 */
	static final String PREF_LAST_PATH= "lastImportExportPath"; //$NON-NLS-1$

	/**
	 * @since 3.6
	 */
	static final String PREF_LAST_URL= "lastImportURL"; //$NON-NLS-1$

	//orientations
	static final int VIEW_ORIENTATION_VERTICAL= 0;
	static final int VIEW_ORIENTATION_HORIZONTAL= 1;
	static final int VIEW_ORIENTATION_AUTOMATIC= 2;

	private IMemento fMemento;

	Image fOriginalViewImage;
	IElementChangedListener fDirtyListener;


//	private CTabFolder fTabFolder;
	private SashForm fSashForm;

	private Composite fCounterComposite;
	private Composite fParent;

	/**
	 * A Job that periodically updates view description, counters, and progress bar.
	 */
	private UpdateUIJob fUpdateJob;

	/**
	 * A Job that runs as long as a test run is running.
	 * It is used to show busyness for running jobs in the view (title in italics).
	 */
	private JUnitIsRunningJob fJUnitIsRunningJob;
	private ILock fJUnitIsRunningLock;
	public static final Object FAMILY_JUNIT_RUN = new Object();

	private IPartListener2 fPartListener= new IPartListener2() {
		@Override
		public void partActivated(IWorkbenchPartReference ref) { }
		@Override
		public void partBroughtToTop(IWorkbenchPartReference ref) { }
		@Override
		public void partInputChanged(IWorkbenchPartReference ref) { }
		@Override
		public void partClosed(IWorkbenchPartReference ref) { }
		@Override
		public void partDeactivated(IWorkbenchPartReference ref) { }
		@Override
		public void partOpened(IWorkbenchPartReference ref) { }

		@Override
		public void partVisible(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible= true;
			}
		}

		@Override
		public void partHidden(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible= false;
			}
		}
	};

	protected boolean fPartIsVisible= false;


	private class RunnerViewHistory extends ViewHistory<TestRunSession> {

		@Override
		public void configureHistoryListAction(IAction action) {
			action.setText(JUnitMessages.TestRunnerViewPart_history);
		}

		@Override
		public void configureHistoryDropDownAction(IAction action) {
			action.setToolTipText(JUnitMessages.TestRunnerViewPart_test_run_history);
			JUnitPlugin.setLocalImageDescriptors(action, "history_list.svg"); //$NON-NLS-1$
		}

		@Override
		public Action getClearAction() {
			return new ClearAction();
		}

		@Override
		public String getHistoryListDialogTitle() {
			return JUnitMessages.TestRunnerViewPart_test_runs;
		}

		@Override
		public String getHistoryListDialogMessage() {
			return JUnitMessages.TestRunnerViewPart_select_test_run;
		}

		@Override
		public Shell getShell() {
			return fParent.getShell();
		}

		@Override
		public List<TestRunSession> getHistoryEntries() {
			return JUnitCorePlugin.getModel().getTestRunSessions();
		}

		@Override
		public TestRunSession getCurrentEntry() {
			return fTestRunSession;
		}

		@Override
		public void setActiveEntry(TestRunSession entry) {
			TestRunSession deactivatedSession= setActiveTestRunSession(entry);
			if (deactivatedSession != null)
				deactivatedSession.swapOut();
		}

		@Override
		public void setHistoryEntries(List<TestRunSession> remainingEntries, TestRunSession activeEntry) {
			setActiveTestRunSession(activeEntry);

			List<TestRunSession> testRunSessions= JUnitCorePlugin.getModel().getTestRunSessions();
			testRunSessions.removeAll(remainingEntries);
			for (TestRunSession testRunSession : testRunSessions) {
				JUnitCorePlugin.getModel().removeTestRunSession(testRunSession);
			}
			for (TestRunSession remaining : remainingEntries) {
				remaining.swapOut();
			}
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object element) {
			TestRunSession session= (TestRunSession) element;
			if (session.isStopped())
				return fSuiteIconDescriptor;

			if (session.isRunning())
				return fSuiteRunningIconDescriptor;

			Result result= session.getTestResult(true);
			if (result == Result.OK)
				return fSuiteOkIconDescriptor;
			else if (result == Result.ERROR)
				return fSuiteErrorIconDescriptor;
			else if (result == Result.FAILURE)
				return fSuiteFailIconDescriptor;
			else
				return fSuiteIconDescriptor;
		}

		@Override
		public String getText(TestRunSession session) {
			String testRunLabel= BasicElementLabels.getJavaElementName(session.getTestRunName());
			if (session.getStartTime() <= 0) {
				return testRunLabel;
			} else {
				String startTime= DateFormat.getDateTimeInstance().format(new Date(session.getStartTime()));
				return Messages.format(JUnitMessages.TestRunnerViewPart_testName_startTime, new Object[] { testRunLabel, startTime });
			}
		}

		@Override
		public void addMenuEntries(MenuManager manager) {
			manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new ImportTestRunSessionAction(fParent.getShell()));
			manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new ImportTestRunSessionFromURLAction(fParent.getShell()));
			manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fPasteAction);
			if (fTestRunSession != null)
				manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new ExportTestRunSessionAction(fParent.getShell(), fTestRunSession));
		}

		@Override
		public String getMaxEntriesMessage() {
			return JUnitMessages.TestRunnerViewPart_max_remembered;
		}

		@Override
		public int getMaxEntries() {
			return Platform.getPreferencesService().getInt(JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.MAX_TEST_RUNS, 10, null);
		}

		@Override
		public void setMaxEntries(int maxEntries) {
			InstanceScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID).putInt(JUnitPreferencesConstants.MAX_TEST_RUNS, maxEntries);
		}
	}

	private static class ImportTestRunSessionAction extends Action {
		private final Shell fShell;

		public ImportTestRunSessionAction(Shell shell) {
			super(JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_name);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("etool16/import_wiz.svg")); //$NON-NLS-1$
			fShell= shell;
		}

		@Override
		public void run() {
			FileDialog importDialog= new FileDialog(fShell, SWT.OPEN | SWT.SHEET);
			importDialog.setText(JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_title);
			IDialogSettings dialogSettings= JUnitPlugin.getDefault().getDialogSettings();
			String lastPath= dialogSettings.get(PREF_LAST_PATH);
			if (lastPath != null) {
				importDialog.setFilterPath(lastPath);
			}
			importDialog.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			String path= importDialog.open();
			if (path == null)
				return;

			//TODO: MULTI: getFileNames()
			File file= new File(path);

			try {
				JUnitModel.importTestRunSession(file);
			} catch (CoreException e) {
				JUnitPlugin.log(e);
				ErrorDialog.openError(fShell, JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_error_title, e.getStatus().getMessage(), e.getStatus());
			}
		}
	}

	private static class JUnitPasteAction extends Action {
		private final Shell fShell;
		private Clipboard fClipboard;

		public JUnitPasteAction(Shell shell, Clipboard clipboard) {
			super(JUnitMessages.TestRunnerViewPart_JUnitPasteAction_label);
			Assert.isNotNull(clipboard);
			fShell= shell;
			fClipboard= clipboard;
		}

		@Override
		public void run() {
			String urlData= (String) fClipboard.getContents(URLTransfer.getInstance());
			if (urlData == null) {
				urlData= (String) fClipboard.getContents(TextTransfer.getInstance());
			}
			if (urlData != null && urlData.length() > 0) {
				if (isValidUrl(urlData)) {
					importTestRunSession(urlData);
					return;
				}
			}
			MessageDialog.openInformation(fShell,
					JUnitMessages.TestRunnerViewPart_JUnitPasteAction_cannotpaste_title,
					JUnitMessages.TestRunnerViewPart_JUnitPasteAction_cannotpaste_message
			);
		}

		private boolean isValidUrl(String urlData) {
			try {
				@SuppressWarnings("unused")
				URL url= new URL(urlData);
			} catch (MalformedURLException e) {
				return false;
			}
			return true;
		}
	}

	private static class ImportTestRunSessionFromURLAction extends Action {
		private static class URLValidator implements IInputValidator {
			@Override
			public String isValid(String newText) {
				if (newText.length() == 0)
					return null;
				try {
					@SuppressWarnings("unused")
					URL url= new URL(newText);
					return null;
				} catch (MalformedURLException e) {
					return JUnitMessages.TestRunnerViewPart_ImportTestRunSessionFromURLAction_invalid_url + e.getLocalizedMessage();
				}
			}
		}

		private static final String DIALOG_SETTINGS= "ImportTestRunSessionFromURLAction"; //$NON-NLS-1$

		private final Shell fShell;

		public ImportTestRunSessionFromURLAction(Shell shell) {
			super(JUnitMessages.TestRunnerViewPart_ImportTestRunSessionFromURLAction_import_from_url);
			fShell= shell;
		}

		@Override
		public void run() {
			String title= JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_title;
			String message= JUnitMessages.TestRunnerViewPart_ImportTestRunSessionFromURLAction_url;

			final IDialogSettings dialogSettings= JUnitPlugin.getDefault().getDialogSettings();
			String url= dialogSettings.get(PREF_LAST_URL);

			IInputValidator validator= new URLValidator();

			InputDialog inputDialog= new InputDialog(fShell, title, message, url, validator) {
				@Override
				protected Control createDialogArea(Composite parent) {
					Control dialogArea2= super.createDialogArea(parent);
					Object layoutData= getText().getLayoutData();
					if (layoutData instanceof GridData) {
						GridData gd= (GridData)layoutData;
						gd.widthHint= convertWidthInCharsToPixels(150);
					}
					return dialogArea2;
				}
				@Override
				protected IDialogSettings getDialogBoundsSettings() {
					IDialogSettings settings= dialogSettings.getSection(DIALOG_SETTINGS);
					if (settings == null) {
						settings= dialogSettings.addNewSection(DIALOG_SETTINGS);
					}
					settings.put("DIALOG_HEIGHT", Dialog.DIALOG_DEFAULT_BOUNDS); //$NON-NLS-1$
					return settings;
				}
				@Override
				protected boolean isResizable() {
					return true;
				}
			};

			int res= inputDialog.open();
			if (res == IDialogConstants.OK_ID) {
				url= inputDialog.getValue();
				dialogSettings.put(PREF_LAST_URL, url);
				importTestRunSession(url);
			}
		}
	}

	private static class ExportTestRunSessionAction extends Action {
		private final TestRunSession fTestRunSession;
		private final Shell fShell;

		public ExportTestRunSessionAction(Shell shell, TestRunSession testRunSession) {
			super(JUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_name);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("etool16/export_wiz.svg")); //$NON-NLS-1$
			fShell= shell;
			fTestRunSession= testRunSession;
		}

		@Override
		public void run() {
			FileDialog exportDialog= new FileDialog(fShell, SWT.SAVE | SWT.SHEET);
			exportDialog.setText(JUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_title);
			IDialogSettings dialogSettings= JUnitPlugin.getDefault().getDialogSettings();
			String lastPath= dialogSettings.get(PREF_LAST_PATH);
			if (lastPath != null) {
				exportDialog.setFilterPath(lastPath);
			}
			exportDialog.setFileName(getFileName());
			exportDialog.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			String path= exportDialog.open();
			if (path == null)
				return;

			//TODO: MULTI: getFileNames()
			File file= new File(path);

			try {
				JUnitModel.exportTestRunSession(fTestRunSession, file);
			} catch (CoreException e) {
				JUnitPlugin.log(e);
				ErrorDialog.openError(fShell, JUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_error_title, e.getStatus().getMessage(), e.getStatus());
			}
		}

		private String getFileName() {
			String testRunName= fTestRunSession.getTestRunName();
			long startTime= fTestRunSession.getStartTime();
			if (startTime <= 0)
				return testRunName;

			String isoTime= new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(startTime)); //$NON-NLS-1$
			return testRunName + " " + isoTime + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private class TestRunSessionListener implements ITestRunSessionListener {
		@Override
		public void sessionAdded(final TestRunSession testRunSession) {
			getDisplay().asyncExec(() -> {
				if (JUnitUIPreferencesConstants.getShowInAllViews() ||
						getSite().getWorkbenchWindow() == JUnitPlugin.getActiveWorkbenchWindow()) {
					if (fInfoMessage == null) {
						String testRunLabel= BasicElementLabels.getJavaElementName(testRunSession.getTestRunName());
						String msg;
						if (testRunSession.getLaunch() != null) {
							msg= Messages.format(JUnitMessages.TestRunnerViewPart_Launching, new Object[]{ testRunLabel });
						} else {
							msg= testRunLabel;
						}
						registerInfoMessage(msg);
					}

					TestRunSession deactivatedSession= setActiveTestRunSession(testRunSession);
					if (deactivatedSession != null)
						deactivatedSession.swapOut();
				}
			});
		}
		@Override
		public void sessionRemoved(final TestRunSession testRunSession) {
			getDisplay().asyncExec(() -> {
				if (testRunSession.equals(fTestRunSession)) {
					List<TestRunSession> testRunSessions= JUnitCorePlugin.getModel().getTestRunSessions();
					TestRunSession deactivatedSession;
					if (! testRunSessions.isEmpty()) {
						deactivatedSession= setActiveTestRunSession(testRunSessions.get(0));
					} else {
						deactivatedSession= setActiveTestRunSession(null);
					}
					if (deactivatedSession != null)
						deactivatedSession.swapOut();
				}
			});
		}
	}

	private class TestSessionListener implements ITestSessionListener {
		@Override
		public void sessionStarted(){
			fTestViewer.registerViewersRefresh();
			fShowOnErrorOnly= getShowOnErrorOnly();

			startUpdateJobs();

			fStopAction.setEnabled(true);
			fRerunLastTestAction.setEnabled(true);

			// While tests are running, always use the execution order
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					fTestViewer.setSortingCriterion(SortingCriterion.SORT_BY_EXECUTION_ORDER);
				}
			});
		}

		@Override
		public void sessionEnded(long elapsedTime){
			deregisterTestSessionListener(false);

			fTestViewer.registerAutoScrollTarget(null);

			String[] keys= {elapsedTimeAsString(elapsedTime)};
			String msg= Messages.format(JUnitMessages.TestRunnerViewPart_message_finish, keys);
			registerInfoMessage(msg);

			postSyncRunnable(() -> {
				if (isDisposed())
					return;
				fStopAction.setEnabled(lastLaunchIsKeptAlive());
				updateRerunFailedFirstAction();
				processChangesInUI();
				if (hasErrorsOrFailures()) {
					selectFirstFailure();
				}
				if (fDirtyListener == null) {
					fDirtyListener= new DirtyListener();
					JavaCore.addElementChangedListener(fDirtyListener);
				}
				warnOfContentChange();
			});
			stopUpdateJobs();
			logMessageIfNoTests();

			// When test session ended, apply user sorting criterion
			getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					setSortingCriterion(fSortingCriterion);
				}
			});
		}

		@Override
		public void sessionStopped(final long elapsedTime) {
			deregisterTestSessionListener(false);

			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(JUnitMessages.TestRunnerViewPart_message_stopped);
			handleStopped();
		}

		@Override
		public void sessionTerminated() {
			deregisterTestSessionListener(true);

			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(JUnitMessages.TestRunnerViewPart_message_terminated);
			handleStopped();
		}

		@Override
		public void runningBegins() {
			if (!fShowOnErrorOnly)
				postShowTestResultsView();
		}

		@Override
		public void testStarted(TestCaseElement testCaseElement) {
			fTestViewer.registerAutoScrollTarget(testCaseElement);
			fTestViewer.registerViewerUpdate(testCaseElement);

			String className= BasicElementLabels.getJavaElementName(testCaseElement.getClassName());
			String method= BasicElementLabels.getJavaElementName(testCaseElement.getTestMethodName());
			String status= Messages.format(JUnitMessages.TestRunnerViewPart_message_started, new String[] { className, method });
			registerInfoMessage(status);
		}

		@Override
		public void testFailed(TestElement testElement, TestElement.Status status, String trace, String expected, String actual) {
			if (isAutoScroll()) {
				fTestViewer.registerFailedForAutoScroll(testElement);
			}
			fTestViewer.registerViewerUpdate(testElement);

		    // show the view on the first error only
		    if (fShowOnErrorOnly && (getErrorsPlusFailures() == 1))
		        postShowTestResultsView();

		    //TODO:
		    // [Bug 35590] JUnit window doesn't report errors from junit.extensions.TestSetup [JUnit]
		    // when a failure occurs in test setup then no test is running
		    // to update the views we artificially signal the end of a test run
//		    if (!fTestIsRunning) {
//				fTestIsRunning= false;
//				testEnded(testCaseElement);
//			}
		}

		@Override
		public void testEnded(TestCaseElement testCaseElement){
			fTestViewer.registerViewerUpdate(testCaseElement);
		}

		@Override
		public void testReran(TestCaseElement testCaseElement, TestElement.Status status, String trace, String expectedResult, String actualResult) {
			fTestViewer.registerViewerUpdate(testCaseElement); //TODO: autoExpand?
			postSyncProcessChanges();
			showFailure(testCaseElement);
		}

		@Override
		public void testAdded(TestElement testElement) {
			fTestViewer.registerTestAdded(testElement);
		}

		@Override
		public boolean acceptsSwapToDisk() {
			return false;
		}
	}

	private class UpdateUIJob extends UIJob {
		private boolean fRunning= true;

		public UpdateUIJob(String name) {
			super(name);
			setSystem(true);
		}
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!isDisposed()) {
				processChangesInUI();
			}
			schedule(REFRESH_INTERVAL);
			return Status.OK_STATUS;
		}

		public void stop() {
			fRunning= false;
		}
		@Override
		public boolean shouldSchedule() {
			return fRunning;
		}
	}

	private class JUnitIsRunningJob extends Job {
		public JUnitIsRunningJob(String name) {
			super(name);
			setSystem(true);
		}
		@Override
		public IStatus run(IProgressMonitor monitor) {
			// wait until the test run terminates
			fJUnitIsRunningLock.acquire();
			return Status.OK_STATUS;
		}
		@Override
		public boolean belongsTo(Object family) {
			return family == TestRunnerViewPart.FAMILY_JUNIT_RUN;
		}
	}

	private class ClearAction extends Action {
		public ClearAction() {
			setText(JUnitMessages.TestRunnerViewPart_clear_history_label);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/rem_all_co.svg")); //$NON-NLS-1$

			boolean enabled= false;
			List<TestRunSession> testRunSessions= JUnitCorePlugin.getModel().getTestRunSessions();
			for (TestRunSession testRunSession : testRunSessions) {
				if (! testRunSession.isRunning() && ! testRunSession.isStarting()) {
					enabled= true;
					break;
				}
			}
			setEnabled(enabled);
		}

		@Override
		public void run() {
			List<TestRunSession> testRunSessions= getRunningSessions();
			TestRunSession first= testRunSessions.isEmpty() ? null : testRunSessions.get(0);
			fViewHistory.setHistoryEntries(testRunSessions, first);
		}

		private List<TestRunSession> getRunningSessions() {
			List<TestRunSession> testRunSessions= JUnitCorePlugin.getModel().getTestRunSessions();
			for (Iterator<TestRunSession> iter= testRunSessions.iterator(); iter.hasNext();) {
				TestRunSession testRunSession= iter.next();
				if (! testRunSession.isRunning() && ! testRunSession.isStarting()) {
					iter.remove();
				}
			}
			return testRunSessions;
		}
	}

	private class StopAction extends Action {
		public StopAction() {
			setText(JUnitMessages.TestRunnerViewPart_stopaction_text);
			setToolTipText(JUnitMessages.TestRunnerViewPart_stopaction_tooltip);
			JUnitPlugin.setLocalImageDescriptors(this, "stop.svg"); //$NON-NLS-1$
		}

		@Override
		public void run() {
			stopTest();
			setEnabled(false);
		}
	}

	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText(JUnitMessages.TestRunnerViewPart_rerunaction_label);
			setToolTipText(JUnitMessages.TestRunnerViewPart_rerunaction_tooltip);
			JUnitPlugin.setLocalImageDescriptors(this, "relaunch.svg"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_LAST_COMMAND);
		}

		@Override
		public void run(){
			rerunTestRun();
		}
	}

	private class RerunLastFailedFirstAction extends Action {
		public RerunLastFailedFirstAction() {
			setText(JUnitMessages.TestRunnerViewPart_rerunfailuresaction_label);
			setToolTipText(JUnitMessages.TestRunnerViewPart_rerunfailuresaction_tooltip);
			JUnitPlugin.setLocalImageDescriptors(this, "relaunchf.svg"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_FAILED_FIRST_COMMAND);
		}

		@Override
		public void run(){
			rerunTestFailedFirst();
		}
	}

	private class ToggleOrientationAction extends Action {
		private final int fActionOrientation;

		public ToggleOrientationAction(int orientation) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			switch (orientation) {
			case TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL:
				setText(JUnitMessages.TestRunnerViewPart_toggle_horizontal_label);
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_horizontal.svg")); //$NON-NLS-1$
				break;
			case TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL:
				setText(JUnitMessages.TestRunnerViewPart_toggle_vertical_label);
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_vertical.svg")); //$NON-NLS-1$
				break;
			case TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC:
				setText(JUnitMessages.TestRunnerViewPart_toggle_automatic_label);
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_automatic.svg")); //$NON-NLS-1$
				break;
			default:
				break;
			}
			fActionOrientation= orientation;
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
		}

		public int getOrientation() {
			return fActionOrientation;
		}

		@Override
		public void run() {
			if (isChecked()) {
				fOrientation= fActionOrientation;
				computeOrientation();
			}
		}
	}

	private class ToggleSortingAction extends Action {
		private final SortingCriterion fActionSortingCriterion;

		public ToggleSortingAction(SortingCriterion sortingCriterion) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			switch (sortingCriterion) {
				case SORT_BY_NAME:
					setText(JUnitMessages.TestRunnerViewPart_toggle_name_label);
					setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/alphab_sort_co.svg")); //$NON-NLS-1$
					break;
				case SORT_BY_EXECUTION_ORDER:
					setText(JUnitMessages.TestRunnerViewPart_toggle_execution_order_label);
					setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/stckframe_running_obj.svg")); //$NON-NLS-1$
					break;
				case SORT_BY_EXECUTION_TIME:
					setText(JUnitMessages.TestRunnerViewPart_toggle_execution_time_label);
					setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/time_obj.svg")); //$NON-NLS-1$
					break;
				default:
					break;
			}
			fActionSortingCriterion= sortingCriterion;
		}

		@Override
		public void run() {
			if (isChecked()) {
				setSortingCriterion(fActionSortingCriterion);
			}
		}

		public SortingCriterion getActionSortingCriterion() {
			return fActionSortingCriterion;
		}
	}

	public SortingCriterion getSortingCriterion() {
		return fSortingCriterion;
	}

	public void setSortingCriterion(SortingCriterion sortingCriterion) {
		fSortingCriterion= sortingCriterion;
		if (fTestRunSession != null && !fTestRunSession.isStarting() && !fTestRunSession.isRunning()) {
			fTestViewer.setSortingCriterion(sortingCriterion);
		}
	}

	/**
	 * Listen for for modifications to Java elements
	 */
	private class DirtyListener implements IElementChangedListener {
		@Override
		public void elementChanged(ElementChangedEvent event) {
			processDelta(event.getDelta());
		}

		private boolean processDelta(IJavaElementDelta delta) {
			int kind= delta.getKind();
			int details= delta.getFlags();
			int type= delta.getElement().getElementType();

			switch (type) {
				// Consider containers for class files.
				case IJavaElement.JAVA_MODEL:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.PACKAGE_FRAGMENT:
					// If we did something different than changing a child we flush the undo / redo stack.
					if (kind != IJavaElementDelta.CHANGED || details != IJavaElementDelta.F_CHILDREN) {
						codeHasChanged();
						return false;
					}
					break;
				case IJavaElement.COMPILATION_UNIT:
					// if we have changed a primary working copy (e.g created, removed, ...)
					// then we do nothing.
					if ((details & IJavaElementDelta.F_PRIMARY_WORKING_COPY) != 0)
						return true;
					codeHasChanged();
					return false;

				case IJavaElement.CLASS_FILE:
					// Don't examine children of a class file but keep on examining siblings.
					return true;
				default:
					codeHasChanged();
					return false;
			}

			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
			if (affectedChildren == null)
				return true;

			for (IJavaElementDelta affectedChild : affectedChildren) {
				if (!processDelta(affectedChild)) {
					return false;
				}
			}
			return true;
		}
	}

	private class FailuresOnlyFilterAction extends Action {
		public FailuresOnlyFilterAction() {
			super(JUnitMessages.TestRunnerViewPart_show_failures_only, AS_CHECK_BOX);
			setToolTipText(JUnitMessages.TestRunnerViewPart_show_failures_only);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/failures.svg")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			setShowFailuresOnly(isChecked());
		}
	}

	private class IgnoredOnlyFilterAction extends Action {
		public IgnoredOnlyFilterAction() {
			super(JUnitMessages.TestRunnerViewPart_show_ignored_only, AS_CHECK_BOX);
			setToolTipText(JUnitMessages.TestRunnerViewPart_show_ignored_only);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/testignored.svg")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			setShowIgnoredOnly(isChecked());
		}
	}

	private class ShowTimeAction extends Action {

		public ShowTimeAction() {
			super(JUnitMessages.TestRunnerViewPart_show_execution_time, IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			setShowExecutionTime(isChecked());
		}
	}

	private class ShowTestHierarchyAction extends Action {

		public ShowTestHierarchyAction() {
			super(JUnitMessages.TestRunnerViewPart_hierarchical_layout, IAction.AS_CHECK_BOX);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/hierarchicalLayout.svg")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			int mode= isChecked() ? LAYOUT_HIERARCHICAL : LAYOUT_FLAT;
			setLayoutMode(mode);
		}
	}

	private class ActivateOnErrorAction extends Action {
		public ActivateOnErrorAction() {
			super(JUnitMessages.TestRunnerViewPart_activate_on_failure_only, IAction.AS_CHECK_BOX);
			//setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/failures.svg")); //$NON-NLS-1$
			update();
		}
		public void update() {
			setChecked(getShowOnErrorOnly());
		}
		@Override
		public void run() {
			boolean checked= isChecked();
			fShowOnErrorOnly= checked;
			InstanceScope.INSTANCE.getNode(JUnitCorePlugin.CORE_PLUGIN_ID).putBoolean(JUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, checked);
		}
	}

	public TestRunnerViewPart() {
		fImagesToDispose= new ArrayList<>();

		fStackViewIcon= createManagedImage("eview16/stackframe.svg");//$NON-NLS-1$
		fTestRunOKIcon= createManagedImage("eview16/junitsucc.svg"); //$NON-NLS-1$
		fTestRunFailIcon= createManagedImage("eview16/juniterr.svg"); //$NON-NLS-1$
		fTestRunOKDirtyIcon= createManagedImage("eview16/junitsuccq.svg"); //$NON-NLS-1$
		fTestRunFailDirtyIcon= createManagedImage("eview16/juniterrq.svg"); //$NON-NLS-1$

		fTestIcon= createManagedImage("obj16/test.svg"); //$NON-NLS-1$
		fTestOkIcon= createManagedImage("obj16/testok.svg"); //$NON-NLS-1$
		fTestErrorIcon= createManagedImage("obj16/testerr.svg"); //$NON-NLS-1$
		fTestFailIcon= createManagedImage("obj16/testfail.svg"); //$NON-NLS-1$
		fTestRunningIcon= createManagedImage("obj16/testrun.svg"); //$NON-NLS-1$
		fTestIgnoredIcon= createManagedImage("obj16/testignored.svg"); //$NON-NLS-1$
		fTestAssumptionFailureIcon = createManagedImage("obj16/testassumptionfailed.svg"); //$NON-NLS-1$

		fSuiteIcon= createManagedImage(fSuiteIconDescriptor);
		fSuiteOkIcon= createManagedImage(fSuiteOkIconDescriptor);
		fSuiteErrorIcon= createManagedImage(fSuiteErrorIconDescriptor);
		fSuiteFailIcon= createManagedImage(fSuiteFailIconDescriptor);
		fSuiteRunningIcon= createManagedImage(fSuiteRunningIconDescriptor);
	}

	private Image createManagedImage(String path) {
		return createManagedImage(JUnitPlugin.getImageDescriptor(path));
	}

	private Image createManagedImage(ImageDescriptor descriptor) {
		Image image= descriptor.createImage();
		if (image == null) {
			image= ImageDescriptor.getMissingImageDescriptor().createImage();
		}
		fImagesToDispose.add(image);
		return image;
	}


	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
		IWorkbenchSiteProgressService progressService= getProgressService();
		if (progressService != null)
			progressService.showBusyForFamily(TestRunnerViewPart.FAMILY_JUNIT_RUN);
	}

	private IWorkbenchSiteProgressService getProgressService() {
		Object siteService= getSite().getAdapter(IWorkbenchSiteProgressService.class);
		if (siteService != null)
			return (IWorkbenchSiteProgressService) siteService;
		return null;
	}


	@Override
	public void saveState(IMemento memento) {
		if (fSashForm == null) {
			// part has not been created
			if (fMemento != null) //Keep the old state;
				memento.putMemento(fMemento);
			return;
		}

//		int activePage= fTabFolder.getSelectionIndex();
//		memento.putInteger(TAG_PAGE, activePage);
		memento.putString(TAG_SCROLL, fScrollLockAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		int weigths[]= fSashForm.getWeights();
		int ratio= (weigths[0] * 1000) / (weigths[0] + weigths[1]);
		memento.putInteger(TAG_RATIO, ratio);
		memento.putInteger(TAG_ORIENTATION, fOrientation);

		memento.putString(TAG_FAILURES_ONLY, fFailuresOnlyFilterAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putString(TAG_IGNORED_ONLY, fIgnoredOnlyFilterAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putInteger(TAG_LAYOUT, fLayout);
		memento.putString(TAG_SHOW_TIME, fShowTimeAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putInteger(TAG_SORTING_CRITERION, fSortingCriterion.ordinal());
	}

	private void restoreLayoutState(IMemento memento) {
//		Integer page= memento.getInteger(TAG_PAGE);
//		if (page != null) {
//			int p= page.intValue();
//			if (p < fTestRunTabs.size()) { // tab count can decrease if a contributing plug-in is removed
//				fTabFolder.setSelection(p);
//				fActiveRunTab= (TestRunTab)fTestRunTabs.get(p);
//			}
//		}
		Integer ratio= memento.getInteger(TAG_RATIO);
		if (ratio != null)
			fSashForm.setWeights(new int[] { ratio, 1000 - ratio} );
		Integer orientation= memento.getInteger(TAG_ORIENTATION);
		if (orientation != null)
			fOrientation= orientation;
		computeOrientation();
		String scrollLock= memento.getString(TAG_SCROLL);
		if (scrollLock != null) {
			fScrollLockAction.setChecked("true".equals(scrollLock)); //$NON-NLS-1$
			setAutoScroll(!fScrollLockAction.isChecked());
		}

		Integer layout= memento.getInteger(TAG_LAYOUT);
		int layoutValue= LAYOUT_HIERARCHICAL;
		if (layout != null)
			layoutValue= layout;

		String failuresOnly= memento.getString(TAG_FAILURES_ONLY);
		boolean showFailuresOnly= false;
		if (failuresOnly != null)
			showFailuresOnly= "true".equals(failuresOnly); //$NON-NLS-1$

		String ignoredOnly= memento.getString(TAG_IGNORED_ONLY);
		boolean showIgnoredOnly= false;
		if (ignoredOnly != null)
			showIgnoredOnly= "true".equals(ignoredOnly); //$NON-NLS-1$

		String time= memento.getString(TAG_SHOW_TIME);
		boolean showTime= true;
		if (time != null)
			showTime= "true".equals(time); //$NON-NLS-1$

		SortingCriterion sortingCriterion= SortingCriterion.SORT_BY_EXECUTION_ORDER;
		Integer tagSortingCriterion= memento.getInteger(TAG_SORTING_CRITERION);
		if (tagSortingCriterion != null) {
			sortingCriterion= SortingCriterion.values() [tagSortingCriterion];
		}
		setSortingCriterion(sortingCriterion);
		for (ToggleSortingAction fToggleSortingAction : fToggleSortingActions) {
			fToggleSortingAction.setChecked(sortingCriterion == fToggleSortingAction.getActionSortingCriterion());
		}

		setFilterAndLayout(showFailuresOnly, showIgnoredOnly, layoutValue);
		setShowExecutionTime(showTime);
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void stopTest() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				setContentDescription(JUnitMessages.TestRunnerViewPart_message_stopping);
			}
			fTestRunSession.stopTestRun();
		}
	}

	private void startUpdateJobs() {
		postSyncProcessChanges();

		if (fUpdateJob != null) {
			return;
		}
		fJUnitIsRunningJob= new JUnitIsRunningJob(JUnitMessages.TestRunnerViewPart_wrapperJobName);
		fJUnitIsRunningLock= Job.getJobManager().newLock();
		// acquire lock while a test run is running
		// the lock is released when the test run terminates
		// the wrapper job will wait on this lock.
		fJUnitIsRunningLock.acquire();
		getProgressService().schedule(fJUnitIsRunningJob);

		fUpdateJob= new UpdateUIJob(JUnitMessages.TestRunnerViewPart_jobName);
		fUpdateJob.schedule(REFRESH_INTERVAL);
	}

	private void stopUpdateJobs() {
		if (fUpdateJob != null) {
			fUpdateJob.stop();
			fUpdateJob= null;
		}
		if (fJUnitIsRunningJob != null && fJUnitIsRunningLock != null) {
			fJUnitIsRunningLock.release();
			fJUnitIsRunningJob= null;
		}
		postSyncProcessChanges();
	}

	private void processChangesInUI() {
		if (fSashForm.isDisposed())
			return;

		doShowInfoMessage();
		refreshCounters();

		if (! fPartIsVisible)
			updateViewTitleProgress();
		else {
			updateViewIcon();
		}
		updateNextPreviousActions();

		fTestViewer.processChangesInUI();
	}

	private void updateNextPreviousActions() {
		boolean hasErrorsOrFailures= !fIgnoredOnlyFilterAction.isChecked() && hasErrorsOrFailures();
		fNextAction.setEnabled(hasErrorsOrFailures);
		fPreviousAction.setEnabled(hasErrorsOrFailures);
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void rerunTestRun() {
		if (lastLaunchIsKeptAlive()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(), JUnitMessages.TestRunnerViewPart_terminate_title, JUnitMessages.TestRunnerViewPart_terminate_message)) {
				stopTest(); // TODO: wait for termination
			}
		}

		if (fTestRunSession == null)
			return;
		ILaunch launch= fTestRunSession.getLaunch();
		if (launch == null)
			return;
		ILaunchConfiguration launchConfiguration= launch.getLaunchConfiguration();
		if (launchConfiguration == null)
			return;

		ILaunchConfiguration configuration= prepareLaunchConfigForRelaunch(launchConfiguration);
		relaunch(configuration, launch.getLaunchMode());
	}

	private ILaunchConfiguration prepareLaunchConfigForRelaunch(ILaunchConfiguration configuration) {
		try {
			String attribute= configuration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES, ""); //$NON-NLS-1$
			if (attribute.length() != 0) {
				String configName= configuration.getName();
				if (configName.endsWith(JUnitMessages.TestRunnerViewPart_failures_first_suffix)) {
					configName= configName.substring(0, configName.length() - JUnitMessages.TestRunnerViewPart_failures_first_suffix.length());
				}
				ILaunchConfigurationWorkingCopy tmp= configuration.copy(configName);
				tmp.setAttribute(JUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES, (String)null);
				return tmp;
			}
		} catch (CoreException e) {
			// fall through
		}
		return configuration;
	}

	public void rerunTestFailedFirst() {
		if (lastLaunchIsKeptAlive()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(), JUnitMessages.TestRunnerViewPart_terminate_title, JUnitMessages.TestRunnerViewPart_terminate_message)) {
				if (fTestRunSession != null)
					fTestRunSession.stopTestRun();
			}
		}
		ILaunch launch= fTestRunSession.getLaunch();
		if (launch != null && launch.getLaunchConfiguration() != null) {
				ILaunchConfiguration launchConfiguration= launch.getLaunchConfiguration();
				if (launchConfiguration != null) {
					try {
						String oldName= launchConfiguration.getName();
						String oldFailuresFilename= launchConfiguration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES, (String) null);
						String configName;
						if (oldFailuresFilename != null) {
							configName= oldName;
						} else {
							configName= Messages.format(JUnitMessages.TestRunnerViewPart_rerunFailedFirstLaunchConfigName, oldName);
						}
						ILaunchConfigurationWorkingCopy tmp= launchConfiguration.copy(configName);
						tmp.setAttribute(JUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES, createFailureNamesFile());
						relaunch(tmp, launch.getLaunchMode());
						return;
					} catch (CoreException e) {
						ErrorDialog.openError(getSite().getShell(),
							JUnitMessages.TestRunnerViewPart_error_cannotrerun, e.getMessage(), e.getStatus()
						);
					}
				}
				MessageDialog.openInformation(getSite().getShell(),
					JUnitMessages.TestRunnerViewPart_cannotrerun_title,
					JUnitMessages.TestRunnerViewPart_cannotrerurn_message
				);
		}
	}

	private void relaunch(ILaunchConfiguration configuration, String launchMode) {
		DebugUITools.launch(configuration, launchMode);
	}

	private String createFailureNamesFile() throws CoreException {
		try {
			File file= File.createTempFile("testFailures", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			TestElement[] failures= fTestRunSession.getAllFailedTestElements();
			try (BufferedWriter bw= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))){
				for (TestElement testElement : failures) {
					bw.write(testElement.getTestName());
					bw.newLine();
				}
			}
			return file.getAbsolutePath();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, JUnitPlugin.PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	public void setAutoScroll(boolean scroll) {
		fAutoScroll = scroll;
	}

	public boolean isAutoScroll() {
		return fAutoScroll;
	}

	public void selectNextFailure() {
		fTestViewer.selectFailure(true);
	}

	public void selectPreviousFailure() {
		fTestViewer.selectFailure(false);
	}

	protected void selectFirstFailure() {
		fTestViewer.selectFirstFailure();
	}

	private boolean hasErrorsOrFailures() {
		return getErrorsPlusFailures() > 0;
	}

	private int getErrorsPlusFailures() {
		if (fTestRunSession == null)
			return 0;
		else
			return fTestRunSession.getErrorCount() + fTestRunSession.getFailureCount();
	}

	private String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}

	private void handleStopped() {
		postSyncRunnable(() -> {
			if (isDisposed())
				return;
			resetViewIcon();
			fStopAction.setEnabled(false);
			updateRerunFailedFirstAction();
		});
		stopUpdateJobs();
		logMessageIfNoTests();
	}

	private void logMessageIfNoTests() {
		if (fTestRunSession != null && TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(fTestRunSession.getTestRunnerKind().getId()) && fTestRunSession.getTotalCount() == 0) {
			String msg= Messages.format(JUnitMessages.TestRunnerViewPart_error_notests_kind, fTestRunSession.getTestRunnerKind().getDisplayName());
			ILog.of(getClass()).error(msg);
		}
	}

	private void resetViewIcon() {
		fViewImage= fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewIcon() {
		if (fTestRunSession == null || fTestRunSession.isStopped() || fTestRunSession.isRunning() || fTestRunSession.getStartedCount() == 0)
			fViewImage= fOriginalViewImage;
		else if (hasErrorsOrFailures())
			fViewImage= fTestRunFailIcon;
		else
			fViewImage= fTestRunOKIcon;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewTitleProgress() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				Image progress= fProgressImages.getImage(
						fTestRunSession.getStartedCount(),
						fTestRunSession.getTotalCount(),
						fTestRunSession.getErrorCount(),
						fTestRunSession.getFailureCount());
				if (progress != fViewImage) {
					fViewImage= progress;
					firePropertyChange(IWorkbenchPart.PROP_TITLE);
				}
			} else {
				updateViewIcon();
			}
		} else {
			resetViewIcon();
		}
	}

	/**
	 * @param testRunSession new active test run session
	 * @return deactivated session, or <code>null</code> iff no session got deactivated
	 */
	private TestRunSession setActiveTestRunSession(TestRunSession testRunSession) {
/*
- State:
fTestRunSession
fTestSessionListener
Jobs
fTestViewer.processChangesInUI();
- UI:
fCounterPanel
fProgressBar
setContentDescription / fInfoMessage
setTitleToolTip
view icons
statusLine
fFailureTrace

action enablement
 */
		if (fTestRunSession == testRunSession)
			return null;

		deregisterTestSessionListener(true);

		TestRunSession deactivatedSession= fTestRunSession;

		fTestRunSession= testRunSession;
		fTestViewer.registerActiveSession(testRunSession);

		if (fSashForm.isDisposed()) {
			stopUpdateJobs();
			return deactivatedSession;
		}

		if (testRunSession == null) {
			setTitleToolTip(null);
			resetViewIcon();
			clearStatus();
			fFailureTrace.clear();

			registerInfoMessage(" "); //$NON-NLS-1$
			stopUpdateJobs();

			fStopAction.setEnabled(false);
			fRerunFailedFirstAction.setEnabled(false);
			fRerunLastTestAction.setEnabled(false);

		} else {
			if (fTestRunSession.isStarting() || fTestRunSession.isRunning() || fTestRunSession.isKeptAlive()) {
				fTestSessionListener= new TestSessionListener();
				fTestRunSession.addTestSessionListener(fTestSessionListener);
			}
			if (!fTestRunSession.isStarting() && !fShowOnErrorOnly)
				showTestResultsView();

			setTitleToolTip();

			clearStatus();
			fFailureTrace.clear();
			registerInfoMessage(BasicElementLabels.getJavaElementName(fTestRunSession.getTestRunName()));

			updateRerunFailedFirstAction();
			fRerunLastTestAction.setEnabled(fTestRunSession.getLaunch() != null);

			if (fTestRunSession.isRunning()) {
				startUpdateJobs();

				fStopAction.setEnabled(true);
				fTestViewer.setSortingCriterion(SortingCriterion.SORT_BY_EXECUTION_ORDER);

			} else /* old or fresh session: don't want jobs at this stage */ {
				stopUpdateJobs();

				fStopAction.setEnabled(fTestRunSession.isKeptAlive());
				fTestViewer.expandFirstLevel();
				setSortingCriterion(fSortingCriterion);
			}
		}
		return deactivatedSession;
	}

	private void deregisterTestSessionListener(boolean force) {
		if (fTestRunSession != null && fTestSessionListener != null && (force || !fTestRunSession.isKeptAlive())) {
			fTestRunSession.removeTestSessionListener(fTestSessionListener);
			fTestSessionListener= null;
		}
	}

	private void updateRerunFailedFirstAction() {
		boolean state= hasErrorsOrFailures() && fTestRunSession.getLaunch() != null;
		fRerunFailedFirstAction.setEnabled(state);
	}

	/**
     * @return the display name of the current test run sessions kind, or <code>null</code>
     */
    public String getTestKindDisplayName() {
    	ITestKind kind= fTestRunSession.getTestRunnerKind();
		if (!kind.isNull()) {
			return  kind.getDisplayName();
		}
		return null;
	}

	private void setTitleToolTip() {
		String testKindDisplayStr= getTestKindDisplayName();

		String testRunLabel= BasicElementLabels.getJavaElementName(fTestRunSession.getTestRunName());
		if (testKindDisplayStr != null)
			setTitleToolTip(MessageFormat.format(JUnitMessages.TestRunnerViewPart_titleToolTip, testRunLabel, testKindDisplayStr));
		else
			setTitleToolTip(testRunLabel);
	}

	@Override
	public synchronized void dispose(){
		fIsDisposed= true;
		if (fTestRunSessionListener != null)
			JUnitCorePlugin.getModel().removeTestRunSessionListener(fTestRunSessionListener);

		IHandlerService handlerService= getSite().getWorkbenchWindow().getService(IHandlerService.class);
		handlerService.deactivateHandler(fRerunLastActivation);
		handlerService.deactivateHandler(fRerunFailedFirstActivation);
		setActiveTestRunSession(null);

		if (fProgressImages != null)
			fProgressImages.dispose();
		getViewSite().getPage().removePartListener(fPartListener);

		disposeImages();
		if (fClipboard != null)
			fClipboard.dispose();
		if (fViewMenuListener != null) {
			getViewSite().getActionBars().getMenuManager().removeMenuListener(fViewMenuListener);
		}
		if (fDirtyListener != null) {
			JavaCore.removeElementChangedListener(fDirtyListener);
			fDirtyListener= null;
		}
		if (fFailureTrace != null) {
			fFailureTrace.dispose();
		}
	}

	private void disposeImages() {
		for (Image imageToDispose : fImagesToDispose) {
			imageToDispose.dispose();
		}
	}

	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}

	private void refreshCounters() {
		 // TODO: Inefficient. Either
		// - keep a boolean fHasTestRun and update only on changes, or
		// - improve components to only redraw on changes (once!).

		int startedCount;
		int ignoredCount;
		int totalCount;
		int errorCount;
		int failureCount;
		int assumptionFailureCount;
		boolean hasErrorsOrFailures;
		boolean stopped;

		if (fTestRunSession != null) {
			startedCount= fTestRunSession.getStartedCount();
			ignoredCount= fTestRunSession.getIgnoredCount();
			totalCount= fTestRunSession.getTotalCount();
			errorCount= fTestRunSession.getErrorCount();
			failureCount= fTestRunSession.getFailureCount();
			assumptionFailureCount = fTestRunSession.getAssumptionFailureCount();
			hasErrorsOrFailures= errorCount + failureCount > 0;
			stopped= fTestRunSession.isStopped();
		} else {
			startedCount= 0;
			ignoredCount= 0;
			totalCount= 0;
			errorCount= 0;
			failureCount= 0;
			assumptionFailureCount = 0;
			hasErrorsOrFailures= false;
			stopped= false;
		}

		fCounterPanel.setTotal(totalCount);
		fCounterPanel.setRunValue(startedCount, ignoredCount, assumptionFailureCount);
		fCounterPanel.setErrorValue(errorCount);
		fCounterPanel.setFailureValue(failureCount);

		int ticksDone;
		if (startedCount == 0)
			ticksDone= 0;
		else if (startedCount == totalCount && ! fTestRunSession.isRunning())
			ticksDone= totalCount;
		else
			ticksDone= startedCount - 1;

		fProgressBar.reset(hasErrorsOrFailures, stopped, ticksDone, totalCount);
	}

	protected void postShowTestResultsView() {
		postSyncRunnable(() -> {
			if (isDisposed())
				return;
			showTestResultsView();
		});
	}

	public void showTestResultsView() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		TestRunnerViewPart testRunner= null;

		if (page != null) {
			try { // show the result view
				testRunner= (TestRunnerViewPart)page.findView(TestRunnerViewPart.NAME);
				if(testRunner == null) {
					IWorkbenchPart activePart= page.getActivePart();
					testRunner= (TestRunnerViewPart)page.showView(TestRunnerViewPart.NAME, null, IWorkbenchPage.VIEW_VISIBLE);
					//restore focus
					page.activate(activePart);
				} else {
					page.bringToTop(testRunner);
				}
			} catch (PartInitException pie) {
				JUnitPlugin.log(pie);
			}
		}
	}

	protected void doShowInfoMessage() {
		if (fInfoMessage != null) {
			setContentDescription(fInfoMessage);
			fInfoMessage= null;
		}
	}

	protected void registerInfoMessage(String message) {
		fInfoMessage= message;
	}

	private SashForm createSashForm(Composite parent) {
		fSashForm= new SashForm(parent, SWT.VERTICAL);

		ViewForm top= new ViewForm(fSashForm, SWT.NONE);

		Composite empty= new Composite(top, SWT.NONE);
		empty.setLayout(new Layout() {
			@Override
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				return new Point(1, 1); // (0, 0) does not work with super-intelligent ViewForm
			}
			@Override
			protected void layout(Composite composite, boolean flushCache) {
			}
		});
		top.setTopLeft(empty); // makes ViewForm draw the horizontal separator line ...
		fTestViewer= new TestViewer(top, fClipboard, this);
		top.setContent(fTestViewer.getTestViewerControl());

		ViewForm bottom= new ViewForm(fSashForm, SWT.NONE);

		CLabel label= new CLabel(bottom, SWT.NONE);
		label.setText(JUnitMessages.TestRunnerViewPart_label_failure);
		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);
		ToolBar failureToolBar= new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		fFailureTrace= new FailureTrace(bottom, fClipboard, this, failureToolBar);
		bottom.setContent(fFailureTrace.getComposite());

		fSashForm.setWeights(new int[]{50, 50});
		return fSashForm;
	}

	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}

	@Override
	public void setFocus() {
		if (fTestViewer != null)
			fTestViewer.getTestViewerControl().setFocus();
	}

	@Override
	public void createPartControl(Composite parent) {
		fParent= parent;
		addResizeListener(parent);
		fClipboard= new Clipboard(parent.getDisplay());

		GridLayout gridLayout= new GridLayout();
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		parent.setLayout(gridLayout);

		fViewHistory= new RunnerViewHistory();
		configureToolBar();

		fCounterComposite= createProgressCountPanel(parent);
		fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		SashForm sashForm= createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		IActionBars actionBars= getViewSite().getActionBars();

		fCopyAction = new JUnitCopyAction(fFailureTrace, fClipboard);
		fCopyAction.setActionDefinitionId(ActionFactory.COPY.getCommandId());
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);

		fPasteAction= new JUnitPasteAction(parent.getShell(), fClipboard);
		fPasteAction.setActionDefinitionId(ActionFactory.PASTE.getCommandId());
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);

		initPageSwitcher();
		addDropAdapter(parent);

		fOriginalViewImage= getTitleImage();
		fProgressImages= new ProgressImages();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJUnitHelpContextIds.RESULTS_VIEW);

		getViewSite().getPage().addPartListener(fPartListener);

		setFilterAndLayout(false, false, LAYOUT_HIERARCHICAL);
		setShowExecutionTime(true);
		if (fMemento != null) {
			restoreLayoutState(fMemento);
		}
		fMemento= null;

		fTestRunSessionListener= new TestRunSessionListener();
		JUnitCorePlugin.getModel().addTestRunSessionListener(fTestRunSessionListener);

		// always show youngest test run in view. simulate "sessionAdded" event to do that
		List<TestRunSession> testRunSessions= JUnitCorePlugin.getModel().getTestRunSessions();
		if (!testRunSessions.isEmpty()) {
			fTestRunSessionListener.sessionAdded(testRunSessions.get(0));
		}
	}

	private void addDropAdapter(Composite parent) {
		DropTarget dropTarget = new DropTarget(parent, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT);
		dropTarget.setTransfer(TextTransfer.getInstance());
		class DropAdapter extends DropTargetAdapter {
		    @Override
			public void dragEnter(DropTargetEvent event) {
		        event.detail = DND.DROP_COPY;
		        event.feedback = DND.FEEDBACK_NONE;
		    }
		    @Override
			public void dragOver(DropTargetEvent event) {
		        event.detail = DND.DROP_COPY;
		        event.feedback = DND.FEEDBACK_NONE;
		    }
		    @Override
			public void dragOperationChanged(DropTargetEvent event) {
		        event.detail = DND.DROP_COPY;
		        event.feedback = DND.FEEDBACK_NONE;
		    }
		    @Override
			public void drop(final DropTargetEvent event) {
		        if (TextTransfer.getInstance().isSupportedType(event.currentDataType)) {
					String url= (String) event.data;
					importTestRunSession(url);
		        }
		    }
		}
		dropTarget.addDropListener(new DropAdapter());
	}

	private void initPageSwitcher() {
		@SuppressWarnings("unused")
		PageSwitcher pageSwitcher= new PageSwitcher(this) {
			@Override
			public Object[] getPages() {
				return fViewHistory.getHistoryEntries().toArray();
			}

			@Override
			public String getName(Object page) {
				return fViewHistory.getText((TestRunSession) page);
			}

			@Override
			public ImageDescriptor getImageDescriptor(Object page) {
				return fViewHistory.getImageDescriptor(page);
			}

			@Override
			public void activatePage(Object page) {
				fViewHistory.setActiveEntry((TestRunSession) page);
			}

			@Override
			public int getCurrentPageIndex() {
				return fViewHistory.getHistoryEntries().indexOf(fViewHistory.getCurrentEntry());
			}
		};
	}

	private void addResizeListener(Composite parent) {
		parent.addControlListener(new ControlListener() {
			@Override
			public void controlMoved(ControlEvent e) {
			}
			@Override
			public void controlResized(ControlEvent e) {
				computeOrientation();
			}
		});
	}

	void computeOrientation() {
		if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
			fCurrentOrientation= fOrientation;
			setOrientation(fCurrentOrientation);
		}
		else {
			Point size= fParent.getSize();
			if (size.x != 0 && size.y != 0) {
				if (size.x > size.y)
					setOrientation(VIEW_ORIENTATION_HORIZONTAL);
				else
					setOrientation(VIEW_ORIENTATION_VERTICAL);
			}
		}
	}

	private void configureToolBar() {
		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		IMenuManager viewMenu = actionBars.getMenuManager();

		fNextAction= new ShowNextFailureAction(this);
		fNextAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNextAction);

		fPreviousAction= new ShowPreviousFailureAction(this);
		fPreviousAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPreviousAction);

		fStopAction= new StopAction();
		fStopAction.setEnabled(false);

		fRerunLastTestAction= new RerunLastAction();
		IHandlerService handlerService= getSite().getWorkbenchWindow().getService(IHandlerService.class);
		IHandler handler = new AbstractHandler() {
			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {
				fRerunLastTestAction.run();
				return null;
			}
			@Override
			public boolean isEnabled() {
				return fRerunLastTestAction.isEnabled();
			}
		};
        fRerunLastActivation= handlerService.activateHandler(RERUN_LAST_COMMAND, handler);

		fRerunFailedFirstAction= new RerunLastFailedFirstAction();
		handler = new AbstractHandler() {
			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {
				fRerunFailedFirstAction.run();
				return null;
			}
			@Override
			public boolean isEnabled() {
				return fRerunFailedFirstAction.isEnabled();
			}
		};
		fRerunFailedFirstActivation= handlerService.activateHandler(RERUN_FAILED_FIRST_COMMAND, handler);

		fFailuresOnlyFilterAction= new FailuresOnlyFilterAction();
		fIgnoredOnlyFilterAction= new IgnoredOnlyFilterAction();

		fScrollLockAction= new ScrollLockAction(this);
		fScrollLockAction.setChecked(!fAutoScroll);

		fToggleOrientationActions =
			new ToggleOrientationAction[] {
				new ToggleOrientationAction(VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_AUTOMATIC)};

		fShowTestHierarchyAction= new ShowTestHierarchyAction();
		fShowTimeAction= new ShowTimeAction();

		toolBar.add(fNextAction);
		toolBar.add(fPreviousAction);
		toolBar.add(fFailuresOnlyFilterAction);
		toolBar.add(fIgnoredOnlyFilterAction);
		toolBar.add(fScrollLockAction);
		toolBar.add(new Separator());
		toolBar.add(fRerunLastTestAction);
		toolBar.add(fRerunFailedFirstAction);
		toolBar.add(fStopAction);
		toolBar.add(fViewHistory.createHistoryDropDownAction());


		viewMenu.add(fShowTestHierarchyAction);
		viewMenu.add(fShowTimeAction);
		viewMenu.add(new Separator());

		fToggleSortingActions=
				new ToggleSortingAction[] {
						new ToggleSortingAction(SortingCriterion.SORT_BY_EXECUTION_ORDER),
						new ToggleSortingAction(SortingCriterion.SORT_BY_EXECUTION_TIME),
						new ToggleSortingAction(SortingCriterion.SORT_BY_NAME)};
		fSortByMenu= new MenuManager(JUnitMessages.TestRunnerViewPart_sort_by_menu);
		for (ToggleSortingAction fToggleSortingAction : fToggleSortingActions) {
			fSortByMenu.add(fToggleSortingAction);
		}
		viewMenu.add(fSortByMenu);
		viewMenu.add(new Separator());

		MenuManager layoutSubMenu= new MenuManager(JUnitMessages.TestRunnerViewPart_layout_menu);
		for (ToggleOrientationAction toggleOrientationAction : fToggleOrientationActions) {
			layoutSubMenu.add(toggleOrientationAction);
		}
		viewMenu.add(layoutSubMenu);
		viewMenu.add(new Separator());

		viewMenu.add(fFailuresOnlyFilterAction);
		viewMenu.add(fIgnoredOnlyFilterAction);


		fActivateOnErrorAction= new ActivateOnErrorAction();
		viewMenu.add(fActivateOnErrorAction);
		fViewMenuListener= manager -> fActivateOnErrorAction.update();

		viewMenu.addMenuListener(fViewMenuListener);

		actionBars.updateActionBars();
	}

	private IStatusLineManager getStatusLine() {
		// we want to show messages globally hence we
		// have to go through the active part
		IViewSite site= getViewSite();
		IWorkbenchPage page= site.getPage();
		IWorkbenchPart activePart= page.getActivePart();

		if (activePart instanceof IViewPart) {
			IViewPart activeViewPart= (IViewPart)activePart;
			IViewSite activeViewSite= activeViewPart.getViewSite();
			return activeViewSite.getActionBars().getStatusLineManager();
		}

		if (activePart instanceof IEditorPart) {
			IEditorPart activeEditorPart= (IEditorPart)activePart;
			IEditorActionBarContributor contributor= activeEditorPart.getEditorSite().getActionBarContributor();
			if (contributor instanceof EditorActionBarContributor)
				return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
		// no active part
		return getViewSite().getActionBars().getStatusLineManager();
	}

	protected Composite createProgressCountPanel(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);
		setCounterColumns(layout);

		fCounterPanel = new CounterPanel(composite);
		fCounterPanel.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fProgressBar = new JUnitProgressBar(composite);
		fProgressBar.setLayoutData(
				new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}

	public void handleTestSelected(TestElement test) {
		showFailure(test);
		fCopyAction.handleTestSelected(test);
	}

	private void showFailure(final TestElement test) {
		postSyncRunnable(() -> {
			if (!isDisposed())
				fFailureTrace.showFailure(test);
		});
	}

	/**
	 * @return the Java project, or <code>null</code>
	 */
	public IJavaProject getLaunchedProject() {
		return fTestRunSession == null ? null : fTestRunSession.getLaunchedProject();
	}

	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}

	private Display getDisplay() {
		return getViewSite().getShell().getDisplay();
	}

	/*
	 * @see IWorkbenchPart#getTitleImage()
	 */
	@Override
	public Image getTitleImage() {
		if (fOriginalViewImage == null)
			fOriginalViewImage= super.getTitleImage();

		if (fViewImage == null)
			return super.getTitleImage();
		return fViewImage;
	}

	void codeHasChanged() {
		if (fDirtyListener != null) {
			JavaCore.removeElementChangedListener(fDirtyListener);
			fDirtyListener= null;
		}
		if (fViewImage == fTestRunOKIcon)
			fViewImage= fTestRunOKDirtyIcon;
		else if (fViewImage == fTestRunFailIcon)
			fViewImage= fTestRunFailDirtyIcon;

		Runnable r= () -> {
			if (isDisposed())
				return;
			firePropertyChange(IWorkbenchPart.PROP_TITLE);
		};
		if (!isDisposed())
			getDisplay().asyncExec(r);
	}

	public void rerunTest(String testId, String className, String testName, String testDisplayName, String uniqueId, String launchMode) {
		if (lastLaunchIsKeptAlive()) {
			fTestRunSession.rerunTest(testId, className, testName);
			TestCaseElement testCaseElement= (TestCaseElement) fTestRunSession.getTestElement(testId);
			testCaseElement.setStatus(TestElement.Status.RUNNING, null, null, null);
			fTestViewer.registerViewerUpdate(testCaseElement);
			postSyncProcessChanges();
			return;
		}

		if (fTestRunSession != null) {
			ILaunch launch= fTestRunSession.getLaunch();
			if (launch != null) {
				// run the selected test using the previous launch configuration
				ILaunchConfiguration launchConfiguration= launch.getLaunchConfiguration();
				if (launchConfiguration != null) {
					try {
						String name;
						if (testDisplayName != null) {
							name= testDisplayName;
						} else {
							name= className;
							if (testName != null)
								name+= "."+testName; //$NON-NLS-1$
						}
						String configName= Messages.format(JUnitMessages.TestRunnerViewPart_configName, name);
						ILaunchConfigurationWorkingCopy tmp = launchConfiguration.copy(configName);
						// fix for bug: 64838  junit view run single test does not use correct class [JUnit]
						tmp.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, className);
						// reset the container
						tmp.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
						tmp.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_NAME, testName);
						tmp.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_UNIQUE_ID, uniqueId);
						relaunch(tmp, launchMode);
						return;
						} catch (CoreException e) {
							ErrorDialog.openError(getSite().getShell(),
									JUnitMessages.TestRunnerViewPart_error_cannotrerun, e.getMessage(), e.getStatus()
								);
							return;
						}
				}
			}
		}

		MessageDialog.openInformation(getSite().getShell(),
				JUnitMessages.TestRunnerViewPart_cannotrerun_title,
				JUnitMessages.TestRunnerViewPart_cannotrerurn_message);
	}

	private void postSyncProcessChanges() {
		postSyncRunnable(this::processChangesInUI);
	}

	public void warnOfContentChange() {
		IWorkbenchSiteProgressService service= getProgressService();
		if (service != null)
			service.warnOfContentChange();
	}

	public boolean lastLaunchIsKeptAlive() {
		return fTestRunSession != null && fTestRunSession.isKeptAlive();
	}

	private void setOrientation(int orientation) {
		if ((fSashForm == null) || fSashForm.isDisposed())
			return;
		boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
		fSashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		for (ToggleOrientationAction toggleOrientationAction : fToggleOrientationActions)
			toggleOrientationAction.setChecked(fOrientation == toggleOrientationAction.getOrientation());
		fCurrentOrientation = orientation;
		GridLayout layout= (GridLayout) fCounterComposite.getLayout();
		setCounterColumns(layout);
		fParent.layout();
	}

	private void setCounterColumns(GridLayout layout) {
		if (fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL)
			layout.numColumns= 2;
		else
			layout.numColumns= 1;
	}

	static boolean getShowOnErrorOnly() {
		return Platform.getPreferencesService().getBoolean(JUnitCorePlugin.CORE_PLUGIN_ID, JUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, false, null);
	}

	static void importTestRunSession(final String url) {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> JUnitModel.importTestRunSession(url, monitor));
		} catch (InterruptedException e) {
			// cancelled
		} catch (InvocationTargetException e) {
			CoreException ce= (CoreException) e.getCause();
			StatusManager.getManager().handle(ce.getStatus(), StatusManager.SHOW | StatusManager.LOG);
		}
	}

	public FailureTrace getFailureTrace() {
		return fFailureTrace;
	}

	public TestViewer getTestViewer() {
		return fTestViewer;
	}

	public TestRunSession getTestRunSession() {
		return fTestRunSession;
	}

	void setShowFailuresOnly(boolean failuresOnly) {
		setFilterAndLayout(failuresOnly, false /*ignoredOnly must be off*/, fLayout);
	}

	void setShowIgnoredOnly(boolean ignoredOnly) {
		setFilterAndLayout( false /*failuresOnly must be off*/, ignoredOnly, fLayout);
	}

	public void setLayoutMode(int mode) {
		setFilterAndLayout(fFailuresOnlyFilterAction.isChecked(), fIgnoredOnlyFilterAction.isChecked(), mode);
	}

	private void setFilterAndLayout(boolean failuresOnly, boolean ignoredOnly,  int layoutMode) {
		fShowTestHierarchyAction.setChecked(layoutMode == LAYOUT_HIERARCHICAL);
		fLayout= layoutMode;
		fFailuresOnlyFilterAction.setChecked(failuresOnly);
		fIgnoredOnlyFilterAction.setChecked(ignoredOnly);
		fTestViewer.setShowFailuresOrIgnoredOnly(failuresOnly, ignoredOnly, layoutMode);
		updateNextPreviousActions();
	}

	private void setShowExecutionTime(boolean showTime) {
		fTestViewer.setShowTime(showTime);
		fShowTimeAction.setChecked(showTime);

	}

	TestElement[] getAllFailures() {
		return fTestRunSession.getAllFailedTestElements();
	}
}
