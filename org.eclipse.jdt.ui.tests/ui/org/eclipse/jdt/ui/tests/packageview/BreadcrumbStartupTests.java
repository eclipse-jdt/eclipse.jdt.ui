/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.util.TestUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.IBreadcrumb;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class BreadcrumbStartupTests {
	private enum Scenario { NORMAL, SLOW, CLOSE, CANCEL, REPLACE }

	@Test
	public void coldEditorWithBreadcrumbDoesNotInitializeContainerOnUIThread() throws Exception {
		exercise(Scenario.NORMAL);
	}

	@Test
	public void slowContainerDoesNotBlockEditorCreation() throws Exception {
		exercise(Scenario.SLOW);
	}

	@Test
	public void closingEditorDiscardsPendingBreadcrumbUpdate() throws Exception {
		exercise(Scenario.CLOSE);
	}

	@Test
	public void cancelledInitializationCanBeRetried() throws Exception {
		exercise(Scenario.CANCEL);
	}

	@Test
	public void latestInputWinsWhileInitializationIsPending() throws Exception {
		exercise(Scenario.REPLACE);
	}

	private static void drainEvents() {
		while (Display.getCurrent().readAndDispatch()) {
		}
	}

	private static void awaitInitialization(IBreadcrumb breadcrumb) throws Exception {
		long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (Job.getJobManager().find(breadcrumb).length > 0) {
			assertTrue(System.nanoTime() < deadline, "Breadcrumb initialization did not finish");
			drainEvents();
			Thread.sleep(10);
		}
		drainEvents();
	}

	private static void awaitInput(Viewer viewer, String expected) throws Exception {
		long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (!(viewer.getInput() instanceof IJavaElement element) || !expected.equals(element.getElementName())) {
			assertTrue(System.nanoTime() < deadline, "Breadcrumb did not publish the latest input: " + expected);
			drainEvents();
			Thread.sleep(10);
		}
	}

	private void exercise(Scenario scenario) throws Exception {
		TestUtils.waitForEditorJobs(60_000, true);
		TestUtils.waitForJobFamily(60_000, JavaUI.ID_PLUGIN);
		IWorkbenchPage page= JavaPlugin.getActivePage();
		assertNotNull(page.getPerspective());
		IViewPart oldPackageExplorer= page.findView(JavaUI.ID_PACKAGES);
		String key= JavaEditor.EDITOR_SHOW_BREADCRUMB + "." + page.getPerspective().getId();
		IPreferenceStore preferences= JavaPlugin.getDefault().getPreferenceStore();
		boolean wasDefault= preferences.isDefault(key);
		boolean oldBreadcrumb= preferences.getBoolean(key);
		boolean oldAutoBuilding= ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		IJavaProject project= null;
		IEditorPart editor= null;
		IBreadcrumb breadcrumb= null;
		CountDownLatch entered= new CountDownLatch(1);
		CountDownLatch release= new CountDownLatch(scenario == Scenario.NORMAL ? 0 : 1);
		AtomicReference<IStatus> cancellation= new AtomicReference<>();
		CountDownLatch cancelledDone= new CountDownLatch(1);
		try {
			CoreUtility.setAutoBuilding(false);
			preferences.setValue(key, true);
			project= JavaProjectHelper.createJavaProject("BreadcrumbStartupTests", "bin");
			IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(project, "src");
			IPackageFragment pack= root.createPackageFragment("p", true, null);
			String source= "package p; public class A {} class B {}";
			ICompilationUnit first= pack.createCompilationUnit("A.java", source, true, null);
			IType second= first.getType("B");
			JavaProjectHelper.addToClasspath(project, JavaCore.newContainerEntry(StartupClasspathContainerInitializer.PATH));
			project.getResolvedClasspath(true);
			page.showView(JavaUI.ID_PACKAGES);
			TestUtils.waitForIndexer();
			project.close();
			((JavaProject) project).resetResolvedClasspath();
			JavaModelManager.getJavaModelManager().containerPut(project, StartupClasspathContainerInitializer.PATH, null);
			StartupClasspathContainerInitializer.CALLS.set(0);
			StartupClasspathContainerInitializer.UI_CALLS.clear();
			StartupClasspathContainerInitializer.TIMED_OUT.set(false);
			StartupClasspathContainerInitializer.entered= entered;
			StartupClasspathContainerInitializer.release= release;
			StartupClasspathContainerInitializer.watchedProject= project.getElementName();
			editor= page.openEditor(new FileEditorInput(project.getProject().getFile("src/p/A.java")), JavaUI.ID_CU_EDITOR);
			JavaEditor javaEditor= assertInstanceOf(JavaEditor.class, editor);
			breadcrumb= javaEditor.getBreadcrumb();
			assertTrue(entered.await(10, TimeUnit.SECONDS), "Cold container initializer was not reached");
			assertFalse(StartupClasspathContainerInitializer.TIMED_OUT.get(), "Editor creation waited for the blocked container initializer");
			assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
			Viewer viewer= assertInstanceOf(Viewer.class, breadcrumb.getSelectionProvider());
			Control control= viewer.getControl();
			if (scenario == Scenario.CLOSE) {
				page.closeEditor(editor, false);
				editor= null;
			} else if (scenario == Scenario.CANCEL) {
				Job[] jobs= Job.getJobManager().find(breadcrumb);
				assertEquals(1, jobs.length, "Expected the pending breadcrumb initialization");
				jobs[0].addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						cancellation.set(event.getResult());
						cancelledDone.countDown();
					}
				});
				jobs[0].cancel();
			} else if (scenario == Scenario.REPLACE) {
				// Keep the actual editor selection consistent with the requested input:
				// queued selection/reconcile events must refer to the new location too.
				javaEditor.selectAndReveal(source.indexOf("B"), 0);
				breadcrumb.setInput(second);
			}
			release.countDown();
			if (scenario == Scenario.CANCEL) {
				// Deliberately do not dispatch UI events before requesting the retry:
				// the cancelled worker has finished, but its UI callback is still pending.
				assertTrue(cancelledDone.await(10, TimeUnit.SECONDS), "Cancelled worker did not finish");
				assertNotNull(cancellation.get());
				assertEquals(IStatus.CANCEL, cancellation.get().getSeverity());
				breadcrumb.setInput(first);
			}
			awaitInitialization(breadcrumb);
			if (scenario != Scenario.CLOSE)
				awaitInput(viewer, scenario == Scenario.REPLACE ? "B" : "A");
			assertTrue(StartupClasspathContainerInitializer.CALLS.get() > 0, "Test must actually initialize the cold container");
			assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
			if (scenario == Scenario.CLOSE) {
				assertTrue(control.isDisposed());
			} else {
				IJavaElement input= assertInstanceOf(IJavaElement.class, viewer.getInput(), "Breadcrumb must eventually show the Java input");
				String expected= scenario == Scenario.REPLACE ? "B" : "A";
				assertEquals(expected, input.getElementName());
			}
		} finally {
			release.countDown();
			StartupClasspathContainerInitializer.watchedProject= null;
			try {
				if (editor != null)
					page.closeEditor(editor, false);
				if (breadcrumb != null)
					awaitInitialization(breadcrumb);
				TestUtils.waitForEditorJobs(60_000, true);
				if (project != null)
					JavaProjectHelper.delete(project);
			} finally {
				StartupClasspathContainerInitializer.entered= null;
				StartupClasspathContainerInitializer.release= null;
				if (wasDefault)
					preferences.setToDefault(key);
				else
					preferences.setValue(key, oldBreadcrumb);
				CoreUtility.setAutoBuilding(oldAutoBuilding);
				if (oldPackageExplorer == null && page.findView(JavaUI.ID_PACKAGES) != null)
					page.hideView(page.findView(JavaUI.ID_PACKAGES));
			}
		}
	}
}
