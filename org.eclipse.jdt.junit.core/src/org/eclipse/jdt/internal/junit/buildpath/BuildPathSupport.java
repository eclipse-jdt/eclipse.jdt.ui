/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.buildpath;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

import org.osgi.framework.Bundle;

/**
 * 
 */
public class BuildPathSupport {
	
	public static final String JUNIT3_PLUGIN_ID= "org.junit"; //$NON-NLS-1$
	public static final String JUNIT4_PLUGIN_ID= "org.junit4"; //$NON-NLS-1$
	
	public static IPath getBundleLocation(String bundleName) {
		Bundle bundle= JUnitPlugin.getDefault().getBundle(bundleName);
		if (bundle == null)
			return null;
		
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
		Bundle bundle= JUnitPlugin.getDefault().getBundle(bundleName);
		if (bundle == null)
			return null;
			
		String version= (String)bundle.getHeaders().get("Bundle-Version"); //$NON-NLS-1$
		if (version == null) {
			return null;
		}
		bundle= JUnitPlugin.getDefault().getBundle("org.eclipse.jdt.source"); //$NON-NLS-1$
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
	
	public static IClasspathEntry getJUnit3ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
	}
	
	public static IClasspathEntry getJUnit4ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT4_PATH);
	}
	
	public static IClasspathEntry getJUnit3LibraryEntry() {
		IPath bundleBase= getBundleLocation(JUNIT3_PLUGIN_ID);
		if (bundleBase != null) {
			IPath jarLocation= bundleBase.append("junit.jar"); //$NON-NLS-1$
			
			IPath sourceBase= getSourceLocation(JUNIT3_PLUGIN_ID);
			IPath srcLocation= sourceBase != null ? sourceBase.append("junitsrc.zip") : null; //$NON-NLS-1$
			
			IAccessRule[] accessRules= { };
			
			String javadocLocation= "http://www.junit.org/junit/javadoc/3.8.1"; //$NON-NLS-1$
			IClasspathAttribute[] attributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocLocation) };
			
			return JavaCore.newLibraryEntry(jarLocation, srcLocation, null, accessRules, attributes, false);
		}
		return null;
	}
	
	public static IClasspathEntry getJUnit4LibraryEntry() {
		IPath bundleBase= getBundleLocation(JUNIT4_PLUGIN_ID);
		if (bundleBase != null) {
			IPath jarLocation= bundleBase.append("junit.jar"); //$NON-NLS-1$
			
			IPath sourceBase= getSourceLocation(JUNIT4_PLUGIN_ID);
			IPath srcLocation= sourceBase != null ? sourceBase.append("junitsrc.zip") : null; //$NON-NLS-1$
			
			IAccessRule[] accessRules= { };
			
			IClasspathAttribute[] attributes= { };// no complete reference found yet
			
			return JavaCore.newLibraryEntry(jarLocation, srcLocation, null, accessRules, attributes, false);
		}
		return null;
	}
	
}
