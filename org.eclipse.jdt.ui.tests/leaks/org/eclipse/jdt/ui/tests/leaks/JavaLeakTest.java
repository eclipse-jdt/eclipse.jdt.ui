/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.leaks;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.leaktest.LeakTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewProjectCreationWizard;

public class JavaLeakTest extends LeakTestCase {
	
	private IJavaProject fJProject1;

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


	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	private ICompilationUnit createTestCU() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("public class List1 {\n}\n");
		return pack2.createCompilationUnit("List1.java", buf.toString(), false, null);
	}
	

	private Object fGlobalReference;
					
	public void testJavaEditor() throws Exception {
		Class cl= CompilationUnitEditor.class;

		// count before opening the editor
		int count1= getInstanceCount(cl);
		
		// open an editor on a CU
		ICompilationUnit unit= createTestCU();
		fGlobalReference= EditorUtility.openInEditor(unit);
		// make sure the received instance has the type we're expecting
		assertEquals(fGlobalReference.getClass(), cl);

		int count2= getInstanceCount(cl);
		assertDifferentCount("JavaEditor", count1, count2);
		
		// close the editor
		boolean res= JavaPlugin.getActivePage().closeEditor((IEditorPart) fGlobalReference, false);
		fGlobalReference= null;
		assertTrue("Could not close editor", res);
		
		int count3= getInstanceCount(cl);
		assertEqualCount("JavaEditor", count1, count3);
	}
	
	public void testNewClassWizard() throws Exception {
		int count1= getInstanceCount(NewClassCreationWizard.class);
		doWizardLeakTest(new NewClassCreationWizard());
		int count2= getInstanceCount(NewClassCreationWizard.class);
		assertEqualCount("NewClassCreationWizard", count1, count2);
	}
	
	public void testNewInterfaceWizard() throws Exception {
		int count1= getInstanceCount(NewInterfaceCreationWizard.class);
		doWizardLeakTest(new NewInterfaceCreationWizard());
		int count2= getInstanceCount(NewInterfaceCreationWizard.class);
		assertEqualCount("NewInterfaceCreationWizard", count1, count2);
	}
	
	public void testNewJavaProjectWizard() throws Exception {
		int count1= getInstanceCount(NewProjectCreationWizard.class);
		doWizardLeakTest(new NewProjectCreationWizard());
		int count2= getInstanceCount(NewProjectCreationWizard.class);
		assertEqualCount("NewProjectCreationWizard", count1, count2);
	}		
	
	private void doWizardLeakTest(INewWizard wizard) throws Exception {
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fJProject1));
		
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
