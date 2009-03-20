/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;


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
	 * Returns bundles defined by the 'bundles.info' relative to the given home and configuration
	 * area, or <code>null</code> if none. The "bundles.info" file is assumed to be at a fixed
	 * location relative to the configuration area URL.
	 * 
	 * @return all bundles in the installation or <code>null</code> if not able to locate a
	 *         bundles.info
	 */
	private static BundleInfo[] readBundles() {
		URL configurationArea= Platform.getConfigurationLocation().getURL();
		if (configurationArea == null)
			return null;

		try {
			URL bundlesTxt = new URL(configurationArea.getProtocol(), configurationArea.getHost(), new File(configurationArea.getFile(), BUNDLE_INFO_PATH).getAbsolutePath());
			BundleInfo bundles[]= getBundlesFromFile(bundlesTxt);
			if (bundles == null || bundles.length == 0) {
				return null;
			}
			return bundles;
		} catch (MalformedURLException e) {
			JUnitPlugin.log(e);
			return null;
		} catch (IOException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns source bundles defined by the 'source.info' file in the specified location, or
	 * <code>null</code> if none. The "source.info" file is assumed to be at a fixed location
	 * relative to the configuration area URL.
	 * 
	 * @return all source bundles in the installation or <code>null</code> if not able to locate a
	 *         source.info
	 */
	private static BundleInfo[] readSourceBundles() {
		URL configurationArea= Platform.getConfigurationLocation().getURL();
		if (configurationArea == null)
			return null;

		try {
			URL srcBundlesTxt = new URL(configurationArea.getProtocol(), configurationArea.getHost(), configurationArea.getFile().concat(SRC_INFO_PATH));
			BundleInfo srcBundles[]= getBundlesFromFile(srcBundlesTxt);
			if (srcBundles == null || srcBundles.length == 0) {
				return null;
			}
			return srcBundles;
		} catch (MalformedURLException e) {
			JUnitPlugin.log(e);
			return null;
		} catch (IOException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}

	/**
	 * Returns a list of {@link BundleInfo} for each bundle entry or <code>null</code> if there is a
	 * problem reading the file.
	 * 
	 * @param fileURL the URL of the file to read
	 * @return list containing URL locations or <code>null</code>
	 * @throws IOException if loading the configuration fails
	 */
	private static BundleInfo[] getBundlesFromFile(URL fileURL) throws IOException {
		SimpleConfiguratorManipulator manipulator= (SimpleConfiguratorManipulator)JUnitPlugin.getDefault().getService(SimpleConfiguratorManipulator.class.getName());
		if (manipulator == null)
			return null;

		File home= new File(Platform.getInstallLocation().getURL().getFile());
		return manipulator.loadConfiguration(fileURL, home);
	}

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

		BundleInfo[] bundles;
		if (isSourceBundle)
			bundles= P2Utils.readSourceBundles();
		else
			bundles= P2Utils.readBundles();

		for (int i= 0; i < bundles.length; i++) {
			if (symbolicName.equals(bundles[i].getSymbolicName()) && versionRange.isIncluded(new Version(bundles[i].getVersion())))
				return bundles[i];
		}

		return null;
	}

	/**
	 * Returns the bundle location.
	 * 
	 * @param bundleInfo the bundle info
	 * @return the bundle location or <code>null</code> if it is not possible to convert to a path
	 */
	public static IPath getBundleLocationPath(BundleInfo bundleInfo) {
		if (bundleInfo == null)
			return null;
	
		try {
			return new Path(FileLocator.toFileURL(URIUtil.toURL(bundleInfo.getLocation())).getFile());
		} catch (IOException e) {
			JUnitPlugin.log(e);
			return null;
		}
	}

}
