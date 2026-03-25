/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import org.eclipse.osgi.service.debug.DebugOptions;

import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.internal.text.reconciler.ReconcilerJobFamilies;

import org.eclipse.jdt.internal.core.JavaModelManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;

public class TestUtils {

	private static final String RECONCILER_BACKGROUND_WORKER_THREAD_NAME = JavaReconciler.class.getName();

	public static void waitForIndexer() {
		JavaModelManager.getIndexManager().waitForIndex(false, null);
	}

	/**
	 * Closes all open editors (without saving them), then waits for:
	 *
	 * <pre>
	 * 1. Reconciler family, {@link ReconcilerJobFamilies#FAMILY_RECONCILER}
	 * 2. Background reconciler threads, {@link JavaReconciler}
	 * </pre>
	 *
	 * @param timeout wait timeout in milliseconds
	 * @throws RuntimeException if a timeout occurs while waiting
	 */
	@SuppressWarnings("restriction")
	public static void waitForReconciler(long timeout) throws Exception {
		JavaPlugin.getActivePage().closeAllEditors(false);
		long s = System.currentTimeMillis();
		while (System.currentTimeMillis() - s < timeout) {
			Job[] jobs = Job.getJobManager().find(ReconcilerJobFamilies.FAMILY_RECONCILER);
			if (jobs.length == 0) {
				break;
			}
			Thread.sleep(50);
		}
		// the reconciler starts a background thread, wait for it here
		while (System.currentTimeMillis() - s < timeout) {
			boolean reconcilerDone = Thread.getAllStackTraces().keySet().stream().noneMatch(TestUtils::isReconcilerThread);
			if (reconcilerDone) {
				break;
			}
			Thread.sleep(50);
		}
		if (System.currentTimeMillis() - s > timeout) {
			throw new RuntimeException("Timeout occurred while waiting on reconciler");
		}
	}

	/**
	 * Enables or disables debug traces.
	 * @param classFromBundle A class from the bundle for which the debug tracing should be enabled or disabled.
	 * @param enable whether to enable or disable debug tracing
	 */
	public static void setDebugEnabled(Class<?> classFromBundle, boolean enable) {
		Bundle bundle= FrameworkUtil.getBundle(classFromBundle);
		BundleContext context= bundle.getBundleContext();
		ServiceReference<DebugOptions> reference= context.getServiceReference(DebugOptions.class);
		try {
			DebugOptions options= context.getService(reference);
			options.setDebugEnabled(enable);
			String key= bundle.getSymbolicName() + "/debug";
			options.setOption(key, enable ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
		} finally {
			context.ungetService(reference);
		}
	}

	private static boolean isReconcilerThread(Thread thread) {
		return thread != null && RECONCILER_BACKGROUND_WORKER_THREAD_NAME.equals(thread.getName());
	}
}
