/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameRefactoring;

public class RenameNonPrivateFieldTests extends RefactoringTest{
	
	private static final Class clazz= RenameNonPrivateFieldTests.class;
	private static final String REFACTORING_PATH= "RenameNonPrivateField/";

	private Object fPrefixPref;
	public RenameNonPrivateFieldTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
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
	
	private void helper2(String fieldName, String newFieldName, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		RenameFieldProcessor processor= new RenameFieldProcessor(classA.getField(fieldName));
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newFieldName);
		processor.setUpdateReferences(updateReferences);
		RefactoringStatus result= performRefactoring(refactoring);
		assertEquals("was supposed to pass", null, result);
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
		
		assertTrue("anythingToUndo", Refactoring.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !Refactoring.getUndoManager().anythingToRedo());
		
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertTrue("! anythingToUndo", !Refactoring.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", Refactoring.getUndoManager().anythingToRedo());
		
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}
	
	private void helper2(String fieldName, String newFieldName) throws Exception{
		helper2(fieldName, newFieldName, true);
	}
	
	private void helper2() throws Exception{
		helper2(true);
	}
	
	private void helper2(boolean updateReferences) throws Exception{
		helper2("f", "g", updateReferences);
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
		helper2(false);
	}
	
	public void test15() throws Exception{
		helper2(false);
	}

	public void test16() throws Exception{
//		printTestDisabledMessage("text for bug 20693");
		helper2();
	}
	
	public void testBug5821() throws Exception{
		helper2("test", "test1");
	}
	
}
