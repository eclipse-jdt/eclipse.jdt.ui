package org.eclipse.jdt.ui.tests.refactoring;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;

public class ReorderParametersTests extends RefactoringTest {
	private static final Class clazz= ReorderParametersTests.class;
	private static final String REFACTORING_PATH= "ReorderParameters/";
	
	/**
	 * Constructor for ReorderParametersTests
	 */
	public ReorderParametersTests(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	private String getSimpleTestFileName(boolean canReorder, boolean input){
		String fileName = "A_" + getName();
		if (canReorder)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canReorder, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canReorder ? "canReorder/": "cannotReorder/");
		return fileName + getSimpleTestFileName(canReorder, input);
	}
	
	private String getPassingTestFileName(boolean input){
		return getTestFileName(true, input);
	}	
	
	//---helpers 
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}
	
	private void helper1(String[] newOrder, String[] signature) throws Exception{
		helper1(newOrder, signature, null, null);
	}
	
	private void helper1(String[] newOrder, String[] signature, String[] oldNames, String[] newNames) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(method);
		ref.setNewParameterOrder(newOrder);
		if (newNames != null && oldNames != null)
			ref.setNewNames(createRenamings(oldNames, newNames));
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private Map createRenamings(String[] oldNames, String[] newNames) {
		Map map= new HashMap(oldNames.length);
		for (int i = 0; i < newNames.length; i++) {
			map.put(oldNames[i], newNames[i]);
		}
		return map;
	}
	
	private void helperFail(String[] newOrder, String[] signature, int expectedSeverity) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(classA.getMethod("m", signature));
		ref.setNewParameterOrder(newOrder);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);		
		assertEquals("Severity:", expectedSeverity, result.getSeverity());
	}
	
	//------- tests 
	
	public void testFail0() throws Exception{
		helperFail(new String[]{"j", "i"}, new String[]{"I", "I"}, RefactoringStatus.ERROR);
	}
	
	public void testFail1() throws Exception{
		helperFail(new String[]{"j", "i"}, new String[]{"I", "I"}, RefactoringStatus.ERROR);
	}
	
	//---------
	public void test0() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test1() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test2() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test3() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test4() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test5() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test6() throws Exception{
		helper1(new String[]{"k", "i", "j"}, new String[]{"I", "I", "I"});
	}

	public void test7() throws Exception{
		helper1(new String[]{"i", "k", "j"}, new String[]{"I", "I", "I"});
	}

	public void test8() throws Exception{
		helper1(new String[]{"k", "j", "i"}, new String[]{"I", "I", "I"});
	}
	
	public void test9() throws Exception{
		helper1(new String[]{"j", "i", "k"}, new String[]{"I", "I", "I"});
	}

	public void test10() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}

	public void test11() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}

	public void test12() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}

	public void test13() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}
	
	public void test14() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test15() throws Exception{
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test16() throws Exception{
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test17() throws Exception{
		//exception because of bug 11151
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test18() throws Exception{
		//exception because of bug 11151
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test19() throws Exception{
//		printTestDisabledMessage("bug 7274 - reorder parameters: incorrect when parameters have more than 1 modifiers");
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	public void test20() throws Exception{
//		printTestDisabledMessage("bug 18147");
		helper1(new String[]{"b", "a"}, new String[]{"I", "[I"});
	}

//constructor tests
//	public void test21() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test22() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test23() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test24() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test25() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}

	public void test26() throws Exception{
		helper1(new String[]{"a", "y"}, new String[]{"Z", "I"}, new String[]{"y", "a"}, new String[]{"zzz", "bb"});
	}
	
	public void test27() throws Exception{
		helper1(new String[]{"a", "y"}, new String[]{"Z", "I"}, new String[]{"y", "a"}, new String[]{"yyy", "a"});
	}
	
}

