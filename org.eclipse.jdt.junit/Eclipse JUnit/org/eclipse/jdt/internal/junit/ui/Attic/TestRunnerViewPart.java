/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

// !!internal import
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

import org.eclipse.jdt.internal.junit.runner.*;

/**
 * A ViewPart that shows the results of a test run.
 * It listens for the test results.
 */
public class TestRunnerViewPart extends ViewPart implements ITestRunListener, IRunViewContext {

	public static final String NAME= "org.eclipse.jdt.junit.viewpart";

 	public static final int IS_SUITE= -1;	
 		
	protected int fExecutedTests;
	protected int fErrors;
	protected int fFailures;
		
	private Vector fTestInfo= new Vector();
	private Display fDisplay;
	private ProgressBar fProgressBar;
	private CounterPanel fCounterPanel;
	private IStatusLineManager fStatusLine;
	private FailureDetailView fFailureView;
	private CTabFolder fTabFolder;
	
	private Vector fTestRunViews= new Vector();
	private ITestRunView fActiveRunView;
	private ITestRunView fFailureRunView;
	private ITestRunView fTestHierarchyRunView;
	private Tree fHierarchyTree;
	private Vector fTreeItems= new Vector();
	private boolean fActive= true;
	
	private IType fType;
	
	private BaseLauncher fCurrentLauncher;
	private boolean fInReRun;
	
	protected final Image fTestIcon= TestRunnerViewPart.createImage("icons/testIcon.gif", getClass());
	protected final Image fHierarchyIcon= TestRunnerViewPart.createImage("icons/hierarchy.gif", getClass());

	protected RemoteTestRunnerClient fTestRunner;
	
	private static class SuiteInfo {
		public int fTestCount;
		public TreeItem fTreeItem;
		
		public SuiteInfo(TreeItem treeItem, int testCount){
			fTreeItem= treeItem;
			fTestCount= testCount;
		}
	}
	
	private class StopAction extends Action{
		public StopAction() {
			setText("Stop JUnit Test");
			setToolTipText("Stop JUnit Test");
			setImageDescriptor(ImageDescriptor.createFromFile(getClass(), "icons/stopIcon.gif"));
		}
		
		public void run(){
			stopTest();
		}
	}
	
	public void stopTest() {
		if (fTestRunner != null)
			fTestRunner.stopTest();
	}
			
	public void testRunStarted(final int testCount){
		if (fInReRun) {
			fDisplay.asyncExec(new Runnable() {				
				public void run() {
					if(!fActive) 
						return;				
					resetProgressBar(testCount);
					fCounterPanel.setTotal(fCounterPanel.getTotal() + testCount);
				}
			});
			return;
		}
		reset(testCount);
		fExecutedTests++;
	}
	
	public void testRunEnded(long elapsedTime){
		postInfo("Finished: " + TestRunnerViewPart.elapsedTimeAsString(elapsedTime) + " seconds");
		fDisplay.asyncExec(new Runnable() {				
			public void run() {
				if(!fActive) 
					return;	
				Enumeration enum= fTestInfo.elements();
				TestInfo testInfo= null;
				while(enum.hasMoreElements()){
					testInfo= (TestInfo) enum.nextElement();
					if(testInfo != null && testInfo.fTrace != null)
						for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
							ITestRunView v= (ITestRunView) e.nextElement();
								v.setSelectedTest(testInfo.fTestName);
						}
				}
			}
		});	
		fInReRun= false;
		stopTest();
	}
	
	public void testRunStopped(final long elapsedTime){
		String msg= "Stopped after " + TestRunnerViewPart.elapsedTimeAsString(elapsedTime) + " seconds";
		showInformation(msg);
		postError(msg);
		fInReRun= false;
		stopTest();
		fTestRunner.shutDown();
	}
	
	public void testStarted(String testName){
		postInfo("Started: " + testName);
		TestInfo testInfo= getTestInfo(testName);
		if (testInfo == null) {
			testInfo= new TestInfo(testName);
			fTestInfo.addElement(testInfo);
		}
	}
	
	public void testEnded(String testName){
		endTest(testName);
		fExecutedTests++;
	}
	
	public void testFailed(int status, String testName, String trace){
		TestInfo testInfo= getTestInfo(testName);
		if (testInfo == null) {
			testInfo= new TestInfo(testName);
			fTestInfo.addElement(testInfo);
		}
		testInfo.fTrace= trace;
		testInfo.fStatus= status;
		if (status == ITestRunListener.STATUS_ERROR)
			fErrors++;
		else
			fFailures++;
	}
	
	public void testTreeStart(){
		if (fInReRun) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run(){
				if(!fActive) 
					return;
				fHierarchyTree.removeAll();
				fTreeItems.removeAllElements();
			}
		});	
	}
	
	public void testTreeEntry(final String treeEntry){
		if (fInReRun) 
			return;
		fDisplay.syncExec(new Runnable() {
			public void run(){
				if(!fActive) 
					return;
				int index0= treeEntry.indexOf(',');
				int index1= treeEntry.lastIndexOf(',');
				String label= treeEntry.substring(0, index0).trim();
				TestInfo testInfo= new TestInfo(label);
				fTestInfo.addElement(testInfo);
				int index2;
				if((index2= label.indexOf('(')) > 0)
					label= label.substring(0, index2);
				if((index2= label.indexOf('@')) > 0)
					label= label.substring(0, index2);
				
				String isSuite= treeEntry.substring(index0 + 1, index1);
				int testCount= Integer.parseInt(treeEntry.substring(index1 + 1));
				TreeItem treeItem;
			
				while((fTreeItems.size() > 0) && (((SuiteInfo) fTreeItems.lastElement()).fTestCount == 0))	{
						fTreeItems.removeElementAt(fTreeItems.size() - 1);
				}

				if(fTreeItems.size() == 0){
					testInfo.fStatus= IS_SUITE;
					treeItem= new TreeItem(fHierarchyTree, SWT.NONE);
					treeItem.setImage(fHierarchyIcon);
					fTreeItems.addElement(new SuiteInfo(treeItem, testCount));
				}
				else if(isSuite.equals("true")) {
					testInfo.fStatus= IS_SUITE;
					treeItem= new TreeItem(((SuiteInfo) fTreeItems.lastElement()).fTreeItem, SWT.NONE);
					treeItem.setImage(fHierarchyIcon);
					((SuiteInfo) fTreeItems.lastElement()).fTestCount -= 1;
					fTreeItems.addElement(new SuiteInfo(treeItem, testCount));
				}
				else {
					treeItem= new TreeItem(((SuiteInfo) fTreeItems.lastElement()).fTreeItem, SWT.NONE);
					treeItem.setImage(fTestIcon);
					((SuiteInfo) fTreeItems.lastElement()).fTestCount -= 1;
				}
				treeItem.setText(label);
				treeItem.setData(testInfo);
			}
		});		
	}

	public void startTestRunListening(IType type, int port, BaseLauncher launcher) {
		fType= type;
		fCurrentLauncher= launcher;
		String msg= "Launching TestRunner";
		showInformation(msg);
		postInfo(msg);
		fTestRunner= new RemoteTestRunnerClient();
		fTestRunner.startListening(this, port);
	}

	public void reRunTest(String[] classNames) {
		stopTest();
			
		fInReRun= true;
		IType[] testTypes= new IType[classNames.length];
		
		for (int i= 0; i < classNames.length; i ++) {
			testTypes[i]= TestRunnerViewPart.getType(fType.getJavaProject(), classNames[i]);
			removeFromInfo(classNames[i]);
		}
		
		fDisplay.asyncExec(new Runnable() {
			public void run() {
				if (!fActive) 
					return;	
				fFailureView.clear();
			}
		});
		try {	
			fCurrentLauncher.redoLaunch(testTypes);
		} catch (InvocationTargetException e) {
			BaseLauncher.handleException("JUnit rerun error", e);
		}
	}

	protected void removeFromInfo(String className) {
		Iterator iter= fTestInfo.iterator();
		while (iter.hasNext()) {
			TestInfo testInfo= (TestInfo) iter.next();
			if (testInfo.fTestName.indexOf(className) > 0) {
				if (testInfo.fStatus == ITestRunListener.STATUS_ERROR)
					fErrors--;
				else if (testInfo.fStatus == ITestRunListener.STATUS_FAILURE)
					fFailures--;
				iter.remove();
			}
		}
	}

	public synchronized void dispose(){
		fActive= false;
		stopTest();
		if (fFailureRunView != null) {
			fFailureRunView.dispose();
			fFailureRunView= null;
		}
		if (fTestHierarchyRunView != null) {
			fTestHierarchyRunView.dispose();
		}
		if (fCounterPanel != null && !fCounterPanel.isDisposed()) {
			 fCounterPanel.dispose();
			 fCounterPanel= null;
		}
		if (fFailureView != null) {
			fFailureView.dispose();
			fFailureView= null;
		}
	}
	
	private void start(final int total) {
		resetProgressBar(total);
		fCounterPanel.setTotal(total);
		fCounterPanel.setRunValue(0);	
	}

	private void resetProgressBar(final int total) {
		fProgressBar.setMinimum(0);
		fProgressBar.setSelection(0);
		fProgressBar.setForeground(fDisplay.getSystemColor(SWT.COLOR_GREEN));
		fProgressBar.setMaximum(total);
	}

	protected void aboutToStart() {
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if (fActive) {
					for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
						ITestRunView v= (ITestRunView) e.nextElement();
						v.aboutToStart();
					}
				}
			}
		});
	}
	
	protected void endTest(final String testName) {
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if(!fActive) return;
				postEndTest();
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
					v.updateTest(testName);
				}
			}
		});	
	}
	
	protected void postEndTest() {
		fCounterPanel.setErrorValue(fErrors);
		fCounterPanel.setFailureValue(fFailures);
		fCounterPanel.setRunValue(fExecutedTests);
	
		if(fErrors + fFailures > 0)
			fProgressBar.setForeground(fDisplay.getSystemColor(SWT.COLOR_RED));
		fProgressBar.setSelection(fProgressBar.getSelection() + 1);
	}
	
	protected void postInfo(final String message) {
		fDisplay.asyncExec(new Runnable() {
			public void run() {
				if (!fActive) 
					return;
				fStatusLine.setErrorMessage(null);
				fStatusLine.setMessage(message);
			}
		});
	}
	
	protected void postError(final String message) {
		fDisplay.asyncExec(new Runnable() {
			public void run() {
				if (!fActive) 
					return;
				fStatusLine.setMessage(null);
				fStatusLine.setErrorMessage(message);
			}
		});
	}

	protected void showInformation(final String info){
		fDisplay.syncExec(new Runnable() {
			public void run() {
				if(fActive)
					fFailureView.setInformation(info);
			}
		});
	}

	protected CTabFolder createTestRunViews(Composite parent) {
		CTabFolder tabFolder= new CTabFolder(parent, SWT.TOP);
		GridData gridData= new GridData();
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		fFailureRunView= new FailureRunView(tabFolder, this);
		fTestHierarchyRunView= new HierarchyRunView(tabFolder, this);
		fHierarchyTree= ((HierarchyRunView) fTestHierarchyRunView).getTree();
		gridData= new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		fHierarchyTree.setLayoutData(gridData);
		
		fTestRunViews.addElement(fTestHierarchyRunView);
		fTestRunViews.addElement(fFailureRunView);
		
		tabFolder.setSelection(0);				// change always both !!
		fActiveRunView= fFailureRunView;		//
				
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				testViewChanged(event);
			}
		});
		return tabFolder;
	}

	protected void testViewChanged(SelectionEvent event) {
		for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
			ITestRunView v= (ITestRunView) e.nextElement();
			if (((CTabFolder) event.widget).getSelection().getText() == v.getName()){
				v.setSelectedTest(fActiveRunView.getTestName());
				fActiveRunView= v;
				v.activate();
			}
		}
	}

	protected SashForm createSashForm(Composite parent) {
		SashForm sashForm= new SashForm(parent, SWT.VERTICAL);
		GridLayout gridLayout= new GridLayout();
		gridLayout.horizontalSpacing= 0;
		gridLayout.verticalSpacing= 0;
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		sashForm.setLayout(gridLayout);
		
		fTabFolder= createTestRunViews(sashForm);
		fTabFolder.setLayoutData(new TabFolderLayout());
		
		fFailureView= new FailureDetailView(sashForm, this);
		Composite traceView= fFailureView.getComposite();
		traceView.setLayoutData(new GridData(GridData.GRAB_VERTICAL | GridData.FILL_BOTH));
		
		sashForm.setWeights(new int[]{75, 25});
		return sashForm;
	}
	
	protected void reset(final int testCount) {
		fDisplay.asyncExec(new Runnable() {
			public void run() {
				if (!fActive) 
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
		fTestInfo.removeAllElements();
	}
	
	protected void clearStatus() {
		fStatusLine.setMessage(null);
		fStatusLine.setErrorMessage(null);
	}

    public void setFocus() {
    	fProgressBar.setFocus();
    }
	
    public void createPartControl(Composite parent) {
		fDisplay= parent.getDisplay();
		fStatusLine= getViewSite().getActionBars().getStatusLineManager();
		
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginWidth= 0;
		parent.setLayout(gridLayout);

		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		toolBar.add(new StopAction());
		actionBars.updateActionBars();
		
		Composite counterPanel= createProgressCountPanel(parent);
		counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		SashForm sashForm= createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	protected Composite createProgressCountPanel(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		fProgressBar= new ProgressBar(composite, SWT.HORIZONTAL);
		fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		fCounterPanel= new CounterPanel(composite);
		fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}

	public TestInfo getTestInfo(String testName){
		Enumeration enum= fTestInfo.elements();
		TestInfo testInfo= null;
		while (enum.hasMoreElements()){
			testInfo= (TestInfo) enum.nextElement();
			if(testInfo.fTestName.equals(testName))
				return testInfo;
		}
		return null;
	}

	public void handleTestSelected(final String testName) {
		final TestInfo testInfo= getTestInfo(testName);

		if (testInfo == null) {
			fDisplay.syncExec(new Runnable() {
				public void run() {
					if(fActive)
						fFailureView.showFailure("");
				}
			});		
		} else {
			fDisplay.syncExec(new Runnable() {
				public void run() {
					if(fActive)
						fFailureView.showFailure(TestRunnerViewPart.filterStack(testInfo.fTrace));
				}
			});	
		}
	}

	protected void goToFile(String traceLine){	
		try{
			// hack works only for JDK stack trace
			String testName= traceLine;
			testName= testName.substring(testName.indexOf("at "));
			testName= testName.substring(3, testName.indexOf('(')).trim();
			testName= testName.substring(0, testName.lastIndexOf('.'));
			String lineNumber= traceLine;
		
			lineNumber= lineNumber.substring(lineNumber.indexOf(':') + 1, lineNumber.indexOf(")"));
			goToTest(testName, Integer.valueOf(lineNumber).intValue());
		} catch (NumberFormatException e) {
			ITestRunView view= (ITestRunView) fTestRunViews.elementAt(fTabFolder.getSelectionIndex());
			goToTest(view.getTestName(), 0);
		}
		catch(IndexOutOfBoundsException e){	
			ITestRunView view= (ITestRunView) fTestRunViews.elementAt(fTabFolder.getSelectionIndex());
			goToTest(view.getTestName(), 0);
		}	
	}

	public void goToTest(String testName, int lineNumber){
		IType type= TestRunnerViewPart.getType(fType.getJavaProject(), testName);
		if (type == null) {
			fDisplay.beep();
			return;
		}
		ITextEditor textEditor= TestRunnerViewPart.openInEditor(type);
		if (textEditor == null) return;
		
		try {
			IDocument document= textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
			textEditor.selectAndReveal(document.getLineOffset(lineNumber - 1), document.getLineLength(lineNumber - 1));
		} catch (BadLocationException x) {
			// marker refers to invalid text position -> do nothing
		}
	}


	public void goToTestMethod(String testName, String methodName){
		try{
			IType type= TestRunnerViewPart.getType(fType.getJavaProject(), testName);
			if (type == null) {
				fDisplay.beep();
				return;
			}
			ITypeHierarchy typeHierarchy= type.newSupertypeHierarchy(null);
			IType[] types= typeHierarchy.getAllClasses();
			IMethod method= null;
			
			for (int i= 0; i < types.length; i++) {
				type= types[i];
				method= type.getMethod(methodName, new String[0]);
				if (method != null && method.exists())
					break;
			}
			ITextEditor textEditor= TestRunnerViewPart.openInEditor(method);
			if (textEditor == null) return;
			
			ISourceRange range= method.getNameRange();
			textEditor.selectAndReveal(range.getOffset() , range.getLength());
				
		} catch (Exception e){
			fDisplay.beep();
			// can not goToFile
			String msg= "Warning: could not open test: " +  testName + ":" + methodName;
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
		}
	}
		
	protected static ITextEditor openInEditor(IJavaElement javaElement) {
		if (javaElement == null || !javaElement.exists()) {
			Display.getCurrent().beep();
			return null;
		}		
		IEditorPart editor= null;
		try {	
			editor= EditorUtility.openInEditor(javaElement, false);			
		} catch (CoreException e){
			return null;
		}						
		if(!(editor instanceof ITextEditor)) {
			Display.getCurrent().beep();
			return null;
		}			
		return (ITextEditor) editor;
	}	

	public static IType getType(IJavaProject project, String str) {
		if (project == null || str == null) 
			return null;
		String pathStr= str.replace('.', '/') + ".java"; //$NON-NLS-1$
		IJavaElement element= null;
		try {
			element= project.findElement(new Path(pathStr));
		} catch (JavaModelException e) {
			// an illegal path -> no element found
		}
		IType resType= null;
		if (element instanceof ICompilationUnit) {
			String simpleName= Signature.getSimpleName(str);
			resType= ((ICompilationUnit)element).getType(simpleName);
		} else if (element instanceof IClassFile) {
			try {
				resType= ((IClassFile)element).getType();
			} catch (JavaModelException e) {
				// Fall through and return null.
			}
		}
		return resType;
	}

	/**
	 * Returns the formatted string of the elapsed time.
	 */
	protected static String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}

	protected static String filterStack(String stackTrace) {	
		if (!JUnitPreferencePage.getFilterStack() || stackTrace == null) 
			return stackTrace;
			
		StringWriter stringWriter= new StringWriter();
		PrintWriter printWriter= new PrintWriter(stringWriter);
		StringReader stringReader= new StringReader(stackTrace);
		BufferedReader bufferedReader= new BufferedReader(stringReader);		
		String line;
		try {	
			while ((line= bufferedReader.readLine()) != null) {
				if (!filterLine(line))
					printWriter.println(line);
			}
		} catch (IOException e) {
			return stackTrace; // return the stack unfiltered
		}
		return stringWriter.toString();
	}
	
	protected static boolean filterLine(String line) {
		String[] patterns= JUnitPreferencePage.getFilterPatterns();
		for (int i= 0; i < patterns.length; i++) {
			if (line.indexOf(patterns[i]) > 0)
				return true;
		}		
		return false;
	}
	
	protected static Image createImage(String fName, Class clazz) {
		try {
			return (new Image(Display.getCurrent(), clazz.getResourceAsStream(fName)));
		}
		catch (Exception e) {
			String msg= "Warning: could not load image!";
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
			return new Image(Display.getCurrent(), 1, 1);
		}
	}
}