/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILauncherDelegate;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.ProjectSourceLocator;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * An abstract base launcher for running JUnit TestSuite classes. 
 */
public abstract class BaseLauncher implements ILauncherDelegate {

	private String fRunMode;
	private ILauncher fLauncherProxy;

	/*
	 * Called by doLaunch, configures the VM that will be started
	 */
 	public abstract VMRunnerConfiguration configureVM(IType[] testTypes, int port) 
 		throws InvocationTargetException;

	/**
	 * @see ILauncherDelegate#launch(Object[], String, ILauncher)
	 */
	public boolean launch(Object[] objects, String mode, ILauncher launcher) {
		try {
			IStructuredSelection selection = new StructuredSelection(objects);
			List elements= BaseLauncherUtil.getLaunchableElements(selection);
			
			if (elements.size() > 1)
				return BaseLauncherUtil.useWizard(elements, selection, mode, launcher);

			if (elements.size() == 0){
				BaseLauncherUtil.showNoSuiteDialog();
				return false;
			}
			
			Object runnable= elements.iterator().next();
			if (!(runnable instanceof IType)) {
				BaseLauncherUtil.showNoSuiteDialog();
				return false;
			}
			
			IType testType= (IType)runnable;
			IType[] testTypes= new IType[] {testType};

			return doLaunch(testTypes, mode, launcher);
		} catch (InvocationTargetException e) {
			BaseLauncherUtil.logNshowException("JUnit Launcher Error", e);
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
			vmInstall = JavaRuntime.getVMInstall(testType.getJavaProject());
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
		if (vmInstall == null)
		   vmInstall = JavaRuntime.getDefaultVMInstall();
	    
		final IVMRunner vmRunner = vmInstall.getVMRunner(runMode);	
		final VMRunnerResult[] result= new VMRunnerResult[1];

		final int port= SocketUtil.findUnusedLocalPort(4000, 5000);

		IWorkbenchWindow window= JUnitPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		TestRunnerUI testRunnerUI;		
		try {
			testRunnerUI= (TestRunnerUI) page.showView(JUnitPlugin.fgViewPartName);
		} catch (PartInitException e) {
			throw new InvocationTargetException(e);
		}
		testRunnerUI.startRun(testType, port, this);	

		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				if (!testType.getJavaProject().getProject().getWorkspace().isAutoBuilding())
					try {
						testType.getJavaProject().getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, pm);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
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
			BaseLauncherUtil.registerLaunch(newLaunch);
		}
		result[0].getProcesses()[0].getStreamsProxy().getErrorStreamMonitor().addListener(new BaseLauncherUtil.StreamListener());
		result[0].getProcesses()[0].getStreamsProxy().getOutputStreamMonitor().addListener(new BaseLauncherUtil.StreamListener());
		fLauncherProxy= launcherProxy;
		fRunMode= runMode;
		return true;
	}

	/*
	 * called by TestRunnerUI
	 */
	protected boolean redoLaunch(final IType[] testTypes) 
			throws InvocationTargetException {
		return doLaunch(testTypes, fRunMode, fLauncherProxy);
	}

	/**
	 * @see ILauncherDelegate#getLaunchMemento(Object)
	 */
	public String getLaunchMemento(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).getHandleIdentifier();
		}
		return null;
	}

	/**
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
}