/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.util.TestUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.typehierarchy.TypeHierarchyViewPart;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

/** Exercise the actual views without allowing other listeners to warm the cold model. */
public class ViewStartupTests {
	private IWorkbenchPage fPage;
	private IJavaProject fProject;
	private ICompilationUnit fUnit;
	private String fTypeHandle;
	private String fMissingTypeHandle;
	private IEditorPart fEditor;
	private PackageExplorerPart fPackages;
	private TypeHierarchyViewPart fHierarchy;
	private boolean fAutoBuilding;
	private boolean fOldLinking;
	private boolean fOldBreadcrumb;
	private boolean fDefaultBreadcrumb;
	private String fBreadcrumbKey;
	private CountDownLatch fEntered;
	private CountDownLatch fRelease;

	@BeforeEach
	public void setUp() throws Exception {
		TestUtils.waitForEditorJobs(60_000, true);
		TestUtils.waitForJobFamily(60_000, JavaUI.ID_PLUGIN);
		fPage= JavaPlugin.getActivePage();
		fAutoBuilding= ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
		CoreUtility.setAutoBuilding(false);
		fBreadcrumbKey= JavaEditor.EDITOR_SHOW_BREADCRUMB + "." + fPage.getPerspective().getId();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		fDefaultBreadcrumb= store.isDefault(fBreadcrumbKey);
		fOldBreadcrumb= store.getBoolean(fBreadcrumbKey);
		store.setValue(fBreadcrumbKey, false);
		fProject= JavaProjectHelper.createJavaProject("ViewStartupTests", "bin");
		JavaProjectHelper.addRTJar(fProject);
		IPackageFragment pack= JavaProjectHelper.addSourceContainer(fProject, "src").createPackageFragment("p", true, null);
		fUnit= pack.createCompilationUnit("A.java", "package p; public class A {}", true, null);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(StartupClasspathContainerInitializer.PATH));
		fProject.getResolvedClasspath(true);
		// Persist the handles before invalidating the model, just as a real
		// workbench restart reads a memento saved during the previous session.
		fTypeHandle= fUnit.getType("A").getHandleIdentifier();
		fMissingTypeHandle= fUnit.getType("Missing").getHandleIdentifier();
		fPackages= (PackageExplorerPart) fPage.showView(JavaUI.ID_PACKAGES);
		fOldLinking= fPackages.isLinkingEnabled();
		fPackages.setLinkingEnabled(false);
		fEditor= fPage.openEditor(new FileEditorInput(fUnit.getResource().getAdapter(org.eclipse.core.resources.IFile.class)), JavaUI.ID_CU_EDITOR);
		TestUtils.waitForReconciler((JavaEditor) fEditor, 60_000);
		TestUtils.waitForIndexer();
		fPackages.getTreeViewer().setSelection(StructuredSelection.EMPTY);
		fPackages.collapseAll();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (fRelease != null)
			fRelease.countDown();
		StartupClasspathContainerInitializer.watchedProject= null;
		try {
			if (fPackages != null) {
				fPackages.setLinkingEnabled(false);
				awaitJobs(fPackages);
			}
			if (fHierarchy != null) {
				awaitJobs(fHierarchy);
				fPage.hideView(fHierarchy);
			}
			if (fEditor != null)
				fPage.closeEditor(fEditor, false);
			TestUtils.waitForEditorJobs(60_000, true);
			if (fPackages != null) {
				fPackages.setLinkingEnabled(fOldLinking);
				fPage.hideView(fPackages);
			}
			if (fProject != null)
				JavaProjectHelper.delete(fProject);
		} finally {
			StartupClasspathContainerInitializer.entered= null;
			StartupClasspathContainerInitializer.release= null;
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			if (fDefaultBreadcrumb)
				store.setToDefault(fBreadcrumbKey);
			else
				store.setValue(fBreadcrumbKey, fOldBreadcrumb);
			CoreUtility.setAutoBuilding(fAutoBuilding);
		}
	}

	private void armContainer(boolean block) throws Exception {
		fProject.close();
		((JavaProject) fProject).resetResolvedClasspath();
		JavaModelManager.getJavaModelManager().containerPut(fProject, StartupClasspathContainerInitializer.PATH, null);
		StartupClasspathContainerInitializer.CALLS.set(0);
		StartupClasspathContainerInitializer.UI_CALLS.clear();
		StartupClasspathContainerInitializer.TIMED_OUT.set(false);
		fEntered= new CountDownLatch(1);
		fRelease= new CountDownLatch(block ? 1 : 0);
		StartupClasspathContainerInitializer.entered= fEntered;
		StartupClasspathContainerInitializer.release= fRelease;
		StartupClasspathContainerInitializer.watchedProject= fProject.getElementName();
	}

	private void assertBackgroundInitialization() throws Exception {
		assertTrue(fEntered.await(10, TimeUnit.SECONDS), "Initializer was not reached");
		assertFalse(StartupClasspathContainerInitializer.TIMED_OUT.get(), "UI waited for the suspended initializer");
		assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
	}

	private static void await(BooleanSupplier condition) throws Exception {
		long deadline= System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		do {
			while (Display.getCurrent().readAndDispatch()) {
			}
			if (condition.getAsBoolean())
				return;
			assertTrue(System.nanoTime() < deadline, "View did not complete its pending update");
			Thread.sleep(10);
		} while (true);
	}

	private static void awaitJobs(Object family) throws Exception {
		await(() -> Job.getJobManager().find(family).length == 0);
	}

	@Test
	public void editorLinkResolvesColdModelOffUIThread() throws Exception {
		armContainer(true);
		fPackages.setLinkingEnabled(true);
		assertBackgroundInitialization();
		fRelease.countDown();
		await(() -> ((IStructuredSelection) fPackages.getTreeViewer().getSelection()).toList().contains(fUnit));
	}

	@Test
	public void disablingEditorLinkDiscardsPendingSelection() throws Exception {
		armContainer(true);
		fPackages.setLinkingEnabled(true);
		assertBackgroundInitialization();
		fPackages.setLinkingEnabled(false);
		fRelease.countDown();
		awaitJobs(fPackages);
		assertTrue(fPackages.getTreeViewer().getSelection().isEmpty());
	}

	@Test
	public void closingPackageExplorerDiscardsPendingSelection() throws Exception {
		armContainer(true);
		fPackages.setLinkingEnabled(true);
		assertBackgroundInitialization();
		PackageExplorerPart packages= fPackages;
		fPage.hideView(packages);
		fPackages= null;
		fRelease.countDown();
		awaitJobs(packages);
		assertTrue(packages.getTreeViewer().getControl().isDisposed());
	}

	private void restoreHierarchy(String handle) throws Exception {
		XMLMemento memento= XMLMemento.createWriteRoot("hierarchy");
		if (handle != null)
			memento.putString("input", handle);
		// Invoke the same restoration entry point used by createPartControl.
		Method restore= TypeHierarchyViewPart.class.getDeclaredMethod("restoreState", IMemento.class);
		restore.setAccessible(true);
		restore.invoke(fHierarchy, memento);
	}

	private void prepareHierarchy() throws Exception {
		fHierarchy= (TypeHierarchyViewPart) fPage.showView(JavaUI.ID_TYPE_HIERARCHY);
		fHierarchy.setInputElements(null);
	}

	@Test
	public void hierarchyRestoreValidatesColdHandleOffUIThread() throws Exception {
		prepareHierarchy();
		armContainer(true);
		restoreHierarchy(fTypeHandle);
		assertBackgroundInitialization();
		fRelease.countDown();
		await(() -> fUnit.getType("A").equals(fHierarchy.getInputElement()));
	}

	@Test
	public void closingHierarchyWhileRestoringDoesNotWaitForInitializer() throws Exception {
		prepareHierarchy();
		armContainer(true);
		restoreHierarchy(fTypeHandle);
		assertBackgroundInitialization();
		TypeHierarchyViewPart hierarchy= fHierarchy;
		fPage.hideView(hierarchy);
		fHierarchy= null;
		assertFalse(StartupClasspathContainerInitializer.TIMED_OUT.get());
		fRelease.countDown();
		awaitJobs(hierarchy);
	}

	@Test
	public void clearingHierarchyDiscardsPendingRestoreWithoutJoiningWorker() throws Exception {
		prepareHierarchy();
		armContainer(true);
		restoreHierarchy(fTypeHandle);
		assertBackgroundInitialization();
		fHierarchy.setInputElements(null);
		assertFalse(StartupClasspathContainerInitializer.TIMED_OUT.get());
		fRelease.countDown();
		awaitJobs(fHierarchy);
		assertNull(fHierarchy.getInputElement());
	}

	@Test
	public void missingHierarchyHandleRestoresAnEmptyView() throws Exception {
		prepareHierarchy();
		armContainer(false);
		restoreHierarchy(fMissingTypeHandle);
		// A missing working-copy member can be rejected without initializing a
		// container at all; require safe completion, not unnecessary model work.
		awaitJobs(fHierarchy);
		assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
		assertNull(fHierarchy.getInputElement());
		assertNotNull(fPage.findView(JavaUI.ID_TYPE_HIERARCHY));
	}
}
