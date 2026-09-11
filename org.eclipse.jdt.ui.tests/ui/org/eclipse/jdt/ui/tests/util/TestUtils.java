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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import org.eclipse.swt.widgets.Display;

import org.eclipse.osgi.service.debug.DebugOptions;

import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.jface.internal.text.reconciler.ReconcilerJobFamilies;

import org.eclipse.jface.text.reconciler.AbstractReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.internal.decorators.DecoratorManager;

import org.eclipse.jdt.internal.core.JavaModelManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;

public class TestUtils {

	public static void waitForIndexer() {
		JavaModelManager.getIndexManager().waitForIndex(false, null);
	}

	/**
	 * Collects the reconcilers of all open Java editors, then closes all open editors (without
	 * saving them), then:
	 *
	 * <pre>
	 * 1. (optional) cancels the decorator family, {@link DecoratorManager#FAMILY_DECORATE}
	 * 2. (optional) cancels the reconciler family, {@link ReconcilerJobFamilies#FAMILY_RECONCILER}
	 * 3. waits on the decorator family
	 * 4. waits on the reconciler family
	 * 5. waits until the collected reconcilers are idle, {@link AbstractReconciler#isIdle()}
	 * </pre>
	 *
	 * The reconcilers are collected before the editors are closed, since closing an editor disposes
	 * its viewer. Closing uninstalls a reconciler but does not necessarily stop it, which
	 * {@link AbstractReconciler#isIdle()} accounts for.
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
		List<AbstractReconciler> reconcilers= collectReconcilers();
		JavaPlugin.getActivePage().closeAllEditors(false);
		if (cancelJobs) {
			Job.getJobManager().cancel(DecoratorManager.FAMILY_DECORATE);
			Job.getJobManager().cancel(ReconcilerJobFamilies.FAMILY_RECONCILER);
		}
		Job.getJobManager().join(DecoratorManager.FAMILY_DECORATE, null);
		waitForJobFamily(timeout, ReconcilerJobFamilies.FAMILY_RECONCILER);
		waitForReconcilers(reconcilers, timeout);
	}

	/**
	 * Returns the reconcilers of all currently open Java editors.
	 *
	 * @return the reconcilers of all open Java editors, never <code>null</code>
	 */
	private static List<AbstractReconciler> collectReconcilers() {
		List<AbstractReconciler> reconcilers= new ArrayList<>();
		for (IEditorReference editorReference : JavaPlugin.getActivePage().getEditorReferences()) {
			IEditorPart editor= editorReference.getEditor(false);
			if (editor instanceof JavaEditor javaEditor && getReconciler(javaEditor) instanceof AbstractReconciler reconciler) {
				reconcilers.add(reconciler);
			}
		}
		return reconcilers;
	}

	/**
	 * Waits until all given reconcilers are idle.
	 *
	 * @param reconcilers the reconcilers to wait for
	 * @param timeout wait timeout in milliseconds, for all reconcilers together
	 * @throws RuntimeException if a timeout occurs while waiting
	 */
	private static void waitForReconcilers(Collection<AbstractReconciler> reconcilers, long timeout) throws InterruptedException {
		long deadline= System.currentTimeMillis() + timeout;
		for (AbstractReconciler reconciler : reconcilers) {
			waitUntil(reconciler::isIdle, deadline, "Timeout occurred while waiting on reconciler");
		}
	}

	/**
	 * Returns the reconciler of the given editor, if any.
	 *
	 * @param editor the editor to get the reconciler for
	 * @return the editor's reconciler, or <code>null</code> if the editor has no viewer or no
	 *         reconciler (yet)
	 */
	public static IReconciler getReconciler(JavaEditor editor) {
		ISourceViewer viewer= editor.getViewer();
		if (viewer instanceof JavaSourceViewer javaSourceViewer) {
			return javaSourceViewer.getReconciler();
		}
		return null;
	}

	/**
	 * Tells whether the reconciler of the given editor is idle, i.e. no initial process is pending
	 * or running, no changes wait to be processed and no reconciling strategy is running.
	 * <p>
	 * An editor without a reconciler is considered idle.
	 * </p>
	 *
	 * @param editor the editor to check
	 * @return <code>true</code> if the editor's reconciler is idle
	 * @see AbstractReconciler#isIdle()
	 */
	public static boolean isReconcilerIdle(JavaEditor editor) {
		IReconciler reconciler= getReconciler(editor);
		if (reconciler instanceof AbstractReconciler abstractReconciler) {
			return abstractReconciler.isIdle();
		}
		return true;
	}

	/**
	 * Waits until the reconciler of the given editor is idle.
	 *
	 * @param editor the editor whose reconciler to wait for
	 * @param timeout wait timeout in milliseconds
	 * @throws RuntimeException if a timeout occurs while waiting
	 */
	public static void waitForReconciler(JavaEditor editor, long timeout) throws InterruptedException {
		waitUntil(() -> isReconcilerIdle(editor), System.currentTimeMillis() + timeout,
				"Timeout occurred while waiting on reconciler of editor: " + editor.getTitle());
	}

	/**
	 * Waits until the given condition is met.
	 * <p>
	 * If called from the UI thread, the event loop is pumped while waiting, since the awaited work
	 * may require the UI thread to make progress.
	 * </p>
	 *
	 * @param condition the condition to wait for
	 * @param deadline the point in time, in milliseconds, at which waiting is given up
	 * @param timeoutMessage the message of the exception thrown on timeout
	 * @throws RuntimeException if a timeout occurs while waiting
	 */
	private static void waitUntil(BooleanSupplier condition, long deadline, String timeoutMessage) throws InterruptedException {
		Display display= Display.getCurrent();
		while (!condition.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline) {
				throw new RuntimeException(timeoutMessage);
			}
			if (display == null || !display.readAndDispatch()) {
				Thread.sleep(10);
			}
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
}
