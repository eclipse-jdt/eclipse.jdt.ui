/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsp.launching;

import java.util.ArrayList;
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
	public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException {
		boolean useDefault = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
		if (useDefault) {
			IVMInstall vm = JavaRuntime.computeVMInstall(configuration);
			LibraryLocation[] libs = JavaRuntime.getLibraryLocations(vm);
			if (libs == null) {
				return new IRuntimeClasspathEntry[0];
			} else {
				List rtes = new ArrayList();
				// add bootstrap.jar
				String catalinaHome = TomcatLaunchDelegate.getCatalinaHome();
				IPath path = new Path(catalinaHome).append("bin").append("bootstrap.jar"); //$NON-NLS-1$ //$NON-NLS-2$
				IRuntimeClasspathEntry r = JavaRuntime.newArchiveRuntimeClasspathEntry(path);
				rtes.add(r);
				// add class libraries to bootpath				
				for (int i = 0; i < libs.length; i++) {
					r = JavaRuntime.newArchiveRuntimeClasspathEntry(libs[i].getSystemLibraryPath());
					r.setSourceAttachmentPath(libs[i].getSystemLibrarySourcePath());
					r.setSourceAttachmentRootPath(libs[i].getPackageRootPath());
					r.setClasspathProperty(IRuntimeClasspathEntry.STANDARD_CLASSES);
					rtes.add(r);
				}
				return (IRuntimeClasspathEntry[])rtes.toArray(new IRuntimeClasspathEntry[rtes.size()]);
			}				
		} else {
			// recover persisted classpath
			return recoverRuntimePath(configuration, IJavaLaunchConfigurationConstants.ATTR_CLASSPATH);
		}
	}

}
