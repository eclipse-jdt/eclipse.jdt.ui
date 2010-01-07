/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Patrick Higgins <patrick133t@yahoo.com> - [JUnit] JUnit not found when JDT installed as dropin - https://bugs.eclipse.org/bugs/show_bug.cgi?id=297663
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Version;

import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;


/**
 * Utilities to read and write bundle and source information files.
 * <p>
 * This class currently uses provisional p2 API for which official API has been requested:
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=269496
 * </p>
 * 
 * @since 3.5
 */
class P2Utils {

	private static final String SRC_INFO_FOLDER = "org.eclipse.equinox.source"; //$NON-NLS-1$
	private static final String SRC_INFO_PATH= SRC_INFO_FOLDER + File.separator + "source.info"; //$NON-NLS-1$

	private static final String BUNDLE_INFO_FOLDER= "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$
	private static final String BUNDLE_INFO_PATH = BUNDLE_INFO_FOLDER + File.separator + "bundles.info"; //$NON-NLS-1$


	/**
	 * Finds the bundle info for the given arguments.
	 * <p>
	 * The first match will be returned if more than one bundle matches the arguments.
	 * </p>
	 * 
	 * @param symbolicName the symbolic name
	 * @param version the bundle version
	 * @param isSourceBundle <code>true</code> if it is a source bundle <code>false</code> otherwise
	 * @return the bundle info or <code>null</code> if not found
	 */
	public static BundleInfo findBundle(String symbolicName, Version version, boolean isSourceBundle) {
		Assert.isLegal(symbolicName != null);
		Assert.isLegal(version != null);

		return findBundle(symbolicName, new VersionRange(version, true, version, true), isSourceBundle);
	}

	/**
	 * Adds the directories (as <code>java.io.File</code>) for the given location and all of its parents to the given list.
	 * 
	 * @param locations the list to add the URLs to
	 * @param location the location
	 * @param useParent <code>true</code> if location's parent directory should be used <code>false</code> otherwise
	 */
	private static void addLocationDirs(List locations, Location location, boolean useParent) {
		while (location != null) {
			URL url= location.getURL();
			if (url != null) {
				try {
					File dir = new File(FileLocator.toFileURL(url).getPath());
					if (useParent)
						dir = dir.getParentFile();
					if (!locations.contains(dir))
						locations.add(dir);
				} catch (IOException e) {
					JUnitCorePlugin.log(e);
				}
			}
			location= location.getParentLocation();
		}
	}

	/**
	 * Finds the bundle info for the given arguments.
	 * <p>
	 * The first match will be returned if more than one bundle matches the arguments.
	 * </p>
	 * 
	 * @param symbolicName the symbolic name
	 * @param versionRange the version range for the bundle version
	 * @param isSourceBundle <code>true</code> if it is a source bundle <code>false</code> otherwise
	 * @return the bundle info or <code>null</code> if not found
	 */
	public static BundleInfo findBundle(String symbolicName, VersionRange versionRange, boolean isSourceBundle) {
		Assert.isLegal(symbolicName != null);
		Assert.isLegal(versionRange != null);

		SimpleConfiguratorManipulator manipulator= (SimpleConfiguratorManipulator)JUnitCorePlugin.getDefault().getService(SimpleConfiguratorManipulator.class.getName());
		if (manipulator == null)
			return null;

		List bundleLocations = new ArrayList();
		addLocationDirs(bundleLocations, Platform.getConfigurationLocation(), true);
		addLocationDirs(bundleLocations, Platform.getInstallLocation(), false);

		for (Location configLocation= Platform.getConfigurationLocation(); configLocation != null; configLocation= configLocation.getParentLocation()) {
			URL configUrl= configLocation.getURL();
			if (configUrl == null)
				continue;

			try {
				String bundleInfoPath= null;
				if (isSourceBundle)
					bundleInfoPath= SRC_INFO_PATH;
				else
					bundleInfoPath= BUNDLE_INFO_PATH;

				URL bundlesTxt = new URL(configUrl.getProtocol(), configUrl.getHost(), new File(configUrl.getPath(), bundleInfoPath).getAbsolutePath());

				for (Iterator i= bundleLocations.iterator(); i.hasNext(); ) {
					File home= (File) i.next();
					BundleInfo bundles[]= manipulator.loadConfiguration(bundlesTxt, home);
					if (bundles != null) {
						for (int j= 0; j < bundles.length; j++) {
							BundleInfo bundle= bundles[j];
							if (symbolicName.equals(bundle.getSymbolicName()) && versionRange.isIncluded(new Version(bundle.getVersion()))) {
								IPath path= getBundleLocationPath(bundle);
								if (path.toFile().exists())
									return bundle;
							}
						}
					}
				}
			} catch (MalformedURLException e) {
				JUnitCorePlugin.log(e);
			} catch (IOException e) {
				JUnitCorePlugin.log(e);
			}
		}
		
		return null;
	}

	/**
	 * Returns the bundle location path.
	 * 
	 * @param bundleInfo the bundle info or <code>null</code>
	 * @return the bundle location or <code>null</code> if it is not possible to convert to a path
	 */
	public static IPath getBundleLocationPath(BundleInfo bundleInfo) {
		if (bundleInfo == null)
			return null;
	
		URI bundleLocation= bundleInfo.getLocation();
		if (bundleLocation == null)
			return null;
		
		try {
			String fileStr= FileLocator.toFileURL(URIUtil.toURL(bundleLocation)).getPath();
			fileStr= URLDecoder.decode(fileStr, "UTF-8"); //$NON-NLS-1$
			return new Path(fileStr);
		} catch (IOException e) {
			JUnitCorePlugin.log(e);
			return null;
		}
	}

}
