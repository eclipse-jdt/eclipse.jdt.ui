package org.eclipse.jdt.refactoring.tests;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IRefactoring;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.fields.RenameNonPrivateFieldRefactoring;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;


public class RenameNonPrivateFieldTests extends RefactoringTest{
	
	private static final String REFACTORING_PATH= "RenameNonPrivateField/";

	public RenameNonPrivateFieldTests(String name) {
		super(name);
	}

	public static void main(String[] args) {
		args= new String[] { RenameNonPrivateFieldTests.class.getName() };
		TestPluginLauncher.runUI(TestPluginLauncher.getLocationFromProperties(), args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}

	public static Test noSetupSuite() {
		return new TestSuite(RenameNonPrivateFieldTests.class);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1_0(String fieldName, String newFieldName) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), "A"), "A");
		IRefactoring ref= new RenameNonPrivateFieldRefactoring(fgChangeCreator, getScope(), classA.getField(fieldName), newFieldName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}
	
	private void helper1() throws Exception{
		helper1_0("f", "g");
	}
	
	private void helper2(String fieldName, String newFieldName) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType classA= getType(cu, "A");
		IRefactoring ref= new RenameNonPrivateFieldRefactoring(fgChangeCreator, getScope(), classA.getField(fieldName), newFieldName);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("was supposed to pass", null, result);
		assertEquals("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
		
		assert("anythingToUndo", Refactoring.getUndoManager().anythingToUndo());
		assert("! anythingToRedo", !Refactoring.getUndoManager().anythingToRedo());
		
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		assertEquals("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assert("! anythingToUndo", !Refactoring.getUndoManager().anythingToUndo());
		assert("anythingToRedo", Refactoring.getUndoManager().anythingToRedo());
		
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		assertEquals("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
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
		helper1();
	}
	
	public void testFail14() throws Exception{
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
	}

	public void test5() throws Exception{
		helper2();
	}
	
	public void test6() throws Exception{
		helper2();
	}

	public void test7() throws Exception{
		helper2();
	}
	
	public void test8() throws Exception{
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
}