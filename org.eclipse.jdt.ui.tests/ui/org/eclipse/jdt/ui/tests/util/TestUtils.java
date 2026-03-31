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

import org.eclipse.ui.internal.decorators.DecoratorManager;

import org.eclipse.jdt.internal.core.JavaModelManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaReconciler;

public class TestUtils {

	private static final String RECONCILER_BACKGROUND_WORKER_THREAD_NAME = JavaReconciler.class.getName();

	public static void waitForIndexer() {
		JavaModelManager.getIndexManager().waitForIndex(false, null);
	}

	/**
	 * Closes all open editors (without saving them), then:
	 *
	 * <pre>
	 * 1. (optional) cancels the decorator family, {@link DecoratorManager#FAMILY_DECORATE}
	 * 2. (optional) cancels the reconciler family, {@link ReconcilerJobFamilies#FAMILY_RECONCILER}
	 * 3. waits on the decorator family
	 * 4. waits on the reconciler family
	 * 5. waits on background reconciler threads, {@link JavaReconciler}
	 * </pre>
	 *
	 * @param cancelJobs whether the decorator and reconciler families should be cancelled
	 * @param timeout wait timeout in milliseconds
	 * @throws RuntimeException if a timeout occurs while waiting
	 */
	@SuppressWarnings("restriction")
	public static void waitForEditorJobs(long timeout, boolean cancelJobs) throws Exception {
		/*
		 * The decoration job opens and closes buffers in BufferManager,
		 * those buffers are used by the formatter code.
		 * We don't want the job to run in parallel to the formatting done by the test,
		 * but waiting for the job makes the test case up to 5 times slower.
		 * So we cancel the job and then make sure it exits before formatting.
		 */
		JavaPlugin.getActivePage().closeAllEditors(false);
		if (cancelJobs) {
			Job.getJobManager().cancel(DecoratorManager.FAMILY_DECORATE);
			Job.getJobManager().cancel(ReconcilerJobFamilies.FAMILY_RECONCILER);
		}
		Job.getJobManager().join(DecoratorManager.FAMILY_DECORATE, null);
		TestUtils.waitForReconciler(timeout);
	}

	@SuppressWarnings("restriction")
	private static void waitForReconciler(long timeout) throws Exception {
		long s = System.currentTimeMillis();
		waitForJobFamily(timeout, ReconcilerJobFamilies.FAMILY_RECONCILER);
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
	 * Waits for all scheduled and running jobs of the specified job {@code family}.
	 * @param timeout wait timeout in milliseconds
	 * @param family the job family to wait on
	 * @throws RuntimeException if a timeout occurs while waiting
	 */
	public static void waitForJobFamily(long timeout, Object family) throws InterruptedException {
		long s= System.currentTimeMillis();
		while (System.currentTimeMillis() - s < timeout) {
			Job[] jobs = Job.getJobManager().find(family);
			if (jobs.length == 0) {
				break;
			}
			Thread.sleep(50);
		}
		if (System.currentTimeMillis() - s > timeout) {
			throw new RuntimeException("Timeout occurred while waiting on job family: " + family);
		}
	}

	@SuppressWarnings("restriction")
	public static void cancelDecorationJob() throws InterruptedException {
		/*
		 * The decoration job opens and closes buffers in BufferManager,
		 * those buffers are used by the formatter code.
		 * We don't want the job to run in parallel to the formatting done by the test,
		 * but waiting for the job makes the test case up to 5 times slower.
		 * So we cancel the job and then make sure it exits before formatting.
		 */
		Job.getJobManager().cancel(DecoratorManager.FAMILY_DECORATE);
		Job.getJobManager().join(DecoratorManager.FAMILY_DECORATE, null);
	}

	/**
	 * Enables or disables debug traces.
	 * @param classFromBundle A class from the bundle for which the debug tracing should be enabled or disabled.
	 * @param enable whether to enable or disable debug tracing
	 */
	public static void setDebugEnabled(Class<?> classFromBundle, boolean enable) {
		Bundle bundle= FrameworkUtil.getBundle(classFromBundle);
		setDebugEnabled(bundle, enable, "/debug");
	}

	/**
	 * Enables or disables debug traces for the specified bundle and debug option.
	 * @param bundle The bundle for which the debug tracing should be enabled or disabled.
	 * @param debugOptions The debug options to enable or disable, e.g.: {@code "/debug"}, {@code "/debug/buffermanager"}
	 * @param enable whether to enable or disable debug tracing
	 */
	public static void setDebugEnabled(Bundle bundle, boolean enable, String... debugOptions) {
		BundleContext context= bundle.getBundleContext();
		ServiceReference<DebugOptions> reference= context.getServiceReference(DebugOptions.class);
		try {
			DebugOptions options= context.getService(reference);
			options.setDebugEnabled(enable);
			for (String debugOption : debugOptions) {
				String key= bundle.getSymbolicName() + debugOption;
				options.setOption(key, enable ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
			}
		} finally {
			context.ungetService(reference);
		}
	}

	private static boolean isReconcilerThread(Thread thread) {
		return thread != null && RECONCILER_BACKGROUND_WORKER_THREAD_NAME.equals(thread.getName());
	}
}
