/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameEnumConstProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameNonPrivateFieldTests extends RefactoringTest{
	
	private static final Class clazz= RenameNonPrivateFieldTests.class;
	private static final String REFACTORING_PATH= "RenameNonPrivateField/";

	private Object fPrefixPref;
	
	//Test methods can configure these fields:
	private boolean fUpdateReferences= true;
	private boolean fUpdateTextualMatches= false;
	private boolean fRenameGetter= false;
	private boolean fRenameSetter= false;
	
	
	public RenameNonPrivateFieldTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}	
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	protected void setUp() throws Exception {
		super.setUp();
		Hashtable options= JavaCore.getOptions();
		fPrefixPref= options.get(JavaCore.CODEASSIST_FIELD_PREFIXES);
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, getPrefixes());
		JavaCore.setOptions(options);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, fPrefixPref);
		JavaCore.setOptions(options);	
	}
	
	private String getPrefixes(){
		return "";
	}
	
	private void helper1_0(String fieldName, String newFieldName) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), "A"), "A");
		RenameFieldProcessor processor= new RenameFieldProcessor(classA.getField(fieldName));
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newFieldName);
		RefactoringStatus result= performRefactoring(refactoring);
		assertNotNull("precondition was supposed to fail", result);
	}
	
	private void helper1() throws Exception{
		helper1_0("f", "g");
	}
	
	private void helper2(String fieldName, String newFieldName) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		IField field= classA.getField(fieldName);
		String[] handles= ParticipantTesting.createHandles(field);
		RenameFieldProcessor processor= field.getDeclaringType().isEnum() ? new RenameEnumConstProcessor(field) : new RenameFieldProcessor(field);
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newFieldName);
		
		processor.setUpdateReferences(fUpdateReferences);
		processor.setUpdateTextualMatches(fUpdateTextualMatches);
		processor.setRenameGetter(fRenameGetter);
		processor.setRenameSetter(fRenameSetter);
		
		RefactoringStatus result= performRefactoring(refactoring);
		assertEquals("was supposed to pass", null, result);
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
		
		ParticipantTesting.testRename(
			handles,
			new RenameArguments[] {
				new RenameArguments(newFieldName, fUpdateReferences)});
		
		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());
		
		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
		
		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}
	
	private void helper2() throws Exception{
		helper2("f", "g");
	}

	//--------- tests ----------	
	public void testFail0() throws Exception{
		helper1();
	}
	
	public void testFail1() throws Exception{
		helper1();
	}
	
	public void testFail2() throws Exception{
		helper1();
	}
	
	public void testFail3() throws Exception{
		helper1();
	}
	
	public void testFail4() throws Exception{
		helper1();
	}
	
	public void testFail5() throws Exception{
		helper1();
	}	
	
	public void testFail6() throws Exception{
		helper1();
	}
	
	public void testFail7() throws Exception{
		helper1();
	}
	
	public void testFail8() throws Exception{
		helper1();
	}
	
	public void testFail9() throws Exception{
		helper1();
	}
	
	public void testFail10() throws Exception{
		helper1();
	}
	
	public void testFail11() throws Exception{
		helper1();
	}
	
	public void testFail12() throws Exception{
		helper1();
	}	
	
	public void testFail13() throws Exception{
		//printTestDisabledMessage("1GKZ8J6: ITPJCORE:WIN2000 - search: missing field occurrecnces");
		helper1();
	}
	
	public void testFail14() throws Exception{
		//printTestDisabledMessage("1GKZ8J6: ITPJCORE:WIN2000 - search: missing field occurrecnces");
		helper1();
	}
	
	// ------ 
	public void test0() throws Exception{
		helper2();
	}
	
	public void test1() throws Exception{
		helper2();
	}
	
	public void test2() throws Exception{
		helper2();
	}
	
	public void test3() throws Exception{
		helper2();
	}
	
	public void test4() throws Exception{
		helper2();
		//printTestDisabledMessage("1GKZ8J6: ITPJCORE:WIN2000 - search: missing field occurrecnces");
	}

	public void test5() throws Exception{
		helper2();
	}
	
	public void test6() throws Exception{
		//printTestDisabledMessage("1GKB9YH: ITPJCORE:WIN2000 - search for field refs - incorrect results");
		helper2();
	}

	public void test7() throws Exception{
		helper2();
	}
	
	public void test8() throws Exception{
		//printTestDisabledMessage("1GD79XM: ITPJCORE:WINNT - Search - search for field references - not all found");
		helper2();
	}
	
	public void test9() throws Exception{
		helper2();
	}
	
	public void test10() throws Exception{
		helper2();
	}
	
	public void test11() throws Exception{
		helper2();
	}
	
	public void test12() throws Exception{
		//System.out.println("\nRenameNonPrivateField::" + name() + " disabled (1GIHUQP: ITPJCORE:WINNT - search for static field should be more accurate)");
		helper2();
	}
	
	public void test13() throws Exception{
		//System.out.println("\nRenameNonPrivateField::" + name() + " disabled (1GIHUQP: ITPJCORE:WINNT - search for static field should be more accurate)");
		helper2();
	}
	
	public void test14() throws Exception{
		fUpdateReferences= false;
		fUpdateTextualMatches= false;
		helper2();
	}
	
	public void test15() throws Exception{
		fUpdateReferences= false;
		fUpdateTextualMatches= false;
		helper2();
	}

	public void test16() throws Exception{
//		printTestDisabledMessage("text for bug 20693");
		helper2();
	}
	
	public void test17() throws Exception{
//		printTestDisabledMessage("test for bug 66250, 79131 (corner case: reference "A.f" to p.A#f)");
		fUpdateReferences= false;
		fUpdateTextualMatches= true;
		helper2("f", "g");
	}
	
	public void test18() throws Exception{
//		printTestDisabledMessage("test for 79131 (corner case: reference "A.f" to p.A#f)");
		fUpdateReferences= false;
		fUpdateTextualMatches= true;
		helper2("field", "member");
	}
	
//--- test 1.5 features: ---
	public void test19() throws Exception{
		printTestDisabledMessage("generics not supported yet");
		if (true)
			return;
		fRenameGetter= true;
		fRenameSetter= true;
		helper2("fList", "fItems");
	}
	
//--- end test 1.5 features. ---
	
	public void testBug5821() throws Exception{
		helper2("test", "test1");
	}
	
	public void testStaticImport() throws Exception{
		//bug 77622 
		IPackageFragment test1= getRoot().createPackageFragment("test1", true, null);
		ICompilationUnit cuC= null;
		try {
			ICompilationUnit cuB= createCUfromTestFile(test1, "B");
			cuC= createCUfromTestFile(getRoot().getPackageFragment(""), "C");
			
			helper2("PI", "e");
			
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cuB.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("C")), cuC.getSource());
		} finally {
			if (test1.exists())
				test1.delete(true, null);
			if (cuC != null && cuC.exists())
				cuC.delete(true, null);
		}
	}
	
	public void testEnumConst() throws Exception {
		//bug 77619
		IPackageFragment test1= getRoot().createPackageFragment("test1", true, null);
		ICompilationUnit cuC= null;
		try {
			ICompilationUnit cuB= createCUfromTestFile(test1, "B");
			cuC= createCUfromTestFile(getRoot().getPackageFragment(""), "C");
			
			helper2("RED", "REDDISH");
			
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("B")), cuB.getSource());
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("C")), cuC.getSource());
		} finally {
			if (test1.exists())
				test1.delete(true, null);
			if (cuC != null && cuC.exists())
				cuC.delete(true, null);
		}
		
	}
}
