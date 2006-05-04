/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com see bug 25324 Ability to know when tests are finished [junit] 
 *     Vincent Massol: vmassol@octo.com 25324 Ability to know when tests are finished [junit]
 *     Sebastian Davids: sdavids@gmx.de 35762 JUnit View wasting a lot of screen space [JUnit]
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;

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

import org.eclipse.jdt.internal.ui.viewsupport.ViewHistory;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.internal.junit.launcher.TestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestRunSessionListener;
import org.eclipse.jdt.internal.junit.model.ITestSessionListener;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;

/** 
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart {

	public static final String NAME= "org.eclipse.jdt.junit.ResultView"; //$NON-NLS-1$

	static final int REFRESH_INTERVAL= 200;
	
	static final int LAYOUT_FLAT= 0;
	static final int LAYOUT_HIERARCHICAL= 1;
	
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
	 * The current layout mode with FailuresOnlyFilter enabled.
	 */
	private int fLayoutFailures= LAYOUT_FLAT;
	/**
	 * The current layout mode with FailuresOnlyFilter disabled.
	 */
	private int fLayoutAll= LAYOUT_HIERARCHICAL; 
	
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
	
	/**
	 * Actions
	 */
	private Action fNextAction;
	private Action fPreviousAction;
	
	private StopAction fStopAction;
	private JUnitCopyAction fCopyAction;
	
	private Action fRerunLastTestAction;
	private Action fRerunLastFailedFirstAction;
	
	private Action fFailuresOnlyFilterAction;
	private ScrollLockAction fScrollLockAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
	private ShowTestHierarchyAction fShowTestHierarchyAction;
	private ActivateOnErrorAction fActivateOnErrorAction;
	private IMenuListener fViewMenuListener;

	private TestRunSession fTestRunSession;
	private TestSessionListener fTestSessionListener;
	
	private RunnerViewHistory fViewHistory;
	private TestRunSessionListener fTestRunSessionListener;

	final Image fStackViewIcon= TestRunnerViewPart.createImage("eview16/stackframe.gif");//$NON-NLS-1$
	final Image fTestRunOKIcon= TestRunnerViewPart.createImage("eview16/junitsucc.gif"); //$NON-NLS-1$
	final Image fTestRunFailIcon= TestRunnerViewPart.createImage("eview16/juniterr.gif"); //$NON-NLS-1$
	final Image fTestRunOKDirtyIcon= TestRunnerViewPart.createImage("eview16/junitsuccq.gif"); //$NON-NLS-1$
	final Image fTestRunFailDirtyIcon= TestRunnerViewPart.createImage("eview16/juniterrq.gif"); //$NON-NLS-1$
	
	final Image fTestIcon= TestRunnerViewPart.createImage("obj16/test.gif"); //$NON-NLS-1$
	final Image fTestOkIcon= TestRunnerViewPart.createImage("obj16/testok.gif"); //$NON-NLS-1$
	final Image fTestErrorIcon= TestRunnerViewPart.createImage("obj16/testerr.gif"); //$NON-NLS-1$
	final Image fTestFailIcon= TestRunnerViewPart.createImage("obj16/testfail.gif"); //$NON-NLS-1$
	final Image fTestRunningIcon= TestRunnerViewPart.createImage("obj16/testrun.gif"); //$NON-NLS-1$
	
	final ImageDescriptor fSuiteIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuite.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteOkIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuiteok.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteErrorIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuiteerror.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteFailIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuitefail.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteRunningIconDescriptor= JUnitPlugin.getImageDescriptor("obj16/tsuiterun.gif"); //$NON-NLS-1$
	
	final Image fSuiteIcon= fSuiteIconDescriptor.createImage();
	final Image fSuiteOkIcon= fSuiteOkIconDescriptor.createImage();
	final Image fSuiteErrorIcon= fSuiteErrorIconDescriptor.createImage();
	final Image fSuiteFailIcon= fSuiteFailIconDescriptor.createImage();
	final Image fSuiteRunningIcon= fSuiteRunningIconDescriptor.createImage();
	
	// Persistence tags.
	static final String TAG_PAGE= "page"; //$NON-NLS-1$
	static final String TAG_RATIO= "ratio"; //$NON-NLS-1$
	static final String TAG_TRACEFILTER= "tracefilter"; //$NON-NLS-1$ 
	static final String TAG_ORIENTATION= "orientation"; //$NON-NLS-1$
	static final String TAG_SCROLL= "scroll"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_LAYOUT_FAILURES= "layoutFailures"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_LAYOUT_ALL= "layoutAll"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_FAILURES_ONLY= "failuresOnly"; //$NON-NLS-1$
	
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
		public void partActivated(IWorkbenchPartReference ref) { }
		public void partBroughtToTop(IWorkbenchPartReference ref) { }
		public void partInputChanged(IWorkbenchPartReference ref) { }
		public void partClosed(IWorkbenchPartReference ref) { }
		public void partDeactivated(IWorkbenchPartReference ref) { }
		public void partOpened(IWorkbenchPartReference ref) { }
		
		public void partVisible(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible= true;
			}
		}
		
		public void partHidden(IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible= false;
			}
		}
	};
	
	protected boolean fPartIsVisible= false;

	
	private class RunnerViewHistory extends ViewHistory {

		public void configureHistoryListAction(IAction action) {
			action.setText(JUnitMessages.TestRunnerViewPart_history);
		}

		public void configureHistoryDropDownAction(IAction action) {
			action.setToolTipText(JUnitMessages.TestRunnerViewPart_test_run_history);
			JUnitPlugin.setLocalImageDescriptors(action, "history_list.gif"); //$NON-NLS-1$
		}
		
		public Action getClearAction() {
			return new ClearAction();
		}

		public String getHistoryListDialogTitle() {
			return JUnitMessages.TestRunnerViewPart_test_runs;
		}

		public String getHistoryListDialogMessage() {
			return JUnitMessages.TestRunnerViewPart_select_test_run;
		}

		public Shell getShell() {
			return fParent.getShell();
		}

		public List getHistoryEntries() {
			return JUnitPlugin.getModel().getTestRunSessions();
		}

		public Object getCurrentEntry() {
			return fTestRunSession;
		}

		public void setActiveEntry(Object entry) {
			setActiveTestRunSession((TestRunSession) entry);
		}

		public void setHistoryEntries(List remainingEntries, Object activeEntry) {
			setActiveEntry(activeEntry);
			
			List testRunSessions= JUnitPlugin.getModel().getTestRunSessions();
			testRunSessions.removeAll(remainingEntries);
			for (Iterator iter= testRunSessions.iterator(); iter.hasNext();) {
				JUnitPlugin.getModel().removeTestRunSession((TestRunSession) iter.next());
			}
		}

		public ImageDescriptor getImageDescriptor(Object element) {
			TestRunSession session= (TestRunSession) element;
			if (session.isStopped())
				return fSuiteIconDescriptor;
				
			TestElement.Status status= session.getTestRoot().getStatus();
			if (status == TestElement.Status.NOT_RUN)
				return fSuiteIconDescriptor;
			else if (status == TestElement.Status.RUNNING)
				return fSuiteRunningIconDescriptor;
			else if (status == TestElement.Status.OK)
				return fSuiteOkIconDescriptor;
			else if (status == TestElement.Status.ERROR)
				return fSuiteErrorIconDescriptor;
			else if (status == TestElement.Status.FAILURE)
				return fSuiteFailIconDescriptor;
			else
				throw new IllegalStateException(element.toString());
		}

		public String getText(Object element) {
			TestRunSession session= (TestRunSession) element;
			if (session.getStartTime() == 0) {
				return session.getTestRunName();
			} else {
				String startTime= DateFormat.getDateTimeInstance().format(new Date(session.getStartTime()));
				return Messages.format(JUnitMessages.TestRunnerViewPart_testName_startTime, new Object[] { session.getTestRunName(), startTime });
			}
		}

		public String getMaxEntriesMessage() {
			return JUnitMessages.TestRunnerViewPart_max_remembered;
		}

		public int getMaxEntries() {
			IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
			return store.getInt(JUnitPreferencesConstants.MAX_TEST_RUNS);
		}

		public void setMaxEntries(int maxEntries) {
			IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
			store.setValue(JUnitPreferencesConstants.MAX_TEST_RUNS, maxEntries);
		}
	}

	private class TestRunSessionListener implements ITestRunSessionListener {
		public void sessionAdded(TestRunSession testRunSession) {
			if (getSite().getWorkbenchWindow() == JUnitPlugin.getActiveWorkbenchWindow()) {
				setActiveTestRunSession(testRunSession);
				setContentDescription(Messages.format(JUnitMessages.TestRunnerViewPart_Launching, new Object[]{ fTestRunSession.getTestRunName() }));
			}
		}
		public void sessionRemoved(TestRunSession testRunSession) {
			if (testRunSession.equals(fTestRunSession)) {
				List testRunSessions= JUnitPlugin.getModel().getTestRunSessions();
				if (! testRunSessions.isEmpty()) {
					setActiveTestRunSession((TestRunSession) testRunSessions.get(0));
				} else {
					setActiveTestRunSession(null);
				}
			}
		}
	}
	
	private class TestSessionListener implements ITestSessionListener {
		public void sessionStarted(){
			fTestViewer.registerViewersRefresh();
			fShowOnErrorOnly= getShowOnErrorOnly();
			
			startUpdateJobs();
			
			fStopAction.setEnabled(true);
			fRerunLastTestAction.setEnabled(true);
		}

		public void sessionEnded(long elapsedTime){
			fTestViewer.registerAutoScrollTarget(null);
			
			String[] keys= {elapsedTimeAsString(elapsedTime)};
			String msg= Messages.format(JUnitMessages.TestRunnerViewPart_message_finish, keys); 
			if (hasErrorsOrFailures())
				registerInfoMessage(msg);
			else
				registerInfoMessage(msg);
				
			postSyncRunnable(new Runnable() {				
				public void run() {
					if (isDisposed()) 
						return;	
					fStopAction.setEnabled(lastLaunchIsKeptAlive());
					fRerunLastFailedFirstAction.setEnabled(hasErrorsOrFailures());
					processChangesInUI();
					if (hasErrorsOrFailures()) {
						selectFirstFailure();
					}
					if (fDirtyListener == null) {
						fDirtyListener= new DirtyListener();
						JavaCore.addElementChangedListener(fDirtyListener);
					}
					warnOfContentChange();
				}
			});	
			stopUpdateJobs();
		}

		public void sessionStopped(final long elapsedTime) {
			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(JUnitMessages.TestRunnerViewPart_message_stopped);
			handleStopped();
		}

		public void sessionTerminated() {
			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(JUnitMessages.TestRunnerViewPart_message_terminated);
			handleStopped(); 
		}

		public void testStarted(TestCaseElement testCaseElement) {
			fTestViewer.registerAutoScrollTarget(testCaseElement);
			fTestViewer.registerViewerUpdate(testCaseElement);

			if (!fShowOnErrorOnly && fTestRunSession.getStartedCount() == 1) 
				postShowTestResultsView();
				
			String className= testCaseElement.getClassName();
			String method= testCaseElement.getTestMethodName();		
			String status= Messages.format(JUnitMessages.TestRunnerViewPart_message_started, new String[] { className, method }); 
			registerInfoMessage(status); 
		}

		public void testFailed(TestCaseElement testCaseElement, TestElement.Status status, String trace, String expected, String actual) {
			if (isAutoScroll()) {
				fTestViewer.registerFailedForAutoScroll(testCaseElement);
			}
			fTestViewer.registerViewerUpdate(testCaseElement);

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
		
		public void testEnded(TestCaseElement testCaseElement){
			fTestViewer.registerViewerUpdate(testCaseElement);
		}

		public void testReran(TestCaseElement testCaseElement, TestElement.Status status, String trace, String expectedResult, String actualResult) {
			fTestViewer.registerViewerUpdate(testCaseElement); //TODO: autoExpand?
			postSyncProcessChanges();
			showFailure(testCaseElement);
		}
		
		public void testAdded(TestElement testElement) {
			fTestViewer.registerTestAdded(testElement);
		}
	}
	
	private class UpdateUIJob extends UIJob {
		private boolean fRunning= true; 
		
		public UpdateUIJob(String name) {
			super(name);
			setSystem(true);
		}
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
		public boolean shouldSchedule() {
			return fRunning;
		}
	}

	private class JUnitIsRunningJob extends Job {
		public JUnitIsRunningJob(String name) {
			super(name);
			setSystem(true);
		}
		public IStatus run(IProgressMonitor monitor) {
			// wait until the test run terminates
			fJUnitIsRunningLock.acquire();
			return Status.OK_STATUS;
		}
		public boolean belongsTo(Object family) {
			return family == TestRunnerViewPart.FAMILY_JUNIT_RUN;
		}
	}

	private class ClearAction extends Action {
		public ClearAction() {
			setText(JUnitMessages.TestRunnerViewPart_clear_history_label);
			
			boolean enabled= false;
			List testRunSessions= JUnitPlugin.getModel().getTestRunSessions();
			for (Iterator iter= testRunSessions.iterator(); iter.hasNext();) {
				TestRunSession testRunSession= (TestRunSession) iter.next();
				if (! testRunSession.isRunning()) {
					enabled= true;
					break;
				}
			}
			setEnabled(enabled);
		}
		
		public void run() {
			List testRunSessions= getRunningSessions();
			Object first= testRunSessions.isEmpty() ? null : testRunSessions.get(0);
			fViewHistory.setHistoryEntries(testRunSessions, first);
		}

		private List getRunningSessions() {
			List testRunSessions= JUnitPlugin.getModel().getTestRunSessions();
			for (Iterator iter= testRunSessions.iterator(); iter.hasNext();) {
				TestRunSession testRunSession= (TestRunSession) iter.next();
				if (! testRunSession.isRunning()) {
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
			JUnitPlugin.setLocalImageDescriptors(this, "stop.gif"); //$NON-NLS-1$
		}

		public void run() {
			stopTest();
			setEnabled(false);
		}
	}

	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText(JUnitMessages.TestRunnerViewPart_rerunaction_label); 
			setToolTipText(JUnitMessages.TestRunnerViewPart_rerunaction_tooltip); 
			JUnitPlugin.setLocalImageDescriptors(this, "relaunch.gif"); //$NON-NLS-1$
			setEnabled(false);
		}
		
		public void run(){
			rerunTestRun();
		}
	}
	
	private class RerunLastFailedFirstAction extends Action {
		public RerunLastFailedFirstAction() {
			setText(JUnitMessages.TestRunnerViewPart_rerunfailuresaction_label);  
			setToolTipText(JUnitMessages.TestRunnerViewPart_rerunfailuresaction_tooltip);  
			JUnitPlugin.setLocalImageDescriptors(this, "relaunchf.gif"); //$NON-NLS-1$
			setEnabled(false);
		}
		
		public void run(){
			rerunTestFailedFirst();
		}
	}

	private class ToggleOrientationAction extends Action {
		private final int fActionOrientation;
		
		public ToggleOrientationAction(TestRunnerViewPart v, int orientation) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
				setText(JUnitMessages.TestRunnerViewPart_toggle_horizontal_label); 
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$				
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
				setText(JUnitMessages.TestRunnerViewPart_toggle_vertical_label); 
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$				
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
				setText(JUnitMessages.TestRunnerViewPart_toggle_automatic_label);  
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$				
			}
			fActionOrientation= orientation;
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
		}
		
		public int getOrientation() {
			return fActionOrientation;
		}
		
		public void run() {
			if (isChecked()) {
				fOrientation= fActionOrientation;
				computeOrientation();
			}
		}		
	}

	/**
	 * Listen for for modifications to Java elements
	 */
	private class DirtyListener implements IElementChangedListener {
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
	
			for (int i= 0; i < affectedChildren.length; i++) {
				if (!processDelta(affectedChildren[i]))
					return false;
			}
			return true;			
		}
	}
	
	private class FailuresOnlyFilterAction extends Action {
		public FailuresOnlyFilterAction() {
			super(JUnitMessages.TestRunnerViewPart_show_failures_only, AS_CHECK_BOX);
			setToolTipText(JUnitMessages.TestRunnerViewPart_show_failures_only);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/failures.gif")); //$NON-NLS-1$
		}

		public void run() {
			setShowFailuresOnly(isChecked());
		}
	}

	private class ShowTestHierarchyAction extends Action {

		public ShowTestHierarchyAction() {
			super(JUnitMessages.TestRunnerViewPart_hierarchical_layout, IAction.AS_CHECK_BOX);
			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/hierarchicalLayout.gif")); //$NON-NLS-1$
		}
		
		public void run() {
			int mode= isChecked() ? LAYOUT_HIERARCHICAL : LAYOUT_FLAT; 
			setLayoutMode(mode);
		}
	}
	
	private class ActivateOnErrorAction extends Action {
		public ActivateOnErrorAction() {
			super(JUnitMessages.TestRunnerViewPart_activate_on_failure_only, IAction.AS_CHECK_BOX);
			//setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/failures.gif")); //$NON-NLS-1$
			update();
		}
		public void update() {
			setChecked(getShowOnErrorOnly());
		}
		public void run() {
			boolean checked= isChecked();
			fShowOnErrorOnly= checked;
			IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
			store.setValue(JUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, checked);
		}
	}
	
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
		memento.putInteger(TAG_LAYOUT_FAILURES, fLayoutFailures);
		memento.putInteger(TAG_LAYOUT_ALL, fLayoutAll);
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
			fSashForm.setWeights(new int[] { ratio.intValue(), 1000 - ratio.intValue()} );
		Integer orientation= memento.getInteger(TAG_ORIENTATION);
		if (orientation != null)
			fOrientation= orientation.intValue();
		computeOrientation();
		String scrollLock= memento.getString(TAG_SCROLL);
		if (scrollLock != null) {
			fScrollLockAction.setChecked(scrollLock.equals("true")); //$NON-NLS-1$
			setAutoScroll(!fScrollLockAction.isChecked());
		}
		
		Integer layoutFailures= memento.getInteger(TAG_LAYOUT_FAILURES);
		if (layoutFailures != null)
			fLayoutFailures= layoutFailures.intValue();
		Integer layoutAll= memento.getInteger(TAG_LAYOUT_ALL);
		if (layoutAll != null)
			fLayoutAll= layoutAll.intValue();
		
		String failuresOnly= memento.getString(TAG_FAILURES_ONLY);
		boolean showFailuresOnly;
		if (failuresOnly == null) {
			Integer page= memento.getInteger(TAG_PAGE);
			showFailuresOnly= page == null || page.intValue() == 0;
		} else {
			showFailuresOnly= failuresOnly.equals("true"); //$NON-NLS-1$
		}
		fFailuresOnlyFilterAction.setChecked(showFailuresOnly);
		setShowFailuresOnly(showFailuresOnly);
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
		fJUnitIsRunningLock= Platform.getJobManager().newLock(); 
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
		boolean hasErrorsOrFailures= hasErrorsOrFailures();
		fNextAction.setEnabled(hasErrorsOrFailures);
		fPreviousAction.setEnabled(hasErrorsOrFailures);
		
		fTestViewer.processChangesInUI();
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
		if (fTestRunSession != null && fTestRunSession.getLaunch().getLaunchConfiguration() != null) {
			ILaunchConfiguration configuration= prepareLaunchConfigForRelaunch(fTestRunSession.getLaunch().getLaunchConfiguration());
			DebugUITools.launch(configuration, fTestRunSession.getLaunch().getLaunchMode());
		}
	}

	private ILaunchConfiguration prepareLaunchConfigForRelaunch(ILaunchConfiguration configuration) {
		try {
			String attribute= configuration.getAttribute(JUnitBaseLaunchConfiguration.FAILURES_FILENAME_ATTR, ""); //$NON-NLS-1$
			if (attribute.length() != 0) {
				String configName= Messages.format(JUnitMessages.TestRunnerViewPart_configName, configuration.getName()); 
				ILaunchConfigurationWorkingCopy tmp= configuration.copy(configName); 
				tmp.setAttribute(JUnitBaseLaunchConfiguration.FAILURES_FILENAME_ATTR, ""); //$NON-NLS-1$
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
		if (fTestRunSession.getLaunch() != null && fTestRunSession.getLaunch().getLaunchConfiguration() != null) {
				ILaunchConfiguration launchConfiguration= fTestRunSession.getLaunch().getLaunchConfiguration();
				if (launchConfiguration != null) {
					try {
						String oldName= launchConfiguration.getName(); 
						String oldFailuresFilename= launchConfiguration.getAttribute(JUnitBaseLaunchConfiguration.FAILURES_FILENAME_ATTR, (String) null);
						String configName;
						if (oldFailuresFilename != null) {
							configName= oldName;
						} else {
							configName= Messages.format(JUnitMessages.TestRunnerViewPart_rerunFailedFirstLaunchConfigName, oldName); 
						}
						ILaunchConfigurationWorkingCopy tmp= launchConfiguration.copy(configName); 
						tmp.setAttribute(JUnitBaseLaunchConfiguration.FAILURES_FILENAME_ATTR, createFailureNamesFile());
						tmp.launch(fTestRunSession.getLaunch().getLaunchMode(), null);	
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

	private String createFailureNamesFile() throws CoreException {
		try {
			File file= File.createTempFile("testFailures", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			TestCaseElement[] failures= fTestRunSession.getAllFailedTestCaseElements();
			BufferedWriter bw= null;
			try {
				bw= new BufferedWriter(new FileWriter(file));
				for (int i= 0; i < failures.length; i++) {
					TestCaseElement testCaseElement= failures[i];
					bw.write(testCaseElement.getTestName());
					bw.newLine();
				}
			} finally {
				if (bw != null) {
					bw.close();
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
		postSyncRunnable(new Runnable() {				
			public void run() {
				if (isDisposed()) 
					return;	
				resetViewIcon();
				fStopAction.setEnabled(false);
				fRerunLastFailedFirstAction.setEnabled(hasErrorsOrFailures());
			}
		});	
		stopUpdateJobs();
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
	
	private void setActiveTestRunSession(TestRunSession testRunSession) {
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
			return;
		
		if (fTestRunSession != null && fTestSessionListener != null) {
			fTestRunSession.removeTestSessionListener(fTestSessionListener);
			fTestSessionListener= null;
		}
		
		fTestRunSession= testRunSession;
		fTestViewer.registerActiveSession(testRunSession);
		
		if (fSashForm.isDisposed()) {
			stopUpdateJobs();
			return;
		}
		
		if (testRunSession == null) {
			setTitleToolTip(null);
			resetViewIcon();
			clearStatus();
			fFailureTrace.clear();
			
			registerInfoMessage(" "); //$NON-NLS-1$
			stopUpdateJobs();
			
			fStopAction.setEnabled(false);
			fRerunLastFailedFirstAction.setEnabled(false);
			fRerunLastTestAction.setEnabled(false);
			
		} else {
			fTestRunSession= testRunSession;
			fTestSessionListener= new TestSessionListener();
			fTestRunSession.addTestSessionListener(fTestSessionListener);
			
			setTitleToolTip();
						
			clearStatus();
			fFailureTrace.clear();
			registerInfoMessage(fTestRunSession.getTestRunName());
			
			fRerunLastFailedFirstAction.setEnabled(hasErrorsOrFailures());
			fRerunLastTestAction.setEnabled(true);
			
			if (fTestRunSession.isRunning()) {
				startUpdateJobs();
				
				fStopAction.setEnabled(true);
				
			} else /* old or fresh session: don't want jobs at this stage */ {
				stopUpdateJobs();
				
				fStopAction.setEnabled(fTestRunSession.isKeptAlive());
			}
		}
	}
	
	private void setTitleToolTip() {
		String testKindDisplayStr= null;
		ILaunchConfiguration config= fTestRunSession.getLaunch().getLaunchConfiguration();
		if (config != null) {
			TestKind kind= TestKindRegistry.getDefault().getKind(config);
			if (kind.isNull())
				testKindDisplayStr= getJunit3DisplayName();
			else
				testKindDisplayStr= kind.getDisplayName();
		}
		if (testKindDisplayStr != null)
			setTitleToolTip(MessageFormat.format(JUnitMessages.TestRunnerViewPart_titleToolTip, new String[] {fTestRunSession.getTestRunName(), testKindDisplayStr}));
		else
			setTitleToolTip(fTestRunSession.getTestRunName());
	}
	
	private String getJunit3DisplayName() {
		Iterator iter= TestKindRegistry.getDefault().getAllKinds().iterator();
		while (iter.hasNext()) {
			TestKind kind= (TestKind)iter.next();
			if ("org.eclipse.jdt.junit.loader.junit3".equals(kind.getId())) //$NON-NLS-1$
				return kind.getDisplayName();
		}
		return null;
	}

	public synchronized void dispose(){
		fIsDisposed= true;
		if (fTestRunSessionListener != null)
			JUnitPlugin.getModel().removeTestRunSessionListener(fTestRunSessionListener);
		
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
	}

	private void disposeImages() {
		fTestRunOKIcon.dispose();
		fTestRunFailIcon.dispose();
		fStackViewIcon.dispose();
		fTestRunOKDirtyIcon.dispose();
		fTestRunFailDirtyIcon.dispose();
		
		fTestIcon.dispose();
		fTestRunningIcon.dispose();
		fTestOkIcon.dispose();
		fTestErrorIcon.dispose();
		fTestFailIcon.dispose();
		
		fSuiteIcon.dispose();
		fSuiteRunningIcon.dispose();
		fSuiteErrorIcon.dispose();
		fSuiteFailIcon.dispose();
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
		int totalCount;
		int errorCount;
		int failureCount;
		boolean hasErrorsOrFailures;
		boolean stopped;
		
		if (fTestRunSession != null) {
			startedCount= fTestRunSession.getStartedCount();
			totalCount= fTestRunSession.getTotalCount();
			errorCount= fTestRunSession.getErrorCount();
			failureCount= fTestRunSession.getFailureCount();
			hasErrorsOrFailures= errorCount + failureCount > 0;
			stopped= fTestRunSession.isStopped();
		} else {
			startedCount= 0;
			totalCount= 0;
			errorCount= 0;
			failureCount= 0;
			hasErrorsOrFailures= false;
			stopped= false;
		}
		
		fCounterPanel.setTotal(totalCount);
		fCounterPanel.setRunValue(startedCount);
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
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				showTestResultsView();
			}
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
					testRunner= (TestRunnerViewPart)page.showView(TestRunnerViewPart.NAME);
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
			protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
				return new Point(1, 1); // (0, 0) does not work with super-intelligent ViewForm
			}
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
	
	public void setFocus() {
		if (fTestViewer != null)
			fTestViewer.getTestViewerControl().setFocus();
	}
	
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
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		
		fOriginalViewImage= getTitleImage();
		fProgressImages= new ProgressImages();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IJUnitHelpContextIds.RESULTS_VIEW);
		
		getViewSite().getPage().addPartListener(fPartListener);

		setShowFailuresOnly(true);
		if (fMemento != null) {
			restoreLayoutState(fMemento);
		}
		fMemento= null;
		
		fTestRunSessionListener= new TestRunSessionListener();
		JUnitPlugin.getModel().addTestRunSessionListener(fTestRunSessionListener);
	}

	private void addResizeListener(Composite parent) {
		parent.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent e) {
			}
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
		fRerunLastFailedFirstAction= new RerunLastFailedFirstAction();
		
		fFailuresOnlyFilterAction= new FailuresOnlyFilterAction();
		fFailuresOnlyFilterAction.setChecked(true);
		
		fScrollLockAction= new ScrollLockAction(this);
		fScrollLockAction.setChecked(!fAutoScroll);
		
		fToggleOrientationActions =
			new ToggleOrientationAction[] {
				new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC)};
		
		fShowTestHierarchyAction= new ShowTestHierarchyAction();
		
		toolBar.add(fNextAction);
		toolBar.add(fPreviousAction);
		toolBar.add(fFailuresOnlyFilterAction);
		toolBar.add(fScrollLockAction);
		toolBar.add(new Separator());
		toolBar.add(fRerunLastTestAction);
		toolBar.add(fRerunLastFailedFirstAction);
		toolBar.add(fStopAction);
		toolBar.add(fViewHistory.createHistoryDropDownAction());
		
		
		viewMenu.add(fShowTestHierarchyAction);
		viewMenu.add(new Separator());
		
		MenuManager layoutSubMenu= new MenuManager(JUnitMessages.TestRunnerViewPart_layout_menu);
		for (int i = 0; i < fToggleOrientationActions.length; ++i) {
			layoutSubMenu.add(fToggleOrientationActions[i]);
		}
		viewMenu.add(layoutSubMenu);
		viewMenu.add(new Separator());
		
		viewMenu.add(fFailuresOnlyFilterAction);
		
		
		fActivateOnErrorAction= new ActivateOnErrorAction();
		viewMenu.add(fActivateOnErrorAction);
		fViewMenuListener= new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fActivateOnErrorAction.update();
			}
		};

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
		if (test instanceof TestCaseElement) {
			showFailure((TestCaseElement) test);
		} else {
			showFailure(null); 
		}
		
		fCopyAction.handleTestSelected(test);
	}
	
	private void showFailure(final TestCaseElement test) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fFailureTrace.showFailure(test);
			}
		});		
	}

	public IJavaProject getLaunchedProject() {
		return fTestRunSession.getLaunchedType().getJavaProject();
	}
	
	public ILaunch getLastLaunch() {
		return fTestRunSession == null ? null : fTestRunSession.getLaunch();
	}
	
	public static Image createImage(String path) {
		return JUnitPlugin.getImageDescriptor(path).createImage();
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
		
		Runnable r= new Runnable() {
			public void run() {
				if (isDisposed())
					return;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		};
		if (!isDisposed())
			getDisplay().asyncExec(r);
	}
	
	public boolean isCreated() {
		return fCounterPanel != null;
	}

	public void rerunTest(String testId, String className, String testName, String launchMode) {
		DebugUITools.saveAndBuildBeforeLaunch();
		try {
			boolean couldLaunch= fTestRunSession.rerunTest(testId, className, testName, launchMode);
			if (! couldLaunch) {
				MessageDialog.openInformation(getSite().getShell(),
						JUnitMessages.TestRunnerViewPart_cannotrerun_title,
						JUnitMessages.TestRunnerViewPart_cannotrerurn_message);
			} else if (fTestRunSession.isKeptAlive()) {
				TestCaseElement testCaseElement= (TestCaseElement) fTestRunSession.getTestElement(testId);
				testCaseElement.setStatus(TestElement.Status.RUNNING);
				fTestViewer.registerViewerUpdate(testCaseElement);
				postSyncProcessChanges();
			}

		} catch (CoreException e) {
			ErrorDialog.openError(getSite().getShell(), 
				JUnitMessages.TestRunnerViewPart_error_cannotrerun, e.getMessage(), e.getStatus() 
			);
		}
	}

	private void postSyncProcessChanges() {
		postSyncRunnable(new Runnable() {
			public void run() {
				processChangesInUI();
			}
		});
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
		for (int i = 0; i < fToggleOrientationActions.length; ++i)
			fToggleOrientationActions[i].setChecked(fOrientation == fToggleOrientationActions[i].getOrientation());
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

	private static boolean getShowOnErrorOnly() {
		IPreferenceStore store= JUnitPlugin.getDefault().getPreferenceStore();
		return store.getBoolean(JUnitPreferencesConstants.SHOW_ON_ERROR_ONLY);
	}

	public FailureTrace getFailureTrace() {
		return fFailureTrace;
	}
	
	
	public void setShowFailuresOnly(boolean failuresOnly) {
		int layoutMode= failuresOnly ? fLayoutFailures : fLayoutAll;
		setLayoutMode(layoutMode);
	}
	
	private void setLayoutMode(int mode) {
		fShowTestHierarchyAction.setChecked(mode == LAYOUT_HIERARCHICAL);
		boolean failuresOnly= fFailuresOnlyFilterAction.isChecked();
		if (failuresOnly) {
			fLayoutFailures= mode;
		} else {
			fLayoutAll= mode;
		}
		fTestViewer.setShowFailuresOnly(failuresOnly, mode);
	}

	TestCaseElement[] getAllFailures() {
		return fTestRunSession.getAllFailedTestCaseElements();
	}
}
