/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.internal.ui.JavaStatusConstants;

/**
 * The plug-in runtime class for the JUnit plug-in.
 */
public class JUnitPlugin extends AbstractUIPlugin {	
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPlugin fgPlugin= null;
	private static URL fgIconBaseURL;
	
	public JUnitPlugin(IPluginDescriptor desc) {
		super(desc);
		fgPlugin= this;
		String pathSuffix= "icons/"; //$NON-NLS-1$
		try {
			fgIconBaseURL= new URL(getDescriptor().getInstallURL(), pathSuffix);
		} catch (MalformedURLException e) {
			// do nothing
		}
	}
		
	public static JUnitPlugin getDefault() {
		return fgPlugin;
	}
	
	public static Shell getActiveShell() {
		if (fgPlugin == null) 
			return null;
		IWorkbench workBench= fgPlugin.getWorkbench();
		if (workBench == null) 
			return null;
		IWorkbenchWindow workBenchWindow= workBench.getActiveWorkbenchWindow();
		if (workBenchWindow == null) 
			return null;
		return workBenchWindow.getShell();
	}
	
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	
	/*
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		super.initializeDefaultPreferences(store);	
		JUnitPreferencePage.initializeDefaults(store);
	}
	
	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), JavaStatusConstants.INTERNAL_ERROR, "JUnitPlugin internal error", e)); 
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static URL makeIconFileURL(String name) throws MalformedURLException {
		if (JUnitPlugin.fgIconBaseURL == null)
			throw new MalformedURLException();
		return new URL(JUnitPlugin.fgIconBaseURL, name);
	}

}
