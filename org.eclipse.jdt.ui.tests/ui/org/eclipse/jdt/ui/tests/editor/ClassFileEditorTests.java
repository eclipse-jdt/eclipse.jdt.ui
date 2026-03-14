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
import org.eclipse.swt.graphics.Color;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

public class ClassFileEditorTests {
	private static final String TYPE_NAME= "HelloWorld";

	private String fileName;
	private String className;
	private IJavaProject javaProject;

	@BeforeEach
	void setUp() throws Exception {
		fileName= TYPE_NAME + ".java";
		className= TYPE_NAME + ".class";
		javaProject = setUpProject();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
	}

	private IJavaProject setUpProject() throws Exception {
		javaProject= JavaProjectHelper.createJavaProject(ClassFileInputTests.class.getSimpleName(), "bin");
		JavaProjectHelper.addSourceContainer(javaProject, "src");
		IPackageFragment fragment= javaProject.findPackageFragment(javaProject.getProject().getFullPath().append("src"));
		String content= """
			public class HelloWorld {
				void main() {
					IO.println("Hello World");
				}
			}
		""";
		fragment.createCompilationUnit(fileName, content, true, new NullProgressMonitor());
		javaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		return javaProject;
	}

	@Test
	void testHighlightRange() throws CoreException {
		javaProject.getProject().getFile("src/" + fileName).delete(true,new NullProgressMonitor());
		ClassFileEditor classEditor= (ClassFileEditor) EditorUtility.openInEditor(javaProject.getProject().getFile("bin/" + className));
		assertFalse(classEditor.isEditable());
		classEditor.highlightInstruction("main", "()V", 3, "doesnotexist");
		StyledText noSourceTextWidget= classEditor.getNoSourceTextWidget();
		StyledTextContent content= noSourceTextWidget.getContent();
		StyleRange[] ranges= noSourceTextWidget.getStyleRanges();
		assertEquals(1, ranges.length);
		StyleRange range= ranges[0];
		String highlighted= content.getTextRange(range.start, range.length);
		assertEquals("     3  dup", highlighted);
		assertEquals(new Color(0,255,0), range.background);
	}
}
