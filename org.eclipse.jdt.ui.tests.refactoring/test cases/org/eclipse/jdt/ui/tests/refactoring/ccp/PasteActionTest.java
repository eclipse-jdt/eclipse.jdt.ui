/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

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

import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockClipboard;
import org.eclipse.jdt.ui.tests.refactoring.infra.MockWorkbenchSite;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.PasteAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.TypedSourceTransfer;

public class PasteActionTest extends GenericRefactoringTest {
	private Clipboard fClipboard;
	private static final String REFACTORING_PATH= "Paste/";

	public PasteActionTest() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		fClipboard= new MockClipboard(Display.getDefault());
	}

	@Override
	public void genericafter() throws Exception {
		super.genericafter();
		fClipboard.dispose();
	}

	private static Object[] merge(Object[] array1, Object[] array2) {
		Set<Object> elements= new HashSet<>(array1.length + array2.length);
		elements.addAll(Arrays.asList(array1));
		elements.addAll(Arrays.asList(array2));
		return elements.toArray();
	}

	private PasteAction verifyEnabled(IResource[] copySelectedResources, IJavaElement[] copySelectedJavaElements, IResource[] pasteSelectedResources, IJavaElement[] pasteSelectedJavaElements) {
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

	private PasteAction verifyEnabled(IResource[] copySelectedResources, IJavaElement[] copySelectedJavaElements, IWorkingSet pasteSelectedWorkingSet) {
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

	@Test
	public void testEnabled_javaProject() throws Exception {
		IJavaElement[] javaElements= {rts.getProject()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements, new IResource[0], new IJavaElement[0]);
	}

	@Test
	public void testEnabled_project() throws Exception {
		IJavaElement[] javaElements= {};
		IResource[] resources= {rts.getProject().getProject()};
		verifyEnabled(resources, javaElements, new IResource[0], new IJavaElement[0]);
	}

	@Test
	public void testEnabled_workingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		try {
			verifyEnabled(new IResource[0], new IJavaElement[] {rts.getProject()}, ws);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	private void compareContents(String cuName) throws JavaModelException, IOException {
		assertEqualLines(cuName, getFileContents(getOutputTestFileName(cuName)), getPackageP().getCompilationUnit(cuName + ".java").getSource());
	}

	@Ignore("not implemented yet")
	@Test
	public void test0() throws Exception {

		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

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
	}

	@Test
	public void test2() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

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
	}

	@Test
	public void test3() throws Exception {
//		printTestDisabledMessage("test for bug#19007");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

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
	}

	@Test
	public void test4() throws Exception {
//		printTestDisabledMessage("test for bug 20151");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
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
	}

	@Test
	public void testPastingJavaElementIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		try {
			IResource[] resources= {};
			IJavaElement[] jElements= {rts.getProject()};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(rts.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	@Test
	public void testPastingResourceIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		IFolder folder= rts.getProject().getProject().getFolder("folder");
		folder.create(true, true, null);
		try {
			IResource[] resources= {folder};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(folder, ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	@Test
	public void testPastingJavaElementAsResourceIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test", new IAdaptable[] {});
		try {
			IResource[] resources= {rts.getProject().getProject()};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(rts.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	@Test
	public void testPastingExistingElementIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
				new IAdaptable[] {rts.getProject()});
		try {
			IResource[] resources= {};
			IJavaElement[] jElements= {rts.getProject()};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(rts.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	@Test
	public void testPastingChildJavaElementIntoWorkingSet() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
				new IAdaptable[] {rts.getProject()});
		try {
			IResource[] resources= {};
			IJavaElement[] jElements= {getPackageP()};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(rts.getProject(), ws.getElements()[0]);
		} finally {
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	@Test
	public void testPastingChildResourceIntoWorkingSet() throws Exception {
		IFolder folder= rts.getProject().getProject().getFolder("folder");
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
			PlatformUI.getWorkbench().getWorkingSetManager().removeWorkingSet(ws);
		}
	}

	@Test
	public void testPastingChildResourceIntoWorkingSetContainingParent() throws Exception {
		IWorkingSet ws= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet("Test",
				new IAdaptable[] {rts.getProject()});
		IFolder folder= rts.getProject().getProject().getFolder("folder");
		folder.create(true, true, null);
		try {
			IResource[] resources= {folder};
			IJavaElement[] jElements= {};
			PasteAction paste= verifyEnabled(resources , jElements, ws);
			paste.run((IStructuredSelection)paste.getSelection());
			assertEquals("Only one element", 1, ws.getElements().length);
			assertEquals(rts.getProject(), ws.getElements()[0]);
		} finally {
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

	@Test
	public void testPastingTypedResources0() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		IJavaElement methodM= cuA.getType("A").getMethod("m", new String[0]);
		IJavaElement[] elemsForClipboard= {methodM};
		IJavaElement[] pasteSelectedJavaElements= {methodM};
		boolean enabled= true;
		copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
		compareContents("A");
	}

	@Test
	public void testPastingTypedResources1() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cuA.getType("A");
		IJavaElement fieldF= typeA.getField("f");
		IJavaElement[] elemsForClipboard= {fieldF};
		IJavaElement[] pasteSelectedJavaElements= {typeA};
		boolean enabled= true;
		copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
		compareContents("A");
	}

	@Test
	public void testPastingTypedResources2() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cuA.getType("A");
		IJavaElement fieldF= typeA.getField("f");
		IJavaElement[] elemsForClipboard= {fieldF};
		IJavaElement[] pasteSelectedJavaElements= {typeA};
		boolean enabled= true;
		copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
		compareContents("A");
	}

	@Test
	public void testPastingTypedResources3() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		IType typeA= cuA.getType("A");
		IJavaElement fieldF= typeA.getField("f");
		IJavaElement fieldG= typeA.getField("g");
		IJavaElement[] elemsForClipboard= {fieldF, fieldG};
		IJavaElement[] pasteSelectedJavaElements= {typeA};
		boolean enabled= true;
		copyAndPasteTypedSources(elemsForClipboard, pasteSelectedJavaElements, enabled);
		compareContents("A");
	}
}
