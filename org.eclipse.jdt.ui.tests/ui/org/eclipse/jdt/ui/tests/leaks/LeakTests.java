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

import java.lang.ref.WeakReference;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewProjectCreationWizard;

public class LeakTests extends TestCase {
	
	private IJavaProject fJProject1;

	private static final Class THIS= LeakTests.class;

	public LeakTests(String name) {
		super(name);
	}

	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new LeakTests("testJavaEditor"));
			return suite;
		}	
	}


	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	private void garbageCollect() {
		System.gc();
	}
	
	
	public void testLeak() {
		Object leak= new char[100000];
		WeakReference ref= new WeakReference(leak);
		
		garbageCollect();
		assertTrue(ref.get() != null);
		
		leak= null;
		garbageCollect();
		assertTrue(ref.get() == null);
	}
	
	public void testJavaEditor() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("public class List1 {\n");
		buf.append("}\n");
		ICompilationUnit unit= pack2.createCompilationUnit("List1.java", buf.toString(), false, null);
		IEditorPart part= EditorUtility.openInEditor(unit);

		WeakReference ref= new WeakReference(part);
		garbageCollect();
		assertTrue(ref.get() != null);
		
		JavaPlugin.getActivePage().closeEditor(part, false);
		part= null;
		
		garbageCollect();
		assertTrue(ref.get() == null);
	}
	
	public void testNewClassWizard() throws Exception {
		doWizardLeakTest(new NewClassCreationWizard());
	}
	
	public void testNewInterfaceWizard() throws Exception {
		doWizardLeakTest(new NewInterfaceCreationWizard());
	}
	
	public void testNewJavaProjectWizard() throws Exception {
		doWizardLeakTest(new NewProjectCreationWizard());
	}		
	
	private void doWizardLeakTest(INewWizard wizard) throws Exception {
		wizard.init(JavaPlugin.getDefault().getWorkbench(), new StructuredSelection(fJProject1));
		
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		WizardDialog dialog= new WizardDialog(shell, wizard);
		dialog.setBlockOnOpen(false);
		dialog.create();
		dialog.open();

		WeakReference ref= new WeakReference(wizard);
		garbageCollect();
		assertTrue(ref.get() != null);
		
		dialog.close();
		wizard= null;
		dialog= null;
		
		garbageCollect();
		assertTrue(ref.get() == null);
	}	
	

}
