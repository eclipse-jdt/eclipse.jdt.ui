/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILauncher;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class BaseLauncherUtil {

	private static final SearchEngine fgSearchEngine= new SearchEngine();

	private BaseLauncherUtil() {
	}

	public static class StreamListener implements IStreamListener {
		public void streamAppended(String string, IStreamMonitor streamMonitor) {
			logException("Status info from TestRunner", new Exception(string));
			System.err.println(string);
		}
	}
	
	/**
	 * Returns a collection of elements this launcher is capable of launching
	 * in the specified mode based on the given selection. If this launcher cannot launch any
	 * elements in the current selection, an empty collection or <code>null</code>
	 * is returned.
	 */
	public static List getLaunchableElements(IStructuredSelection selection) 
			throws InvocationTargetException {
		if (selection == null) return new ArrayList(0);
		return fgSearchEngine.findTargets(selection);
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
	 * Please call this in launch
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

	public static void logNshowException(String title, Exception e) {
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
	
	public static void showNoSuiteDialog() {
		String title= "JUnit Launcher Warning"; 
		Exception e= new Exception("could not find a JUnit test class"); 
		logNshowException(title , e);
	}
}

