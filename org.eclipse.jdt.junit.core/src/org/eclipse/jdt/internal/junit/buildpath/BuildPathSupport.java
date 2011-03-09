/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import java.io.FilenameFilter;

import org.osgi.framework.Version;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;


public class BuildPathSupport {

	
	public static class JUnitPluginDescription {
		
		private final String bundleId;
		private final VersionRange versionRange;
		private final String bundleRoot;
		private final String binaryImportedRoot;
		private final String sourceBundleId;
		private final String repositorySource;
		private final String javadocPreferenceKey;

		public JUnitPluginDescription(String bundleId, VersionRange versionRange, String bundleRoot, String binaryImportedRoot, String sourceBundleId, String repositorySource, String javadocPreferenceKey) {
			this.bundleId= bundleId;
			this.versionRange= versionRange;
			this.bundleRoot= bundleRoot;
			this.binaryImportedRoot= binaryImportedRoot;
			this.sourceBundleId= sourceBundleId;
			this.repositorySource= repositorySource;
			this.javadocPreferenceKey= javadocPreferenceKey;
		}
		
		public IPath getBundleLocation() {
			return P2Utils.getBundleLocationPath(P2Utils.findBundle(bundleId, versionRange, false));
		}
		
		public IPath getSourceBundleLocation() {
			return getSourceLocation(P2Utils.findBundle(bundleId, versionRange, false));
		}
		
		public IClasspathEntry getLibraryEntry() {
			BundleInfo bundleInfo= P2Utils.findBundle(bundleId, versionRange, false);
			IPath bundleLocation= P2Utils.getBundleLocationPath(bundleInfo);
			if (bundleLocation != null) {
				
				IPath bundleRootLocation= getLibraryLocation(bundleInfo, bundleLocation);
				IPath srcLocation= getSourceLocation(bundleInfo);
				
				IAccessRule[] accessRules= { };

				String javadocLocation= Platform.getPreferencesService().getString(JUnitCorePlugin.CORE_PLUGIN_ID, javadocPreferenceKey, "", null); //$NON-NLS-1$
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

		private IPath getLibraryLocation(BundleInfo bundleInfo, IPath bundleLocation) {
			IPath bundleRootLocation= null;
			if (bundleRoot != null)
				bundleRootLocation= getLocationIfExists(bundleInfo, bundleRoot);
			
			if (bundleRootLocation == null && binaryImportedRoot != null)
				bundleRootLocation= getLocationIfExists(bundleInfo, binaryImportedRoot);
			
			if (bundleRootLocation == null)
				bundleRootLocation= bundleLocation;
			return bundleRootLocation;
		}

		private IPath getSourceLocation(BundleInfo bundleInfo) {
			IPath srcLocation= null;
			if (repositorySource != null) {
				// Try source in workspace (from repository)
				srcLocation= getLocationIfExists(bundleInfo, repositorySource);
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

		private IPath getLocationIfExists(BundleInfo bundleInfo, final String entryInBundle) {
			IPath srcLocation= null;
			IPath bundleLocationPath= P2Utils.getBundleLocationPath(bundleInfo);
			if (bundleLocationPath != null) {
				File bundleFile= bundleLocationPath.toFile();
				if (bundleFile.isDirectory()) {
					File srcFile= null;
					final int starIdx= entryInBundle.indexOf('*');
					if (starIdx != -1) {
						File[] files= bundleFile.listFiles(new FilenameFilter() {
							private String pre= entryInBundle.substring(0, starIdx);
							private String post= entryInBundle.substring(starIdx + 1);
							public boolean accept(File dir, String name) {
								return name.startsWith(pre) && name.endsWith(post);
							}
						});
						if (files.length > 0) {
							srcFile= files[0];
						}
					}
					if (srcFile == null)
						srcFile= new File(bundleFile, entryInBundle);
					if (srcFile.exists()) {
						srcLocation= new Path(srcFile.getPath());
						if (srcFile.isDirectory()) {
							srcLocation= srcLocation.addTrailingSeparator();
						}
					}
				}
			}
			return srcLocation;
		}
	}

	
	public static final JUnitPluginDescription JUNIT3_PLUGIN= new JUnitPluginDescription(
			"org.junit", new VersionRange("[3.8.2,3.9)"), "junit.jar", "junit.jar", "org.junit.source", "source-bundle/", JUnitPreferencesConstants.JUNIT3_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	private static final JUnitPluginDescription JUNIT4_PLUGIN= new JUnitPluginDescription(
			"org.junit", new VersionRange("[4.7.0,5.0.0)"), "junit.jar", "junit.jar", "org.junit.source", "source-bundle/", JUnitPreferencesConstants.JUNIT4_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	private static final JUnitPluginDescription HAMCREST_CORE_PLUGIN= new JUnitPluginDescription(
			"org.hamcrest.core", new VersionRange("[1.1.0,2.0.0)"), null, "org.hamcrest.core_1.*.jar", "org.hamcrest.core.source", "source-bundle/", JUnitPreferencesConstants.HAMCREST_CORE_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	/**
	 * @return the JUnit3 classpath container
	 */
	public static IClasspathEntry getJUnit3ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitCore.JUNIT3_CONTAINER_PATH);
	}

	/**
	 * @return the JUnit4 classpath container
	 */
	public static IClasspathEntry getJUnit4ClasspathEntry() {
		return JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH);
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
