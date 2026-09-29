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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.ui.progress.UIJob;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaElementDelta;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.util.TestUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditorBreadcrumb;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.IBreadcrumb;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class BreadcrumbStartupTests {
	private enum Scenario {
		NORMAL, SLOW, CLOSE, CANCEL, REPLACE,
		CLASSPATH, RESOLVED_CLASSPATH, COALESCED_CLASSPATH, NULL_COLD, PENDING_CLASSPATH,
		ROOT_CLASSPATH, OTHER_PROJECT, CONTENT_ONLY
	}

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

	@Test
	public void classpathChangeReinitializesWithoutUIThreadResolution() throws Exception {
		exercise(Scenario.CLASSPATH);
	}

	@Test
	public void resolvedClasspathChangeReinitializesWithoutUIThreadResolution() throws Exception {
		exercise(Scenario.RESOLVED_CLASSPATH);
	}

	@Test
	public void queuedContentRefreshDoesNotSwallowClasspathChange() throws Exception {
		exercise(Scenario.COALESCED_CLASSPATH);
	}

	@Test
	public void nullInputDoesNotResolveAClosedModelOnUIThread() throws Exception {
		exercise(Scenario.NULL_COLD);
	}

	@Test
	public void classpathChangeBeforeFirstPublicationInvalidatesCompletedWorker() throws Exception {
		exercise(Scenario.PENDING_CLASSPATH);
	}

	@Test
	public void siblingRootClasspathChangeInvalidatesProject() throws Exception {
		exercise(Scenario.ROOT_CLASSPATH);
	}

	@Test
	public void anotherProjectDoesNotScheduleBreadcrumbWork() throws Exception {
		exercise(Scenario.OTHER_PROJECT);
	}

	@Test
	public void contentRefreshDoesNotReinitializeTheClasspath() throws Exception {
		exercise(Scenario.CONTENT_ONLY);
	}

	private static IElementChangedListener modelListener(IBreadcrumb breadcrumb) throws Exception {
		// Deliver only to this listener: other workbench listeners could warm up the
		// deliberately invalidated model and hide the breadcrumb regression.
		Field field= JavaEditorBreadcrumb.class.getDeclaredField("fElementChangeListener");
		field.setAccessible(true);
		return (IElementChangedListener) field.get(breadcrumb);
	}

	private static void invalidateContainer(IJavaProject project) throws Exception {
		((JavaProject) project).resetResolvedClasspath();
		JavaModelManager.getJavaModelManager().containerPut(project, StartupClasspathContainerInitializer.PATH, null);
		StartupClasspathContainerInitializer.CALLS.set(0);
		StartupClasspathContainerInitializer.UI_CALLS.clear();
	}

	private static void sendClasspathDelta(IBreadcrumb breadcrumb, IJavaProject project, int flags) throws Exception {
		JavaElementDelta delta= new JavaElementDelta(project.getJavaModel());
		delta.changed(project, flags);
		deliverDelta(breadcrumb, delta);
	}

	private static void deliverDelta(IBreadcrumb breadcrumb, IJavaElementDelta delta) throws Exception {
		IElementChangedListener listener= modelListener(breadcrumb);
		Job notification= new Job("Deliver breadcrumb model notification") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				listener.elementChanged(new ElementChangedEvent(delta, ElementChangedEvent.POST_CHANGE));
				return Status.OK_STATUS;
			}
		};
		notification.schedule();
		assertTrue(notification.join(10_000, null), "Model notification blocked outside the UI thread");
		assertTrue(notification.getResult().isOK(), () -> notification.getResult().toString());
	}

	private static void assertNoUnnecessaryInitialization(IBreadcrumb breadcrumb, IJavaProject project,
			ICompilationUnit unit, boolean otherProject) throws Exception {
		AtomicInteger scheduled= new AtomicInteger();
		JobChangeAdapter observer= new JobChangeAdapter() {
			@Override
			public void scheduled(IJobChangeEvent event) {
				Job job= event.getJob();
				if (job.belongsTo(breadcrumb) && (otherProject || !(job instanceof UIJob)))
					scheduled.incrementAndGet();
			}
		};
		Job.getJobManager().addJobChangeListener(observer);
		try {
			JavaElementDelta delta= new JavaElementDelta(project.getJavaModel());
			if (otherProject)
				delta.changed(project.getJavaModel().getJavaProject("OtherBreadcrumbProject"), IJavaElementDelta.F_CLASSPATH_CHANGED);
			else
				delta.changed(unit, IJavaElementDelta.F_CONTENT);
			deliverDelta(breadcrumb, delta);
			awaitInitialization(breadcrumb);
			assertEquals(0, scheduled.get(), "Unrelated changes must not start initialization work");
		} finally {
			Job.getJobManager().removeJobChangeListener(observer);
		}
	}

	private static void awaitContainerAndBreadcrumb(IBreadcrumb breadcrumb) throws Exception {
		long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (StartupClasspathContainerInitializer.CALLS.get() == 0) {
			assertTrue(System.nanoTime() < deadline, "Classpath invalidation was not processed");
			drainEvents();
			Thread.sleep(10);
		}
		awaitInitialization(breadcrumb);
		assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
	}

	private static void drainEvents() {
		while (Display.getCurrent().readAndDispatch()) {
		}
	}

	private static void awaitInitialization(IBreadcrumb breadcrumb) throws Exception {
		long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		do {
			assertTrue(System.nanoTime() < deadline, "Breadcrumb initialization did not finish");
			drainEvents();
			if (Job.getJobManager().find(breadcrumb).length == 0)
				return;
			Thread.sleep(10);
		} while (true);
	}

	private static void awaitInput(Viewer viewer, String expected) throws Exception {
		long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (!(viewer.getInput() instanceof IJavaElement element) || !expected.equals(element.getElementName())) {
			assertTrue(System.nanoTime() < deadline, "Breadcrumb did not publish the latest input: " + expected + "; actual: " + viewer.getInput());
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
		CountDownLatch initialDone= new CountDownLatch(1);
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
			// The editor history can restore a selection saved by another scenario.
			// Pin the input before testing classpath changes or pending completions.
			javaEditor.selectAndReveal(source.indexOf("A {}"), 0);
			if (scenario == Scenario.PENDING_CLASSPATH) {
				Job[] jobs= Arrays.stream(Job.getJobManager().find(breadcrumb)).filter(job -> !(job instanceof UIJob)).toArray(Job[]::new);
				assertEquals(1, jobs.length);
				jobs[0].addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						initialDone.countDown();
					}
				});
			} else if (scenario == Scenario.CLOSE) {
				page.closeEditor(editor, false);
				editor= null;
			} else if (scenario == Scenario.CANCEL) {
				Job[] jobs= Arrays.stream(Job.getJobManager().find(breadcrumb)).filter(job -> !(job instanceof UIJob)).toArray(Job[]::new);
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
			if (scenario == Scenario.PENDING_CLASSPATH) {
				assertTrue(initialDone.await(10, TimeUnit.SECONDS));
				invalidateContainer(project);
				sendClasspathDelta(breadcrumb, project, IJavaElementDelta.F_CLASSPATH_CHANGED);
				awaitContainerAndBreadcrumb(breadcrumb);
			}
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
			if (scenario == Scenario.CLASSPATH || scenario == Scenario.RESOLVED_CLASSPATH
					|| scenario == Scenario.COALESCED_CLASSPATH || scenario == Scenario.NULL_COLD || scenario == Scenario.ROOT_CLASSPATH) {
				TestUtils.waitForReconciler(javaEditor, 60_000);
				awaitInitialization(breadcrumb);
				if (scenario == Scenario.COALESCED_CLASSPATH) {
					JavaElementDelta content= new JavaElementDelta(project.getJavaModel());
					content.changed(first, IJavaElementDelta.F_CONTENT);
					deliverDelta(breadcrumb, content);
				}
				if (scenario == Scenario.NULL_COLD)
					project.close();
				invalidateContainer(project);
				if (scenario == Scenario.NULL_COLD) {
					breadcrumb.setInput(null);
				} else if (scenario == Scenario.ROOT_CLASSPATH) {
					JavaElementDelta delta= new JavaElementDelta(project.getJavaModel());
					// A sibling root is not an ancestor of the displayed Java element.
					delta.changed(project.getPackageFragmentRoot(project.getProject().getFolder("other-src")),
							IJavaElementDelta.F_ADDED_TO_CLASSPATH);
					deliverDelta(breadcrumb, delta);
				} else
					sendClasspathDelta(breadcrumb, project, scenario == Scenario.RESOLVED_CLASSPATH
							? IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED : IJavaElementDelta.F_CLASSPATH_CHANGED);
				awaitContainerAndBreadcrumb(breadcrumb);
				awaitInput(viewer, "A");
			}
			if (scenario == Scenario.OTHER_PROJECT || scenario == Scenario.CONTENT_ONLY) {
				TestUtils.waitForReconciler(javaEditor, 60_000);
				awaitInitialization(breadcrumb);
				assertNoUnnecessaryInitialization(breadcrumb, project, first, scenario == Scenario.OTHER_PROJECT);
				awaitInput(viewer, "A");
			}
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
