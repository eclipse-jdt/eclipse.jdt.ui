/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.internal.junit.oldlauncher.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILauncher;

import org.eclipse.debug.ui.actions.RunAction;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

/**
 * The plug-in runtime class for the JUnit plug-in.
 */
public class JUnitPlugin extends AbstractUIPlugin implements ILaunchListener {	
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPlugin fgPlugin= null;
	
	public static final String PLUGIN_ID = "org.eclipse.jdt.junit" ; //$NON-NLS-1$

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
	
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow= getActiveWorkbenchWindow();
		if (workBenchWindow == null) 
			return null;
		return workBenchWindow.getShell();
	}
	
	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (fgPlugin == null) 
			return null;
		IWorkbench workBench= fgPlugin.getWorkbench();
		if (workBench == null) 
			return null;
		return workBench.getActiveWorkbenchWindow();
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
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); 
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static URL makeIconFileURL(String name) throws MalformedURLException {
		if (JUnitPlugin.fgIconBaseURL == null)
			throw new MalformedURLException();
		return new URL(JUnitPlugin.fgIconBaseURL, name);
	}

	/*
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
	}

	/*
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
		ILauncher launcher=launch.getLauncher();
		IType launchedType= null;
		int port= -1;
		
		if (launcher != null && launcher.getDelegate() instanceof IJUnitLauncherDelegate) {
			// old launchers
			IJUnitLauncherDelegate launcherDelegate= (IJUnitLauncherDelegate)launch.getLauncher().getDelegate();
			launchedType= launcherDelegate.getLaunchedType();
			port= launcherDelegate.getPort();
		} else {
			// new launch configs
			ILaunchConfiguration config= launch.getLaunchConfiguration();
			if (config != null) {
				try {
					ILaunchConfigurationType type= config.getType(); 
					// TO DO should not know about org.eclipse.pde.junit.launchconfig
					if (type.getIdentifier().equals("org.eclipse.jdt.junit.launchconfig") ||
						type.getIdentifier().equals("org.eclipse.pde.junit.launchconfig")) {
						port= config.getAttribute(JUnitBaseLaunchConfiguration.PORT_ATTR, 4500);
						String testTypeHandle= config.getAttribute(JUnitBaseLaunchConfiguration.TESTTYPE_ATTR, "");
						IJavaElement element= JavaCore.create(testTypeHandle);
						if (element instanceof IType) 
							launchedType= (IType)element;
					}
				} catch(CoreException e) {
					ErrorDialog.openError(getActiveWorkbenchShell(), 
						"Could not show JUnit Result View", e.getMessage(), e.getStatus()
					);
				}
			}	
		}
		if (launchedType != null) 
			connectTestRunner(launch, launchedType, port);
	}

	public void connectTestRunner(ILaunch launch, IType launchedType, int port) {
		IWorkbenchWindow window= getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		TestRunnerViewPart testRunner= null;
		
		try {
			testRunner= (TestRunnerViewPart)page.showView(TestRunnerViewPart.NAME);
		} catch (PartInitException e) {
			ErrorDialog.openError(getActiveWorkbenchShell(), 
				"Could not show JUnit Result View", e.getMessage(), e.getStatus()
			);
		}
		if (testRunner != null)
			testRunner.startTestRunListening(launchedType, port, launch.getLaunchMode());	
	}

	/*
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}
	
	/*
	 * @see Plugin#startup()
	 */
	public void startup() throws CoreException {
		super.startup();
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(this);	
	}

	/*
	 * @see Plugin#shutdown()
	 */
	public void shutdown() throws CoreException {
		super.shutdown();
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchListener(this);
	}

	public static Display getDisplay() {
		Shell shell= getActiveWorkbenchShell();
		if (shell != null) {
			return shell.getDisplay();
		}
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;
	}		

}
