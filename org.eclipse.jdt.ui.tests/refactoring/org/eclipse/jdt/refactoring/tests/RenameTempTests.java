/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.refactoring.tests;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;
import org.eclipse.jdt.internal.core.refactoring.SourceRange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameParametersRefactoring;
import org.eclipse.jdt.refactoring.tests.MySetup;
import org.eclipse.jdt.refactoring.tests.RefactoringTest;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameTempRefactoring;
import org.eclipse.jdt.testplugin.TestPluginLauncher;public class RenameTempTests extends RefactoringTest{
	
	private static final Class clazz= RenameTempTests.class;
	private static final String REFACTORING_PATH= "RenameTemp/";
	
	public RenameTempTests(String name){
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), RenameTempTests.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(noSetupSuite());
		return new MySetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(clazz);
	}
		
	private String getSimpleTestFileName(boolean canRename, boolean input){
		String fileName = "A_" + getName();
		if (canRename)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canRename, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canRename ? "canRename/": "cannotRename/");
		return fileName + getSimpleTestFileName(canRename, input);
	}
	
	private String getFailingTestFileName(){
		return getTestFileName(false, false);
	}
	private String getPassingTestFileName(boolean input){
		return getTestFileName(true, input);
	}
	
	//------------
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}
	
	private ISourceRange getSelection(ICompilationUnit cu) throws Exception{
		String source= cu.getSource();
		int offset= source.indexOf(ExtractMethodTests.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(ExtractMethodTests.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset, end - offset);
	}

	private void helper1(String newName, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		ISourceRange selection= getSelection(cu);
		RenameTempRefactoring ref= new RenameTempRefactoring(cu, selection.getOffset(), selection.getLength());
		ref.setUpdateReferences(updateReferences);
		ref.setNewName(newName);
		
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}	
	private void helper1(String newName) throws Exception{
		helper1(newName, true);
	}

	private void helper2(String newName, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		IType classA= getType(cu, "A");
		ISourceRange selection= getSelection(cu);
		RenameTempRefactoring ref= new RenameTempRefactoring(cu, selection.getOffset(), selection.getLength());
		ref.setUpdateReferences(updateReferences);
		ref.setNewName(newName);
		
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}

	
	private void helper2(String newName) throws Exception{
		helper2(newName, true);
	}
	
	public void test0() throws Exception{
		helper1("j");
	}
	
	public void test1() throws Exception{
		helper1("j");
	}
	
//	public void test2() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("x", "j");
//		renaming.put("y", "k");
//		helper1(renaming, new String[0]);
//	}
	
	public void test3() throws Exception{
		helper1("j1");
	}

	public void test4() throws Exception{
		helper1("k");
	}

	public void test5() throws Exception{
		helper1("k");
	}

	public void test6() throws Exception{
		helper1("k");
	}

	public void test7() throws Exception{
		helper1("k");
	}
//
//	//8, 9, 10 removed
//	
//
	public void test11() throws Exception{
		helper1("j");
	}

	public void test12() throws Exception{
		helper1("j");
	}

	public void test13() throws Exception{
		helper1("j");
	}

	public void test14() throws Exception{
		helper1("j");
	}

// disabled
//	public void test15() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("i", "j");
//		renaming.put("j", "i");
//		helper1(renaming, new String[0]);
//	}
//
	public void test16() throws Exception{
		helper1("j");
	}

// disabled
//	public void test17() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("i", "j");
//		renaming.put("j", "i");
//		helper1(renaming, new String[0]);
//	}
//
	public void test18() throws Exception{
		helper1("j");
	}

	public void test19() throws Exception{
		helper1("j");
	}

	public void test20() throws Exception{
		helper1("j");
	}

	public void test21() throws Exception{
		helper1("j");
	}
	
	public void test22() throws Exception{
		helper1("j");
	}

//	disabled
//	public void test23() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("i", "j");
//		renaming.put("j", "i");
//		helper1(renaming, new String[0]);
//	}

	public void test24() throws Exception{
		helper1("j");
	}

	public void test25() throws Exception{
		helper1("j");
	}

	public void test26() throws Exception{
		helper1("j");
	}

	public void test27() throws Exception{
		helper1("j");
	}

	public void test28() throws Exception{
		helper1("j");
	}

	public void test29() throws Exception{
		helper1("b");
	}

	public void test30() throws Exception{
		helper1("k");
	}

	public void test31() throws Exception{
		helper1("kk");
	}

	public void test32() throws Exception{
		helper1("j");
	}

	public void test33() throws Exception{
		helper1("b", false);
	}
	
	public void test34() throws Exception{
		helper1("j");
	}

// -----
	public void testFail0() throws Exception{
		helper2("j");
	}
	
	public void testFail1() throws Exception{
		helper2("j");
	}

	public void testFail2() throws Exception{
		helper2("i");
	}
	
	public void testFail3() throws Exception{
		helper2("9");
	}

	public void testFail4() throws Exception{
		helper2("j");
	}

	public void testFail5() throws Exception{
		helper2("j");
	}

	public void testFail6() throws Exception{
		helper2("j");
	}

	public void testFail7() throws Exception{
		helper2("j");
	}

	public void testFail8() throws Exception{
		helper2("j");
	}

	public void testFail9() throws Exception{
		helper2("j");
	}
	
	public void testFail10() throws Exception{
		helper2("uu");
	}

	public void testFail11() throws Exception{
		helper2("uu");
	}
	
	public void testFail12() throws Exception{
		helper2("j");
	}
	
	public void testFail13() throws Exception{
		helper2("j");
	}

	public void testFail14() throws Exception{
		helper2("j");
	}	

	public void testFail15() throws Exception{
		helper2("j");
	}	

	public void testFail16() throws Exception{
		helper2("j");
	}	
	
	public void testFail17() throws Exception{
		helper2("j");
	}	

	public void testFail18() throws Exception{
		helper2("j");
	}	

	public void testFail19() throws Exception{
		helper2("j");
	}	
	
	public void testFail20() throws Exception{
		helper2("j");
	}	
	
	public void testFail21() throws Exception{
		helper2("j");
	}

	public void testFail22() throws Exception{
		helper2("j");
	}

	public void testFail23() throws Exception{
		helper2("j");
	}
}