/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension6;
import org.eclipse.jface.text.IUndoManager;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Tests that Java model operations can be undone in one group.
 * <p>
 * For details see https://bugs.eclipse.org/bugs/show_bug.cgi?id=262389
 * </p>
 *
 * @since 3.5
 */
public class JavaModelOpCompundUndoTest {
	@Rule
	public TestName tn= new TestName();

	private static final String SRC= "src";
	private static final String SEP= "/";

	private static final String CU_NAME= "Bug75423.java";
	private static final String CU_CONTENTS= """
		package com.example.bugs;
		
		public class Bug75423 {
		
		    void foo() {
		       \s
		    }
		   \s
		    void bar() {
		       \s
		    }
		   \s
		}
		""";

	private JavaEditor fEditor;
	private IDocument fDocument;
	private IJavaProject fProject;
	private ICompilationUnit fCompilationUnit;
	private IUndoManager fUndoManager;

	private void setUpProject(String sourceLevel) throws CoreException, JavaModelException {
		fProject= JavaProjectHelper.createJavaProject(tn.getMethodName(), "bin");
		fProject.setOption(JavaCore.COMPILER_SOURCE, sourceLevel);
		JavaProjectHelper.addSourceContainer(fProject, SRC);
		IPackageFragment fragment= fProject.findPackageFragment(new Path(SEP + tn.getMethodName() + SEP + SRC));
		fCompilationUnit= fragment.createCompilationUnit(CU_NAME, CU_CONTENTS, true, new NullProgressMonitor());
	}

	@Before
	public void setUp() throws Exception {
		setUpProject(JavaCore.VERSION_1_5);
		setUpEditor();
	}

	private void setUpEditor() {
		fEditor= openJavaEditor(new Path(SEP + tn.getMethodName() + SEP + SRC + SEP + CU_NAME));
		assertNotNull(fEditor);
		fUndoManager= ((ITextViewerExtension6)fEditor.getViewer()).getUndoManager();
		assertNotNull(fUndoManager);
		fDocument= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		assertNotNull(fDocument);
		assertEquals(CU_CONTENTS, fDocument.get());
	}

	private JavaEditor openJavaEditor(IPath path) {
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		assertNotNull(file);
		assertTrue(file.exists());
		try {
			return (JavaEditor)EditorTestHelper.openInEditor(file, true);
		} catch (PartInitException e) {
			fail();
			return null;
		}
	}

	@After
	public void tearDown() throws Exception {
		EditorTestHelper.closeEditor(fEditor);
		fEditor= null;
		if (fProject != null) {
			JavaProjectHelper.delete(fProject);
			fProject= null;
		}
	}

	@Test
	public void test1() throws Exception {

		assertEquals(CU_CONTENTS, fDocument.get());
		fUndoManager.beginCompoundChange();

		IMethod foo= JavaModelUtil.findMethod("foo", new String[0], false, fCompilationUnit.findPrimaryType());
		IMethod bar= JavaModelUtil.findMethod("bar", new String[0], false, fCompilationUnit.findPrimaryType());
		IJavaModel model= JavaCore.create(JavaPlugin.getWorkspace().getRoot());
		model.delete(new IJavaElement[] { foo, bar }, true, null);

		fUndoManager.endCompoundChange();
		assertNotEquals(CU_CONTENTS, fDocument.get());

		fUndoManager.undo();
		assertEquals(CU_CONTENTS, fDocument.get());

	}

	@Test
	public void test2() throws Exception {
		setUpProject(JavaCore.VERSION_1_5);
		setUpEditor();

		assertEquals(CU_CONTENTS, fDocument.get());
		fUndoManager.beginCompoundChange();

		IMethod foo= JavaModelUtil.findMethod("foo", new String[0], false, fCompilationUnit.findPrimaryType());
		IMethod bar= JavaModelUtil.findMethod("bar", new String[0], false, fCompilationUnit.findPrimaryType());
		IJavaModel model= JavaCore.create(JavaPlugin.getWorkspace().getRoot());
		model.delete(new IJavaElement[] { foo }, true, null);
		model.delete(new IJavaElement[] { bar }, true, null);

		fUndoManager.endCompoundChange();
		assertNotEquals(CU_CONTENTS, fDocument.get());

		fUndoManager.undo();
		assertEquals(CU_CONTENTS, fDocument.get());
	}
}
