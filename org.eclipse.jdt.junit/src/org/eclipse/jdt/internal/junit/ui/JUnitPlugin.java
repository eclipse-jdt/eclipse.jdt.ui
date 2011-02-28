/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com
 * 	   Vincent Massol: vmassol@octo.com
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plug-in runtime class for the JUnit plug-in.
 */
public class JUnitPlugin extends AbstractUIPlugin {

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static JUnitPlugin fgPlugin= null;

	public static final String PLUGIN_ID= "org.eclipse.jdt.junit"; //$NON-NLS-1$
	public static final String ID_EXTENSION_POINT_JUNIT_LAUNCHCONFIGS= PLUGIN_ID + "." + "junitLaunchConfigs"; //$NON-NLS-1$ //$NON-NLS-2$

	private static final IPath ICONS_PATH= new Path("$nl$/icons/full"); //$NON-NLS-1$

	/**
	 * List storing the registered JUnit launch configuration types
	 */
	private List<String> fJUnitLaunchConfigTypeIDs;

	private BundleContext fBundleContext;

	private static boolean fIsStopped= false;


	public JUnitPlugin() {
		fgPlugin= this;
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

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static ImageDescriptor getImageDescriptor(String relativePath) {
		IPath path= ICONS_PATH.append(relativePath);
		return createImageDescriptor(getDefault().getBundle(), path, true);
	}

	public static Image createImage(String path) {
		return getImageDescriptor(path).createImage();
	}

	/**
	 * Sets the three image descriptors for enabled, disabled, and hovered to an action. The actions
	 * are retrieved from the *lcl16 folders.
	 *
	 * @param action the action
	 * @param iconName the icon name
	 */
	public static void setLocalImageDescriptors(IAction action, String iconName) {
		setImageDescriptors(action, "lcl16", iconName); //$NON-NLS-1$
	}

	private static void setImageDescriptors(IAction action, String type, String relPath) {
		ImageDescriptor id= createImageDescriptor("d" + type, relPath, false); //$NON-NLS-1$
		if (id != null)
			action.setDisabledImageDescriptor(id);

		ImageDescriptor descriptor= createImageDescriptor("e" + type, relPath, true); //$NON-NLS-1$
		action.setHoverImageDescriptor(descriptor);
		action.setImageDescriptor(descriptor);
	}

	/*
	 * Creates an image descriptor for the given prefix and name in the JDT UI bundle. The path can
	 * contain variables like $NL$.
	 * If no image could be found, <code>useMissingImageDescriptor</code> decides if either
	 * the 'missing image descriptor' is returned or <code>null</code>.
	 * or <code>null</code>.
	 */
	private static ImageDescriptor createImageDescriptor(String pathPrefix, String imageName, boolean useMissingImageDescriptor) {
		IPath path= ICONS_PATH.append(pathPrefix).append(imageName);
		return createImageDescriptor(JUnitPlugin.getDefault().getBundle(), path, useMissingImageDescriptor);
	}

	/**
	 * Creates an image descriptor for the given path in a bundle. The path can
	 * contain variables like $NL$. If no image could be found,
	 * <code>useMissingImageDescriptor</code> decides if either the 'missing
	 * image descriptor' is returned or <code>null</code>.
	 *
	 * @param bundle a bundle
	 * @param path path in the bundle
	 * @param useMissingImageDescriptor if <code>true</code>, returns the shared image descriptor
	 *            for a missing image. Otherwise, returns <code>null</code> if the image could not
	 *            be found
	 * @return an {@link ImageDescriptor}, or <code>null</code> iff there's
	 *         no image at the given location and
	 *         <code>useMissingImageDescriptor</code> is <code>true</code>
	 */
	private static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path, boolean useMissingImageDescriptor) {
		URL url= FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		if (useMissingImageDescriptor) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
		return null;
	}

	/**
	 * @see AbstractUIPlugin#start(BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext= context;
	}

	/**
	 * @see AbstractUIPlugin#stop(BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		fIsStopped= true;
		super.stop(context);
		fBundleContext= null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 * 
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 * @since 3.5
	 */
	public Object getService(String serviceName) {
		ServiceReference<?> reference= fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return fBundleContext.getService(reference);
	}

	/**
	 * Loads the registered JUnit launch configurations
	 */
	private void loadLaunchConfigTypeIDs() {
		fJUnitLaunchConfigTypeIDs= new ArrayList<String>();
		IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(ID_EXTENSION_POINT_JUNIT_LAUNCHCONFIGS);
		if (extensionPoint == null) {
			return;
		}
		IConfigurationElement[] configs= extensionPoint.getConfigurationElements();

		for (IConfigurationElement config : configs) {
			String configTypeID= config.getAttribute("configTypeID"); //$NON-NLS-1$
			fJUnitLaunchConfigTypeIDs.add(configTypeID);
		}
	}

	/**
	 * @return a list of all JUnit launch configuration types
	 */
	public List<String> getJUnitLaunchConfigTypeIDs() {
		if (fJUnitLaunchConfigTypeIDs == null) {
			loadLaunchConfigTypeIDs();
		}
		return fJUnitLaunchConfigTypeIDs;
	}

	/**
	 * Returns the bundle for a given bundle name,
	 * regardless whether the bundle is resolved or not.
	 *
	 * @param bundleName the bundle name
	 * @return the bundle
	 * @since 3.2
	 */
	public Bundle getBundle(String bundleName) {
		Bundle[] bundles= getBundles(bundleName, null);
		if (bundles != null && bundles.length > 0)
			return bundles[0];
		return null;
	}

	/**
	 * Returns the bundles for a given bundle name,
	 *
	 * @param bundleName the bundle name
	 * @param version the version of the bundle
	 * @return the bundles of the given name
	 */
	public Bundle[] getBundles(String bundleName, String version) {
		Bundle[] bundles= Platform.getBundles(bundleName, version);
		if (bundles != null)
			return bundles;

		// Accessing unresolved bundle
		ServiceReference<PackageAdmin> serviceRef= fBundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin admin= fBundleContext.getService(serviceRef);
		bundles= admin.getBundles(bundleName, version);
		if (bundles != null && bundles.length > 0)
			return bundles;
		return null;
	}

	public static boolean isStopped() {
		return fIsStopped;
	}

	public IDialogSettings getDialogSettingsSection(String name) {
		IDialogSettings dialogSettings= getDialogSettings();
		IDialogSettings section= dialogSettings.getSection(name);
		if (section == null) {
			section= dialogSettings.addNewSection(name);
		}
		return section;
	}
	
	public static void asyncShowTestRunnerViewPart() {
		getDisplay().asyncExec(new Runnable() {
			public void run() {
				showTestRunnerViewPartInActivePage();
			}
		});
	}

	public static TestRunnerViewPart showTestRunnerViewPartInActivePage() {
		try {
			// Have to force the creation of view part contents
			// otherwise the UI will not be updated
			IWorkbenchPage page= JUnitPlugin.getActivePage();
			if (page == null)
				return null;
			TestRunnerViewPart view= (TestRunnerViewPart) page.findView(TestRunnerViewPart.NAME);
			if (view == null) {
				//	create and show the result view if it isn't created yet.
				return (TestRunnerViewPart) page.showView(TestRunnerViewPart.NAME, null, IWorkbenchPage.VIEW_VISIBLE);
			} else {
				return view;
			}
		} catch (PartInitException pie) {
			JUnitPlugin.log(pie);
			return null;
		}
	}
	
	private static Display getDisplay() {
//			Shell shell= getActiveWorkbenchShell();
//			if (shell != null) {
//				return shell.getDisplay();
//			}
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;
	}
}
