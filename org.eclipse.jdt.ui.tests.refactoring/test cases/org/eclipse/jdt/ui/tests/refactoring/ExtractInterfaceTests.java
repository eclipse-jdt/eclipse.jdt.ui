package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

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
		
	/******* shortcuts **********/
	
	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, className), className);
	}

	private void helper(String className, String newInterfaceName) throws Exception {
		IType clas= getClassFromTestFile(getPackageP(), className);
		ICompilationUnit cu= clas.getCompilationUnit();
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		ExtractInterfaceRefactoring ref= new ExtractInterfaceRefactoring(clas);
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		assertEquals("incorrect changes in " + className, getFileContents(getOutputTestFileName(className)), cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEquals("incorrect interface created", getFileContents(getOutputTestFileName(newInterfaceName)), interfaceCu.getSource());
	}

	//---------------tests ----------------------
	
	public void test0() throws Exception{
		helper("A", "I");
	}

	public void test1() throws Exception{
		helper("A", "I");
	}

	public void test2() throws Exception{
		helper("A", "I");
	}

	public void test3() throws Exception{
		helper("A", "I");
	}

	public void test4() throws Exception{
		helper("A", "I");
	}

}
