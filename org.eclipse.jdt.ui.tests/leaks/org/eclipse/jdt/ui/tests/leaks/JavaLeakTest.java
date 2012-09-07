/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.commands.common.EventManager;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.leaktest.LeakTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileEditor;
import org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.internal.ui.wizards.NewClassCreationWizard;
import org.eclipse.jdt.internal.ui.wizards.NewInterfaceCreationWizard;

public class JavaLeakTest extends LeakTestCase {

	private IJavaProject fJProject;

	private static final Class THIS= JavaLeakTest.class;

	public JavaLeakTest(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new LeakTestSetup(test);
	}

	public void testTextEditorClose() throws Exception {
		IFile file= createTestFile("Test.txt");
		internalTestEditorClose(file, TextEditor.class, false);
	}

	public void testTextEditorCloseOneOfTwo() throws Exception {
		IFile file1= createTestFile("Test1.txt");
		IEditorPart editor1= EditorUtility.openInEditor(file1);
		assertEquals(editor1.getClass(), TextEditor.class);
		assertInstanceCount(TextEditor.class, 1);

		IFile file2= createTestFile("Test2.txt");
		IEditorPart editor2= EditorUtility.openInEditor(file2);
		assertEquals(editor2.getClass(), TextEditor.class);
		assertInstanceCount(TextEditor.class, 2);

		assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor2, false));
		editor2= null;

		assertInstanceCount(TextEditor.class, 1);

		assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor1, false));
		editor1= null;
		
		assertInstanceCount(TextEditor.class, 0);
	}

	public void testTextEditorCloseAll() throws Exception {
		IFile file= createTestFile("Test.txt");
		internalTestEditorClose(file, TextEditor.class, true);
	}

	public void testPropertiesEditorClose() throws Exception {
		IFile file= createTestFile("Test.properties");
		internalTestEditorClose(file, PropertiesFileEditor.class, false);
	}

	public void testPropertiesEditorCloseOneOfTwo() throws Exception {
		IFile file1= createTestFile("Test1.properties");
		IEditorPart editor1= EditorUtility.openInEditor(file1);
		assertEquals(editor1.getClass(), PropertiesFileEditor.class);
		assertInstanceCount(PropertiesFileEditor.class, 1);

		IFile file2= createTestFile("Test2.properties");
		IEditorPart editor2= EditorUtility.openInEditor(file2);
		assertEquals(editor2.getClass(), PropertiesFileEditor.class);
		assertInstanceCount(PropertiesFileEditor.class, 2);

		assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor2, false));
		editor2= null;

		assertInstanceCount(PropertiesFileEditor.class, 1);

		assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor1, false));
		editor1= null;

		assertInstanceCount(PropertiesFileEditor.class, 0);
	}

	public void testPropertiesEditorCloseAll() throws Exception {
		IFile file= createTestFile("Test.properties");
		internalTestEditorClose(file, PropertiesFileEditor.class, true);
	}

	public void testJavaEditorClose() throws Exception {
		ICompilationUnit cu= createTestCU("Test");
		internalTestEditorClose(cu, CompilationUnitEditor.class, false);
	}

	public void testJavaEditorCloseOneOfTwo() throws Exception {
		ICompilationUnit cu1= createTestCU("Test1");
		IEditorPart editor1= EditorUtility.openInEditor(cu1);
		assertEquals(editor1.getClass(), CompilationUnitEditor.class);
		assertInstanceCount(CompilationUnitEditor.class, 1);

		ICompilationUnit cu2= createTestCU("Test2");
		IEditorPart editor2= EditorUtility.openInEditor(cu2);
		assertEquals(editor2.getClass(), CompilationUnitEditor.class);
		assertInstanceCount(CompilationUnitEditor.class, 2);

		assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor2, false));
		editor2= null;

		assertInstanceCount(CompilationUnitEditor.class, 1);

		assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor1, false));
		editor1= null;

		assertInstanceCount(CompilationUnitEditor.class, 0);
	}

	public void testJavaEditorCloseAll() throws Exception {
		ICompilationUnit cu= createTestCU("Test");
		internalTestEditorClose(cu, CompilationUnitEditor.class, true);
	}

	public void testJavaEditorBreadcrumbCloseAll() throws Exception {
		try {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), true);
			ICompilationUnit cu= createTestCU("Test");
			internalTestEditorClose(cu, CompilationUnitEditor.class, true, true);
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), false);
		}
	}

	public void testJavaEditorBreadcrumbClose() throws Exception {
		try {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), true);
			ICompilationUnit cu= createTestCU("Test");
			internalTestEditorClose(cu, CompilationUnitEditor.class, false, true);
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), false);
		}
	}

	public void testJavaEditorBreadcrumbCloseOneOfTwo1() throws Exception {
		try {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), true);
			ICompilationUnit cu1= createTestCU("Test1");
			IEditorPart editor1= EditorUtility.openInEditor(cu1);
			assertEquals(editor1.getClass(), CompilationUnitEditor.class);
			activateBreadcrumb((JavaEditor) editor1);
			assertInstanceCount(CompilationUnitEditor.class, 1);

			ICompilationUnit cu2= createTestCU("Test2");
			IEditorPart editor2= EditorUtility.openInEditor(cu2);
			assertEquals(editor2.getClass(), CompilationUnitEditor.class);
			activateBreadcrumb((JavaEditor) editor2);
			assertInstanceCount(CompilationUnitEditor.class, 2);

			assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor2, false));
			editor2= null;

			assertInstanceCount(CompilationUnitEditor.class, 1);

			assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor1, false));
			editor1= null;

			assertInstanceCount(CompilationUnitEditor.class, 0);
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), false);
		}
	}

	public void testJavaEditorBreadcrumbCloseOneOfTwo2() throws Exception {
		try {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), true);
			ICompilationUnit cu1= createTestCU("Test1");
			IEditorPart editor1= EditorUtility.openInEditor(cu1);
			assertEquals(editor1.getClass(), CompilationUnitEditor.class);
			assertInstanceCount(CompilationUnitEditor.class, 1);

			ICompilationUnit cu2= createTestCU("Test2");
			IEditorPart editor2= EditorUtility.openInEditor(cu2);
			assertEquals(editor2.getClass(), CompilationUnitEditor.class);
			activateBreadcrumb((JavaEditor) editor2);
			assertInstanceCount(CompilationUnitEditor.class, 2);

			assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor2, false));
			editor2= null;

			assertInstanceCount(CompilationUnitEditor.class, 1);

			assertTrue("Could not close editor", JavaPlugin.getActivePage().closeEditor(editor1, false));
			editor1= null;

			assertInstanceCount(CompilationUnitEditor.class, 0);
		} finally {
			JavaPlugin.getDefault().getPreferenceStore().setValue(getBreadcrumbPreferenceKey(), false);
		}

	}

	protected void setUp() throws Exception {
		super.setUp();
		fJProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("RT not found", JavaProjectHelper.addRTJar(fJProject) != null);
		assertTrue(JavaPlugin.getActivePage().closeAllEditors(false));
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		JavaProjectHelper.delete(fJProject);
	}

	private ICompilationUnit createTestCU(String typeName) throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack0;\n");
		buf.append("public class " + typeName + " {\n}\n");
		return pack2.createCompilationUnit(typeName + ".java", buf.toString(), false, null);
	}

	private IFile createTestFile(String fileName) throws Exception {
		IProject project= fJProject.getProject();
		IFile file= project.getFile(fileName);
		file.create(new ByteArrayInputStream("test\n".getBytes(project.getDefaultCharset())), false, null);
		assertTrue(file.exists());
		return file;
	}

	private void internalTestEditorClose(Object objectToOpen, final Class clazz, boolean closeAll) throws Exception {
		internalTestEditorClose(objectToOpen, clazz, closeAll, false);
	}

	private void internalTestEditorClose(Object objectToOpen, final Class clazz, boolean closeAll, boolean activateBreadcrumb) throws Exception {
		IEditorPart part= internalTestEditorOpen(objectToOpen, clazz);

		if (activateBreadcrumb && part instanceof JavaEditor) {
			activateBreadcrumb((JavaEditor) part);
		}

		ListenerList listenerList= getPreferenceStoreListeners(part);

		// Can't close and assert abandonment in a separate method, since that would leave 'part' as a stack-local reference.
		boolean res;
		if (closeAll)
			res= JavaPlugin.getActivePage().closeAllEditors(false);
		else
			res= JavaPlugin.getActivePage().closeEditor(part, false);
		part= null;
		assertTrue("Could not close editor", res);


		// Check for listener leaks in the editor's preference store.
		assertEmptyListenerList(listenerList);

		// Check for listener leaks in Editors UI preference store.
		Accessor storeAccessor= new Accessor(EditorsUI.getPreferenceStore(), EventManager.class);
		listenerList= (ListenerList)storeAccessor.get("listenerList");
		assertEmptyListenerList(listenerList);

		// Verify that the editor instance is gone.
		assertInstanceCount(clazz, 0);
	}

	private static ListenerList getPreferenceStoreListeners(IEditorPart part) {
		if (part instanceof AbstractTextEditor) {
			Accessor editorAccessor= new Accessor(part, AbstractTextEditor.class);
			Object store= editorAccessor.get("fPreferenceStore");
			if (store instanceof ChainedPreferenceStore) {
				Accessor storeAccessor= new Accessor(store, ChainedPreferenceStore.class);
				return (ListenerList)storeAccessor.get("fClientListeners");
			} else if (store instanceof ScopedPreferenceStore) {
				Accessor storeAccessor= new Accessor(store, EventManager.class);
				return (ListenerList)storeAccessor.get("listenerList");
			}
		}
		return null;
	}

	private static void assertEmptyListenerList(ListenerList listenerList) {
		if (listenerList == null)
			return;

		String message= null;
		Object[] listeners= listenerList.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			if (listeners[i] instanceof SpellCheckEngine)
				continue; // The SpellCheckEngine instance adds one listener when the first editor is created.

			message= "\n" + listeners[i];
		}
		if (message != null)
			fail("Property listeners leaked:" + message);
	}

	private void activateBreadcrumb(JavaEditor editor) {
		editor.getBreadcrumb().activate();
		DisplayHelper.sleep(editor.getSite().getShell().getDisplay(), 10 * 1000);
	}

	private IEditorPart internalTestEditorOpen(Object objectToOpen, final Class clazz) throws PartInitException {
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

		IWorkbenchWindow workbenchWindow= part.getSite().getWorkbenchWindow();
		Shell shell= workbenchWindow.getShell();
		shell.forceActive();
		// run display loop, such that GTK can send shell activate events, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=286244
		DisplayHelper.sleep(shell.getDisplay(), 5000);
		
		IWorkbench workbench= workbenchWindow.getWorkbench();
		part.getEditorSite().getPage().activate(part);
		IHandlerService handlerService= (IHandlerService) workbench.getService(IHandlerService.class);
		handlerService.executeCommand("org.eclipse.jdt.ui.tests.JavaLeakTestActionDelegate", null);

		boolean res= JavaPlugin.getActivePage().closeEditor(part, false);
		part= null;
		assertTrue("Could not close editor", res);

		// verify that the editor instance is gone
		assertInstanceCount(CompilationUnitEditor.class, 0);
	}

	private String getBreadcrumbPreferenceKey() {
		IPerspectiveDescriptor perspective= JavaPlugin.getActivePage().getPerspective();
		if (perspective == null)
			return null;
		return JavaEditor.EDITOR_SHOW_BREADCRUMB + "." + perspective.getId(); //$NON-NLS-1$
	}

}
