/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.TypedSource;

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockClipboard;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.PasteAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.TypedSourceTransfer;


public class PasteActionTest extends RefactoringTest{

	private Clipboard fClipboard;
	private static final Class clazz= PasteActionTest.class;
	private static final String REFACTORING_PATH= "Paste/";

	public PasteActionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fClipboard= new MockClipboard(Display.getDefault());
	}
	protected void tearDown() throws Exception {
		super.tearDown();
		fClipboard.dispose();
	}

	private static Object[] merge(Object[] array1, Object[] array2) {
		Set elements= new HashSet(array1.length + array2.length);
		elements.addAll(Arrays.asList(array1));
		elements.addAll(Arrays.asList(array2));
		return elements.toArray();
	}

	private PasteAction verifyEnabled(IResource[] copySelectedResources, IJavaElement[] copySelectedJavaElements, IResource[] pasteSelectedResources, IJavaElement[] pasteSelectedJavaElements) throws JavaModelException {
		PasteAction pasteAction= new PasteAction(new MockWorkbenchSite(merge(pasteSelectedResources, pasteSelectedJavaElements)), fClipboard);
		CopyToClipboardAction copyToClipboardAction= new CopyToClipboardAction(new MockWorkbenchSite(merge(copySelectedResources, copySelectedJavaElements)), fClipboard);
		copyToClipboardAction.setAutoRepeatOnFailure(true);
		copyToClipboardAction.update(copyToClipboardAction.getSelection());
		assertTrue("copy not enabled", copyToClipboardAction.isEnabled());
		copyToClipboardAction.run();

		pasteAction.update(pasteAction.getSelection());
		assertTrue("paste should be enabled", pasteAction.isEnabled());
		return pasteAction;
	}

	private PasteAction verifyEnabled(IResource[] copySelectedResources, IJavaElement[] copySelectedJavaElements, IWorkingSet pasteSelectedWorkingSet) throws JavaModelException {
		PasteAction pasteAction= new PasteAction(new MockWorkbenchSite(new Object[] {pasteSelectedWorkingSet}), fClipboard);
		CopyToClipboardAction copyToClipboardAction= new CopyToClipboardAction(new MockWorkbenchSite(merge(copySelectedResources, copySelectedJavaElements)), fClipboard);
		copyToClipboardAction.setAutoRepeatOnFailure(true);
		copyToClipboardAction.update(copyToClipboardAction.getSelection());
		assertTrue("copy not enabled", copyToClipboardAction.isEnabled());
		copyToClipboardAction.run();

		pasteAction.update(pasteAction.getSelection());
		assertTrue("paste should be enabled", pasteAction.isEnabled());
		return pasteAction;
	}

	public void testEnabled_javaProject() throws Exception {
		IJavaElement[] javaElements= {RefactoringTestSetup.getProject()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements, new IResource[0], new IJavaElement[0]);
	}

	public void testEnabled_project() throws Exception {
		IJavaElement[] javaElements= {};
		IResource[] resources= {RefactoringTestSetup.getProject().getProject()};
		verifyEnabled(resources, javaElements, new IResource[0], new IJavaElement[0]);
	}

	public void testEnabled_workingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		try {
			verifyEnabled(new IResource[0], new IJavaElement[] {RefactoringTestSetup.getProject()}, ws);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	private void compareContents(String cuName) throws JavaModelException, IOException {
		assertEqualLines(cuName, getFileContents(getOutputTestFileName(cuName)), getPackageP().getCompilationUnit(cuName + ".java").getSource());
	}

	private void delete(ICompilationUnit cu) throws Exception {
		try {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		} catch (JavaModelException e) {
			e.printStackTrace();
			//ingore and keep going
		}
	}

	public void test0() throws Exception{
		if (true) {
			printTestDisabledMessage("not implemented yet");
			return;
		}

		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try {
			IType typeA= cuA.getType("A");
			IType typeB= cuB.getType("B");

			assertTrue("A does not exist", typeA.exists());
			assertTrue("B does not exist", typeB.exists());

			IJavaElement[] copyJavaElements= {typeA};
			IResource[] copyResources= {};
			IJavaElement[] pasteJavaElements= {typeB};
			IResource[] pasteResources= {};
			PasteAction paste= verifyEnabled(copyResources, copyJavaElements, pasteResources, pasteJavaElements);
			paste.run((IStructuredSelection)paste.getSelection());
			compareContents("A");
			compareContents("B");
		} finally{
			delete(cuA);
			delete(cuB);
		}
	}

	public void test2() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try {
			IField fieldY= cuA.getType("A").getField("y");
			IType typeB= cuB.getType("B");

			assertTrue("y does not exist", fieldY.exists());
			assertTrue("B does not exist", typeB.exists());

			IJavaElement[] copyJavaElements= {fieldY};
			IResource[] copyResources= {};
			IJavaElement[] pasteJavaElements= {typeB};
			IResource[] pasteResources= {};
			PasteAction paste= verifyEnabled(copyResources, copyJavaElements, pasteResources, pasteJavaElements);
			paste.run((IStructuredSelection)paste.getSelection());
			compareContents("A");
			compareContents("B");
		} finally{
			delete(cuA);
			delete(cuB);
		}
	}

	public void test3() throws Exception{
//		printTestDisabledMessage("test for bug#19007");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try {
			IJavaElement elem0= cuA.getImport("java.lang.*");
			IImportContainer importContainer= cuB.getImportContainer();

			assertTrue("y does not exist", elem0.exists());
			assertTrue("B does not exist", importContainer.exists());

			IJavaElement[] copyJavaElements= {elem0};
			IResource[] copyResources= {};
			IJavaElement[] pasteJavaElements= {importContainer};
			IResource[] pasteResources= {};
			PasteAction paste= verifyEnabled(copyResources, copyJavaElements, pasteResources, pasteJavaElements);
			paste.run((IStructuredSelection)paste.getSelection());
			compareContents("A");
			compareContents("B");
		} finally{
			delete(cuA);
			delete(cuB);
		}
	}

	public void test4() throws Exception{
//		printTestDisabledMessage("test for bug 20151");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		try {
			IJavaElement elem0= cuA.getType("A").getMethod("f", new String[0]);
			IMethod method= cuA.getType("A").getMethod("f1", new String[0]);

			assertTrue("y does not exist", elem0.exists());
			assertTrue("B does not exist", method.exists());

			IJavaElement[] copyJavaElements= {elem0};
			IResource[] copyResources= {};
			IJavaElement[] pasteJavaElements= {method};
			IResource[] pasteResources= {};
			PasteAction paste= verifyEnabled(copyResources, copyJavaElements, pasteResources, pasteJavaElements);
			paste.run((IStructuredSelection)paste.getSelection());
			compareContents("A");
		} finally{
			delete(cuA);
		}
	}

	public void testPastingJavaElementIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		try {
			IResource[] resources= {};
			IJavaElement[] jElements= {RefactoringTestSetup.getProject()};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(RefactoringTestSetup.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	public void testPastingResourceIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		IFolder folder= RefactoringTestSetup.getProject().getProject().getFolder("folder");
		folder.create(true, true, null);
		try {
			IResource[] resources= {folder};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(folder, ws.getElements()[0]);
		} finally {
			performDummySearch();
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
			folder.delete(true, false, null);
		}
	}

	public void testPastingJavaElementAsResourceIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		try {
			IResource[] resources= {RefactoringTestSetup.getProject().getProject()};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(RefactoringTestSetup.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	public void testPastingExistingElementIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
			new IAdaptable[] {RefactoringTestSetup.getProject()});
		try {
			IResource[] resources= {};
			IJavaElement[] jElements= {RefactoringTestSetup.getProject()};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(RefactoringTestSetup.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	public void testPastingChildJavaElementIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
			new IAdaptable[] {RefactoringTestSetup.getProject()});
		try {
			IResource[] resources= {};
			IJavaElement[] jElements= {getPackageP()};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(RefactoringTestSetup.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	public void testPastingChildResourceIntoWorkingSet() throws Exception {
		IFolder folder= RefactoringTestSetup.getProject().getProject().getFolder("folder");
		folder.create(true, true, null);
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
			new IAdaptable[] {folder});
		IFolder sub= folder.getFolder("sub");
		sub.create(true, true, null);
		try {
			IResource[] resources= {sub};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(folder, ws.getElements()[0]);
		} finally {
			performDummySearch();
			folder.delete(true, false, null);
			sub.delete(true, false, null);
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	public void testPastingChildResourceIntoWorkingSetContainingParent() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
			new IAdaptable[] {RefactoringTestSetup.getProject()});
		IFolder folder= RefactoringTestSetup.getProject().getProject().getFolder("folder");
		folder.create(true, true, null);
		try {
			IResource[] resources= {folder};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(RefactoringTestSetup.getProject(), ws.getElements()[0]);
		} finally {
			performDummySearch();
			folder.delete(true, false, null);
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	private void setClipboardContents(TypedSource[] typedSources, int repeat) {
		final int maxRepeat= 10;
		try {
			fClipboard.setContents(new Object[] {typedSources}, new Transfer[] {TypedSourceTransfer.getInstance()});
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD || repeat >= maxRepeat)
				throw e;
			setClipboardContents(typedSources, repeat+1);
		}
	}

	private void copyAndPasteTypedSources(IJavaElement[] elemsForClipboard, IJavaElement[] pasteSelectedJavaElements, boolean pasteEnabled) throws CoreException {
		setClipboardContents(TypedSource.createTypedSources(elemsForClipboard), 0);
		PasteAction pasteAction= new PasteAction(new MockWorkbenchSite(pasteSelectedJavaElements), fClipboard);
		pasteAction.update(pasteAction.getSelection());
		assertEquals("action enablement", pasteEnabled, pasteAction.isEnabled());
		if (pasteEnabled)
			pasteAction.run((IStructuredSelection)pasteAction.getSelection());
	}

	public void testPastingTypedResources0() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		try {
			IJavaElement methodM= cuA.getType("A").getMethod("m", new String[0]);
			IJavaElement[] elemsForClipboard= {methodM};
			IJavaElement[] pasteSelectedJavaElements= {methodM};
			boolean enabled= true;
			copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
			compareContents("A");
		} finally{
			delete(cuA);
		}
	}

	public void testPastingTypedResources1() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		try {
			IType typeA= cuA.getType("A");
			IJavaElement fieldF= typeA.getField("f");
			IJavaElement[] elemsForClipboard= {fieldF};
			IJavaElement[] pasteSelectedJavaElements= {typeA};
			boolean enabled= true;
			copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
			compareContents("A");
		} finally{
			delete(cuA);
		}
	}

	public void testPastingTypedResources2() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		try {
			IType typeA= cuA.getType("A");
			IJavaElement fieldF= typeA.getField("f");
			IJavaElement[] elemsForClipboard= {fieldF};
			IJavaElement[] pasteSelectedJavaElements= {typeA};
			boolean enabled= true;
			copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
			compareContents("A");
		} finally{
			delete(cuA);
		}
	}

	public void testPastingTypedResources3() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		try {
			IType typeA= cuA.getType("A");
			IJavaElement fieldF= typeA.getField("f");
			IJavaElement fieldG= typeA.getField("g");
			IJavaElement[] elemsForClipboard= {fieldF, fieldG};
			IJavaElement[] pasteSelectedJavaElements= {typeA};
			boolean enabled= true;
			copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
			compareContents("A");
		} finally{
			delete(cuA);
		}
	}
}
