/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.io.File;import java.text.DateFormat;import java.util.Date;import java.util.List;import org.eclipse.core.runtime.IStatus;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;import org.eclipse.jdt.internal.ui.util.Utilities;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.swt.widgets.Display;import com.sun.jdi.VirtualMachine;

public abstract class JavaLauncher implements IVMRunner {
	protected final static String PREFIX= "launcher.";
	protected final static String ERROR_CREATE_PROCESS= PREFIX+"error.create_process.";
	protected final static String ERROR_LAUNCHING= PREFIX+"error.launch.";

	protected IVMInstall fVMInstance;

	public JavaLauncher(IVMInstall vmInstance) {
		fVMInstance= vmInstance;
	}
	
	protected String renderDebugTarget(String classToRun, int host) {
		return classToRun+" at localhost:"+host;
	}

	public static String renderProcessLabel(String[] commandLine) {
		StringBuffer buf= new StringBuffer(commandLine[0]);
		String timestamp= DateFormat.getInstance().format(new Date(System.currentTimeMillis()));
		buf.append(" (");
		buf.append(timestamp);
		buf.append(")");
		return buf.toString();
	}
	
	protected static String renderCommandLine(String[] commandLine) {
		if (commandLine.length < 1)
			return "";
		StringBuffer buf= new StringBuffer(commandLine[0]);
		for (int i= 1; i < commandLine.length; i++) {
			buf.append(' ');
			buf.append(commandLine[i]);
		}	
		return buf.toString();
	}
	
	protected void addArguments(String[] args, List v) {
		if (args == null)
			return;
		for (int i= 0; i < args.length; i++)
			v.add(args[i]);
	}
	
	protected void showErrorDialog(final String resourceKey, final IStatus error) {
		Display d= Utilities.getDisplay(null);
		if (d != null) {
			d.syncExec(new Runnable() {
				public void run() {
					JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), resourceKey, error);
				}
			});
		} else {
			JavaPlugin.logErrorStatus(JavaLaunchUtils.getResourceString(resourceKey+"message"), error);
		}
	}
	
	protected boolean askRetry(final String reason) {
		final String title= JavaLaunchUtils.getResourceString(reason+"title");
		final String msg= JavaLaunchUtils.getResourceString(reason+"message");
		final boolean[] result= new boolean[1];
		Utilities.getDisplay(null).syncExec(new Runnable() {
			public void run() {
				result[0]= (MessageDialog.openConfirm(JavaPlugin.getActiveWorkbenchShell(), title, msg));
			}
		});
		return result[0];
	}
	
	
	protected void setTimeout(VirtualMachine vm) {		
		if (vm instanceof org.eclipse.jdi.VirtualMachine) {
			int timeout= fVMInstance.getDebuggerTimeout();
			org.eclipse.jdi.VirtualMachine vm2= (org.eclipse.jdi.VirtualMachine)vm;
			vm2.setRequestTimeout(timeout);
		}
	}

	protected String getJDKLocation(String dflt) {
		File location= fVMInstance.getInstallLocation();
		if (location == null)
			return dflt;
		return location.getAbsolutePath();
	}
}