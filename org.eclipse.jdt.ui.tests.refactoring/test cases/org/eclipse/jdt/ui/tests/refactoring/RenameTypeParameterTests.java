/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeParameterProcessor;

public class RenameTypeParameterTests extends RefactoringTest {

	private static final Class clazz= RenameTypeParameterTests.class;

	private static final String REFACTORING_PATH= "RenameTypeParameter/";

	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	public RenameTypeParameterTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1(String parameterName, String newParameterName, String typeName, boolean references) throws Exception {
		IType declaringType= getType(createCUfromTestFile(getPackageP(), "A"), typeName);
		RenameTypeParameterProcessor processor= new RenameTypeParameterProcessor(declaringType.getTypeParameter(parameterName));
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newParameterName);
		processor.setUpdateReferences(references);
		RefactoringStatus result= performRefactoring(refactoring);
		assertNotNull("precondition was supposed to fail", result);
	}

	private void helper1(String parameterName, String newParameterName, String typeName, String methodName, String[] methodSignature, boolean references) throws Exception {
		IType declaringType= getType(createCUfromTestFile(getPackageP(), "A"), typeName);
		IMethod[] declaringMethods= getMethods(declaringType, new String[] { methodName}, new String[][] { methodSignature});
		RenameTypeParameterProcessor processor= new RenameTypeParameterProcessor(declaringMethods[0].getTypeParameter(parameterName));
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newParameterName);
		processor.setUpdateReferences(references);
		RefactoringStatus result= performRefactoring(refactoring);
		assertNotNull("precondition was supposed to fail", result);
	}

	private void helper2(String parameterName, String newParameterName, String typeName, boolean references) throws Exception {
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType declaringType= getType(cu, typeName);
		ITypeParameter typeParameter= declaringType.getTypeParameter(parameterName);
		RenameTypeParameterProcessor processor= new RenameTypeParameterProcessor(typeParameter);
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newParameterName);
		processor.setUpdateReferences(references);

		RefactoringStatus result= performRefactoring(refactoring);
		assertEquals("was supposed to pass", null, result);
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	private void helper2(String parameterName, String newParameterName, String typeName, String methodName, String[] methodSignature, boolean references) throws Exception {
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType declaringType= getType(cu, typeName);
		IMethod[] declaringMethods= getMethods(declaringType, new String[] { methodName}, new String[][] { methodSignature});
		ITypeParameter typeParameter= declaringMethods[0].getTypeParameter(parameterName);
		RenameTypeParameterProcessor processor= new RenameTypeParameterProcessor(typeParameter);
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newParameterName);
		processor.setUpdateReferences(references);

		RefactoringStatus result= performRefactoring(refactoring);
		assertEquals("was supposed to pass", null, result);
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	public void test0() throws Exception {
		helper2("T", "S", "A", true);
	}

	public void test1() throws Exception {
		helper2("T", "S", "A", true);
	}

	public void test2() throws Exception {
		helper2("T", "S", "A", false);
	}

	public void test3() throws Exception {
		helper2("T", "S", "A", true);
	}

	public void test4() throws Exception {
		helper2("T", "S", "A", false);
	}

	public void test5() throws Exception {
		helper2("T", "S", "A", true);
	}

	public void test6() throws Exception {
		helper2("S", "T", "A", true);
	}

	public void test7() throws Exception {
		helper2("T", "S", "A", false);
	}

	public void test8() throws Exception {
		helper2("S", "T", "A", false);
	}

	public void test9() throws Exception {
		helper2("T", "S", "A", "f", new String[] { "QT;"}, true);
	}

	public void test10() throws Exception {
		helper2("T", "S", "B", "f", new String[] { "QT;"}, true);
	}

	public void test11() throws Exception {
		helper2("T", "S", "A", "f", new String[] { "QT;"}, false);
	}

	public void test12() throws Exception {
		helper2("T", "S", "B", "f", new String[] { "QT;"}, false);
	}

	public void test13() throws Exception {
		helper2("T", "S", "A", true);
	}

	public void test14() throws Exception {
		helper2("ELEMENT", "E", "A", true);
	}

	public void test15() throws Exception {
		helper2("T", "S", "A", true);
	}
	
// ------------------------------------------------

	public void testFail0() throws Exception {
		helper1("T", "S", "A", true);
	}

	public void testFail1() throws Exception {
		helper1("T", "S", "A", true);
	}

	public void testFail2() throws Exception {
		helper1("T", "S", "A", true);
	}

	public void testFail3() throws Exception {
		helper1("T", "S", "A", true);
	}

	public void testFail4() throws Exception {
		helper1("T", "S", "A", true);
	}

	public void testFail5() throws Exception {
		helper1("T", "S", "B", "f", new String[] { "QT;"}, true);
	}
}
