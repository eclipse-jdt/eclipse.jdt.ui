package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MoveInnerToTopLevelTests extends RefactoringTest {

	private static final Class clazz= MoveInnerToTopLevelTests.class;
	private static final String REFACTORING_PATH= "MoveInnerToTopLevel/";

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
	}

	/******* shortcuts **********/
	
	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void validatePassingTest(String parentClassName, String className, String[] cuNames, String[] packageNames) throws Exception {
		IType parentClas= getClassFromTestFile(getPackageP(), parentClassName);
		IType clas= parentClas.getType(className);
				
		MoveInnerToTopRefactoring ref= new MoveInnerToTopRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings());
		
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
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}
	
	public void test1() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test2() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test3() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test4() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test5() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test6() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}
	public void test7() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test8() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}
	public void test9() throws Exception{
		printTestDisabledMessage("removing unused imports");
//		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test10() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}
	public void test11() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test12() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}
	public void test13() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test14() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test15() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"});
	}

	public void test16() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"});
	}

	public void test17() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A"}, new String[]{"p"});
	}

	public void test18() throws Exception{
		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"});
	}

	public void test19() throws Exception{
		printTestDisabledMessage("23078");
//		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"});
	}

	public void test20() throws Exception{
		printTestDisabledMessage("bug 23077 ");
//		validatePassingTest("A", "Inner", new String[]{"A", "A1"}, new String[]{"p", "p1"});
	}

}
