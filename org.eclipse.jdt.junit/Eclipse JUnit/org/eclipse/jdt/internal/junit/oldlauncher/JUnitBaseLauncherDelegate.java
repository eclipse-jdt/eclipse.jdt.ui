/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.oldlauncher;


import java.lang.reflect.InvocationTargetException;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.ILauncherDelegate;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.jdt.internal.junit.util.*;
import org.eclipse.jdt.internal.junit.util.SocketUtil;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.ProjectSourceLocator;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.jdt.launching.VMRunnerResult;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * An abstract base launcher for running JUnit TestSuites.
 * Subclasses have to override: VMRunnerConfiguration configureVM(IType[] testTypes, int port)
 */
public abstract class JUnitBaseLauncherDelegate implements ILauncherDelegate, IJUnitLauncherDelegate {
	
	/** 
	 * The port we are connecting to
	 */
	private int fPort;
	
	/** 
	 * The type to be launched
	 */
	private IType fType;
	
	/**
	 * The launcher
	 */
	private ILauncher fLauncher;
	/**
	 * Configure a VM for the given test types.
	 */
 	protected abstract VMRunnerConfiguration configureVM(IType[] testTypes, int port, String mode) throws CoreException;
	
	/*
	 * @see ILauncherDelegate#launch(Object[], String, ILauncher)
	 */
	public boolean launch(Object[] objects, String mode, ILauncher launcher) {
		IStructuredSelection selection= new StructuredSelection(objects);
		Object[] elements= getLaunchableElements(selection);
		
		if (elements.length > 1)
			return useWizard(elements, selection, mode, launcher);

		if (elements.length == 0) {
			MessageDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), 
				"JUnit Launcher", "Could not find a JUnit test class"
			);
			return true;
		}
		
		Object runnable= elements[0];
		if (!(runnable instanceof IType)) {
			MessageDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), 
				"JUnit Launcher", "Could not find a launchable test type"
			);
			return true;
		}
		IType[] testTypes= new IType[] {(IType)runnable};
		return doLaunch(testTypes, mode, launcher);
	}

	protected boolean doLaunch(final IType[] testTypes, final String runMode, ILauncher launcher) {
		final IType testType= testTypes[0];
		fType= testType;
		
		IVMInstall vmInstall;
		try {
			vmInstall= JavaRuntime.getVMInstall(testType.getJavaProject());
		} catch (CoreException e) {
			ErrorDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), "JUnit Launch", e.getMessage(), e.getStatus());
			return true;
		}
		if (vmInstall == null)
		   vmInstall= JavaRuntime.getDefaultVMInstall();
	    
	    if (vmInstall == null) {
	    	MessageDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), "JUnit Launch", "No JRE Runtime found");
	    	return true;
	    }
	    
		final IVMRunner vmRunner= vmInstall.getVMRunner(runMode);	
		final VMRunnerResult returnResult[]= new VMRunnerResult[1];
		VMRunnerResult result;
		
		fPort= SocketUtil.findUnusedLocalPort(4000, 5000);  
		final int finalPort= fPort;
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				pm.beginTask("Starting VM ...", IProgressMonitor.UNKNOWN);
				try {
					VMRunnerConfiguration vmConfig= configureVM(testTypes, finalPort, runMode);
					returnResult[0]= vmRunner.run(vmConfig);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			new ProgressMonitorDialog(JUnitPlugin.getActiveWorkbenchShell()).run(true, false, runnable);		
		} catch (InterruptedException e) {
			// do nothing - user canceled action
		} catch (InvocationTargetException e) {
			Throwable te= e.getTargetException();
			JUnitPlugin.log(te);
			MessageDialog.openError(JUnitPlugin.getActiveWorkbenchShell(), "Could not launch VM", te.getMessage());			
			return false;
		}
		result= returnResult[0];
		if (result != null && launcher != null) {
			// TO DO should use JavaUISourceLocator, but this would break 1.0 compatibility
			Launch newLaunch= new Launch(launcher, runMode, testType.getCompilationUnit(), new ProjectSourceLocator(testType.getJavaProject()), result.getProcesses(), result.getDebugTarget());
			registerLaunch(newLaunch);
		}
		fLauncher= launcher;
		fType= testType;
		return true;
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
		IJavaElement e= JavaCore.create(memento);
		if (e.exists()) 
			return e;
		return null;
	}
		
	public int getPort() {
		return fPort;
	}
	
	public IType getLaunchedType() {
		return fType;
	}
	
	/**
	 * Returns a collection of elements this launcher is capable of launching
	 * in the specified mode based on the given selection. If this launcher cannot launch any
	 * elements in the current selection, an empty collection or <code>null</code>
	 * is returned.
	 */
	public IType[] getLaunchableElements(IStructuredSelection selection) {
		try {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(JUnitPlugin.getActiveWorkbenchShell());
			return TestSearchEngine.findTests(dialog, selection.toArray());
		} catch (InvocationTargetException e) {
			JUnitPlugin.log(e);
		} catch (InterruptedException e) {
			// user pressed cancel
		}
		return new IType[0];
	}

	/**
	 * Use the wizard to do the launch.
	 */
	private boolean useWizard(Object[] elements, IStructuredSelection selection, String mode, ILauncher launcher) {
		JUnitLaunchWizard wizard= new JUnitLaunchWizard(elements);
		wizard.init(launcher, mode, selection);
		Shell shell= JUnitPlugin.getActiveWorkbenchShell();
		if (shell != null) {
			WizardDialog dialog= new WizardDialog(shell, wizard);
			int status= dialog.open();
			return (status == dialog.OK || status == dialog.CANCEL);
		}
		return false;
	}

	/**
	 * Registers the Process in the Debug View
	 */
	private void registerLaunch(final ILaunch launch) {
		Display.getCurrent().syncExec(new Runnable() {
			public void run() {
				DebugPlugin.getDefault().getLaunchManager().registerLaunch(launch);
			}
		});
	}
}