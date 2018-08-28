/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.osgi.framework.BundleContext;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

/**
 * The main plug-in class to be used in the workbench.
 */
public class JavaManipulationPlugin extends Plugin implements DebugOptionsListener {

	public static boolean DEBUG_AST_PROVIDER;

	//The shared instance.
	private static JavaManipulationPlugin fgDefault;

	private MembersOrderPreferenceCacheCommon fMembersOrderPreferenceCacheCommon;

	/**
	 * The constructor.
	 */
	public JavaManipulationPlugin() {
		fgDefault = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		fgDefault = null;
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

	public static String getPluginId() {
		return JavaManipulation.ID_PLUGIN;
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatusConstants.INTERNAL_ERROR, message, null));
	}

	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi= new MultiStatus(getPluginId(), IStatusConstants.INTERNAL_ERROR, message, null);
		multi.add(status);
		log(multi);
	}

	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatusConstants.INTERNAL_ERROR, JavaManipulationMessages.JavaManipulationMessages_internalError, e));
	}

	public static void logException(String message, Throwable ex) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatusConstants.INTERNAL_ERROR, message, ex));
	}

	@Override
	public void optionsChanged(DebugOptions options) {
		DEBUG_AST_PROVIDER= options.getBooleanOption("org.eclipse.jdt.core.manipulation/debug/ASTProvider", false); //$NON-NLS-1$
	}
}
