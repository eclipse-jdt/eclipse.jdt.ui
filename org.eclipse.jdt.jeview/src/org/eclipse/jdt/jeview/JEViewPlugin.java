package org.eclipse.jdt.jeview;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.osgi.framework.BundleContext;

public class JEViewPlugin extends AbstractUIPlugin {

	private static JEViewPlugin fDefault;

	public JEViewPlugin() {
		fDefault= this;
	}

	public static String getPluginId() {
		return "org.eclipse.jdt.astview"; //$NON-NLS-1$
	}
	
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		fDefault= null;
	}

	/**
	 * @return the shared instance
	 */
	public static JEViewPlugin getDefault() {
		return fDefault;
	}

	/**
	 * @return the workspace instance
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}
	
	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi= new MultiStatus(getPluginId(), IStatus.ERROR, message, null);
		multi.add(status);
		log(multi);
	}
	
	public static void log(String message, Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, e)); //$NON-NLS-1$
	}
}
