/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import java.io.IOException;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.FileLocator;
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
		
		private String resolvedVersion = null;

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
			return getBundleLocation(bundleId, versionRange);
		}
		
		public IPath getSourceBundleLocation() {
			return getSourceLocation(getBundleLocation());
		}

		private IPath getBundleLocation(String aBundleId, VersionRange aVersionRange) {
			return getBundleLocation(aBundleId, aVersionRange, false);
		}
		
		private IPath getBundleLocation(String aBundleId, VersionRange aVersionRange, boolean isSourceBundle) {
			BundleInfo bundleInfo = P2Utils.findBundle(aBundleId, aVersionRange, isSourceBundle);
			if (bundleInfo != null) {
				resolvedVersion = bundleInfo.getVersion();
				return P2Utils.getBundleLocationPath(bundleInfo);
			} else {
				// p2's simple configurator is not available. Let's try with installed bundles from the running platform.
				// Note: Source bundles are typically not available at run time!
				Bundle[] bundles= Platform.getBundles(aBundleId, aVersionRange.toString());
				Bundle bestMatch = null;
				if (bundles != null) {
					for (int i= 0; i < bundles.length; i++) {
						Bundle bundle= bundles[i];
						if (bestMatch == null || bundle.getState() > bestMatch.getState()) {
							bestMatch= bundle;
						}
					}
				}
				if (bestMatch != null) {
					try {
						resolvedVersion = bestMatch.getVersion().toString();
						URL rootUrl= bestMatch.getEntry("/"); //$NON-NLS-1$
						URL fileRootUrl= FileLocator.toFileURL(rootUrl);
						return new Path(fileRootUrl.getPath());
					} catch (IOException ex) {
						JUnitCorePlugin.log(ex);
					}
				}
			}
			return null;
		}
		
		public IClasspathEntry getLibraryEntry() {
			IPath bundleLocation = getBundleLocation(bundleId, versionRange);
			if (bundleLocation != null) {
				IPath bundleRootLocation= null;
				if (bundleRoot != null) {
					bundleRootLocation= getLocationIfExists(bundleLocation, bundleRoot);
				}
				if (bundleRootLocation == null && binaryImportedRoot != null) {
					bundleRootLocation= getLocationIfExists(bundleLocation, binaryImportedRoot);
				}
				if (bundleRootLocation == null) {
					bundleRootLocation= getBundleLocation(bundleId, versionRange);
				}

				IPath srcLocation= getSourceLocation(bundleLocation);

				String javadocLocation= Platform.getPreferencesService().getString(JUnitCorePlugin.CORE_PLUGIN_ID, javadocPreferenceKey, "", null); //$NON-NLS-1$
				IClasspathAttribute[] attributes;
				if (javadocLocation.length() == 0) {
					attributes= new IClasspathAttribute[0];
				} else {
					attributes= new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, javadocLocation) };
				}
				
				return JavaCore.newLibraryEntry(bundleRootLocation, srcLocation, null, getAccessRules(), attributes, false);
			}
			return null;
		}

		public IAccessRule[] getAccessRules() {
			return new IAccessRule[0];
		}

		private IPath getSourceLocation(IPath bundleLocation) {
			IPath srcLocation= null;
			if (repositorySource != null) {
				// Try source in workspace (from repository)
				srcLocation= getLocationIfExists(bundleLocation, repositorySource);
			}
			
			if (srcLocation == null) {
				if (bundleLocation != null) {
					// Try exact version
					Version version= new Version(resolvedVersion);
					srcLocation= getBundleLocation(sourceBundleId, new VersionRange(version, true, version, true), true);
				}
				if (srcLocation == null) {
					// Try version range
					srcLocation= getBundleLocation(sourceBundleId, versionRange, true);
				}
			}

			return srcLocation;
		}

		private IPath getLocationIfExists(IPath bundleLocationPath, final String entryInBundle) {
			IPath srcLocation= null;
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
	
	public static final JUnitPluginDescription JUNIT4_PLUGIN= new JUnitPluginDescription(
			"org.junit", new VersionRange("[4.7.0,5.0.0)"), "junit.jar", "junit.jar", "org.junit.source", "source-bundle/", JUnitPreferencesConstants.JUNIT4_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	private static final JUnitPluginDescription HAMCREST_CORE_PLUGIN= new JUnitPluginDescription(
			"org.hamcrest.core", new VersionRange("[1.1.0,2.0.0)"), null, "org.hamcrest.core_1.*.jar", "org.hamcrest.core.source", "source-bundle/", JUnitPreferencesConstants.HAMCREST_CORE_JAVADOC); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	public static final JUnitPluginDescription JUNIT4_AS_3_PLUGIN= new JUnitPluginDescription(
			JUNIT4_PLUGIN.bundleId, JUNIT4_PLUGIN.versionRange, JUNIT4_PLUGIN.bundleRoot, JUNIT4_PLUGIN.binaryImportedRoot,
			JUNIT4_PLUGIN.sourceBundleId, JUNIT4_PLUGIN.repositorySource, JUNIT3_PLUGIN.javadocPreferenceKey) {
		public IAccessRule[] getAccessRules() {
			return new IAccessRule[] {
					JavaCore.newAccessRule(new Path("junit/"), IAccessRule.K_ACCESSIBLE), //$NON-NLS-1$
					JavaCore.newAccessRule(new Path("**/*"), IAccessRule.K_NON_ACCESSIBLE) //$NON-NLS-1$
			};
		}
	};
	
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
	 * @return the org.junit version 3 library, or <code>null</code> if not available
	 */
	public static IClasspathEntry getJUnit3LibraryEntry() {
		return JUNIT3_PLUGIN.getLibraryEntry();
	}

	/**
	 * @return the org.junit version 4 library, or <code>null</code> if not available
	 */
	public static IClasspathEntry getJUnit4LibraryEntry() {
		return JUNIT4_PLUGIN.getLibraryEntry();
	}

	/**
	 * @return the org.junit version 4 library with access rules for JUnit 3, or <code>null</code> if not available
	 */
	public static IClasspathEntry getJUnit4as3LibraryEntry() {
		return JUNIT4_AS_3_PLUGIN.getLibraryEntry();
	}
	
	/**
	 * @return the org.hamcrest.core library, or <code>null</code> if not available
	 */
	public static IClasspathEntry getHamcrestCoreLibraryEntry() {
		return HAMCREST_CORE_PLUGIN.getLibraryEntry();
	}

}
