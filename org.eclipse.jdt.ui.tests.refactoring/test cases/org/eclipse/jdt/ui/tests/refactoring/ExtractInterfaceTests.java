package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ExtractInterfaceTests extends RefactoringTest {

	private static final Class clazz= ExtractInterfaceTests.class;
	private static final String REFACTORING_PATH= "ExtractInterface/";
	
	public ExtractInterfaceTests(String name) {
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
		Template[] typecomments= Templates.getInstance().getTemplates("typecomment");
		for (int i= 0; i < typecomments.length; i++) {
			typecomments[i].setPattern("/** typecomment template*/");	
		}
		Template[] filecomments= Templates.getInstance().getTemplates("filecomment");
		for (int i= 0; i < typecomments.length; i++) {
			filecomments[i].setPattern("/** filecomment template */");	
		}
	}
		
	/******* shortcuts **********/
	
	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void validatePassingTest(String className, String newInterfaceName, boolean extractAll, boolean replaceOccurrences) throws Exception {
		IType clas= getClassFromTestFile(getPackageP(), className);
		ICompilationUnit cu= clas.getCompilationUnit();
		IPackageFragment pack= (IPackageFragment)cu.getParent();
				
		ExtractInterfaceRefactoring ref= new ExtractInterfaceRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings());
		assertEquals("interface name should be accepted", RefactoringStatus.OK, ref.checkNewInterfaceName(newInterfaceName).getSeverity());
		
		ref.setNewInterfaceName(newInterfaceName);
		if (extractAll)
			ref.setExtractedMembers(ref.getExtractableMembers());
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		assertEquals("incorrect changes in " + className, getFileContents(getOutputTestFileName(className)), cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEquals("incorrect interface created", getFileContents(getOutputTestFileName(newInterfaceName)), interfaceCu.getSource());
	}

	private void validateFailingTest(String className, String newInterfaceName, boolean extractAll, int expectedSeverity) throws Exception {
		IType clas= getClassFromTestFile(getPackageP(), className);
		ExtractInterfaceRefactoring ref= new ExtractInterfaceRefactoring(clas, JavaPreferencesSettings.getCodeGenerationSettings());
		
		ref.setNewInterfaceName(newInterfaceName);
		if (extractAll)
			ref.setExtractedMembers(ref.getExtractableMembers());
		assertTrue("was not supposed to pass", performRefactoring(ref) != null);	
		assertEquals("was not supposed to fail with different severity", expectedSeverity, performRefactoring(ref).getSeverity());
	}

	//---------------tests ----------------------
	
	public void test0() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test1() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test2() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test3() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test4() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test5() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test6() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test7() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test8() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test9() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test10() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test11() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test12() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test13() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void testFail0() throws Exception{
		validateFailingTest("A", "I", true, RefactoringStatus.FATAL);
	}

	public void testFail1() throws Exception{
		validateFailingTest("A", "I", true, RefactoringStatus.FATAL);
	}
}