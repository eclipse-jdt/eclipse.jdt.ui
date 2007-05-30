/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencesConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * 
 */
public class BuildPathSupport {
	
	public static class JUnitPluginDescription {
		private final String fBundleId;
		private final VersionRange fVersionRange;
		private final boolean fIsOrbitBundle;
		
		public JUnitPluginDescription(String bundleId, VersionRange versionRange, boolean isOrbitBundle) {
			fBundleId= bundleId;
			fVersionRange= versionRange;
			fIsOrbitBundle= isOrbitBundle;
		}
		
		public Bundle getBundle() {
			Bundle[] bundles= JUnitPlugin.getDefault().getBundles(fBundleId, null);
			if (bundles != null) {
				for (int i= 0; i < bundles.length; i++) {
					Bundle curr= bundles[i];
					String version= (String) curr.getHeaders().get(Constants.BUNDLE_VERSION);
					try {
						if (fVersionRange.isIncluded(Version.parseVersion(version))) {
							return curr;
						}
					} catch (IllegalArgumentException e) {
						// ignore
					}
				}
			}
			return null;
		}
		
		public String getBundleId() {
			return fBundleId;
		}
		
		public boolean isOrbitBundle() {
			return fIsOrbitBundle;
		}
	}
	
	public static final JUnitPluginDescription JUNIT3_PLUGIN= new JUnitPluginDescription("org.junit", new VersionRange("[3.8.2,3.9)"), true);  //$NON-NLS-1$//$NON-NLS-2$
	public static final JUnitPluginDescription JUNIT4_PLUGIN= new JUnitPluginDescription("org.junit4", new VersionRange("[4.3.1,4.4.0)"), false); //$NON-NLS-1$ //$NON-NLS-2$
	
	public static IPath getBundleLocation(JUnitPluginDescription pluginDesc) {
		Bundle bundle= pluginDesc.getBundle();
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
	
	public static IPath getSourceLocation(JUnitPluginDescription pluginDesc) {
		Bundle bundle= pluginDesc.getBundle();
		if (bundle == null)
			return null;
			
		String version= (String)bundle.getHeaders().get(Constants.BUNDLE_VERSION);
		if (version == null) {
			return null;
		}

		Bundle sourceBundle= null;
		if (pluginDesc.isOrbitBundle()) {
			 Bundle[] bundles= JUnitPlugin.getDefault().getBundles(pluginDesc.getBundleId() + ".source", version); //$NON-NLS-1$
			 if (bundles != null && bundles.length > 0) {
				sourceBundle= bundles[0];
			 }
		} else {
			sourceBundle= JUnitPlugin.getDefault().getBundle("org.eclipse.jdt.source"); //$NON-NLS-1$
		}
		if (sourceBundle == null) {
			return null;
		}
		URL local= null;
		try {
			local= FileLocator.toFileURL(sourceBundle.getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
		String fullPath= new File(local.getPath()).getAbsolutePath() 
			+ File.separator + "src" + File.separator + pluginDesc.getBundleId() + "_" + version;   //$NON-NLS-1$ //$NON-NLS-2$
		return Path.fromOSString(fullPath);
	}
	
	public static IClasspathEntry getJUnit3ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
	}
	
	public static IClasspathEntry getJUnit4ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT4_PATH);
	}
	
	public static IClasspathEntry getJUnit3LibraryEntry() {
		IPath bundleBase= getBundleLocation(JUNIT3_PLUGIN);
		if (bundleBase != null) {
			IPath jarLocation= bundleBase.append("junit.jar"); //$NON-NLS-1$
			
			IPath sourceBase= getSourceLocation(JUNIT3_PLUGIN);
			IPath srcLocation= sourceBase != null ? sourceBase.append("junitsrc.zip") : null; //$NON-NLS-1$
			
			IAccessRule[] accessRules= { };
			
			String javadocLocation= JUnitPlugin.getDefault().getPreferenceStore().getString(JUnitPreferencesConstants.JUNIT3_JAVADOC);
			IClasspathAttribute[] attributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocLocation) };
			
			return JavaCore.newLibraryEntry(jarLocation, srcLocation, null, accessRules, attributes, false);
		}
		return null;
	}
	
	public static IClasspathEntry getJUnit4LibraryEntry() {
		IPath bundleBase= getBundleLocation(JUNIT4_PLUGIN);
		if (bundleBase != null) {
			IPath jarLocation= bundleBase.append("junit.jar"); //$NON-NLS-1$
			
			IPath sourceBase= getSourceLocation(JUNIT4_PLUGIN);
			IPath srcLocation= sourceBase != null ? sourceBase.append("junitsrc.zip") : null; //$NON-NLS-1$
			
			IAccessRule[] accessRules= { };
			
			String javadocLocation= JUnitPlugin.getDefault().getPreferenceStore().getString(JUnitPreferencesConstants.JUNIT4_JAVADOC);
			IClasspathAttribute[] attributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocLocation) };
			
			return JavaCore.newLibraryEntry(jarLocation, srcLocation, null, accessRules, attributes, false);
		}
		return null;
	}
}
