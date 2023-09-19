/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
 *     Patrick Higgins <patrick133t@yahoo.com> - [JUnit] JUnit not found when JDT installed as dropin - https://bugs.eclipse.org/bugs/show_bug.cgi?id=297663
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;


/**
 * Utilities to read and write bundle and source information files.
 *
 * @since 3.5
 */
class P2Utils {

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

		return findBundle(symbolicName, new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED), isSourceBundle);
	}

	/**
	 * Finds the bundle info for the given arguments.
	 * <p>
	 * The best match (latest version) will be returned if more than one bundle matches the arguments.
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

		SimpleConfiguratorManipulator manipulator= (SimpleConfiguratorManipulator) JUnitCorePlugin.getDefault().getService(SimpleConfiguratorManipulator.class.getName());
		if (manipulator == null)
			return null;

		BundleInfo bestMatch= null;
		Version bestVersion= null;

		// A null bundleInfoPath means load the bundles.info according to the BundleContext property "org.eclipse.equinox.simpleconfigurator.configUrl"
		String bundleInfoPath= null;
		if (isSourceBundle)
			bundleInfoPath= SimpleConfiguratorManipulator.SOURCE_INFO;

		BundleContext context= JUnitCorePlugin.getDefault().getBundle().getBundleContext();
		BundleInfo[] bundles= null;
		try {
			bundles= manipulator.loadConfiguration(context, bundleInfoPath);
		} catch (IOException e) {
			JUnitCorePlugin.log(e);
		}

		if (bundles != null) {
			for (BundleInfo bundleInfo : bundles) {
				if (symbolicName.equals(bundleInfo.getSymbolicName())) {
					Version version= new Version(bundleInfo.getVersion());
					if (versionRange.includes(version)) {
						IPath path= getBundleLocationPath(bundleInfo);
						if ((bestVersion == null || bestVersion.compareTo(version) < 0) && path != null && path.toFile().exists()) {
							bestMatch= bundleInfo;
							bestVersion= version;
						}
					}
				}
			}
		}

		return bestMatch;
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
			URL localFileURL= FileLocator.toFileURL(URIUtil.toURL(bundleLocation));
			URI localFileURI= new URI(localFileURL.toExternalForm());
			return new Path(localFileURI.getPath());
		} catch (IOException | URISyntaxException e) {
			JUnitCorePlugin.log(e);
			return null;
		}
	}

	private P2Utils() {
	}

}
