/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp.launching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.StandardClasspathProvider;

/**
 * TomcatClasspathProvider
 */
public class TomcatClasspathProvider extends StandardClasspathProvider {

	/**
	 * Tomcat requires <code>tools.jar</code> and <code>bootstrap.jar</code> on its
	 * classpath.
	 *
	 * @see org.eclipse.jdt.launching.IRuntimeClasspathProvider#computeUnresolvedClasspath(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException {
		boolean useDefault = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
		if (useDefault) {
			IRuntimeClasspathEntry[] defaults = super.computeUnresolvedClasspath(configuration);
			IVMInstall vm = JavaRuntime.computeVMInstall(configuration);
			List rtes = new ArrayList();
			rtes.addAll(Arrays.asList(defaults));
			// add bootstrap.jar
			String catalinaHome = TomcatLaunchDelegate.getCatalinaHome();
			IPath path = new Path(catalinaHome).append("bin").append("bootstrap.jar"); //$NON-NLS-1$ //$NON-NLS-2$
			IRuntimeClasspathEntry r = JavaRuntime.newArchiveRuntimeClasspathEntry(path);
			r.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
			rtes.add(r);
			// add class libraries to bootpath
			boolean tools = false; // keeps track of whether a tools.jar was found
			for (LibraryLocation lib : JavaRuntime.getLibraryLocations(vm)) {
				if (lib.getSystemLibraryPath().toString().endsWith("tools.jar")) { //$NON-NLS-1$
					tools = true;
				}
			}
			if (!tools) {
				// add a tools.jar
				IPath toolsPath = new Path(vm.getInstallLocation().getAbsolutePath()).append("lib").append("tools.jar"); //$NON-NLS-1$ //$NON-NLS-2$
				if (toolsPath.toFile().exists()) {
					r = JavaRuntime.newArchiveRuntimeClasspathEntry(toolsPath);
					r.setClasspathProperty(IRuntimeClasspathEntry.USER_CLASSES);
					rtes.add(r);
				}
			}
			return (IRuntimeClasspathEntry[])rtes.toArray(new IRuntimeClasspathEntry[rtes.size()]);
		} else {
			// recover persisted classpath
			return recoverRuntimePath(configuration, IJavaLaunchConfigurationConstants.ATTR_CLASSPATH);
		}
	}

}
