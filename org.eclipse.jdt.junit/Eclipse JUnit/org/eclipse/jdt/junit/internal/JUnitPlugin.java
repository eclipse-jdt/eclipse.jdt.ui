/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the JUnitUI plug-in.
 */
public final class JUnitPlugin extends AbstractUIPlugin {

	/**
	 * Unique identifier constant (value <code>"org.eclipse.jdt.junit.viewpart"</code>)
	 * for the JUnit UI plug-in.
	 */
	public static final String fgViewPartName= "org.eclipse.jdt.junit.viewpart";
	
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPlugin fgPlugin= null;

	public JUnitPlugin(IPluginDescriptor desc) {
		super(desc);
		fgPlugin= this;
	}
		
	public static JUnitPlugin getDefault() {
		return fgPlugin;
	}
	
	public static Shell getActiveShell() {
		if (fgPlugin == null) return null;
		IWorkbench workBench= fgPlugin.getWorkbench();
		if (workBench == null) return null;
		IWorkbenchWindow workBenchWindow= workBench.getActiveWorkbenchWindow();
		if (workBenchWindow == null) return null;
		return workBenchWindow.getShell();
	}
	
	public static String getPluginID() {
		if (fgPlugin == null) return null;
		IPluginDescriptor desc= fgPlugin.getDescriptor();
		if (desc == null) return null;
		return desc.getUniqueIdentifier();
	}
}
