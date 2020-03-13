/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameRecordElementsTests extends RefactoringTest {

	private static final Class<RenameRecordElementsTests> clazz= RenameRecordElementsTests.class;
	private static final String REFACTORING_PATH= "RenameRecordElements/";

	private String fPrefixPref;
	public RenameRecordElementsTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java14Setup( new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java14Setup(someTest);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Hashtable<String, String> options= JavaCore.getOptions();
		fPrefixPref= options.get(JavaCore.CODEASSIST_FIELD_PREFIXES);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, getPrefixes());
		JavaCore.setOptions(options);
		fIsPreDeltaTest= true;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, fPrefixPref);
		JavaCore.setOptions(options);
	}

	private String getPrefixes(){
		return "f";
	}

	private void renameRecordCompactConstructor(String fieldName, String newFieldName, boolean updateReferences, boolean fail) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cu2= createCUfromTestFile(getPackageP(), "B");
		IType recordA= getType(cu, "A");
		IField field= recordA.getField(fieldName);
		IMethod method= recordA.getMethod(fieldName, new String[] {});
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_FIELD);
		descriptor.setJavaElement(field);
		descriptor.setNewName(newFieldName);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setUpdateTextualOccurrences(false);
		descriptor.setRenameGetters(false);
		descriptor.setRenameSetters(false);

		RenameRefactoring refactoring= (RenameRefactoring) createRefactoring(descriptor);
		RenameFieldProcessor processor= (RenameFieldProcessor) refactoring.getProcessor();
		assertEquals("getter rename enabled", false, processor.canEnableGetterRenaming() == null);
		assertEquals("setter rename enabled", false, processor.canEnableSetterRenaming() == null);

		List<IAnnotatable> elements= new ArrayList<>();
		elements.add(field);
		List<RenameArguments> args= new ArrayList<>();
		args.add(new RenameArguments(newFieldName, updateReferences));
		if (method != null && method.exists()) {
			elements.add(method);
			args.add(new RenameArguments(newFieldName, updateReferences));
		}


		String[] renameHandles= ParticipantTesting.createHandles(elements.toArray());

		RefactoringStatus result= performRefactoring(refactoring);
		if (fail) {
			assertNotNull("was supposed to fail", result);
		} else {
			assertEquals("was supposed to pass", null, result);
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());

			ParticipantTesting.testRename(
				renameHandles,
				args.toArray(new RenameArguments[args.size()]));

			assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());

			assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("B")), cu2.getSource());
		}
	}

	private void renameRecordExplicitAccessor(String methodName, String newMethodName, boolean updateReferences, boolean fail) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cu2= createCUfromTestFile(getPackageP(), "B");
		IType recordA= getType(cu, "A");
		IMethod method= recordA.getMethod(methodName,new String[] {});
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setJavaElement(method);
		descriptor.setNewName(newMethodName);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setKeepOriginal(false);
		descriptor.setDeprecateDelegate(false);

		RenameRefactoring refactoring= (RenameRefactoring) createRefactoring(descriptor);

		List<IAnnotatable> elements= new ArrayList<>();
		elements.add(method);
		List<RenameArguments> args= new ArrayList<>();
		args.add(new RenameArguments(newMethodName, updateReferences));
		IField field= recordA.getField(methodName);
		if (field != null && field.exists()) {
			elements.add(field);
			args.add(new RenameArguments(newMethodName, updateReferences));
		}



		String[] renameHandles= ParticipantTesting.createHandles(elements.toArray());

		RefactoringStatus result= performRefactoring(refactoring);
		if (fail) {
			assertNotNull("was supposed to fail", result);
		} else {
			assertEquals("was supposed to pass", null, result);
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());

			ParticipantTesting.testRename(
				renameHandles,
				args.toArray(new RenameArguments[args.size()]));

			assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());

			assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("B")), cu2.getSource());
		}
	}

	private void renameRecord(String recordName, String newRecordName, boolean updateReferences, boolean fail) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), recordName);
		ICompilationUnit cu2= createCUfromTestFile(getPackageP(), "B");
		IType recordA= getType(cu, "A");
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(recordA);
		descriptor.setNewName(newRecordName);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setKeepOriginal(false);
		descriptor.setDeprecateDelegate(false);

		RenameRefactoring refactoring= (RenameRefactoring) createRefactoring(descriptor);

		RefactoringStatus result= performRefactoring(refactoring);
		assertEquals("was supposed to pass", null, result);
		ICompilationUnit newcu= pack.getCompilationUnit(newRecordName +".java");
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName(newRecordName)), newcu.getSource());
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName(recordName)), cu.getSource());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());

		assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName(newRecordName)), newcu.getSource());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("B")), cu2.getSource());
	}

	//--------- tests ----------

	public void testRenameRecordCompactConstructorImplicitAccessor() throws Exception{
		renameRecordCompactConstructor("f", "g", true, false);
	}

	public void testRenameRecordCompactConstructorExplicitAccessor() throws Exception{
		renameRecordCompactConstructor("f", "g", true, false);
	}

	public void testRenameRecordExplicitAccessor() throws Exception{
		renameRecordExplicitAccessor("f", "g", true, false);
	}

	public void testRenameRecord() throws Exception{
		renameRecord("A", "C", true, false);
	}

	public void testRenameRecordExplicitAccessorFailMethodConflict() throws Exception{
		renameRecordExplicitAccessor("f", "g", true, true);
	}

	public void testRenameRecordExplicitAccessorFailFieldConflict() throws Exception{
		renameRecordExplicitAccessor("f", "g", true, true);
	}

	public void testRenameRecordCompactConstructorFailFieldConflict() throws Exception{
		renameRecordCompactConstructor("f", "g", true, true);
	}

	public void testRenameRecordCompactConstructorFailMethodConflict() throws Exception{
		renameRecordCompactConstructor("f", "g", true, true);
	}

}
