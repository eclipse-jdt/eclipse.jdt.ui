/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com see bug 25324 Ability to know when tests are finished [junit] 
 *     Vincent Massol: vmassol@octo.com 25324 Ability to know when tests are finished [junit]
 *     Sebastian Davids: sdavids@gmx.de 35762 JUnit View wasting a lot of screen space [JUnit]
 ******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

/**
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart implements ITestRunListener3, IPropertyChangeListener {

	public static final String NAME= "org.eclipse.jdt.junit.ResultView"; //$NON-NLS-1$
	public static final String ID_EXTENSION_POINT_TESTRUN_TABS= JUnitPlugin.PLUGIN_ID + "." + "internal-testRunTabs"; //$NON-NLS-1$ //$NON-NLS-2$

	static final int REFRESH_INTERVAL= 200;
 	/**
 	 * Number of executed tests during a test run
 	 */
	protected volatile int fExecutedTests;
	/**
	 * Number of errors during this test run
	 */
	protected volatile int fErrorCount;
	/**
	 * Number of failures during this test run
	 */
	protected volatile int fFailureCount;
	/**
	 * Number of tests run
	 */
	protected volatile int fTestCount;
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
	 * Map storing TestInfos for each executed test keyed by
	 * the test name.
	 */
	private Map fTestInfos= new HashMap();
	/**
	 * The first failure of a test run. Used to reveal the
	 * first failed tests at the end of a run.
	 */
	private List fFailures= new ArrayList();

	protected JUnitProgressBar fProgressBar;
	protected ProgressImages fProgressImages;
	protected Image fViewImage;
	protected CounterPanel fCounterPanel;
	protected boolean fShowOnErrorOnly= false;
	protected Clipboard fClipboard;
	protected volatile String fStatus;

	/** 
	 * The tab that shows the stack trace of a failure
	 */
	private FailureTrace fFailureTrace;
	/** 
	 * The collection of ITestRunTabs
	 */
	protected Vector fTestRunTabs = new Vector();
	/**
	 * The currently active run tab
	 */
	private TestRunTab fActiveRunTab;
	/**
	 * Is the UI disposed
	 */
	private boolean fIsDisposed= false;
	/**
	 * The launched project
	 */
	private IJavaProject fTestProject;
	/**
	 * The launcher that has started the test
	 */
	private String fLaunchMode;
	private ILaunch fLastLaunch;
	
	/**
	 * Actions
	 */
	private Action fRerunLastTestAction;
	private ScrollLockAction fScrollLockAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
	
	/**
	 * The client side of the remote test runner
	 */
	private RemoteTestRunnerClient fTestRunnerClient;

	final Image fStackViewIcon= TestRunnerViewPart.createImage("eview16/stackframe.gif");//$NON-NLS-1$
	final Image fTestRunOKIcon= TestRunnerViewPart.createImage("eview16/junitsucc.gif"); //$NON-NLS-1$
	final Image fTestRunFailIcon= TestRunnerViewPart.createImage("eview16/juniterr.gif"); //$NON-NLS-1$
	final Image fTestRunOKDirtyIcon= TestRunnerViewPart.createImage("eview16/junitsuccq.gif"); //$NON-NLS-1$
	final Image fTestRunFailDirtyIcon= TestRunnerViewPart.createImage("eview16/juniterrq.gif"); //$NON-NLS-1$
	
	// Persistence tags.
	static final String TAG_PAGE= "page"; //$NON-NLS-1$
	static final String TAG_RATIO= "ratio"; //$NON-NLS-1$
	static final String TAG_TRACEFILTER= "tracefilter"; //$NON-NLS-1$ 
	static final String TAG_ORIENTATION= "orientation"; //$NON-NLS-1$
	static final String TAG_SCROLL= "scroll"; //$NON-NLS-1$
	
	//orientations
	static final int VIEW_ORIENTATION_VERTICAL= 0;
	static final int VIEW_ORIENTATION_HORIZONTAL= 1;
	static final int VIEW_ORIENTATION_AUTOMATIC= 2;
	
	private IMemento fMemento;	

	Image fOriginalViewImage;
	IElementChangedListener fDirtyListener;
	
	
	private CTabFolder fTabFolder;
	private SashForm fSashForm;
	
	private Action fNextAction;
	private Action fPreviousAction;
	private Composite fCounterComposite;
	private Composite fParent;
	private UpdateUIJob fUpdateJob;

	private StopAction fStopAction;


	
	private class StopAction extends Action {
		public StopAction() {
			setText(JUnitMessages.getString("TestRunnerViewPart.stopaction.text"));//$NON-NLS-1$
			setToolTipText(JUnitMessages.getString("TestRunnerViewPart.stopaction.tooltip"));//$NON-NLS-1$
			setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/stop.gif")); //$NON-NLS-1$
			setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/stop.gif")); //$NON-NLS-1$
			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/stop.gif")); //$NON-NLS-1$
		}

		public void run() {
			stopTest();
			setEnabled(false);
		}
	}

	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText(JUnitMessages.getString("TestRunnerViewPart.rerunaction.label")); //$NON-NLS-1$
			setToolTipText(JUnitMessages.getString("TestRunnerViewPart.rerunaction.tooltip")); //$NON-NLS-1$
			setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/relaunch.gif")); //$NON-NLS-1$
			setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
		}
		
		public void run(){
			rerunTestRun();
		}
	}
	
	private class ToggleOrientationAction extends Action {
		private final int fActionOrientation;
		
		public ToggleOrientationAction(TestRunnerViewPart v, int orientation) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
				setText(JUnitMessages.getString("TestRunnerViewPart.toggle.horizontal.label")); //$NON-NLS-1$
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$				
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
				setText(JUnitMessages.getString("TestRunnerViewPart.toggle.vertical.label")); //$NON-NLS-1$
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$				
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
				setText(JUnitMessages.getString("TestRunnerViewPart.toggle.automatic.label"));  //$NON-NLS-1$
				setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$				
			}
			fActionOrientation= orientation;
			WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
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
					// If we did some different than changing a child we flush the the undo / redo stack.
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
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
	}

	private void restoreLayoutState(IMemento memento) {
		Integer page= memento.getInteger(TAG_PAGE);
		if (page != null) {
			int p= page.intValue();
			fTabFolder.setSelection(p);
			fActiveRunTab= (TestRunTab)fTestRunTabs.get(p);
		}
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
	}
	
	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void stopTest() {
		if (fTestRunnerClient != null)
			fTestRunnerClient.stopTest();
		stopUpdateJob();
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void rerunTestRun() {
		if (lastLaunchIsKeptAlive()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(), JUnitMessages.getString("TestRunnerViewPart.terminate.title"), JUnitMessages.getString("TestRunnerViewPart.terminate.message"))) {  //$NON-NLS-1$ //$NON-NLS-2$
				if (fTestRunnerClient != null)
					fTestRunnerClient.stopTest();
			}
		}
		if (fLastLaunch != null && fLastLaunch.getLaunchConfiguration() != null) {
			DebugUITools.launch(fLastLaunch.getLaunchConfiguration(), fLastLaunch.getLaunchMode());
		}
	}

	public void setAutoScroll(boolean scroll) {
		fAutoScroll = scroll;
	}
	
	public boolean isAutoScroll() {
		return fAutoScroll;
	}	

	/*
	 * @see ITestRunListener#testRunStarted(testCount)
	 */
	public void testRunStarted(final int testCount){
		reset(testCount);
		fShowOnErrorOnly= JUnitPreferencePage.getShowOnErrorOnly();
		fExecutedTests++;
		stopUpdateJob();
		fUpdateJob= new UpdateUIJob(JUnitMessages.getString("TestRunnerViewPart.jobName")); //$NON-NLS-1$  
		fUpdateJob.schedule(REFRESH_INTERVAL);
	}
	
	public void selectNextFailure() {
		fActiveRunTab.selectNext();
	}
	
	public void selectPreviousFailure() {
		fActiveRunTab.selectPrevious();
	}

	public void showTest(TestRunInfo test) {
		fActiveRunTab.setSelectedTest(test.getTestId());
		handleTestSelected(test.getTestId());
		new OpenTestAction(this, test.getClassName(), test.getTestMethodName()).run();
	}

	
	public void reset(){
		reset(0);
		setViewPartTitle(null);
		clearStatus();
		resetViewIcon();
	}

	/*
	 * @see ITestRunListener#testRunEnded
	 */
	public void testRunEnded(long elapsedTime){
		fExecutedTests--;
		String[] keys= {elapsedTimeAsString(elapsedTime)};
		String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.finish", keys); //$NON-NLS-1$
		if (hasErrorsOrFailures())
			postError(msg);
		else
			setInfoMessage(msg);
			
		postSyncRunnable(new Runnable() {				
			public void run() {
				if(isDisposed()) 
					return;	
				fStopAction.setEnabled(lastLaunchIsKeptAlive());
				if (fFailures.size() > 0) {
					selectFirstFailure();
				}
				updateViewIcon();
				if (fDirtyListener == null) {
					fDirtyListener= new DirtyListener();
					JavaCore.addElementChangedListener(fDirtyListener);
				}
				for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
					TestRunTab v= (TestRunTab) e.nextElement();
					v.aboutToEnd();
				}
			}
		});	
		stopUpdateJob();
	}

	private void stopUpdateJob() {
		if (fUpdateJob != null) {
			fUpdateJob.stop();
			fUpdateJob= null;
		}
	}

	protected void selectFirstFailure() {
		TestRunInfo firstFailure= (TestRunInfo)fFailures.get(0);
		if (firstFailure != null && fAutoScroll) {
			fActiveRunTab.setSelectedTest(firstFailure.getTestId());
			handleTestSelected(firstFailure.getTestId());
		}
	}

	private void updateViewIcon() {
		if (hasErrorsOrFailures()) 
			fViewImage= fTestRunFailIcon;
		else 
			fViewImage= fTestRunOKIcon;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);	
	}

	private boolean hasErrorsOrFailures() {
		return fErrorCount+fFailureCount > 0;
	}

	private String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}

	/*
	 * @see ITestRunListener#testRunStopped
	 */
	public void testRunStopped(final long elapsedTime) {
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.stopped"); //$NON-NLS-1$ 
		setInfoMessage(msg);
		handleStopped();
	}

	private void handleStopped() {
		postSyncRunnable(new Runnable() {				
			public void run() {
				if(isDisposed()) 
					return;	
				resetViewIcon();
				fStopAction.setEnabled(false);
				fProgressBar.stopped();
			}
		});	
		stopUpdateJob();
	}

	private void resetViewIcon() {
		fViewImage= fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	/*
	 * @see ITestRunListener#testRunTerminated
	 */
	public void testRunTerminated() {
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.terminated"); //$NON-NLS-1$
		showMessage(msg);
		handleStopped(); 
	}

	private void showMessage(String msg) {
		//showInformation(msg);
		postError(msg);
	}

	/*
	 * @see ITestRunListener#testStarted
	 */
	public void testStarted(String testId, String testName) {
		postStartTest(testId, testName);
		// reveal the part when the first test starts
		if (!fShowOnErrorOnly && fExecutedTests == 1) 
			postShowTestResultsView();
			
		TestRunInfo testInfo= getTestInfo(testId);
		if (testInfo == null) {
			testInfo= new TestRunInfo(testId, testName);
			fTestInfos.put(testId, testInfo);
		}
		String className= testInfo.getClassName();
		String method= testInfo.getTestMethodName();		
		String status= JUnitMessages.getFormattedString("TestRunnerViewPart.message.started", new String[] { className, method }); //$NON-NLS-1$
		setInfoMessage(status); 
	}
	
	/*
	 * @see ITestRunListener#testEnded
	 */
	public void testEnded(String testId, String testName){
		postEndTest(testId, testName);
		fExecutedTests++;
	}

	/*
	 * @see ITestRunListener#testFailed
	 */
	public void testFailed(int status, String testId, String testName, String trace){
		testFailed(status, testId, testName, trace, null, null);
	}

	/*
	 * @see ITestRunListener#testFailed
	 */
	public void testFailed(int status, String testId, String testName, String trace, String expected, String actual) {
	    TestRunInfo testInfo= getTestInfo(testId);
	    if (testInfo == null) {
	        testInfo= new TestRunInfo(testId, testName);
	        fTestInfos.put(testName, testInfo);
	    }
	    testInfo.setTrace(trace);
	    testInfo.setStatus(status);
	    if (expected != null) {
			testInfo.setExpected(expected.substring(0, expected.length()-1));
		}
	    if (actual != null)
	        testInfo.setActual(actual.substring(0, actual.length()-1));
	    
	    if (status == ITestRunListener.STATUS_ERROR)
	        fErrorCount++;
	    else
	        fFailureCount++;
	    fFailures.add(testInfo);
	    // show the view on the first error only
	    if (fShowOnErrorOnly && (fErrorCount + fFailureCount == 1)) 
	        postShowTestResultsView();
	}
	
	/*
	 * @see ITestRunListener#testReran
	 */
	public void testReran(String testId, String className, String testName, int status, String trace) {
		if (status == ITestRunListener.STATUS_ERROR) {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.error", new String[]{testName, className}); //$NON-NLS-1$
			postError(msg); 
		} else if (status == ITestRunListener.STATUS_FAILURE) {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.failure", new String[]{testName, className}); //$NON-NLS-1$
			postError(msg);
		} else {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.success", new String[]{testName, className}); //$NON-NLS-1$
			setInfoMessage(msg);
		}
		TestRunInfo info= getTestInfo(testId);
		updateTest(info, status);
		if (info.getTrace() == null || !info.getTrace().equals(trace)) {
			info.setTrace(trace);
			showFailure(info);
		}
	}

	public void testReran(String testId, String className, String testName, int statusCode, String trace, String expectedResult, String actualResult) {
		testReran(testId, className, testName, statusCode, trace);
		TestRunInfo info= getTestInfo(testId);
		info.setActual(actualResult);
		info.setExpected(expectedResult);
		fFailureTrace.updateEnablement(info);
	}
	
	private void updateTest(TestRunInfo info, final int status) {
		if (status == info.getStatus())
			return;
		if (info.getStatus() == ITestRunListener.STATUS_OK) {
			if (status == ITestRunListener.STATUS_FAILURE) 
				fFailureCount++;
			else if (status == ITestRunListener.STATUS_ERROR)
				fErrorCount++;
		} else if (info.getStatus() == ITestRunListener.STATUS_ERROR) {
			if (status == ITestRunListener.STATUS_OK) 
				fErrorCount--;
			else if (status == ITestRunListener.STATUS_FAILURE) {
				fErrorCount--;
				fFailureCount++;
			}
		} else if (info.getStatus() == ITestRunListener.STATUS_FAILURE) {
			if (status == ITestRunListener.STATUS_OK) 
				fFailureCount--;
			else if (status == ITestRunListener.STATUS_ERROR) {
				fFailureCount--;
				fErrorCount++;
			}
		}			
		info.setStatus(status);	
		final TestRunInfo finalInfo= info;
		postSyncRunnable(new Runnable() {
			public void run() {
				//refreshCounters();
				for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
					TestRunTab v= (TestRunTab) e.nextElement();
					v.testStatusChanged(finalInfo);
				}
			}
		});
		
	}

	/*
	 * @see ITestRunListener#testTreeEntry
	 */
	public void testTreeEntry(final String treeEntry){
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
					TestRunTab v= (TestRunTab) e.nextElement();
					v.newTreeEntry(treeEntry);
				}
			}
		});	
	}

	public void startTestRunListening(IJavaElement type, int port, ILaunch launch) {
		fTestProject= type.getJavaProject();
		fLaunchMode= launch.getLaunchMode();
		aboutToLaunch();
		
		if (fTestRunnerClient != null) {
			stopTest();
		}
		fTestRunnerClient= new RemoteTestRunnerClient();
		
		// add the TestRunnerViewPart to the list of registered listeners
		List listeners= JUnitPlugin.getDefault().getTestRunListeners();	
		ITestRunListener[] listenerArray= new ITestRunListener[listeners.size()+1];
		listeners.toArray(listenerArray);
		System.arraycopy(listenerArray, 0, listenerArray, 1, listenerArray.length-1);
		listenerArray[0]= this;
		fTestRunnerClient.startListening(listenerArray, port);
		
		fLastLaunch= launch;
		setViewPartTitle(type);
		if (type instanceof IType)
			setTitleToolTip(((IType)type).getFullyQualifiedName());
		else
			setTitleToolTip(type.getElementName());
			
	}

	private void setViewPartTitle(IJavaElement type) {
		String title;
		if (type == null)
			title= " "; //$NON-NLS-1$
		else	
			title= type.getElementName();
		setContentDescription(title);
	}

	protected void aboutToLaunch() {
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.launching"); //$NON-NLS-1$
		//showInformation(msg);
		setInfoMessage(msg);
		fViewImage= fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	public synchronized void dispose(){
		fIsDisposed= true;
		stopTest();
		if (fProgressImages != null)
			fProgressImages.dispose();
		JUnitPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		fTestRunOKIcon.dispose();
		fTestRunFailIcon.dispose();
		fStackViewIcon.dispose();
		fTestRunOKDirtyIcon.dispose();
		fTestRunFailDirtyIcon.dispose();
		if (fClipboard != null) 
			fClipboard.dispose();
	}

	protected void start(final int total) {
		resetProgressBar(total);
		fCounterPanel.setTotal(total);
		fCounterPanel.setRunValue(0);	
	}

	private void resetProgressBar(final int total) {
		fProgressBar.reset();
		fProgressBar.setMaximum(total);
	}

	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}

	private void aboutToStart() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
						TestRunTab v= (TestRunTab) e.nextElement();
						v.aboutToStart();
					}
					fNextAction.setEnabled(false);
					fPreviousAction.setEnabled(false);
				}
			}
		});
	}
	
	private void postEndTest(final String testId, final String testName) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				handleEndTest();
				for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
					TestRunTab v= (TestRunTab) e.nextElement();
					v.endTest(testId);
				}
				
				if (fFailureCount + fErrorCount > 0) {
					fNextAction.setEnabled(true);
					fPreviousAction.setEnabled(true);
				}
			}
		});	
	}

	private void postStartTest(final String testId, final String testName) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
					TestRunTab v= (TestRunTab) e.nextElement();
					v.startTest(testId);
				}
			}
		});	
	}

	private void handleEndTest() {
		//refreshCounters();
		fProgressBar.step(fFailureCount+fErrorCount);
		if (fShowOnErrorOnly) {
			Image progress= fProgressImages.getImage(fExecutedTests, fTestCount, fErrorCount, fFailureCount);
			if (progress != fViewImage) {
				fViewImage= progress;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		}
	}

	private void refreshCounters() {
		fCounterPanel.setErrorValue(fErrorCount);
		fCounterPanel.setFailureValue(fFailureCount);
		fCounterPanel.setRunValue(fExecutedTests);
		fProgressBar.refresh(fErrorCount+fFailureCount> 0);
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
					//restore focus stolen by the creation of the console
					page.activate(activePart);
				} else {
					page.bringToTop(testRunner);
				}
			} catch (PartInitException pie) {
				JUnitPlugin.log(pie);
			}
		}
	}
	
	class UpdateUIJob extends UIJob {
		private boolean fRunning= true; 
		
		public UpdateUIJob(String name) {
			super(name);
			setSystem(true);
		}
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!isDisposed()) { 
				doShowStatus();
				refreshCounters();
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

	protected void doShowStatus() {
		setContentDescription(fStatus);
	}

	protected void setInfoMessage(final String message) {
		fStatus= message;
	}

	protected void postError(final String message) {
		fStatus= message;
	}

	protected void showInformation(final String info){
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fFailureTrace.setInformation(info);
			}
		});
	}

	protected CTabFolder createTestRunTabs(Composite parent) {
		CTabFolder tabFolder= new CTabFolder(parent, SWT.TOP);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		loadTestRunTabs(tabFolder);
		tabFolder.setSelection(0);				
		fActiveRunTab= (TestRunTab)fTestRunTabs.firstElement();		
				
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				testTabChanged(event);
			}
		});
		return tabFolder;
	}

	private void loadTestRunTabs(CTabFolder tabFolder) {
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_TABS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs= extensionPoint.getConfigurationElements();
		MultiStatus status= new MultiStatus(JUnitPlugin.PLUGIN_ID, IStatus.OK, "Could not load some testRunTabs extension points", null); //$NON-NLS-1$ 	

		for (int i= 0; i < configs.length; i++) {
			try {
				TestRunTab testRunTab= (TestRunTab) configs[i].createExecutableExtension("class"); //$NON-NLS-1$
				testRunTab.createTabControl(tabFolder, fClipboard, this);
				fTestRunTabs.addElement(testRunTab);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (!status.isOK()) {
			JUnitPlugin.log(status);
		}
	}

	private void testTabChanged(SelectionEvent event) {
		for (Enumeration e= fTestRunTabs.elements(); e.hasMoreElements();) {
			TestRunTab v= (TestRunTab) e.nextElement();
			if (((CTabFolder) event.widget).getSelection().getText() == v.getName()){
				v.setSelectedTest(fActiveRunTab.getSelectedTestId());
				fActiveRunTab= v;
				fActiveRunTab.activate();
			}
		}
	}

	private SashForm createSashForm(Composite parent) {
		fSashForm= new SashForm(parent, SWT.VERTICAL);
		ViewForm top= new ViewForm(fSashForm, SWT.NONE);
		fTabFolder= createTestRunTabs(top);
		fTabFolder.setLayoutData(new TabFolderLayout());
		top.setContent(fTabFolder);
		
		ViewForm bottom= new ViewForm(fSashForm, SWT.NONE);
		CLabel label= new CLabel(bottom, SWT.NONE);
		label.setText(JUnitMessages.getString("TestRunnerViewPart.label.failure")); //$NON-NLS-1$
		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);

		ToolBar failureToolBar= new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		fFailureTrace= new FailureTrace(bottom, fClipboard, this, failureToolBar);
		bottom.setContent(fFailureTrace.getComposite()); 
		
		fSashForm.setWeights(new int[]{50, 50});
		return fSashForm;
	}

	private void reset(final int testCount) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				fCounterPanel.reset();
				fFailureTrace.clear();
				fProgressBar.reset();
				fStopAction.setEnabled(true);
				clearStatus();
				start(testCount);
			}
		});
		fExecutedTests= 0;
		fFailureCount= 0;
		fErrorCount= 0;
		fTestCount= testCount;
		aboutToStart();
		fTestInfos.clear();
		fFailures= new ArrayList();
	}

	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}

    public void setFocus() {
    	if (fActiveRunTab != null)
    		fActiveRunTab.setFocus();
    }

    public void createPartControl(Composite parent) {	
    	fParent= parent;
    	addResizeListener(parent);
		fClipboard= new Clipboard(parent.getDisplay());

		GridLayout gridLayout= new GridLayout(); 
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		parent.setLayout(gridLayout);

		configureToolBar();
		
		fCounterComposite= createProgressCountPanel(parent);
		fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		SashForm sashForm= createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		IActionBars actionBars= getViewSite().getActionBars();
		actionBars.setGlobalActionHandler(
			ActionFactory.COPY.getId(),
			new CopyTraceAction(fFailureTrace, fClipboard));
		
		JUnitPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		fOriginalViewImage= getTitleImage();
		fProgressImages= new ProgressImages();
		WorkbenchHelp.setHelp(parent, IJUnitHelpContextIds.RESULTS_VIEW);
		
		if (fMemento != null) {
			restoreLayoutState(fMemento);
		}
		fMemento= null;
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

	public void saveState(IMemento memento) {
		if (fSashForm == null) {
			// part has not been created
			if (fMemento != null) //Keep the old state;
				memento.putMemento(fMemento);
			return;
		}
		
		int activePage= fTabFolder.getSelectionIndex();
		memento.putInteger(TAG_PAGE, activePage);
		memento.putString(TAG_SCROLL, fScrollLockAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		int weigths[]= fSashForm.getWeights();
		int ratio= (weigths[0] * 1000) / (weigths[0] + weigths[1]);
		memento.putInteger(TAG_RATIO, ratio);
		memento.putInteger(TAG_ORIENTATION, fOrientation);
	}
	
	private void configureToolBar() {
		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		IMenuManager viewMenu = actionBars.getMenuManager();
		fRerunLastTestAction= new RerunLastAction();
		fScrollLockAction= new ScrollLockAction(this);
		fToggleOrientationActions =
			new ToggleOrientationAction[] {
				new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC)};
		fNextAction= new ShowNextFailureAction(this);
		fPreviousAction= new ShowPreviousFailureAction(this);
		fStopAction= new StopAction();
		fNextAction.setEnabled(false);
		fPreviousAction.setEnabled(false);
		fStopAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(), fNextAction);
		actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(), fPreviousAction);
		
		toolBar.add(fNextAction);
		toolBar.add(fPreviousAction);
		toolBar.add(fStopAction);
		toolBar.add(new Separator());
		toolBar.add(fRerunLastTestAction);
		toolBar.add(fScrollLockAction);

		for (int i = 0; i < fToggleOrientationActions.length; ++i)
			viewMenu.add(fToggleOrientationActions[i]);
		
		fScrollLockAction.setChecked(!fAutoScroll);

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

	public TestRunInfo getTestInfo(String testId) {
		if (testId == null)
			return null;
		return (TestRunInfo) fTestInfos.get(testId);
	}

	public void handleTestSelected(String testId) {
		TestRunInfo testInfo= getTestInfo(testId);

		if (testInfo == null) {
			showFailure(null); //$NON-NLS-1$
		} else {
			showFailure(testInfo);
		}
	}

	private void showFailure(final TestRunInfo failure) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fFailureTrace.showFailure(failure);
			}
		});		
	}

	public IJavaProject getLaunchedProject() {
		return fTestProject;
	}
	
	public ILaunch getLastLaunch() {
		return fLastLaunch;
	}
	
	public static Image createImage(String path) {
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL(path));
			return id.createImage();
		} catch (MalformedURLException e) {
			// fall through
		}  
		return null;
	}

	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}

	private Display getDisplay() {
		return getViewSite().getShell().getDisplay();
	}
	/**
	 * @see IWorkbenchPart#getTitleImage()
	 */
	public Image getTitleImage() {
		if (fOriginalViewImage == null)
			fOriginalViewImage= super.getTitleImage();
			
		if (fViewImage == null)
			return super.getTitleImage();
		return fViewImage;
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (isDisposed())
			return;

		if (IJUnitPreferencesConstants.SHOW_ON_ERROR_ONLY.equals(event.getProperty())) {
			if (!JUnitPreferencePage.getShowOnErrorOnly()) {
				fViewImage= fOriginalViewImage;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		}
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
	
	boolean isCreated() {
		return fCounterPanel != null;
	}

	public void rerunTest(String testId, String className, String testName, String launchMode) {
		DebugUITools.saveAndBuildBeforeLaunch();
		if (lastLaunchIsKeptAlive())
			fTestRunnerClient.rerunTest(testId, className, testName);
		else if (fLastLaunch != null) {
			// run the selected test using the previous launch configuration
			ILaunchConfiguration launchConfiguration= fLastLaunch.getLaunchConfiguration();
			if (launchConfiguration != null) {
				try {
					String name= className;
					if (testName != null) 
						name+= "."+testName; //$NON-NLS-1$
					String configName= JUnitMessages.getFormattedString("TestRunnerViewPart.configName", name); //$NON-NLS-1$
					ILaunchConfigurationWorkingCopy tmp= launchConfiguration.copy(configName); 
					// fix for bug: 64838  junit view run single test does not use correct class [JUnit] 
					tmp.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, className);
					// reset the container
					tmp.setAttribute(JUnitBaseLaunchConfiguration.LAUNCH_CONTAINER_ATTR, ""); //$NON-NLS-1$
					if (testName != null) {
						tmp.setAttribute(JUnitBaseLaunchConfiguration.TESTNAME_ATTR, testName);
						//	String args= "-rerun "+testId;
						//	tmp.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, args);
					}
					tmp.launch(launchMode, null);	
					return;	
				} catch (CoreException e) {
					ErrorDialog.openError(getSite().getShell(), 
						JUnitMessages.getString("TestRunnerViewPart.error.cannotrerun"), e.getMessage(), e.getStatus() //$NON-NLS-1$
					);
				}
			}
			MessageDialog.openInformation(getSite().getShell(), 
				JUnitMessages.getString("TestRunnerViewPart.cannotrerun.title"),  //$NON-NLS-1$
				JUnitMessages.getString("TestRunnerViewPart.cannotrerurn.message") //$NON-NLS-1$
			); 
		}
	}
	
	public boolean lastLaunchIsKeptAlive() {
		return fTestRunnerClient != null && fTestRunnerClient.isRunning() && ILaunchManager.DEBUG_MODE.equals(fLaunchMode);
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


}