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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenamePrivateFieldTests extends RefactoringTest {

	private static final Class clazz= RenamePrivateFieldTests.class;
	private static final String REFACTORING_PATH= "RenamePrivateField/";

	private Object fPrefixPref;
	public RenamePrivateFieldTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup( new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new MySetup(someTest);
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
		return "f";
	}
	
	private void helper1_0(String fieldName, String newFieldName, String typeName,
							boolean renameGetter, boolean renameSetter) throws Exception{
		IType declaringType= getType(createCUfromTestFile(getPackageP(), "A"), typeName);
		RenameFieldProcessor processor= new RenameFieldProcessor(declaringType.getField(fieldName));
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setNewElementName(newFieldName);
		processor.setRenameGetter(renameGetter);
		processor.setRenameSetter(renameSetter);
		RefactoringStatus result= performRefactoring(refactoring);
		assertNotNull("precondition was supposed to fail", result);
	}
	
	private void helper1_0(String fieldName, String newFieldName) throws Exception{
		helper1_0(fieldName, newFieldName, "A", false, false);
	}

	private void helper1() throws Exception{
		helper1_0("f", "g");
	}
	
	//--
		
	private void helper2(String fieldName, String newFieldName, boolean updateReferences, boolean updateTextualMatches,
											boolean renameGetter, boolean renameSetter,
											boolean expectedGetterRenameEnabled, boolean expectedSetterRenameEnabled) throws Exception{
		ParticipantTesting.reset();
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		IField field= classA.getField(fieldName);
		RenameFieldProcessor processor= new RenameFieldProcessor(field);
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setUpdateReferences(updateReferences);
		processor.setUpdateTextualMatches(updateTextualMatches);
		assertEquals("getter rename enabled", expectedGetterRenameEnabled, processor.canEnableGetterRenaming() == null);
		assertEquals("setter rename enabled", expectedSetterRenameEnabled, processor.canEnableSetterRenaming() == null);
		processor.setRenameGetter(renameGetter);
		processor.setRenameSetter(renameSetter);
		processor.setNewElementName(newFieldName);
		String newGetterName= processor.getNewGetterName();
		String newSetterName= processor.getNewSetterName();

		int numbers= 1;
		List elements= new ArrayList();
		elements.add(field);
		List args= new ArrayList();
		args.add(new RenameArguments(newFieldName, updateReferences));
		if (renameGetter && expectedGetterRenameEnabled) {
			elements.add(processor.getGetter());
			args.add(new RenameArguments(newGetterName, updateReferences));
			numbers++;
		}
		if (renameSetter && expectedSetterRenameEnabled) {
			elements.add(processor.getSetter());
			args.add(new RenameArguments(newSetterName, updateReferences));
			numbers++;
		}
		String[] renameHandles= ParticipantTesting.createHandles(elements.toArray());
		
		RefactoringStatus result= performRefactoring(refactoring);
		assertEquals("was supposed to pass", null, result);
		assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

		ParticipantTesting.testRename(
			renameHandles, 
			(RenameArguments[]) args.toArray(new RenameArguments[args.size()]));
		
		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());
		
		RefactoringCore.getUndoManager().performUndo(new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
		
		RefactoringCore.getUndoManager().performRedo(new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	private void helper2(boolean updateReferences) throws Exception{
		helper2("f", "g", updateReferences, false, false, false, false, false);
	}
	
	private void helper2() throws Exception{
		helper2(true);
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
		helper1_0("gg", "f", "A", false, false);
	}	

	public void testFail9() throws Exception{
		helper1_0("y", "e", "getE", true, true);
	}	

	public void testFail10() throws Exception{
		helper1_0("y", "e", "setE", true, true);
	}	
	
	// ------ 
	public void test0() throws Exception{
		helper2();
	}
	
	public void test1() throws Exception{
		helper2();
	}

	public void test2() throws Exception{
		helper2(false);
	}	
	
	public void test3() throws Exception{
		helper2("f", "gg", true, true, false, false, false, false);
	}	

	public void test4() throws Exception{
		helper2("fMe", "fYou", true, false, true, true, true, true);
	}		
	
	public void test5() throws Exception{
		//regression test for 9895
		helper2("fMe", "fYou", true, false, true, false, true, false);
	}		
	
	public void test6() throws Exception{
		//regression test for 9895 - opposite case
		helper2("fMe", "fYou", true, false, false, true, false, true);
	}		

	public void test7() throws Exception{
		//regression test for 21292 
		helper2("fBig", "fSmall", true, false, true, true, true, true);
	}		
	
	public void test8() throws Exception{
		//regression test for 26769
		helper2("f", "g", true, false, true, false, true, false);
	}

	public void test9() throws Exception{
		//regression test for 30906
		helper2("fBig", "fSmall", true, false, true, true, true, true);
	}		
	
}
