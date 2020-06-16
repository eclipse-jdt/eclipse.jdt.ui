/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.util;


import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

public class ClasspathVMUtil {

	public static IVMInstall findRequiredOrGreaterVMInstall(String requiredVersion, boolean allowNullCompliance, boolean getHighestVersion) {
		if (requiredVersion == null) {
			return null;
		}
		String bestMatchingCompliance= null;
		IVMInstall bestMatchingVMInstall= null;
		for (IVMInstallType installType : JavaRuntime.getVMInstallTypes()) {
			for (IVMInstall install : installType.getVMInstalls()) {
				String vmInstallCompliance= getVMInstallCompliance(install, allowNullCompliance);
				if (requiredVersion.equals(vmInstallCompliance)) {
					if (!getHighestVersion) {
						return install; // perfect match
					}
				} else if (JavaModelUtil.isVersionLessThan(vmInstallCompliance, requiredVersion)) {
					continue; // no match

				} else if (bestMatchingVMInstall != null) {
					if ((!getHighestVersion && JavaModelUtil.isVersionLessThan(bestMatchingCompliance, vmInstallCompliance))
						|| (getHighestVersion && JavaModelUtil.isVersionLessThan(vmInstallCompliance, bestMatchingCompliance))) {
						continue; // the other one is the least matching
					}
				}
				if (getHighestVersion) {
					if (JavaModelUtil.isVersionLessThan(bestMatchingCompliance, vmInstallCompliance)) {
						bestMatchingCompliance= vmInstallCompliance;
						bestMatchingVMInstall= install;
					}
				} else {
					bestMatchingCompliance= vmInstallCompliance;
					bestMatchingVMInstall= install;
				}
			}
		}
		if (getHighestVersion) {
			return bestMatchingVMInstall;
		}
		return null;
	}

	public static String getVMInstallCompliance(IVMInstall install, boolean allowNullCompliance) {
		String defCompliance1= JavaCore.VERSION_1_1;
		String defCompliance2= JavaCore.VERSION_1_3;
		if (allowNullCompliance) {
			defCompliance1= null;
			defCompliance2= null;
		}
		if (install instanceof IVMInstall2) {
			String compliance= JavaModelUtil.getCompilerCompliance((IVMInstall2) install, defCompliance2);
			return compliance;
		}
		return defCompliance1;
	}

	public static IExecutionEnvironment findBestMatchingEE(String requiredVersion) {
		IExecutionEnvironmentsManager eeManager= JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment bestEE= null;
		String bestEECompliance= null;

		for (IExecutionEnvironment ee : eeManager.getExecutionEnvironments()) {
			String eeCompliance= JavaModelUtil.getExecutionEnvironmentCompliance(ee);
			String eeId= ee.getId();

			if (requiredVersion.equals(eeCompliance)) {
				if (eeId.startsWith("J") && eeId.endsWith(requiredVersion)) { //$NON-NLS-1$
					bestEE= ee;
					break; // perfect match
				}

			} else if (JavaModelUtil.isVersionLessThan(eeCompliance, requiredVersion)) {
				continue; // no match

			} else { // possible match
				if (bestEE != null) {
					if (!eeId.startsWith("J")) { //$NON-NLS-1$
						continue; // avoid taking e.g. OSGi profile if a Java profile is available
					}
					if (JavaModelUtil.isVersionLessThan(bestEECompliance, eeCompliance)) {
						continue; // the other one is the least matching
					}
				}
			}
			// found a new best
			bestEE= ee;
			bestEECompliance= eeCompliance;
		}
		return bestEE;
	}

	public static boolean updateClasspath(IPath newPath, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
		boolean updated= false;

		IClasspathEntry[] classpath= project.getRawClasspath();
		IPath jreContainerPath= new Path(JavaRuntime.JRE_CONTAINER);
		for (int i= 0; i < classpath.length; i++) {
			IClasspathEntry curr= classpath[i];
			if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER && curr.getPath().matchingFirstSegments(jreContainerPath) > 0) {
				if (!newPath.equals(curr.getPath())) {
					updated= true;
					classpath[i]= JavaCore.newContainerEntry(newPath, curr.getAccessRules(), curr.getExtraAttributes(), curr.isExported());
				}
			}
		}
		if (updated) {
			project.setRawClasspath(classpath, monitor);
		}
		return updated;
	}

	private ClasspathVMUtil() {
	}
}
