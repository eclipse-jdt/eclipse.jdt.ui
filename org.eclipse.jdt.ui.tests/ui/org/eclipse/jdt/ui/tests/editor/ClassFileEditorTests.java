/*******************************************************************************
 * Copyright (c) 2026, Daniel Schmid and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Daniel Schmid - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

public class ClassFileEditorTests {
	private static final String TYPE_NAME= "HelloWorld";

	private IJavaProject javaProject;
	private boolean wasAutoBuilding;

	@BeforeEach
	void setUp() throws Exception {
		javaProject = setUpProject();

		wasAutoBuilding = CoreUtility.setAutoBuilding(false);
	}

	@AfterEach
	void tearDown() throws Exception {
		JavaPlugin.getActivePage().closeAllEditors(false);
		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
		CoreUtility.setAutoBuilding(wasAutoBuilding);
	}

	private IJavaProject setUpProject() throws Exception {
		javaProject= JavaProjectHelper.createJavaProject(ClassFileEditorTests.class.getSimpleName(), "bin");
		JavaProjectHelper.addSourceContainer(javaProject, "src");
		JavaProjectHelper.addRTJar(javaProject);
		return javaProject;
	}

	@Test
	void testHighlightRange() throws CoreException {
		createHelloWorldClass();
		ClassFileEditor classEditor= createClassFileEditor(TYPE_NAME);
		classEditor.highlightInstruction("main", "([Ljava/lang/String;)V", 5);
		assertEquals("    5  invokevirtual java.io.PrintStream.println(java.lang.String) : void [24]", getSingleHighlightedRange(classEditor));
	}

	@Test
	void testHighlightInConstructor() throws CoreException {
		createHelloWorldClass();
		ClassFileEditor classEditor= createClassFileEditor(TYPE_NAME);
		classEditor.highlightInstruction(TYPE_NAME, "()V", 1);
		assertEquals("    1  invokespecial java.lang.Object() [8]", getSingleHighlightedRange(classEditor));
	}

	@Test
	void testChangeHighlightRange() throws CoreException {
		createHelloWorldClass();
		ClassFileEditor classEditor= createClassFileEditor(TYPE_NAME);
		classEditor.highlightInstruction("main", "([Ljava/lang/String;)V", 5);
		classEditor.highlightInstruction("main", "([Ljava/lang/String;)V", 0);
		assertEquals("    0  getstatic java.lang.System.out : java.io.PrintStream [16]", getSingleHighlightedRange(classEditor));
	}

	@Test
	void testUnhighlight() throws CoreException {
		createHelloWorldClass();
		ClassFileEditor classEditor= createClassFileEditor(TYPE_NAME);
		classEditor.highlightInstruction("main", "([Ljava/lang/String;)V", 5);
		StyledText noSourceTextWidget= classEditor.getNoSourceTextWidget();
		assertEquals(1, noSourceTextWidget.getStyleRanges().length);
		classEditor.unhighlight();
		assertEquals(0, noSourceTextWidget.getStyleRanges().length);
	}

	@Test
	void testHighlightNonexistentCodeIndex() throws CoreException {
		createHelloWorldClass();
		ClassFileEditor classEditor= createClassFileEditor(TYPE_NAME);
		classEditor.highlightInstruction("main", "([Ljava/lang/String;)V", 100);
		StyledText noSourceTextWidget= classEditor.getNoSourceTextWidget();
		assertEquals(0, noSourceTextWidget.getStyleRanges().length);
	}

	@Test
	void testHighlightNonexistentMethod() throws CoreException {
		createHelloWorldClass();
		ClassFileEditor classEditor= createClassFileEditor(TYPE_NAME);
		classEditor.highlightInstruction("main", "a", 5);
		assertEquals(0, classEditor.getNoSourceTextWidget().getStyleRanges().length);
	}

	@Test
	void testHighlightMultipleCompilationUnits() throws CoreException {
		createClassFromSource("A.java", """
				public class A {
					void a() {
						System.out.println("a");
					}
				}
				class B {
					void b() {
						System.out.println("b");
					}
				}
				""");
		ClassFileEditor classEditor= createClassFileEditor("A");
		classEditor.highlightInstruction("a", "()V", 3);
		assertEquals("    3  ldc <String \"a\"> [21]", getSingleHighlightedRange(classEditor));

		classEditor= createClassFileEditor("B");
		classEditor.highlightInstruction("b", "()V", 3);
		assertEquals("    3  ldc <String \"b\"> [21]", getSingleHighlightedRange(classEditor));

	}

	private String getSingleHighlightedRange(ClassFileEditor classEditor) {
		StyledText noSourceTextWidget= classEditor.getNoSourceTextWidget();
		StyleRange[] ranges= noSourceTextWidget.getStyleRanges();
		assertEquals(1, ranges.length);
		StyledTextContent content= noSourceTextWidget.getContent();
		StyleRange range= ranges[0];
		return content.getTextRange(range.start, range.length);
	}

	private void createHelloWorldClass() throws CoreException {
		createClassFromSource(TYPE_NAME + ".java", """
			public class HelloWorld {
				public static void main(String[] args) {
					System.out.println("Hello World");
				}
			}
		""");
	}

	private void createClassFromSource(String fileName, String content) throws CoreException {
		IPackageFragment fragment= javaProject.findPackageFragment(javaProject.getProject().getFullPath().append("src"));
		fragment.createCompilationUnit(fileName, content, true, new NullProgressMonitor());
		javaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		javaProject.getProject().getFile("src/" + fileName).delete(true,new NullProgressMonitor());
	}

	private ClassFileEditor createClassFileEditor(String typeName) throws CoreException {
		ClassFileEditor editor= (ClassFileEditor) EditorUtility.openInEditor(javaProject.getProject().getFile("bin/" + typeName + ".class"));
		assertFalse(editor.isEditable());
		return editor;
	}
}
