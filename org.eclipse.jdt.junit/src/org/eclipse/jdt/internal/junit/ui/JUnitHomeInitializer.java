/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;

public class JUnitHomeInitializer extends ClasspathVariableInitializer {	
	/**
	 * @see ClasspathVariableInitializer#initialize(String)
	 */
	public void initialize(String variable) {
		if (JUnitPlugin.JUNIT_HOME.equals(variable)) {
			initializeHome();
		} else if (JUnitPlugin.JUNIT_SRC_HOME.equals(variable)) {
			initializeSource();
		}
	}

	private void initializeHome() {
		try {
			IPath location= getBundleLocation("org.junit"); //$NON-NLS-1$
			if (location != null) {
				JavaCore.setClasspathVariable(JUnitPlugin.JUNIT_HOME, location, null);
			} else {
				JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
			}
		} catch (JavaModelException e1) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_HOME, null);
		}
	}
	
	private void initializeSource() {
		try {
			IPath sourceLocation= getSourceLocation("org.junit"); //$NON-NLS-1$
			if (sourceLocation != null) {
				JavaCore.setClasspathVariable(JUnitPlugin.JUNIT_SRC_HOME, sourceLocation, null);
			} else {
				JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_SRC_HOME, null);
			}
		} catch (JavaModelException e1) {
			JavaCore.removeClasspathVariable(JUnitPlugin.JUNIT_SRC_HOME, null);
		}
	}
	
	public static IPath getBundleLocation(String bundleName) {
		Bundle bundle= Platform.getBundle(bundleName);
		if (bundle == null) {
			return null;
		}
		URL local= null;
		try {
			local= FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
		String fullPath= new File(local.getPath()).getAbsolutePath();
		return Path.fromOSString(fullPath);
	}
	
	public static IPath getSourceLocation(String bundleName) {
		Bundle bundle= Platform.getBundle(bundleName);
		if (bundle == null) {
			return null;
		}
		String version= (String)bundle.getHeaders().get("Bundle-Version"); //$NON-NLS-1$
		if (version == null) {
			return null;
		}
		bundle= Platform.getBundle("org.eclipse.jdt.source"); //$NON-NLS-1$
		if (bundle == null) {
			return null;
		}
		URL local= null;
		try {
			local= FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
		String fullPath= new File(local.getPath()).getAbsolutePath() 
			+ File.separator + "src" + File.separator + bundleName + "_" + version;   //$NON-NLS-1$ //$NON-NLS-2$
		return Path.fromOSString(fullPath);
	}
	
	
}