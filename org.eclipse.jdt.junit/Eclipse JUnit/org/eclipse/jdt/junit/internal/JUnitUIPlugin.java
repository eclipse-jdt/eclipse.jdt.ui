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
public final class JUnitUIPlugin extends AbstractUIPlugin {
	
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitUIPlugin fgPlugin= null;

	public JUnitUIPlugin(IPluginDescriptor desc) {
		super(desc);
		fgPlugin= this;
	}
		
	public static JUnitUIPlugin getDefault() {
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
