/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.text.DateFormat;import java.util.Date;import java.util.List;import org.eclipse.core.runtime.IStatus;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.Utilities;import org.eclipse.jdt.launching.IVMRunner;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.swt.widgets.Display;

public abstract class JavaLauncher implements IVMRunner {
	protected final static String PREFIX= "launcher.";
	protected final static String ERROR_CREATE_PROCESS= PREFIX+"error.create_process.";
	protected final static String ERROR_LAUNCHING= PREFIX+"error.launch.";

	
	public static String renderCommandLine(String[] commandLine) {
		StringBuffer buf= new StringBuffer(commandLine[0]);
		String timestamp= DateFormat.getInstance().format(new Date(System.currentTimeMillis()));
		buf.append(" (");
		buf.append(timestamp);
		buf.append(")");
		return buf.toString();
	}
	
	protected void addArguments(String[] args, List v) {
		if (args == null)
			return;
		for (int i= 0; i < args.length; i++)
			v.add(args[i]);
	}
	protected String renderDebugTarget(String classToRun, int host) {
		return classToRun+" at localhost:"+host;
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
}