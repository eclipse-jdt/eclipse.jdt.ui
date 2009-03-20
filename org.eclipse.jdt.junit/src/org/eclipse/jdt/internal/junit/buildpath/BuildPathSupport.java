/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;


import org.osgi.framework.Version;

import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencesConstants;


public class BuildPathSupport {

	
	public static class JUnitPluginDescription {
		
		private final String bundleId;
		private final String sourceBundleId;
		private final VersionRange versionRange;

		public JUnitPluginDescription(String bundleId, String sourceBundleId, VersionRange versionRange) {
			this.bundleId= bundleId;
			this.sourceBundleId= sourceBundleId;
			this.versionRange= versionRange;
		}
	}

	
	public static final JUnitPluginDescription JUNIT3_PLUGIN= new JUnitPluginDescription("org.junit", "org.junit.source", new VersionRange("[3.8.2,3.9)")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	public static final JUnitPluginDescription JUNIT4_PLUGIN= new JUnitPluginDescription("org.junit4", "org.junit4.source", new VersionRange("[4.5.0,5.0.0)")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	public static IPath getBundleLocation(JUnitPluginDescription pluginDesc) {
		return P2Utils.getBundleLocationPath(P2Utils.findBundle(pluginDesc.bundleId, pluginDesc.versionRange, false));
	}

	public static IPath getSourceBundleLocation(JUnitPluginDescription pluginDesc) {
		return P2Utils.getBundleLocationPath(P2Utils.findBundle(pluginDesc.sourceBundleId, pluginDesc.versionRange, true));
	}

	public static IClasspathEntry getJUnit3ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
	}

	public static IClasspathEntry getJUnit4ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT4_PATH);
	}

	public static IClasspathEntry getJUnit3LibraryEntry() {
		return getJUnitLibraryEntry(JUNIT3_PLUGIN, JUnitPreferencesConstants.JUNIT3_JAVADOC);
	}

	public static IClasspathEntry getJUnit4LibraryEntry() {
		return getJUnitLibraryEntry(JUNIT4_PLUGIN, JUnitPreferencesConstants.JUNIT4_JAVADOC);
	}

	// Official API has been requested for the provisional p2 APIs: https://bugs.eclipse.org/bugs/show_bug.cgi?id=269496
	private static IClasspathEntry getJUnitLibraryEntry(JUnitPluginDescription pluginDesc, String javadocPreferenceKey) {
		BundleInfo bundleInfo= P2Utils.findBundle(pluginDesc.bundleId, pluginDesc.versionRange, false);
		IPath bundleBase= P2Utils.getBundleLocationPath(bundleInfo);
		if (bundleBase != null) {

			// Try exact version
			BundleInfo sourceBundleInfo= P2Utils.findBundle(pluginDesc.sourceBundleId, new Version(bundleInfo.getVersion()), true);
			if (sourceBundleInfo == null) {
				// Try exact version range
				sourceBundleInfo= P2Utils.findBundle(pluginDesc.sourceBundleId, pluginDesc.versionRange, true);
			}

			IPath jarLocation= bundleBase.append("junit.jar"); //$NON-NLS-1$
			IPath srcLocation= P2Utils.getBundleLocationPath(sourceBundleInfo);

			IAccessRule[] accessRules= { };

			String javadocLocation= JUnitPlugin.getDefault().getPreferenceStore().getString(javadocPreferenceKey);
			IClasspathAttribute[] attributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocLocation) };

			return JavaCore.newLibraryEntry(jarLocation, srcLocation, null, accessRules, attributes, false);
		}
		return null;
	}

}
