/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Helper class to compute a batch of serial version IDs.
 * 
 * @since 3.2
 */
public final class SerialVersionComputationHelper {

	/** The launch configuration type */
	private static final String LAUNCH_CONFIG_TYPE= "org.eclipse.jdt.ui.serial.support"; //$NON-NLS-1$

	/** The serial support class */
	private static final String SERIAL_SUPPORT_CLASS= "org.eclipse.jdt.internal.ui.text.correction.SerialVersionComputer"; //$NON-NLS-1$

	/**
	 * Computes the serial IDs for the specified fully qualified class names.
	 * 
	 * @param classPath
	 *            the class path to use
	 * @param project
	 *            the project used to associated the private launch
	 *            configuration
	 * @param classNames
	 *            the fully qualified class names
	 * @param monitor
	 *            the progress monitor to use
	 * @return the array of serial version ids corresponding
	 * @throws CoreException
	 *             if any error occurs during serial version ID computation
	 */
	public static long[] computeSerialIDs(final IRuntimeClasspathEntry[] classPath, final IJavaProject project, final String[] classNames, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(classPath);
		Assert.isNotNull(project);
		Assert.isNotNull(classNames);
		Assert.isNotNull(monitor);
		long[] result= {};
		final ILaunchConfigurationWorkingCopy copy= DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(LAUNCH_CONFIG_TYPE).newInstance(null, LAUNCH_CONFIG_TYPE + System.currentTimeMillis());
		copy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, SERIAL_SUPPORT_CLASS);
		final StringBuffer buffer= new StringBuffer();
		for (int index= 0; index < classNames.length; index++) {
			buffer.append(classNames[index]);
			buffer.append(' ');
		}
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, buffer.toString());
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getElementName());
		final List mementos= new ArrayList(classPath.length);
		IRuntimeClasspathEntry entry= null;
		for (int index= 0; index < classPath.length; index++) {
			entry= classPath[index];
			mementos.add(entry.getMemento());
		}
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, mementos);
		final ILaunchConfiguration configuration= copy.doSave();
		try {
			final ILaunchConfigurationDelegate delegate= configuration.getType().getDelegate(ILaunchManager.RUN_MODE);
			if (delegate instanceof SerialVersionLaunchConfigurationDelegate) {
				final SerialVersionLaunchConfigurationDelegate extension= (SerialVersionLaunchConfigurationDelegate) delegate;
				configuration.launch(ILaunchManager.RUN_MODE, monitor, true, false);
				result= extension.getSerialVersionIDs();
				final String message= extension.getErrorMessage();
				if (message != null)
					throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, null));
			} else
				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, CorrectionMessages.SerialVersionHashProposal_wrong_launch_delegate, null));
		} finally {
			configuration.delete();
		}
		return result;
	}

	/**
	 * Creates a new serial version computation helper.
	 */
	private SerialVersionComputationHelper() {
		// Not for instantiation
	}
}
