package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ExtractTempTests extends RefactoringTest {

	private static final Class clazz= ExtractTempTests.class;
	private static final String REFACTORING_PATH= "ExtractTemp/";

	private static final String COMPACT= JavaCore.COMPACT;

	private Object fCompactPref; 
		
	public ExtractTempTests(String name) {
		super(name);
	} 
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	private String getSimpleTestFileName(boolean canInline, boolean input){
		String fileName = "A_" + getName();
		if (canInline)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canExtract, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canExtract ? "canExtract/": "cannotExtract/");
		return fileName + getSimpleTestFileName(canExtract, input);
	}
	
	private String getFailingTestFileName(){
		return getTestFileName(false, false);
	}
	private String getPassingTestFileName(boolean input){
		return getTestFileName(true, input);
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canExtract, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canExtract, input), getFileContents(getTestFileName(canExtract, input)));
	}

	protected void setUp() throws Exception {
		super.setUp();
		Hashtable options= JavaCore.getOptions();
		fCompactPref= options.get(JavaCore.FORMATTER_COMPACT_ASSIGNMENT);
		options.put(JavaCore.FORMATTER_COMPACT_ASSIGNMENT, COMPACT);
		JavaCore.setOptions(options);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_COMPACT_ASSIGNMENT, fCompactPref);
		JavaCore.setOptions(options);	
	}

	private void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractTempRefactoring ref= new ExtractTempRefactoring(cu, selection.getOffset(), selection.getLength(), 
																									JavaPreferencesSettings.getCodeGenerationSettings());
		
		ref.setReplaceAllOccurrences(replaceAll);
		ref.setDeclareFinal(makeFinal);
		ref.setTempName(tempName);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("incorrect extraction", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}	
	
	private void failHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractTempRefactoring ref= new ExtractTempRefactoring(cu, selection.getOffset(), selection.getLength(), 
																									JavaPreferencesSettings.getCodeGenerationSettings());
		
		ref.setReplaceAllOccurrences(replaceAll);
		ref.setDeclareFinal(makeFinal);
		ref.setTempName(tempName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}	

	//--- TESTS
	
	public void test0() throws Exception{
		helper1(4, 16, 4, 17, false, false, "temp");
	}
	
	public void test1() throws Exception{
		helper1(4, 16, 4, 17, true, false, "temp");
	}
	
	public void test2() throws Exception{
		helper1(4, 16, 4, 17, true, true, "temp");
	}
	
	public void test3() throws Exception{
		helper1(4, 16, 4, 17, false, true, "temp");
	}	
	
	public void test4() throws Exception{
		helper1(4, 16, 4, 21, false, false, "temp");
	}	
	
	public void test5() throws Exception{
		helper1(4, 16, 4, 21, true, false, "temp");
	}	
	
	public void test6() throws Exception{
		helper1(4, 16, 4, 21, true, true, "temp");
	}	
	
	public void test7() throws Exception{
		helper1(4, 16, 4, 21, false, true, "temp");
	}	

	public void test8() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp");
	}	

	public void test9() throws Exception{
		helper1(5, 20, 5, 25, false, false, "temp");
	}	

	public void test10() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp");
	}	

	public void test11() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp");
	}	

	public void test12() throws Exception{
		helper1(5, 17, 5, 22, true, false, "temp");
	}	

	public void test13() throws Exception{
		helper1(7, 16, 7, 42, true, false, "temp");
	}	

	public void test14() throws Exception{
		helper1(6, 15, 6, 20, false, false, "temp");
	}	
	
	public void test15() throws Exception{
		helper1(7, 23, 7, 28, false, false, "temp");
	}	

	public void test16() throws Exception{
		helper1(7, 23, 7, 28, false, false, "temp");
	}	
	
	public void test17() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp");
	}	
	
	public void test18() throws Exception{
		helper1(6, 20, 6, 25, true, false, "temp");
	}	
	
	public void test19() throws Exception{
		helper1(5, 20, 5, 23, true, false, "temp");
	}	

//cannot do it - see testFail16
//	public void test20() throws Exception{
//		printTestDisabledMessage("regression test for bug#11474");
//		helper1(5, 9, 5, 12, false, false, "temp");
//	}	
	
	public void test21() throws Exception{
		helper1(5, 16, 5, 17, false, false, "temp");
	}	

//cannot do it - see testFail17
//	public void test22() throws Exception{
//		printTestDisabledMessage("regression test for bug#11474");
//		helper1(6, 13, 6, 16, false, false, "temp");
//	}	

	public void test23() throws Exception{
		helper1(7, 17, 7, 20, false, false, "temp");
	}		
	
	public void test24() throws Exception{
		//regression test for bug#8116
		helper1(4, 16, 4, 18, false, false, "temp");
	}
	
	public void test25() throws Exception{
//		printTestDisabledMessage("regression test for bug#8895");
		helper1(4, 17, 4, 22, true, false, "temp");
	}
	
	public void test26() throws Exception{
//		printTestDisabledMessage("regression test for 9905");
		helper1(5, 19, 5, 23, true, false, "temp");
	}
	
	public void test27() throws Exception{
//		printTestDisabledMessage("regression test for 8123");
		helper1(4, 15, 4, 19, true, false, "temp");
	}
	
	public void test28() throws Exception{
//		printTestDisabledMessage("regression test for 11026");
		helper1(4, 16, 4, 31, true, false, "temp");
	}
	
	public void test29() throws Exception{
		helper1(4, 19, 4, 22, true, false, "temp");
	}
	
	public void test30() throws Exception{
		helper1(5, 16, 5, 20, true, false, "temp");
	}

	public void test31() throws Exception{
		helper1(5, 16, 5, 20, true, false, "temp");
	}

	public void test32() throws Exception{
		helper1(4, 16, 4, 22, true, false, "temp");
	}
	
	public void test33() throws Exception{
//		printTestDisabledMessage("regression test for bug#11449");
		helper1(4, 19, 4, 33, true, false, "temp");
	}

	public void test34() throws Exception{
//		printTestDisabledMessage("another regression test for bug#11449");
		helper1(4, 19, 4, 46, true, false, "temp");
	}

	public void test35() throws Exception{
//		printTestDisabledMessage("another regression test for bug#11622");
		helper1(8, 19, 8, 28, true, false, "temp");
	}

	public void test36() throws Exception{
//		printTestDisabledMessage("another regression test for bug#12205");
		helper1(11, 15, 11, 25, true, false, "temp");
	}

	public void test37() throws Exception{
//		printTestDisabledMessage("another regression test for bug#15196");
		helper1(8, 20, 8, 25, true, false, "temp");
	}

	public void test38() throws Exception{
//		printTestDisabledMessage("regression test for bug#17473");
		helper1(5, 28, 5, 32, true, false, "temp1");
	}

	public void test39() throws Exception{
//		printTestDisabledMessage("regression test for bug#20520 ");
		helper1(4, 14, 4, 26, true, false, "temp");
	}
	
	
	// -- testing failing preconditions
	public void testFail0() throws Exception{
		failHelper1(5, 16, 5, 17, false, false, "temp");
	}	

	public void testFail1() throws Exception{
		failHelper1(4, 9, 5, 13, false, false, "temp");
	}	

	public void testFail2() throws Exception{
		failHelper1(4, 9, 4, 20, false, false, "temp");
	}	

	public void testFail3() throws Exception{
		failHelper1(4, 9, 4, 20, false, false, "temp");
	}	

	public void testFail4() throws Exception{
		failHelper1(5, 9, 5, 12, false, false, "temp");
	}	
	
	public void testFail5() throws Exception{
		failHelper1(3, 12, 3, 15, false, false, "temp");
	}	

	public void testFail6() throws Exception{
		failHelper1(4, 14, 4, 19, false, false, "temp");
	}	

	public void testFail7() throws Exception{
		failHelper1(4, 15, 4, 20, false, false, "temp");
	}	

//	public void testFail8() throws Exception{
//		printTestDisabledMessage("removed");
//		failHelper1(5, 16, 5, 20, false, false, "temp");
//	}	

	public void testFail9() throws Exception{
		failHelper1(4, 19, 4, 23, false, false, "temp");
	}	

	public void testFail10() throws Exception{
		failHelper1(4, 33, 4, 39, false, false, "temp");
	}	

	public void testFail11() throws Exception{
//		printTestDisabledMessage("regression test for bug#13061");
		failHelper1(4, 18, 4, 19, false, false, "temp");
	}	

	public void testFail12() throws Exception{
		failHelper1(4, 16, 4, 29, false, false, "temp");
	}	

//removed
//	public void testFail13() throws Exception{
//		failHelper1(5, 16, 5, 20, false, false, "temp");
//	}	

//removed
//	public void testFail14() throws Exception{
//		failHelper1(4, 16, 4, 22, false, false, "temp");
//	}	

//removed
//	public void testFail15() throws Exception{
//		failHelper1(4, 19, 4, 22, false, false, "temp");
//	}	

	public void testFail16() throws Exception{
		failHelper1(5, 9, 5, 12, false, false, "temp");
	}	

	public void testFail17() throws Exception{
		failHelper1(6, 13, 6, 16, false, false, "temp");
	}	

	public void testFail18() throws Exception{
//		printTestDisabledMessage("regression test for bug#8149");
		failHelper1(4, 27, 4, 28, false, false, "temp");
	}	

	public void testFail19() throws Exception{
		printTestDisabledMessage("regression test for bug#8149");
//		failHelper1(6, 16, 6, 18, false, false, "temp");
	}	

	public void testFail20() throws Exception{
//		printTestDisabledMessage("regression test for bug#13249");
		failHelper1(3, 9, 3, 41, false, false, "temp");
	}	
	
	public void testFail21() throws Exception{
		//test for bug 19851
		failHelper1(6, 9, 6, 24, false, false, "temp");
	}	
	
}
