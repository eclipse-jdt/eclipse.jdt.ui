/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ToolBar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.junit.runner.ITestRunListener;

/**
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart implements ITestRunListener {

	public static final String NAME= "org.eclipse.jdt.junit.viewpart"; //$NON-NLS-1$
 	/**
 	 * Number of executed tests during a test run
 	 */
	protected int fExecutedTests;
 	/**
 	 * Number of errors during this test run
 	 */
	protected int fErrors;
 	/**
 	 * Number of failures during this test run
 	 */
	protected int fFailures;	
	/**
	 * Map storing TestInfos for each executed test keyed by
	 * the test name.
	 */
	private Map fTestInfos= new HashMap();
	/**
	 * The first failure of a test run. Used to reveal the
	 * first failed tests at the end of a run.
	 */
	private TestRunInfo fFirstFailure;
	
	private ProgressBar fProgressBar;
	private CounterPanel fCounterPanel;
	/** 
	 * The view that shows the stack trace of a failure
	 */
	private FailureTraceView fFailureView;
	/** 
	 * The collection of ITestRunViews
	 */
	private Vector fTestRunViews= new Vector();
	/**
	 * The currently active run view
	 */
	private ITestRunView fActiveRunView;
	/**
	 * Is the UI disposed
	 */
	private boolean fIsDisposed= false;
	/**
	 * The launched test type
	 */
	private IType fTestType;
	/**
	 * The launcher that has started the test
	 */
	private String fLaunchMode;
	private ILaunch fLastLaunch= null;
	/**
	 * The client side of the remote test runner
	 */
	private RemoteTestRunnerClient fTestRunnerClient;
	
	protected final Image fHierarchyIcon= TestRunnerViewPart.createImage("icons/hierarchy.gif", getClass()); //$NON-NLS-1$
	protected final Image fStackViewIcon= TestRunnerViewPart.createImage("icons/stckframe_obj.gif", getClass()); //$NON-NLS-1$

	private class StopAction extends Action{
		public StopAction() {
			setText(JUnitMessages.getString("TestRunnerViewPart.stopaction.text")); //$NON-NLS-1$
			setToolTipText(JUnitMessages.getString("TestRunnerViewPart.stopaction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(ImageDescriptor.createFromFile(getClass(), "icons/stopIcon.gif")); //$NON-NLS-1$
		}
		
		public void run(){
			stopTest();
		}
	}
	
	private class RerunAction extends Action{
		public RerunAction() {
			setText(JUnitMessages.getString("TestRunnerViewPart.rerunaction.label")); //$NON-NLS-1$
			setToolTipText(JUnitMessages.getString("TestRunnerViewPart.rerunaction.tooltip")); //$NON-NLS-1$
			setImageDescriptor(ImageDescriptor.createFromFile(getClass(), "icons/relaunch.gif")); //$NON-NLS-1$
		}
		
		public void run(){
			rerunTestRun();
		}
	}
	
	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void stopTest() {
		if (fTestRunnerClient != null)
			fTestRunnerClient.stopTest();
	}
			
	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void rerunTestRun() {
		if (fLastLaunch != null && fLastLaunch.getLaunchConfiguration() != null) {
			try {
				fLastLaunch.getLaunchConfiguration().launch(fLastLaunch.getLaunchMode(), null);		
			} catch (CoreException e) {
				ErrorDialog.openError(getSite().getShell(), 
					JUnitMessages.getString("TestRunnerViewPart.error.cannotrerun"), e.getMessage(), e.getStatus() //$NON-NLS-1$
				);
			}
		}
	}
	
	/*
	 * @see ITestRunListener#testRunStarted(testCount)
	 */
	public void testRunStarted(final int testCount){
		reset(testCount);
		fExecutedTests++;
	}
	
	/*
	 * @see ITestRunListener#testRunEnded
	 */
	public void testRunEnded(long elapsedTime){
		fExecutedTests--;
		String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.finish", elapsedTimeAsString(elapsedTime)); //$NON-NLS-1$
		postInfo(msg);
		postAsyncRunnable(new Runnable() {				
			public void run() {
				if(isDisposed()) 
					return;	
				if (fFirstFailure != null) {
					fActiveRunView.setSelectedTest(fFirstFailure.fTestName);
					handleTestSelected(fFirstFailure.fTestName);
				}
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		});	
	}

	private String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}
	
	/*
	 * @see ITestRunListener#testRunStopped
	 */
	public void testRunStopped(final long elapsedTime) {
		String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.stopped", elapsedTimeAsString(elapsedTime)); //$NON-NLS-1$
		showMessage(msg);
	}

	/*
	 * @see ITestRunListener#testRunTerminated
	 */
	public void testRunTerminated() {
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.terminated"); //$NON-NLS-1$
		showMessage(msg);
	}

	private void showMessage(String msg) {
		showInformation(msg);
		postError(msg);
	}
		
	/*
	 * @see ITestRunListener#testStarted
	 */
	public void testStarted(String testName) {
		// reveal the part when the first test starts
		if (fExecutedTests == 1) 
			postShowTestResultsView();
			
		postInfo(JUnitMessages.getFormattedString("TestRunnerViewPart.message.started", testName)); //$NON-NLS-1$
		TestRunInfo testInfo= getTestInfo(testName);
		if (testInfo == null) 
			fTestInfos.put(testName, new TestRunInfo(testName));
	}
	
	/*
	 * @see ITestRunListener#testEnded
	 */
	public void testEnded(String testName){
		postEndTest(testName);
		fExecutedTests++;
	}
	
	/*
	 * @see ITestRunListener#testFailed
	 */
	public void testFailed(int status, String testName, String trace){
		TestRunInfo testInfo= getTestInfo(testName);
		if (testInfo == null) {
			testInfo= new TestRunInfo(testName);
			fTestInfos.put(testName, testInfo);
		}
		testInfo.fTrace= trace;
		testInfo.fStatus= status;
		if (status == ITestRunListener.STATUS_ERROR)
			fErrors++;
		else
			fFailures++;
		if (fFirstFailure == null)
			fFirstFailure= testInfo;
		// show the view on the first error only
		//if (fErrors + fFailures == 1) 
		//	postShowTestResultsView();
	}
	
	/*
	 * @see ITestRunListener#testReran
	 */
	public void testReran(String className, String testName, int status, String trace) {
		if (status == ITestRunListener.STATUS_ERROR) {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.error", new String[]{testName, className}); //$NON-NLS-1$
			postError(msg); 
		} else if (status == ITestRunListener.STATUS_FAILURE) {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.failure", new String[]{testName, className}); //$NON-NLS-1$
			postError(msg);
		} else {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.success", new String[]{testName, className}); //$NON-NLS-1$
			postInfo(msg);
		}
		String test= testName+"("+className+")"; //$NON-NLS-1$ //$NON-NLS-2$
		TestRunInfo info= getTestInfo(test);
		updateTest(info, status);
		if (info.fTrace == null || !info.fTrace.equals(trace)) {
			info.fTrace= trace;
			showFailure(info.fTrace);
		}
	}
	
	private void updateTest(TestRunInfo info, final int status) {
		if (status == info.fStatus)
			return;
		if (info.fStatus == ITestRunListener.STATUS_OK) {
			if (status == ITestRunListener.STATUS_FAILURE) 
				fFailures++;
			else if (status == ITestRunListener.STATUS_ERROR)
				fErrors++;
		} else if (info.fStatus == ITestRunListener.STATUS_ERROR) {
			if (status == ITestRunListener.STATUS_OK) 
				fErrors--;
			else if (status == ITestRunListener.STATUS_FAILURE) {
				fErrors--;
				fFailures++;
			}
		} else if (info.fStatus == ITestRunListener.STATUS_FAILURE) {
			if (status == ITestRunListener.STATUS_OK) 
				fFailures--;
			else if (status == ITestRunListener.STATUS_ERROR) {
				fFailures--;
				fErrors++;
			}
		}			
		info.fStatus= status;	
		final TestRunInfo finalInfo= info;
		postAsyncRunnable(new Runnable() {
			public void run() {
				refreshCounters();
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
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
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
					v.newTreeEntry(treeEntry);
				}
			}
		});	
	}

	public void startTestRunListening(IType type, int port, ILaunch launch) {
		fTestType= type;
		fLaunchMode= launch.getLaunchMode();
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.launching"); //$NON-NLS-1$
		showInformation(msg);
		postInfo(msg);
		
		if (fTestRunnerClient != null) {
			stopTest();
		}
		fTestRunnerClient= new RemoteTestRunnerClient();
		fTestRunnerClient.startListening(this, port);
		fLastLaunch= launch;
		String title= JUnitMessages.getFormattedString("TestRunnerViewPart.title", fTestType.getElementName()); //$NON-NLS-1$
		setTitle(title);
		setTitleToolTip(fTestType.getFullyQualifiedName());
	}
	
	public void rerunTest(String className, String testName) {
		if (fTestRunnerClient != null && fTestRunnerClient.isRunning() && ILaunchManager.DEBUG_MODE.equals(fLaunchMode))
			fTestRunnerClient.rerunTest(className, testName);
		else {
			MessageDialog.openInformation(getSite().getShell(), 
				JUnitMessages.getString("TestRunnerViewPart.cannotrerun.title"),  //$NON-NLS-1$
				JUnitMessages.getString("TestRunnerViewPart.cannotrerurn.message") //$NON-NLS-1$
 			); 
		}
	}

	public synchronized void dispose(){
		fIsDisposed= true;
		stopTest();
	}
	
	private void start(final int total) {
		resetProgressBar(total);
		fCounterPanel.setTotal(total);
		fCounterPanel.setRunValue(0);	
	}

	private void resetProgressBar(final int total) {
		fProgressBar.setMinimum(0);
		fProgressBar.setSelection(0);
		fProgressBar.setForeground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
		fProgressBar.setMaximum(total);
	}

	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}
		
	private void postAsyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().asyncExec(r);
	}
	
	private void aboutToStart() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
						ITestRunView v= (ITestRunView) e.nextElement();
						v.aboutToStart();
					}
				}
			}
		});
	}
	
	private void postEndTest(final String testName) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				handleEndTest();
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
					v.endTest(testName);
				}
			}
		});	
	}
	
	private void handleEndTest() {
		refreshCounters();
		updateProgressColor(fFailures+fErrors);
		fProgressBar.setSelection(fProgressBar.getSelection() + 1);
	}

	private void updateProgressColor(int failures) {
		if (failures > 0)
			fProgressBar.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
		else 
			fProgressBar.setForeground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
	}

	private void refreshCounters() {
		fCounterPanel.setErrorValue(fErrors);
		fCounterPanel.setFailureValue(fFailures);
		fCounterPanel.setRunValue(fExecutedTests);
		updateProgressColor(fErrors + fFailures);
	}
	
	protected void postShowTestResultsView() {
		postAsyncRunnable(new Runnable() {
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
		
		try {
			testRunner= (TestRunnerViewPart)page.showView(TestRunnerViewPart.NAME);
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), 
				JUnitMessages.getString("TestRunnerViewPart.message.cannotshow"), e.getMessage(), e.getStatus() //$NON-NLS-1$
			);
		}
	}
	
	protected void postInfo(final String message) {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				getStatusLine().setErrorMessage(null);
				getStatusLine().setMessage(message);
			}
		});
	}
	
	protected void postError(final String message) {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				getStatusLine().setMessage(null);
				getStatusLine().setErrorMessage(message);
			}
		});
	}

	protected void showInformation(final String info){
		postSyncRunnable(new Runnable() {
			public void run() {
				if(!isDisposed())
					fFailureView.setInformation(info);
			}
		});
	}

	private CTabFolder createTestRunViews(Composite parent) {
		CTabFolder tabFolder= new CTabFolder(parent, SWT.TOP);
		GridData gridData= new GridData();
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		ITestRunView failureRunView= new FailureRunView(tabFolder, this); 
		ITestRunView testHierarchyRunView= new HierarchyRunView(tabFolder, this);
		
		fTestRunViews.addElement(failureRunView);
		fTestRunViews.addElement(testHierarchyRunView);
		
		tabFolder.setSelection(0);				
		fActiveRunView= (ITestRunView)fTestRunViews.firstElement();		
				
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				testViewChanged(event);
			}
		});
		return tabFolder;
	}

	private void testViewChanged(SelectionEvent event) {
		for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
			ITestRunView v= (ITestRunView) e.nextElement();
			if (((CTabFolder) event.widget).getSelection().getText() == v.getName()){
				v.setSelectedTest(fActiveRunView.getTestName());
				fActiveRunView= v;
				fActiveRunView.activate();
			}
		}
	}

	private SashForm createSashForm(Composite parent) {
		SashForm sashForm= new SashForm(parent, SWT.VERTICAL);		
		ViewForm top= new ViewForm(sashForm, SWT.NONE);
		CTabFolder tabFolder= createTestRunViews(top);
		tabFolder.setLayoutData(new TabFolderLayout());
		top.setContent(tabFolder);
		
		ViewForm bottom= new ViewForm(sashForm, SWT.NONE);
		ToolBar failureToolBar= new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		
		fFailureView= new FailureTraceView(bottom, this);
		bottom.setContent(fFailureView.getComposite()); 
		CLabel label= new CLabel(bottom, SWT.NONE);
		label.setText(JUnitMessages.getString("TestRunnerViewPart.label.failure")); //$NON-NLS-1$
		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);

		Composite traceView= fFailureView.getComposite();
		// fill the failure trace viewer toolbar
		ToolBarManager failureToolBarmanager= new ToolBarManager(failureToolBar);
		failureToolBarmanager.add(new EnableStackFilterAction(fFailureView));			
		failureToolBarmanager.update(true);
		
		sashForm.setWeights(new int[]{50, 50});
		return sashForm;
	}
		
	private void reset(final int testCount) {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				fCounterPanel.reset();
				fFailureView.clear();
				clearStatus();
				start(testCount);
			}
		});
		fExecutedTests= 0;
		fFailures= 0;
		fErrors= 0;
		aboutToStart();
		fTestInfos.clear();
		fFirstFailure= null;
	}
	
	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}

    public void setFocus() {
    	fProgressBar.setFocus();
    }
	
    public void createPartControl(Composite parent) {		
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginWidth= 0;
		parent.setLayout(gridLayout);

		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		toolBar.add(new StopAction());
		toolBar.add(new RerunAction());

		actionBars.updateActionBars();
		
		Composite counterPanel= createProgressCountPanel(parent);
		counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		SashForm sashForm= createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, new CopyTraceAction(fFailureView));
	}

	private IStatusLineManager getStatusLine() {
		return getViewSite().getActionBars().getStatusLineManager();
	}

	private Composite createProgressCountPanel(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		fProgressBar= new ProgressBar(composite, SWT.HORIZONTAL);
		fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fCounterPanel= new CounterPanel(composite);
		fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}

	public TestRunInfo getTestInfo(String testName) {
		return (TestRunInfo) fTestInfos.get(testName);
	}

	public void handleTestSelected(String testName) {
		TestRunInfo testInfo= getTestInfo(testName);

		if (testInfo == null) {
			showFailure(""); //$NON-NLS-1$
		} else {
			showFailure(testInfo.fTrace);
		}
	}

	private void showFailure(final String failure) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(!isDisposed())
					fFailureView.showFailure(failure);
			}
		});		
	}

	public IJavaProject getLaunchedProject() {
		return fTestType.getJavaProject();
	}

	protected static Image createImage(String fName, Class clazz) {
		return (new Image(Display.getCurrent(), clazz.getResourceAsStream(fName)));
	}
	
	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}
	
	private Display getDisplay() {
		return fCounterPanel.getDisplay();
	}
}