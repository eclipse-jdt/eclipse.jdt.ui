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

import org.eclipse.jdt.ui.tests.refactoring.infra.TestExceptionHandler;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameFieldRefactoring;

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

	private CodeGenerationSettings getSettings(){
		CodeGenerationSettings settings= new CodeGenerationSettings();
		settings.createComments= true;
		settings.createNonJavadocComments= true;
		settings.importOrder= new String[]{"java", "javax", "com"};
		settings.importThreshold= 99;
		return settings;
	}
	
	private String getPrefixes(){
		return "f";
	}
	
	private void helper1_0(String fieldName, String newFieldName, String typeName,
							boolean renameGetter, boolean renameSetter) throws Exception{
		IType declaringType= getType(createCUfromTestFile(getPackageP(), "A"), typeName);
		RenameFieldRefactoring ref= new RenameFieldRefactoring(declaringType.getField(fieldName));
		ref.setNewName(newFieldName);
		ref.setRenameGetter(renameGetter);
		ref.setRenameSetter(renameSetter);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}
	
	private void helper1_0(String fieldName, String newFieldName) throws Exception{
		helper1_0(fieldName, newFieldName, "A", false, false);
	}

	private void helper1() throws Exception{
		helper1_0("f", "g");
	}
	
	//--
		
	private void helper2(String fieldName, String newFieldName, boolean updateReferences,  boolean updateJavaDoc, 
											boolean updateComments, boolean updateStrings,
											boolean renameGetter, boolean renameSetter,
											boolean expectedGetterRenameEnabled, boolean expectedSetterRenameEnabled) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		RenameFieldRefactoring ref= new RenameFieldRefactoring(classA.getField(fieldName));
		ref.setUpdateReferences(updateReferences);
		ref.setUpdateJavaDoc(updateJavaDoc);
		ref.setUpdateComments(updateComments);
		ref.setUpdateStrings(updateStrings);
		assertEquals("getter rename enabled", expectedGetterRenameEnabled, ref.canEnableGetterRenaming() == null);
		assertEquals("setter rename enabled", expectedSetterRenameEnabled, ref.canEnableSetterRenaming() == null);
		ref.setRenameGetter(renameGetter);
		ref.setRenameSetter(renameSetter);
		ref.setNewName(newFieldName);
		
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("was supposed to pass", null, result);
		assertEquals("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
		
		assertTrue("anythingToUndo", Refactoring.getUndoManager().anythingToUndo());
		assertTrue("! anythingToRedo", !Refactoring.getUndoManager().anythingToRedo());
		
		Refactoring.getUndoManager().performUndo(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
		assertEquals("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertTrue("! anythingToUndo", !Refactoring.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", Refactoring.getUndoManager().anythingToRedo());
		
		Refactoring.getUndoManager().performRedo(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
		assertEquals("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	private void helper2(boolean updateReferences) throws Exception{
		helper2("f", "g", updateReferences, false, false, false, false, false, false, false);
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
		helper2("f", "gg", true, true, true, true, false, false, false, false);
	}	

	public void test4() throws Exception{
		helper2("fMe", "fYou", true, false, false, false, true, true, true, true);
	}		
	
	public void test5() throws Exception{
		//regression test for 9895
		helper2("fMe", "fYou", true, false, false, false, true, false, true, false);
	}		
	
	public void test6() throws Exception{
		//regression test for 9895 - opposite case
		helper2("fMe", "fYou", true, false, false, false, false, true, false, true);
	}		

	public void test7() throws Exception{
		//regression test for 21292 
		helper2("fBig", "fSmall", true, false, false, false, true, true, true, true);
	}		
	
	public void test8() throws Exception{
		//regression test for 26769
		helper2("f", "g", true, false, false, false, true, false, true, false);
	}

	public void test9() throws Exception{
		//regression test for 30906
		helper2("fBig", "fSmall", true, false, false, false, true, true, true, true);
	}		
	
}
