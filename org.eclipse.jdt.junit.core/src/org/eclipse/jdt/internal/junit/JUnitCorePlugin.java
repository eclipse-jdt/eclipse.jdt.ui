/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *   Julien Ruaux: jruaux@octo.com
 * 	 Vincent Massol: vmassol@octo.com
 ******************************************************************************/

package org.eclipse.jdt.internal.junit.ui;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration;
import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The plug-in runtime class for the JUnit plug-in.
 */
public class JUnitPlugin extends AbstractUIPlugin implements ILaunchListener {
	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPlugin fgPlugin= null;

	public static final String PLUGIN_ID= "org.eclipse.jdt.junit"; //$NON-NLS-1$
	public static final String ID_EXTENSION_POINT_TESTRUN_LISTENERS= PLUGIN_ID + "." + "testRunListeners"; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String ID_EXTENSION_POINT_JUNIT_LAUNCHCONFIGS= PLUGIN_ID + "." + "junitLaunchConfigs"; //$NON-NLS-1$ //$NON-NLS-2$

	public final static String TEST_SUPERCLASS_NAME= "junit.framework.TestCase"; //$NON-NLS-1$
	public final static String TEST_INTERFACE_NAME= "junit.framework.Test"; //$NON-NLS-1$
	public static final String SIMPLE_TEST_INTERFACE_NAME= "Test"; //$NON-NLS-1$
	
	/**
	 * The class path variable referring to the junit home location
	 */
	public final static String JUNIT_HOME= "JUNIT_HOME"; //$NON-NLS-1$

	private static URL fgIconBaseURL;

	/**
	 * Use to track new launches. We need to do this
	 * so that we only attach a TestRunner once to a launch.
	 * Once a test runner is connected it is removed from the set.
	 */
	private AbstractSet fTrackedLaunches= new HashSet(20);

	/**
	 * List storing the registered test run listeners
	 */
	private List fTestRunListeners;

	/**
	 * List storing the registered JUnit launch configuration types
	 */
	private List fJUnitLaunchConfigTypeIDs;


	public JUnitPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgPlugin= this;
		String pathSuffix= "icons/full/"; //$NON-NLS-1$
		try {
			fgIconBaseURL= new URL(Platform.getBundle(PLUGIN_ID).getEntry("/"), pathSuffix); //$NON-NLS-1$
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

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow activeWorkbenchWindow= getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
			return null;
		return activeWorkbenchWindow.getActivePage();
	}

	public static String getPluginId() {
		return PLUGIN_ID;
	}

	/*
	 * @see AbstractUIPlugin#initializeDefaultPreferences
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		super.initializeDefaultPreferences(store);
		JUnitPreferencePage.initializeDefaults(store);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static URL makeIconFileURL(String name) throws MalformedURLException {
		if (JUnitPlugin.fgIconBaseURL == null)
			throw new MalformedURLException();
		return new URL(JUnitPlugin.fgIconBaseURL, name);
	}

	static ImageDescriptor getImageDescriptor(String relativePath) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(relativePath));
		} catch (MalformedURLException e) {
			// should not happen
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	/*
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	public void launchRemoved(final ILaunch launch) {
		fTrackedLaunches.remove(launch);
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				TestRunnerViewPart testRunnerViewPart= findTestRunnerViewPartInActivePage();
				if (testRunnerViewPart != null && testRunnerViewPart.isCreated() && launch.equals(testRunnerViewPart.getLastLaunch()))
					testRunnerViewPart.reset();
			}
		});
	}

	/*
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
		fTrackedLaunches.add(launch);
	}

	public void connectTestRunner(ILaunch launch, IType launchedType, int port) {
		TestRunnerViewPart testRunnerViewPart= showTestRunnerViewPartInActivePage(findTestRunnerViewPartInActivePage());
		if (testRunnerViewPart != null)
			testRunnerViewPart.startTestRunListening(launchedType, port, launch);
	}

	private TestRunnerViewPart showTestRunnerViewPartInActivePage(TestRunnerViewPart testRunner) {
		IWorkbenchPart activePart= null;
		IWorkbenchPage page= null;
		try {
			// TODO: have to force the creation of view part contents 
			// otherwise the UI will not be updated
			if (testRunner != null && testRunner.isCreated())
				return testRunner;
			page= getActivePage();
			if (page == null)
				return null;
			activePart= page.getActivePart();
			//	show the result view if it isn't shown yet
			return (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME);
		} catch (PartInitException pie) {
			log(pie);
			return null;
		} finally{
			//restore focus stolen by the creation of the result view
			if (page != null && activePart != null)
				page.activate(activePart);
		}
	}

	private TestRunnerViewPart findTestRunnerViewPartInActivePage() {
		IWorkbenchPage page= getActivePage();
		if (page == null)
			return null;
		return (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
	}

	/*
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	public void launchChanged(final ILaunch launch) {
		if (!fTrackedLaunches.contains(launch))
			return;

		ILaunchConfiguration config= launch.getLaunchConfiguration();
		IType launchedType= null;
		int port= -1;
		if (config != null) {
			// test whether the launch defines the JUnit attributes
			String portStr= launch.getAttribute(JUnitBaseLaunchConfiguration.PORT_ATTR);
			String typeStr= launch.getAttribute(JUnitBaseLaunchConfiguration.TESTTYPE_ATTR);
			if (portStr != null && typeStr != null) {
				port= Integer.parseInt(portStr);
				IJavaElement element= JavaCore.create(typeStr);
				if (element instanceof IType)
					launchedType= (IType) element;
			}
		}
		if (launchedType != null) {
			fTrackedLaunches.remove(launch);
			final int finalPort= port;
			final IType finalType= launchedType;
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					connectTestRunner(launch, finalType, finalPort);
				}
			});
		}
	}

	/**
	 * @see AbstractUIPlugin#start(BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(this);
	}

	/**
	 * @see AbstractUIPlugin#stop(BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			ILaunchManager launchManager= DebugPlugin.getDefault().getLaunchManager();
			launchManager.removeLaunchListener(this);
		} finally {
			super.stop(context);
		}
	}

	public static Display getDisplay() {
//		Shell shell= getActiveWorkbenchShell();
//		if (shell != null) {
//			return shell.getDisplay();
//		}
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;
	}
	/**
	 * Utility method to create and return a selection dialog that allows
	 * selection of a specific Java package.  Empty packages are not returned.
	 * If Java Projects are provided, only packages found within those projects
	 * are included.  If no Java projects are provided, all Java projects in the
	 * workspace are considered.
	 */
	public static ElementListSelectionDialog createAllPackagesDialog(Shell shell, IJavaProject[] originals, final boolean includeDefaultPackage) throws JavaModelException {
		final List packageList= new ArrayList();
		if (originals == null) {
			IWorkspaceRoot wsroot= ResourcesPlugin.getWorkspace().getRoot();
			IJavaModel model= JavaCore.create(wsroot);
			originals= model.getJavaProjects();
		}
		final IJavaProject[] projects= originals;
		final JavaModelException[] exception= new JavaModelException[1];
		
		IRunnableWithProgress r= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				try {
					Set packageNameSet= new HashSet();
					pm.beginTask(JUnitMessages.getString("JUnitPlugin.searching"), projects.length); //$NON-NLS-1$
					for (int i= 0; i < projects.length; i++) {
						IPackageFragment[] pkgs= projects[i].getPackageFragments();
						for (int j= 0; j < pkgs.length; j++) {
							IPackageFragment pkg= pkgs[j];
							if (!pkg.hasChildren() && (pkg.getNonJavaResources().length > 0))
								continue;

							String pkgName= pkg.getElementName();
							if (!includeDefaultPackage && pkgName.length() == 0)
								continue;

							if (packageNameSet.add(pkgName))
								packageList.add(pkg);
						}
						pm.worked(1);
					}
					pm.done();
				} catch (JavaModelException jme) {
					exception[0]= jme;
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(r);
		} catch (InvocationTargetException e) {
			JUnitPlugin.log(e);
		} catch (InterruptedException e) {
			JUnitPlugin.log(e);
		}
		if (exception[0] != null)
			throw exception[0];

		int flags= JavaElementLabelProvider.SHOW_DEFAULT;
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setIgnoreCase(false);
		dialog.setElements(packageList.toArray()); // XXX inefficient
		return dialog;
	}

	/**
	 * Initializes TestRun Listener extensions
	 */
	private void loadTestRunListeners() {
		fTestRunListeners= new ArrayList();
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_TESTRUN_LISTENERS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs= extensionPoint.getConfigurationElements();
		MultiStatus status= new MultiStatus(PLUGIN_ID, IStatus.OK, "Could not load some testRunner extension points", null); //$NON-NLS-1$ 	

		for (int i= 0; i < configs.length; i++) {
			try {
				ITestRunListener testRunListener= (ITestRunListener) configs[i].createExecutableExtension("class"); //$NON-NLS-1$
				fTestRunListeners.add(testRunListener);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
		if (!status.isOK()) {
			JUnitPlugin.log(status);
		}
	}

	/**
	 * Loads the registered JUnit launch configurations
	 */
	private void loadLaunchConfigTypeIDs() {
		fJUnitLaunchConfigTypeIDs= new ArrayList();
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_JUNIT_LAUNCHCONFIGS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs= extensionPoint.getConfigurationElements();

		for (int i= 0; i < configs.length; i++) {
			String configTypeID= configs[i].getAttribute("configTypeID"); //$NON-NLS-1$
			fJUnitLaunchConfigTypeIDs.add(configTypeID);
		}
	}

	/**
	 * Returns an array of all TestRun listeners
	 */
	public List getTestRunListeners() {
		if (fTestRunListeners == null) {
			loadTestRunListeners();
		}
		return fTestRunListeners;
	}

	/**
	 * Returns an array of all JUnit launch config types
	 */
	public List getJUnitLaunchConfigTypeIDs() {
		if (fJUnitLaunchConfigTypeIDs == null) {
			loadLaunchConfigTypeIDs();
		}
		return fJUnitLaunchConfigTypeIDs;
	}

	/**
	 * Adds a TestRun listener to the collection of listeners
	 */
	public void addTestRunListener(ITestRunListener newListener) {
		if (fTestRunListeners == null) 
			loadTestRunListeners();
		for (Iterator iter= fTestRunListeners.iterator(); iter.hasNext();) {
			Object o= iter.next();
			if (o == newListener)
				return;
		}
		fTestRunListeners.add(newListener);
	}

	/**
	 * Removes a TestRun listener to the collection of listeners
	 */
	public void removeTestRunListener(ITestRunListener newListener) {
		if (fTestRunListeners != null) 
			fTestRunListeners.remove(newListener);
	}

}
