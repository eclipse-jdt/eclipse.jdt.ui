/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.leaks;

import java.io.ByteArrayInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;

import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

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
			suite.addTest(new JavaLeakTest("testJavaEditorActionDelegate"));
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
		assertTrue(JavaPlugin.getActivePage().closeAllEditors(false));
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
		IProject project= fJProject.getProject();
		IFile file= project.getFile(fileName);
		file.create(new ByteArrayInputStream("test\n".getBytes(project.getDefaultCharset())), false, null);
		assertTrue(file.exists());
		return file;
	}
	
	private void internalTestEditorClose(Object objectToOpen, final Class clazz, boolean closeAll) throws Exception {
		IEditorPart part= internalTestEditorOpen(objectToOpen, clazz);
		
		// can't close and assert abandonment in a separate method, since that would leave 'part' as a stack-local reference
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

	private IEditorPart internalTestEditorOpen(Object objectToOpen, final Class clazz) throws JavaModelException, PartInitException {
		// verify that no instance is there when we start
		assertInstanceCount(clazz, 0);
		
		// open an editor on given object
		IEditorPart part= EditorUtility.openInEditor(objectToOpen);

		// make sure the received instance has the type we're expecting
		assertEquals(part.getClass(), clazz);

		// verify that the editor instance is there
		assertInstanceCount(clazz, 1);
		return part;
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
	
	public void testJavaEditorContextMenu() throws Exception {
		//regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=166761
		
		ICompilationUnit cu= createTestCU("Test");
		IEditorPart part= internalTestEditorOpen(cu, CompilationUnitEditor.class);
				
		final Menu menu= ((CompilationUnitEditor) part).getViewer().getTextWidget().getMenu();
		openContextMenu(menu);
		
		boolean res= JavaPlugin.getActivePage().closeEditor(part, false);
		part= null;
		assertTrue("Could not close editor", res);
		
		// verify that the editor instance is gone
		assertInstanceCount(CompilationUnitEditor.class, 0);
	}

	private void openContextMenu(final Menu menu) {
		Display display= menu.getDisplay();
		
        while (!menu.isDisposed() && display.readAndDispatch()) {
        	//loop, don't sleep
        }
        
        // 2 work arounds for https://bugs.eclipse.org/bugs/show_bug.cgi?id=204289 :
        
        // activate shell:
        menu.getShell().forceActive();
        
        // dislocate menu from cursor:
        Point cLoc= display.getCursorLocation();
        Rectangle dBounds= display.getBounds();
        menu.setLocation(
        		cLoc.x < dBounds.width /2 ? cLoc.x + 5 : 5,
        		cLoc.y < dBounds.height/2 ? cLoc.y + 5 : 5
        );
		
        menu.setVisible(true);
		
		display.asyncExec(new Runnable() {
            public void run() {
                menu.setVisible(false);
            }
        });
		
        while (!menu.isDisposed() && display.readAndDispatch()) {
        	//loop, don't sleep
        }
	}

	public void testJavaEditorActionDelegate() throws Exception {
		//regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=166761
		
		ICompilationUnit cu= createTestCU("Test");
		IEditorPart part= internalTestEditorOpen(cu, CompilationUnitEditor.class);
				
		IWorkbench workbench= part.getSite().getWorkbenchWindow().getWorkbench();
		IHandlerService handlerService = (IHandlerService) workbench.getService(IHandlerService.class);
		handlerService.executeCommand("org.eclipse.jdt.ui.tests.JavaLeakTestActionDelegate", null);
		
		boolean res= JavaPlugin.getActivePage().closeEditor(part, false);
		part= null;
		assertTrue("Could not close editor", res);
		
		// verify that the editor instance is gone
		assertInstanceCount(CompilationUnitEditor.class, 0);
	}
	
}
