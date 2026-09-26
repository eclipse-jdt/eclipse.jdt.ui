/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.util.TestUtils;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class BreadcrumbStartupTests {
	@Test
	public void coldEditorWithBreadcrumbDoesNotInitializeContainerOnUIThread() throws Exception {
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
		try {
			CoreUtility.setAutoBuilding(false);
			preferences.setValue(key, true);
			project= JavaProjectHelper.createJavaProject("BreadcrumbStartupTests", "bin");
			IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(project, "src");
			root.createPackageFragment("p", true, null).createCompilationUnit("A.java", "package p; public class A {}", true, null);
			JavaProjectHelper.addToClasspath(project, JavaCore.newContainerEntry(StartupClasspathContainerInitializer.PATH));
			project.getResolvedClasspath(true);
			page.showView(JavaUI.ID_PACKAGES);
			TestUtils.waitForIndexer();
			project.close();
			((JavaProject) project).resetResolvedClasspath();
			JavaModelManager.getJavaModelManager().containerPut(project, StartupClasspathContainerInitializer.PATH, null);
			StartupClasspathContainerInitializer.CALLS.set(0);
			StartupClasspathContainerInitializer.UI_CALLS.clear();
			StartupClasspathContainerInitializer.watchedProject= project.getElementName();
			editor= page.openEditor(new FileEditorInput(project.getProject().getFile("src/p/A.java")), JavaUI.ID_CU_EDITOR);
			JavaEditor javaEditor= assertInstanceOf(JavaEditor.class, editor);
			assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
			TestUtils.waitForJobFamily(60_000, JavaUI.ID_PLUGIN);
			TestUtils.waitForReconciler(javaEditor, 60_000);
			while (Display.getCurrent().readAndDispatch()) {
			}
			assertTrue(StartupClasspathContainerInitializer.CALLS.get() > 0, "Test must actually initialize the cold container");
			assertTrue(StartupClasspathContainerInitializer.UI_CALLS.isEmpty(), () -> String.join("\n", StartupClasspathContainerInitializer.UI_CALLS));
			Viewer viewer= assertInstanceOf(Viewer.class, javaEditor.getBreadcrumb().getSelectionProvider());
			assertInstanceOf(IJavaElement.class, viewer.getInput(), "Breadcrumb must eventually show the Java input");
		} finally {
			StartupClasspathContainerInitializer.watchedProject= null;
			try {
				if (editor != null)
					page.closeEditor(editor, false);
				TestUtils.waitForEditorJobs(60_000, true);
				if (project != null)
					JavaProjectHelper.delete(project);
			} finally {
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
