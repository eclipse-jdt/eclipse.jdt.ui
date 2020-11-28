/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class RenameTests18 extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "RenameTests18/";

	public RenameTests18() {
		super(new Java1d8Setup() {
			@Override
			public void before() throws Exception {
				JavaProjectHelper.PERFORM_DUMMY_SEARCH++;
				super.before();
			}
			@Override
			public void after() {
				super.after();
				JavaProjectHelper.PERFORM_DUMMY_SEARCH--;
			}
		});
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		Hashtable<String, String> options= JavaCore.getOptions();
		JavaCore.setOptions(options);
		fIsPreDeltaTest= true;
	}

	@Override
	public void genericafter() throws Exception {
		super.genericafter();
		Hashtable<String, String> options= JavaCore.getOptions();
		JavaCore.setOptions(options);
	}

	private ISourceRange getSelection(ICompilationUnit cu) throws Exception {
		String source= cu.getSource();
		//Warning: this *includes* the SQUARE_BRACKET_OPEN!
		int offset= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset + AbstractJunit4SelectionTestCase.SQUARE_BRACKET_OPEN.length(), end - offset);
	}

	private void renameLocalVariable(String newFieldName, boolean updateReferences) throws Exception {
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");

		ISourceRange selection= getSelection(cu);
		IJavaElement[] elements= cu.codeSelect(selection.getOffset(), selection.getLength());
		assertEquals(1, elements.length);
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		descriptor.setJavaElement(elements[0]);
		descriptor.setNewName(newFieldName);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setUpdateTextualOccurrences(false);

		RenameRefactoring refactoring= (RenameRefactoring) createRefactoring(descriptor);
		List<IJavaElement> list= new ArrayList<>();
		list.add(elements[0]);
		List<RenameArguments> args= new ArrayList<>();
		args.add(new RenameArguments(newFieldName, updateReferences));
		String[] renameHandles= ParticipantTesting.createHandles(list.toArray());

		try {
//			org.eclipse.jdt.internal.core.JavaModelManager.VERBOSE= true;
			RefactoringStatus result= performRefactoring(refactoring);
			assertNull("was supposed to pass", result);
		} catch (CoreException e) {
			System.out.println("RenameTest18." + getName() + ": " + e.toString());
			System.out.println(cu.getResource().getLocationURI());
			System.out.println(cu.getResource().getModificationStamp());
			System.out.println(cu.getResource().getLocalTimeStamp());
			System.out.println(cu.getResource().isSynchronized(0));
			ITextFileBuffer fileBuffer= ITextFileBufferManager.DEFAULT.getTextFileBuffer(cu.getResource().getFullPath(), LocationKind.IFILE);
			if (fileBuffer != null) {
				System.out.println(fileBuffer.getModificationStamp());
				System.out.println("isCommittable:" + fileBuffer.isCommitable()
						+ "\nisDirty:" + fileBuffer.isDirty()
						+ "\nisShared:" + fileBuffer.isShared()
						+ "\nisStateValidated:" + fileBuffer.isStateValidated()
						+ "\nisSynchronizationContextRequested:" + fileBuffer.isSynchronizationContextRequested()
						+ "\nisSynchronized:" + fileBuffer.isSynchronized()
						);
				System.out.println(fileBuffer.getStatus());
				System.out.println("--- fileBuffer.getDocument().get():");
				System.out.println(fileBuffer.getDocument().get());
			}
			System.out.println("--- getContents of File:");
			System.out.println(getContents(new FileInputStream(cu.getResource().getLocation().toFile())));
			System.out.println("--- cu.getSource():");
			System.out.println(cu.getSource());
			System.out.println("---");
			System.out.println("cu.getOwner(): " + cu.getOwner());
			System.out.println("cu.isWorkingCopy(): " + cu.isWorkingCopy());
			System.out.println("cu.isConsistent(): " + cu.isConsistent());
			System.out.println("cu.getBuffer().getClass(): " + cu.getBuffer().getClass());
			System.out.println("cu.getBuffer().hasUnsavedChanges(): " + cu.getBuffer().hasUnsavedChanges());
			System.out.println(cu.getBuffer());
			/*
			 * Problems:
			 * fileBuffer still has contents from previous test, and
			 * textEdits from current test are applied on that buffer.
			 * But the cu.getBuffer() has this test's content!
			 * cu.getBuffer() is a jdt.internal.core.Buffer and not a DocumentAdapter.
			 * The cu is NOT in working copy mode.
			 * Somehow, the BecomeWorkingCopyOperation failed.
			 *
			 * Observation: The A.java files contain secondary types. Maybe a threading problem
			 * with the indexer?
			 *
			 * A failing test execution order of RenameTests18 with jdk7:
			 * - green: testMethodReference0
			 * - fails: testLambda0
			 */
			throw e;
		} finally {
//			org.eclipse.jdt.internal.core.JavaModelManager.VERBOSE= false;
		}
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

		ParticipantTesting.testRename(
			renameHandles,
			args.toArray(new RenameArguments[args.size()]));

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	@Test
	public void testLambda0() throws Exception {
		renameLocalVariable("renamedF", true);
	}

	@Test
	public void testLambda1() throws Exception {
		renameLocalVariable("renamedP", true);
	}

	@Test
	public void testLambda2() throws Exception {
		renameLocalVariable("renamedIi", true);
	}

	@Test
	public void testLambda3() throws Exception {
		renameLocalVariable("x_renamed", true);
	}


	private void renameMethod(String methodName, String newMethodName, String[] signatures, boolean shouldPass, boolean updateReferences, boolean createDelegate) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType typeI= getType(cu, "I");
		IMethod method= typeI.getMethod(methodName, signatures);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setJavaElement(method);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setNewName(newMethodName);
		descriptor.setKeepOriginal(createDelegate);
		descriptor.setDeprecateDelegate(true);

		try {
			assertNull("was supposed to pass", performRefactoring(descriptor));
		} catch (CoreException e) {
			System.out.println("RenameTest18." + getName() + ": " + e.toString());
			System.out.println(cu.getResource().getLocationURI());
			System.out.println(cu.getResource().getModificationStamp());
			System.out.println(cu.getResource().getLocalTimeStamp());
			System.out.println(cu.getResource().isSynchronized(0));
			System.out.println("---");
			System.out.println(cu.getSource());
			System.out.println("---");
			throw e;
		}
		if (!shouldPass){
			assertNotEquals("incorrect renaming because of a java model bug", getFileContents(getOutputTestFileName("A")), cu.getSource());
			return;
		}
		assertEqualLines("incorrect renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
		//assertEquals("1 to undo", 1, Refactoring.getUndoManager().getRefactoringLog().size());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
		//assertEquals("1 to redo", 1, Refactoring.getUndoManager().getRedoStack().size());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	private void renameMethodInInterface() throws Exception{
		renameMethod("m", "k", new String[0], true, true, false);
	}

	// method with a lambda method as reference
	@Test
	public void testMethod0() throws Exception{
		renameMethodInInterface();
	}

	// method with method references as reference
	@Test
	public void testMethod1() throws Exception{
		renameMethodInInterface();
	}

	@Test
	public void testMethod2() throws Exception {
		renameMethodInInterface();
	}

	@Test
	public void testMethodReference0() throws Exception {
		renameMethod("searchForRefs", "searchForRefs1", new String[0], true, true, false);
	}
}
