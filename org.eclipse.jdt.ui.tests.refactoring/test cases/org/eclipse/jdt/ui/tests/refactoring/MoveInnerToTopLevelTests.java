package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveInnerToTopLevelTests extends RefactoringTest {

	private static final Class clazz= MoveInnerToTopLevelTests.class;
	private static final String REFACTORING_PATH= "MoveInnerToTopLevel/";

	private Object fCompactPref;

	public MoveInnerToTopLevelTests(String name) {
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
		Template[] filecomments= Templates.getInstance().getTemplates("filecomment");
		for (int i= 0; i < filecomments.length; i++) {
			filecomments[i].setPattern("/** filecomment template */");	
		}
		Hashtable options= JavaCore.getOptions();
		fCompactPref= options.get(JavaCore.FORMATTER_COMPACT_ASSIGNMENT);
		options.put(JavaCore.FORMATTER_COMPACT_ASSIGNMENT, JavaCore.COMPACT);
		JavaCore.setOptions(options);
	}
	
	
	protected void tearDown() throws Exception {
		super.tearDown();
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_COMPACT_ASSIGNMENT, fCompactPref);
		JavaCore.setOptions(options);	
	}
	

	/******* shortcuts **********/
	
	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void validatePassingTest(String parentClassName, String className, String[] cuNames, String[] packageNames, String enclosingInstanceName) throws Exception {
		IType parentClas= getClassFromTestFile(getPackageP(), parentClassName);
		IType clas= parentClas.getType(className);
				
		MoveInnerToTopRefactoring ref= new MoveInnerToTopRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings());
		if (enclosingInstanceName != null){
			ref.setEnclosingInstanceName(enclosingInstanceName);
			assertTrue("name should be ok ", ref.checkEnclosingInstanceName(enclosingInstanceName).isOK());
		}	
		
		ICompilationUnit[] cus= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			cus[i]= createCUfromTestFile(getPackage(packageNames[i]), cuNames[i]);			
		}
		assertEquals("precondition check was supposed to pass", null, performRefactoring(ref));

		for (int i= 0; i < cus.length; i++) {
			assertEquals("incorrect changes in " + cus[i].getElementName(), getFileContents(getOutputTestFileName(cuNames[i])), cus[i].getSource());
		}

		ICompilationUnit newCu= clas.getPackageFragment().getCompilationUnit(className + ".java");
		assertEquals("incorrect new cu created", getFileContents(getOutputTestFileName(className)), newCu.getSource());
	}

	private void validateFailingTest(String parentClassName, String className, String[] cuNames, String[] packageNames, String enclosingInstanceName, int expectedSeverity) throws Exception {
		IType parentClas= getClassFromTestFile(getPackageP(), parentClassName);
		IType clas= parentClas.getType(className);
				
		MoveInnerToTopRefactoring ref= new MoveInnerToTopRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings());
		if (enclosingInstanceName != null){
			ref.setEnclosingInstanceName(enclosingInstanceName);
		}	
		
		ICompilationUnit[] cus= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			cus[i]= createCUfromTestFile(getPackage(packageNames[i]), cuNames[i]);			
		}
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition check was supposed to fail", result);
		assertEquals("different severity expected", expectedSeverity, result.getSeverity());
	}
	private IPackageFragment getPackage(String name) throws JavaModelException {
		if ("p".equals(name))
			return getPackageP();
		IPackageFragment pack= getRoot().getPackageFragment(name);
		if (pack.exists())	
			return pack;
		return getRoot().createPackageFragment(name, false, new NullProgressMonitor());	
	}


	//-- tests 

	public void test0() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}
	
	public void test1() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test2() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test3() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test4() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test5() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test6() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}
	public void test7() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test8() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}
	public void test9() throws Exception{
		printTestDisabledMessage("removing unused imports");
//		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test10() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}
	public void test11() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test12() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}
	public void test13() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test14() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test15() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null);
	}

	public void test16() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null);
	}

	public void test17() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, null);
	}

	public void test18() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null);
	}

	public void test19() throws Exception{
		printTestDisabledMessage("bug 23078");
//		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null);
	}

	public void test20() throws Exception{
//		printTestDisabledMessage("bug 23077 ");
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"}, null);
	}

	public void test_nonstatic_0() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}

	public void test_nonstatic_1() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_2() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_3() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_4() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_5() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_6() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_7() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_8() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_9() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_10() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_11() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_12() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_13() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_14() throws Exception{
		printTestDisabledMessage("bug 23488");
//		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_15() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_16() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_17() throws Exception{ 
		printTestDisabledMessage("bug 23488");
//		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_18() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_19() throws Exception{
//		printTestDisabledMessage("bug 23464 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_20() throws Exception{
//		printTestDisabledMessage("bug 23464 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_21() throws Exception{
//		printTestDisabledMessage("must fix - consequence of fix for 23464");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_22() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_23() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_24() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_25() throws Exception{
//		printTestDisabledMessage("bug 23464 ");
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_26() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_27() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}
	public void test_nonstatic_28() throws Exception{
		printTestDisabledMessage("test for bug 23725");
//		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a");
	}

	public void testFail_nonstatic_0() throws Exception{
		validateFailingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", RefactoringStatus.ERROR);
	}
	public void testFail_nonstatic_1() throws Exception{
		validateFailingTest("A", "Inner", new String[]{"A"}, new String[]{"p"}, "a", RefactoringStatus.ERROR);
	}

}
