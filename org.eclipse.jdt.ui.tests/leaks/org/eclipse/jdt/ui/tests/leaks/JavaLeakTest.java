/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.leaks;

import java.io.StringBufferInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;

import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.leaktest.LeakTestSetup;

public class JavaLeakTest extends LeakTestCase {
	
	private IJavaProject fJProject;

	private static final Class THIS= JavaLeakTest.class;
	
	public JavaLeakTest(String name) {
		super(name);
	}

	public static Test suite() {
		if (true) {
			return new LeakTestSetup(new TestSuite(THIS));
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new JavaLeakTest("testLeak"));
			return new LeakTestSetup(suite);
		}	
	}
	
	public void testTextEditorClose() throws Exception {
		IFile file= createTestFile("Test.txt");
		internalTestEditorClose(file, TextEditor.class, false);
	}
	
	public void testTextEditorCloseAll() throws Exception {
		IFile file= createTestFile("Test.txt");
		internalTestEditorClose(file, TextEditor.class, true);
	}

	public void testJavaEditorClose() throws Exception {
		ICompilationUnit cu= createTestCU("Test");
		internalTestEditorClose(cu, CompilationUnitEditor.class, false);
	}
	
	public void testJavaEditorCloseAll() throws Exception {
		ICompilationUnit cu= createTestCU("Test");
		internalTestEditorClose(cu, CompilationUnitEditor.class, true);
		
	}

	protected void setUp() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("RT not found", JavaProjectHelper.addRTJar(fJProject) != null);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
	}

	private ICompilationUnit createTestCU(String typeName) throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("public class "+typeName+" {\n}\n");
		return pack2.createCompilationUnit(typeName+".java", buf.toString(), false, null);
	}
	
	private IFile createTestFile(String fileName) throws Exception {
		IFile file= fJProject.getProject().getFile(fileName);
		file.create(new StringBufferInputStream("test\n"), false, null);
		assertTrue(file.exists());
		return file;
	}
	
	private void internalTestEditorClose(Object objectToOpen, final Class clazz, boolean closeAll) throws Exception {
		
		// open an editor on given object
		IEditorPart part= EditorUtility.openInEditor(objectToOpen);

		// make sure the received instance has the type we're expecting
		assertEquals(part.getClass(), clazz);

		// verify that the editor instance is there
		assertInstanceCount(clazz, 1);
		
		// close the editor
		boolean res;
		if (closeAll)
			res= JavaPlugin.getActivePage().closeAllEditors(false);
		else
			res= JavaPlugin.getActivePage().closeEditor(part, false);
		part= null;
		assertTrue("Could not close editor", res);
		
		// verify that the editor instance is gone
		assertInstanceCount(clazz, 0);
	}
	
	public void testNewClassWizard() throws Exception {
		assertInstanceCount(NewClassCreationWizard.class, 0);
		doWizardLeakTest(new NewClassCreationWizard());
		assertInstanceCount(NewClassCreationWizard.class, 0);
	}
	
	public void testNewInterfaceWizard() throws Exception {
		assertInstanceCount(NewInterfaceCreationWizard.class, 0);
		doWizardLeakTest(new NewInterfaceCreationWizard());
		assertInstanceCount(NewInterfaceCreationWizard.class, 0);
	}
	
	public void testNewJavaProjectWizard() throws Exception {
		assertInstanceCount(JavaProjectWizard.class, 0);
		doWizardLeakTest(new JavaProjectWizard());
		assertInstanceCount(JavaProjectWizard.class, 0);
	}		
	
	private void doWizardLeakTest(INewWizard wizard) throws Exception {
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fJProject));
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		WizardDialog dialog= new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(false);
		dialog.create();
		dialog.open();
		
		dialog.close();
		wizard= null;
		dialog= null;
	}	
}
