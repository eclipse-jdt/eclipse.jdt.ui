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
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

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

import org.eclipse.jdt.ui.tests.refactoring.rules.Java14Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameRecordElementsTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "RenameRecordElements/";

	private String fPrefixPref;
	@Rule
	public RefactoringTestSetup fts= new Java14Setup();

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	@Override
	public void genericbefore() throws Exception {
		super.genericbefore();
		Hashtable<String, String> options= JavaCore.getOptions();
		fPrefixPref= options.get(JavaCore.CODEASSIST_FIELD_PREFIXES);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, getPrefixes());
		JavaCore.setOptions(options);
		fIsPreDeltaTest= true;
	}

	@Override
	public void genericafter() throws Exception {
		super.genericafter();
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
		assertNotNull("Getter rename should be enabled", processor.canEnableGetterRenaming());
		assertNotNull("Setter rename should be enabled", processor.canEnableSetterRenaming());

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
			assertNull("was supposed to pass", result);
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());

			ParticipantTesting.testRename(
				renameHandles,
				args.toArray(new RenameArguments[args.size()]));

			assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());

			assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("B")), cu2.getSource());
		}
	}

	private void renameRecordCompactConstructor2(String fieldName, String newFieldName, boolean updateReferences, boolean fail) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cu2= createCUfromTestFile(getPackageP(), "B");
		ICompilationUnit cu3= createCUfromTestFile(getPackageP(), "C");
		ICompilationUnit cu4= createCUfromTestFile(getPackageP(), "D");
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
		assertNotNull("getter rename enabled", processor.canEnableGetterRenaming());
		assertNotNull("setter rename enabled", processor.canEnableSetterRenaming());

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
			assertNull("was supposed to pass", result);
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("C")), cu3.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("D")), cu4.getSource());

			ParticipantTesting.testRename(
				renameHandles,
				args.toArray(new RenameArguments[args.size()]));

			assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("C")), cu3.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("D")), cu4.getSource());

			assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("B")), cu2.getSource());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("C")), cu3.getSource());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("D")), cu4.getSource());
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
			assertNull("was supposed to pass", result);
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());

			ParticipantTesting.testRename(
				renameHandles,
				args.toArray(new RenameArguments[args.size()]));

			assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

			RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());

			assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
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
		assertNull("was supposed to pass", result);
		ICompilationUnit newcu= pack.getCompilationUnit(newRecordName +".java");
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName(newRecordName)), newcu.getSource());
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cu2.getSource());

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName(recordName)), cu.getSource());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("B")), cu2.getSource());

		assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName(newRecordName)), newcu.getSource());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("B")), cu2.getSource());
	}

	//--------- tests ----------

	@Test
	public void testRenameRecordCompactConstructorImplicitAccessor() throws Exception{
		renameRecordCompactConstructor("f", "g", true, false);
	}

	@Test
	public void testRenameRecordCompactConstructorImplicitAccessor2() throws Exception{
		renameRecordCompactConstructor2("f", "g", true, false);
	}

	@Test
	public void testRenameRecordCompactConstructorExplicitAccessor() throws Exception{
		renameRecordCompactConstructor("f", "g", true, false);
	}

	@Test
	public void testRenameRecordExplicitAccessor() throws Exception{
		renameRecordExplicitAccessor("f", "g", true, false);
	}

	@Test
	public void testRenameRecord() throws Exception{
		renameRecord("A", "C", true, false);
	}

	@Test
	public void testRenameRecordExplicitAccessorFailMethodConflict() throws Exception{
		renameRecordExplicitAccessor("f", "g", true, true);
	}

	@Test
	public void testRenameRecordExplicitAccessorFailFieldConflict() throws Exception{
		renameRecordExplicitAccessor("f", "g", true, true);
	}

	@Test
	public void testRenameRecordCompactConstructorFailFieldConflict() throws Exception{
		renameRecordCompactConstructor("f", "g", true, true);
	}

	@Test
	public void testRenameRecordCompactConstructorFailMethodConflict() throws Exception{
		renameRecordCompactConstructor("f", "g", true, true);
	}
}
