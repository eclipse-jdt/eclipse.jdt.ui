/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILauncherDelegate;
import org.eclipse.debug.core.model.IStreamMonitor;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.ProjectSourceLocator;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;
import org.eclipse.jdt.internal.junit.runner.*;
import org.eclipse.jdt.internal.junit.ui.*;

/**
 * An abstract base launcher for running JUnit TestSuite classes.
 * Subclasses have to override: VMRunnerConfiguration configureVM(IType[] testTypes, int port)
 */
public abstract class BaseLauncher implements ILauncherDelegate {
	private String fRunMode;
	private ILauncher fLauncherProxy;
	/*
	 * Called by doLaunch, configures the VM that will be started
	 */
 	public abstract VMRunnerConfiguration configureVM(IType[] testTypes, int port) 
 		throws InvocationTargetException;

	/*
	 * @see ILauncherDelegate#launch(Object[], String, ILauncher)
	 */
	public boolean launch(Object[] objects, String mode, ILauncher launcher) {
		try {
			IStructuredSelection selection= new StructuredSelection(objects);
			List elements= getLaunchableElements(selection);
			
			if (elements.size() > 1)
				return useWizard(elements, selection, mode, launcher);

			if (elements.size() == 0) {
				showNoSuiteDialog();
				return true;
			}
			
			Object runnable= elements.iterator().next();
			if (!(runnable instanceof IType)) {
				showNoSuiteDialog();
				return true;
			}
			IType testType= (IType)runnable;
			IType[] testTypes= new IType[] {testType};
			return doLaunch(testTypes, mode, launcher);
			
		} catch (InvocationTargetException e) {
			handleException("JUnit Launcher Error", e);
		} 
		return false;
	}

	protected boolean doLaunch(final IType[] testTypes, final String runMode, ILauncher launcherProxy) 
			throws InvocationTargetException {
		
		if (testTypes == null || testTypes.length == 0 || testTypes[0] == null || runMode == null || launcherProxy == null) {
			throw new InvocationTargetException(new Exception("no JUnit test class specified or rerun not possible."));
		}
		final IType testType= testTypes[0];
		IVMInstall vmInstall;
		try {
			vmInstall= JavaRuntime.getVMInstall(testType.getJavaProject());
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
		if (vmInstall == null)
		   vmInstall= JavaRuntime.getDefaultVMInstall();
	    
		final IVMRunner vmRunner= vmInstall.getVMRunner(runMode);	
		final VMRunnerResult[] result= new VMRunnerResult[1];
		final int port= SocketUtil.findUnusedLocalPort(4000, 5000);

		IWorkbenchWindow window= JUnitPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();

		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				pm.beginTask("Starting VM ...", IProgressMonitor.UNKNOWN);
				VMRunnerConfiguration vmConfig= configureVM(testTypes, port);
				try {
					result[0]= vmRunner.run(vmConfig);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			new ProgressMonitorDialog(JUnitPlugin.getActiveShell()).run(true, false, runnable);		
		} catch (InterruptedException e) {
			// do nothing - user canceled action
		}
		if (result[0] != null && launcherProxy != null) {
			Launch newLaunch= new Launch(launcherProxy, runMode, testType.getCompilationUnit(), new ProjectSourceLocator(testType.getJavaProject()),result[0].getProcesses(), result[0].getDebugTarget());
			registerLaunch(newLaunch);
		}
		result[0].getProcesses()[0].getStreamsProxy().getErrorStreamMonitor().addListener(new StreamListener());
		result[0].getProcesses()[0].getStreamsProxy().getOutputStreamMonitor().addListener(new StreamListener());
		fLauncherProxy= launcherProxy;
		fRunMode= runMode;
		
		TestRunnerViewPart testRunner;
		try {
			testRunner= (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME);
		} catch (PartInitException e) {
			throw new InvocationTargetException(e);
		}
		testRunner.startTestRunListening(testType, port, this);	
		return true;
	}

	/*
	 * called by TestRunnerViewPart
	 */
	protected boolean redoLaunch(final IType[] testTypes) throws InvocationTargetException {
		return doLaunch(testTypes, fRunMode, fLauncherProxy);
	}

	/*
	 * @see ILauncherDelegate#getLaunchMemento(Object)
	 */
	public String getLaunchMemento(Object element) {
		if (element instanceof IJavaElement) 
			return ((IJavaElement)element).getHandleIdentifier();
		return null;
	}

	/*
	 * @see ILauncherDelegate#getLaunchObject(String)
	 */
	public Object getLaunchObject(String memento) {
		IJavaElement e = JavaCore.create(memento);
		if (e.exists()) {
			return e;
		} else {
			return null;
		}
	}
	
	public static class StreamListener implements IStreamListener {
		public void streamAppended(String string, IStreamMonitor streamMonitor) {
			logException("Status info from RemoteTestRunner", new Exception(string));
			System.err.println(string);
		}
	}
	
	/**
	 * Returns a collection of elements this launcher is capable of launching
	 * in the specified mode based on the given selection. If this launcher cannot launch any
	 * elements in the current selection, an empty collection or <code>null</code>
	 * is returned.
	 */
	public static List getLaunchableElements(IStructuredSelection selection) throws InvocationTargetException {
		if (selection == null) 
			return new ArrayList(0);
		return new TestSearchEngine().findTargets(selection);
	}

	/**
	 * Use the wizard to do the launch.
	 */
	public static boolean useWizard(List elements, IStructuredSelection selection, String mode, ILauncher launcher) {
		ApplicationWizard wizard= new ApplicationWizard(elements);
		wizard.init(launcher, mode, selection);
		Shell shell= JUnitPlugin.getActiveShell();
		if (shell != null) {
			WizardDialog dialog= new WizardDialog(shell, wizard);
			int status = dialog.open();
			return (status == dialog.OK || status == dialog.CANCEL);
		}
		return false;
	}


	/**
	 * Registers the Process in the Debug View
	 */
	public static void registerLaunch(final ILaunch launch) {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				DebugPlugin plugin= DebugPlugin.getDefault();
				if (plugin != null) {
					ILaunchManager manager= plugin.getLaunchManager();
					if (manager != null) manager.registerLaunch(launch);
				}
			}
		});
	}

	/**
	 * Handles an exception by logging it to the error log and
	 * by showing a dialog to the user.
	 */
	public static void handleException(String title, Exception e) {
		Throwable throwable;
		Exception ex= e;
		if (e instanceof InvocationTargetException) {
			throwable= ((InvocationTargetException) e).getTargetException();
			if (throwable instanceof Exception)
				ex= (Exception)throwable;
			else
				ex= new Exception(throwable.getMessage());
		}
		Status status= new Status(IStatus.ERROR, JUnitPlugin.getPluginID(), IStatus.OK, title, ex);
		JUnitPlugin plugin= JUnitPlugin.getDefault();
		if(plugin != null) {
			ILog log= plugin.getLog();
			if (log != null) log.log(status);

			Shell shell= JUnitPlugin.getActiveShell();
			if (shell != null)
				MessageDialog.openError(shell, title, ex.getMessage());
		}				
	}
	
	public static void logException(String title, Exception e) {
		Throwable throwAble;
		Exception ex= e;
		if (e instanceof InvocationTargetException) {
			throwAble= ((InvocationTargetException) e).getTargetException();
			if (throwAble instanceof Exception)
				ex= (Exception) throwAble;
			else
				ex= new Exception(throwAble.getMessage());
		}
		Status status= new Status(IStatus.ERROR, JUnitPlugin.getPluginID(), IStatus.OK, title, ex);
		JUnitPlugin plugin= JUnitPlugin.getDefault();
		if(plugin != null) {
			ILog log= plugin.getLog();
			if (log != null) log.log(status);
		}
	}
	
	static void showNoSuiteDialog() {
		String title= "JUnit Launcher"; 
		Exception e= new Exception("Could not find a JUnit test class"); 
		handleException(title , e);
	}
}