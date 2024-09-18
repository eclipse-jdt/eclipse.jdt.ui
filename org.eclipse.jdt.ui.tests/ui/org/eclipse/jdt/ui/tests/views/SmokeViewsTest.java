/*******************************************************************************
 * Copyright (c) 2023, 2024 Andrey Loskutov (loskutov@gmx.de) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Andrey Loskutov (loskutov@gmx.de) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.views;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput;

public class SmokeViewsTest {

	private static boolean welcomeClosed;
	IViewPart view;
	private IWorkbenchWindow window;
	private IJavaProject javaProject ;
	private IEditorPart editor;

	@Before
	public void setUp() throws Exception {
		window = setupWindow();
		javaProject = createProject();
		editor = openClassEditor();
		DisplayHelper.driveEventQueue(Display.getDefault());
	}

	@After
	public void tearDown() throws Exception {
		if (window != null) {
			if (editor != null) {
				window.getActivePage().closeEditor(editor, false);
			}
			if (view != null) {
				window.getActivePage().hideView(view);
			}
			DisplayHelper.driveEventQueue(Display.getDefault());
		}
		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
	}

	@Test
	public void testOpenAstView() throws Exception {
		smokeTest("org.eclipse.jdt.astview.views.ASTView");
	}

	@Test
	public void testOpenJavaElementView() throws Exception {
		smokeTest("org.eclipse.jdt.jeview.views.JavaElementView");
	}

	@Test
	public void testOpenBytecodeOutlineView() throws Exception {
		smokeTest("org.eclipse.jdt.bcoview.views.BytecodeOutlineView");
	}

	@Test
	public void testOpenBytecodeReferenceView() throws Exception {
		smokeTest("org.eclipse.jdt.bcoview.views.BytecodeReferenceView");
	}

	@Test
	public void testOpenJavadocView() throws Exception {
		smokeTest(JavaUI.ID_JAVADOC_VIEW);
	}

	@Test
	public void testOpenPackagesView() throws Exception {
		smokeTest(JavaUI.ID_PACKAGES_VIEW);
	}

	@Test
	public void testOpenTypesView() throws Exception {
		smokeTest(JavaUI.ID_TYPES_VIEW);
	}

	@Test
	public void testOpenMembersView() throws Exception {
		smokeTest(JavaUI.ID_MEMBERS_VIEW);
	}

	@Test
	public void testOpenProjectsView() throws Exception {
		smokeTest(JavaUI.ID_PROJECTS_VIEW);
	}

	@Test
	public void testOpenSourceView() throws Exception {
		smokeTest(JavaUI.ID_SOURCE_VIEW);
	}

	@SuppressWarnings("restriction") // org.eclipse.ui.internal.ErrorViewPart
	private void smokeTest(String viewId) throws PartInitException {
		view = window.getActivePage().showView(viewId);
		assertNotNull("View " + viewId + " should be created", view);
		DisplayHelper.driveEventQueue(Display.getDefault());
		if(view instanceof org.eclipse.ui.internal.ErrorViewPart errorView) {
			System.out.println(errorView.getContentDescription());
			fail("Error happened on opening view " + viewId);
		}
	}

	private static IWorkbenchWindow setupWindow() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (!welcomeClosed) {
			closeIntro(workbench);
		}
		IWorkbenchWindow wwindow = workbench.getActiveWorkbenchWindow();
		if (wwindow == null) {
			wwindow =  workbench.getWorkbenchWindows()[0];
		}
		return wwindow;
	}

	private static void closeIntro(final IWorkbench wb) {
		IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
		if (window != null) {
			IIntroManager im = wb.getIntroManager();
			IIntroPart intro = im.getIntro();
			if (intro != null) {
				welcomeClosed = im.closeIntro(intro);
				DisplayHelper.driveEventQueue(Display.getDefault());
			}
		}
	}

	private IEditorPart openClassEditor() throws JavaModelException, PartInitException {
		IOrdinaryClassFile classfile = (IOrdinaryClassFile) javaProject.findElement(new Path("java/lang/String.java"));
		assertNotNull("java.lang.String class not found", classfile);
		InternalClassFileEditorInput input = new InternalClassFileEditorInput(classfile);
		IEditorPart ceditor = IDE.openEditor(window.getActivePage(), input, "org.eclipse.jdt.ui.ClassFileEditor", true);
		assertNotNull("Class file editor should be opened", ceditor);
		DisplayHelper.driveEventQueue(Display.getDefault());
		return ceditor;
	}

	private IJavaProject createProject() throws CoreException {
		IJavaProject project = JavaProjectHelper.createJavaProject("SmokeViewsTest", "bin");

		String javaHome = System.getProperty("java.home") + File.separator;
		String latestSupportedJavaVersion = JavaCore.latestSupportedJavaVersion();
		String jdkRelease = System.getProperty("java.specification.version");
		if(jdkRelease.compareTo(latestSupportedJavaVersion) >= 0) {
			JavaProjectHelper.setLatestCompilerOptions(project);
		} else {
			// Smallest version available for tests on Jenkins/jipp
			JavaProjectHelper.set18CompilerOptions(project);
		}

		Path bootModPath = new Path(javaHome + "/lib/jrt-fs.jar");
		Path sourceAttachment = new Path(javaHome + "/lib/src.zip");

		IClasspathAttribute[] attributes = { JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, "true") };
		IClasspathEntry jrtEntry = JavaCore.newLibraryEntry(bootModPath, sourceAttachment, null, null, attributes, false);
		IClasspathEntry[] old = project.getRawClasspath();
		IClasspathEntry[] newPath = new IClasspathEntry[old.length + 1];
		System.arraycopy(old, 0, newPath, 0, old.length);
		newPath[old.length] = jrtEntry;
		project.setRawClasspath(newPath, null);
		DisplayHelper.driveEventQueue(Display.getDefault());
		return project;
	}

}
