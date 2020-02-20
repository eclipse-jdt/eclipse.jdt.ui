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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameRecordComponentFieldTests extends RefactoringTest {

	private static final Class<RenameRecordComponentFieldTests> clazz= RenameRecordComponentFieldTests.class;
	private static final String REFACTORING_PATH= "RenameRecordComponentField/";

	private String fPrefixPref;
	public RenameRecordComponentFieldTests(String name) {
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

	private void helper1(String fieldName, String newFieldName, boolean updateReferences) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cu2= createCUfromTestFile(getPackageP(), "B");
		IType classA= getType(cu, "A");
		IField field= classA.getField(fieldName);
		IMethod method= classA.getMethod(fieldName, new String[] {});
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

	private void helper1(boolean updateReferences) throws Exception{
		helper1("f", "g", updateReferences);
	}

	private void helper1() throws Exception{
		helper1(true);
	}

	//--------- tests ----------

	public void test0() throws Exception{
		helper1();
	}

	public void test1() throws Exception{
		helper1();
	}

}
