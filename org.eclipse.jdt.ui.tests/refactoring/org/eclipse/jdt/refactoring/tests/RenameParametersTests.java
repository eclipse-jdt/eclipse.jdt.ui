package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;import junit.framework.TestSuite;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.testplugin.JavaTestSetup;import org.eclipse.jdt.testplugin.TestPluginLauncher;import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;import org.eclipse.jdt.core.refactoring.IRefactoring;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.core.refactoring.methods.RenameParametersRefactoring;public class RenameParametersTests extends RefactoringTest{
	
	private static final String REFACTORING_PATH= "RenameParameters/";
	
	public RenameParametersTests(String name){
		super(name);
		fgIsVerbose= true;
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), RenameParametersTests.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(RenameParametersTests.class);
	}
		
	private String getSimpleTestFileName(boolean canRename, boolean input){
		String fileName = "A_" + name();
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
//	private RenameParametersRefactoring createRefactoring(IMethod m, String[] newNames){
//		return new RenameParametersRefactoring(fgChangeCreator, m, newNames);
//	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}
	
	private void helper1(String[] newNames, String[] signature) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		//DebugUtils.dump("cu" + cu.getSource());
		IType classA= getType(cu, "A");
		//DebugUtils.dump("classA" + classA);
		IRefactoring ref= new RenameParametersRefactoring(fgChangeCreator, classA.getMethod("m", signature), newNames);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assert(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());		
	}
	
	private void helper2(String[] newNames, String[] signature) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		//DebugUtils.dump("classA" + classA);
		IRefactoring ref= new RenameParametersRefactoring(fgChangeCreator, classA.getMethod("m", signature), newNames);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);		
	}
	
	public void test0() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test1() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test2() throws Exception{
		helper1(new String[]{"j", "k"}, new String[]{"I", "I"});
	}
	
	public void test3() throws Exception{
		helper1(new String[]{"j", "j1"}, new String[]{"I", "I"});
	}
	
	public void test4() throws Exception{
		helper1(new String[]{"k"}, new String[]{"QA;"});
	}
	
	public void test5() throws Exception{
		helper1(new String[]{"k"}, new String[]{"I"});
	}
	
	public void test6() throws Exception{
		helper1(new String[]{"k"}, new String[]{"I"});
	}
	
	public void test7() throws Exception{
		helper1(new String[]{"k"}, new String[]{"QA;"});
	}
	
	public void test8() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test9() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test10() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test11() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test12() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	public void test13() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test14() throws Exception{
		helper1(new String[]{"j"}, new String[]{"QA;"});
	}

	public void test15() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}

	public void test16() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}

	public void test17() throws Exception{
		helper1(new String[]{"j", "i", "k"}, new String[]{"I", "I", "I"});
	}

	public void test18() throws Exception{
		helper1(new String[]{"j"}, new String[]{"QObject;"});
	}
	
	public void test19() throws Exception{
		helper1(new String[]{"j"}, new String[]{"QA;"});
	}

	public void test20() throws Exception{
		helper1(new String[]{"j"}, new String[]{"Qi;"});
	}
	
	public void test21() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}	
	
	public void test22() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test23() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}	
	
	public void test24() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test25() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}	
	
	public void test26() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}
	
	public void test27() throws Exception{
		helper1(new String[]{"j"}, new String[]{"I"});
	}	
	
	public void test28() throws Exception{
		helper1(new String[]{"j"}, new String[]{"[I"});
	}		
	
	public void test29() throws Exception{
		helper1(new String[]{"B"}, new String[]{"QA;"});
	}

	public void test30() throws Exception{
		helper1(new String[]{"i", "k"}, new String[]{"I", "I"});
	}	
	
	public void test31() throws Exception{
		helper1(new String[]{"kk", "j"}, new String[]{"I", "I"});
	}	
	
	// -----
	
	public void testFail0() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail1() throws Exception{
		helper2(new String[]{"j"}, new String[0]);
	}
	
	public void testFail2() throws Exception{
		helper2(new String[]{"i", "i"}, new String[]{"I", "I"});
	}
	
	public void testFail3() throws Exception{
		helper2(new String[]{"i", "9"}, new String[]{"I", "I"});
	}
	
	public void testFail4() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail5() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail6() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail7() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail8() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail9() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail10() throws Exception{
		helper2(new String[]{"j", "j"}, new String[]{"I", "I"});
	}
	
	public void testFail11() throws Exception{
		helper2(new String[]{"j", "j"}, new String[]{"I", "I"});
	}
	
	public void testFail12() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail13() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail14() throws Exception{
		helper2(new String[]{"j"}, new String[]{"QA;"});
	}
	
	public void testFail15() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail16() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail17() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail18() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
	
	public void testFail19() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}

	public void testFail20() throws Exception{
		helper2(new String[]{"j"}, new String[]{"I"});
	}
}