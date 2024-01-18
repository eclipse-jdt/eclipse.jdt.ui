/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

import org.eclipse.jdt.internal.corext.util.TypeFilter;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;

/**
 * The main plug-in class to be used in the workbench.
 */
public class JavaManipulationPlugin extends Plugin implements DebugOptionsListener {

	public static final boolean CODEASSIST_SUBSTRING_MATCH_ENABLED= //
			Boolean.parseBoolean(System.getProperty("jdt.codeCompleteSubstringMatch", "true")); //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * A named preference that holds the favorite static members.
	 * <p>
	 * Value is of type <code>String</code>: semicolon separated list of favorites.
	 * </p>
	 */
	public final static String CODEASSIST_FAVORITE_STATIC_MEMBERS= "content_assist_favorite_static_members"; //$NON-NLS-1$

	public static boolean DEBUG_AST_PROVIDER;

	public static boolean DEBUG_TYPE_CONSTRAINTS;

	//The shared instance.
	private static JavaManipulationPlugin fgDefault;

	private MembersOrderPreferenceCacheCommon fMembersOrderPreferenceCacheCommon;

	/**
	 * Default instance of the appearance type filters.
	 */
	private volatile TypeFilter fTypeFilter;

	private BundleContext fBundleContext;

	/**
	 * The constructor.
	 */
	public JavaManipulationPlugin() {
		fgDefault= this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		fBundleContext= context;
		fgDefault= null;

		if (fTypeFilter != null) {
			fTypeFilter.dispose();
			fTypeFilter= null;
		}
	}

	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance.
	 */
	public static JavaManipulationPlugin getDefault() {
		return fgDefault;
	}

	/**
	 * Returns the shared Members Order Preference Common Cache
	 *
	 * @return the shared cache
	 */
	public MembersOrderPreferenceCacheCommon getMembersOrderPreferenceCacheCommon() {
		if (fMembersOrderPreferenceCacheCommon == null) {
			fMembersOrderPreferenceCacheCommon= new MembersOrderPreferenceCacheCommon();
		}
		return fMembersOrderPreferenceCacheCommon;
	}

	/**
	 * Set the default Members Order Preference Cache Common
	 *
	 * @param mpcc - MembersOrderPreferenceCacheCommon default
	 */
	public void setMembersOrderPreferenceCacheCommon(MembersOrderPreferenceCacheCommon mpcc) {
		fMembersOrderPreferenceCacheCommon= mpcc;
	}


	public TypeFilter getTypeFilter() {
		TypeFilter result= fTypeFilter;
		if (result != null) { // First check (no locking)
			return result;
		}
		synchronized(this) {
			if (fTypeFilter == null) { // Second check (with locking)
				fTypeFilter= new TypeFilter();
			}
			return fTypeFilter;
		}
	}

	public static void log(Throwable e) {
		ILog.of(JavaManipulationPlugin.class).log(new Status(IStatus.ERROR, JavaManipulation.ID_PLUGIN, IStatusConstants.INTERNAL_ERROR, JavaManipulationMessages.JavaManipulationMessages_internalError, e));
	}

	public static void log(IStatus status) {
		ILog.of(JavaManipulationPlugin.class).log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi= new MultiStatus(getPluginId(), IJavaStatusConstants.INTERNAL_ERROR, message, null);
		multi.add(status);
		log(multi);
	}

	public static String getPluginId() {
		return JavaManipulation.ID_PLUGIN;
	}

	/**
	 * Returns the bundles for a given bundle name and version range,
	 * regardless whether the bundle is resolved or not.
	 *
	 * @param bundleName the bundle name
	 * @param version the version of the bundle, or <code>null</code> for all bundles
	 * @return the bundles of the given name belonging to the given version range
	 * @since 3.10
	 */
	public Bundle[] getBundles(String bundleName, String version) {
		Bundle[] bundles= Platform.getBundles(bundleName, version);
		if (bundles != null)
			return bundles;

		// Accessing unresolved bundle
		ServiceReference<PackageAdmin> serviceRef= fBundleContext.getServiceReference(PackageAdmin.class);
		PackageAdmin admin= fBundleContext.getService(serviceRef);
		bundles= admin.getBundles(bundleName, version);
		if (bundles != null && bundles.length > 0)
			return bundles;
		return null;
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		DEBUG_AST_PROVIDER= options.getBooleanOption("org.eclipse.jdt.core.manipulation/debug/ASTProvider", false); //$NON-NLS-1$
		DEBUG_TYPE_CONSTRAINTS= options.getBooleanOption("org.eclipse.jdt.core.manipulation/debug/TypeConstraints", false); //$NON-NLS-1$
	}
}
