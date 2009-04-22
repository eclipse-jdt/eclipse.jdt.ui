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


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.Version;

import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencesConstants;


public class BuildPathSupport {

	
	public static class JUnitPluginDescription {
		
		private final String bundleId;
		private final VersionRange versionRange;
		private final String sourceBundleId;
		private final String bundleRoot;
		private final String repositorySource;
		private final String javadocPreferenceKey;

		public JUnitPluginDescription(String bundleId, VersionRange versionRange, String bundleRoot, String sourceBundleId, String repositorySource, String javadocPreferenceKey) {
			this.bundleId= bundleId;
			this.versionRange= versionRange;
			this.sourceBundleId= sourceBundleId;
			this.bundleRoot= bundleRoot;
			this.repositorySource= repositorySource;
			this.javadocPreferenceKey= javadocPreferenceKey;
		}
		
		public IPath getBundleLocation() {
			return P2Utils.getBundleLocationPath(P2Utils.findBundle(bundleId, versionRange, false));
		}
		
		public IPath getSourceBundleLocation() {
			return getSourceLocation(P2Utils.findBundle(bundleId, versionRange, false));
		}
		
		//XXX: Official API has been requested for the provisional p2 APIs: https://bugs.eclipse.org/bugs/show_bug.cgi?id=269496
		
		public IClasspathEntry getLibraryEntry() {
			BundleInfo bundleInfo= P2Utils.findBundle(bundleId, versionRange, false);
			IPath bundleLocation= P2Utils.getBundleLocationPath(bundleInfo);
			if (bundleLocation != null) {
				
				IPath bundleRootLocation= bundleLocation;
				if (bundleRoot != null)
					bundleRootLocation= bundleLocation.append(bundleRoot);
				
				IPath srcLocation= getSourceLocation(bundleInfo);
				
				IAccessRule[] accessRules= { };

				String javadocLocation= JUnitPlugin.getDefault().getPreferenceStore().getString(javadocPreferenceKey);
				IClasspathAttribute[] attributes;
				if (javadocLocation.length() == 0) {
					attributes= new IClasspathAttribute[0];
				} else {
					attributes= new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocLocation) };
				}
				
				return JavaCore.newLibraryEntry(bundleRootLocation, srcLocation, null, accessRules, attributes, false);
			}
			return null;
		}

		private IPath getSourceLocation(BundleInfo bundleInfo) {
			if (bundleInfo == null)
				return null;
			
			IPath srcLocation= null;
			if (repositorySource != null) {
				// Try source in workspace (from repository)
				try {
					URL bundleUrl= FileLocator.toFileURL(URIUtil.toURL(bundleInfo.getLocation()));
					File bundleFile= new File(bundleUrl.getFile());
					if (bundleFile.isDirectory()) {
						File srcFile= new File(bundleFile, repositorySource);
						if (srcFile.exists()) {
							srcLocation= new Path(srcFile.getPath());
							if (srcFile.isDirectory()) {
								srcLocation= srcLocation.addTrailingSeparator();
							}
						}
					}
				} catch (MalformedURLException e) {
					//continue
				} catch (IOException e) {
					//continue
				}
			}
			
			if (srcLocation == null) {
				// Try exact version
				BundleInfo sourceBundleInfo= P2Utils.findBundle(sourceBundleId, new Version(bundleInfo.getVersion()), true);
				if (sourceBundleInfo == null) {
					// Try version range
					sourceBundleInfo= P2Utils.findBundle(sourceBundleId, versionRange, true);
				}
				srcLocation= P2Utils.getBundleLocationPath(sourceBundleInfo);
			}
			return srcLocation;
		}
	}

	
	public static final JUnitPluginDescription JUNIT3_PLUGIN= new JUnitPluginDescription(
			"org.junit", new VersionRange("[3.8.2,3.9)"), "junit.jar", "org.junit.source", "source-bundle/", JUnitPreferencesConstants.JUNIT3_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	
	private static final JUnitPluginDescription JUNIT4_PLUGIN= new JUnitPluginDescription(
			"org.junit4", new VersionRange("[4.5.0,5.0.0)"), "junit.jar", "org.junit4.source", "junitsrc.zip", JUnitPreferencesConstants.JUNIT4_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	
	private static final JUnitPluginDescription HAMCREST_CORE_PLUGIN= new JUnitPluginDescription(
			"org.hamcrest.core", new VersionRange("[1.1.0,2.0.0)"), null, "org.hamcrest.core.source", "source-bundle/", JUnitPreferencesConstants.HAMCREST_CORE_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/**
	 * @return the JUnit3 classpath container
	 */
	public static IClasspathEntry getJUnit3ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
	}

	/**
	 * @return the JUnit4 classpath container
	 */
	public static IClasspathEntry getJUnit4ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT4_PATH);
	}

	/**
	 * @return the org.junit library
	 */
	public static IClasspathEntry getJUnit3LibraryEntry() {
		return JUNIT3_PLUGIN.getLibraryEntry();
	}

	/**
	 * @return the org.junit4 library
	 */
	public static IClasspathEntry getJUnit4LibraryEntry() {
		return JUNIT4_PLUGIN.getLibraryEntry();
	}

	/**
	 * @return the org.hamcrest.core library
	 */
	public static IClasspathEntry getHamcrestCoreLibraryEntry() {
		return HAMCREST_CORE_PLUGIN.getLibraryEntry();
	}

}
