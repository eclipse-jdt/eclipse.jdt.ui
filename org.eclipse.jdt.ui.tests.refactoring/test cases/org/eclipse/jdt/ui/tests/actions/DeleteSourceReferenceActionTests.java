package org.eclipse.jdt.ui.tests.actions;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

public class DeleteSourceReferenceActionTests extends RefactoringTest{

	private static final Class clazz= DeleteSourceReferenceActionTests.class;
	private static final String REFACTORING_PATH= "DeleteSourceReference/";
	
	public DeleteSourceReferenceActionTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private ICompilationUnit fCuA;
	private ICompilationUnit fNewCuA;
	private static final String CU_NAME= "A";
		
	protected void setUp() throws Exception{
		super.setUp();
		
		fCuA= createCUfromTestFile(getPackageP(), CU_NAME);
		assertTrue("A.java does not exist", fCuA.exists());
	}
	
	protected void tearDown() throws Exception{
		super.tearDown();
		if (fNewCuA != null && fNewCuA.exists())
			fNewCuA.delete(false, null);		
		if (fCuA != null && fCuA.exists())
			fCuA.delete(false, null);		
	}

	private void check() throws IOException, JavaModelException {
		fNewCuA= getPackageP().getCompilationUnit(CU_NAME + ".java");
		assertTrue("A.java does not exist", fNewCuA.exists());
		assertEquals("incorrect content of A.java", getFileContents(getOutputTestFileName(CU_NAME)), fNewCuA.getSource());	
	}
		
	public void test0() throws Exception{
		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");
		if (true)
			return;
		Object elem0= fCuA.getType("A").getField("i");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test1() throws Exception{
		Object elem0= fCuA.getType("A");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}
	
	public void test2() throws Exception{
		Object elem0= fCuA.getType("A").getField("i");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test3() throws Exception{
		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
		if (true)
			return;
		Object elem0= fCuA.getType("A").getField("i");
		Object elem1= fCuA.getType("A").getField("j");
		Object[] elems= new Object[]{elem0, elem1};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test4() throws Exception{
		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
		if (true)
			return;
		Object elem0= fCuA.getType("A").getField("i");
		Object elem1= fCuA.getType("A").getField("k");
		Object[] elems= new Object[]{elem0, elem1};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test5() throws Exception{
		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
		if (true)
			return;
		Object elem0= fCuA.getType("A").getField("j");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test6() throws Exception{
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
		printTestDisabledMessage("test for bug#9382 IField::delete incorrect on multiple field declarations with initializers");		
		if (true)
			return;
		Object elem0= fCuA.getType("A").getField("j");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test7() throws Exception{
		//exposes bug#9381 IPackageDeclaration is not ISourceManipulation 
		Object elem0= fCuA.getPackageDeclaration("p");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}
	
	public void test8() throws Exception{
		Object elem0= fCuA.getType("A").getMethod("m", new String[0]);
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test9() throws Exception{
		Object elem0= fCuA.getType("A").getInitializer(1);
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test10() throws Exception{
		Object elem0= fCuA.getType("A").getInitializer(1);
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test11() throws Exception{
		Object elem0= fCuA.getImport("java.util.List");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test12() throws Exception{
		Object elem0= fCuA.getType("A").getType("B");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}
	
	public void test13() throws Exception{
		Object elem0= fCuA.getType("A").getType("B");
		Object elem1= fCuA.getType("A");
		Object[] elems= new Object[]{elem0, elem1};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test14() throws Exception{
		Object elem0= fCuA.getType("A").getType("B");
		Object elem1= fCuA.getType("A");
		Object elem2= fCuA.getPackageDeclaration("p");
		Object[] elems= new Object[]{elem0, elem1, elem2};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test15() throws Exception{
		Object elem0= fCuA.getType("A").getField("field");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test16() throws Exception{
//		printTestDisabledMessage("test for bug#15412 deleting type removes too much from editor");		
//		if (true)
//			return;
		
		Object elem0= fCuA.getType("Test");
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test17() throws Exception{
//		printTestDisabledMessage("test for bug#15936 delete methods deletes too much (indent messing)");		
//		if (true)
//			return;
		
		Object elem0= fCuA.getType("A").getMethod("f", new String[0]);
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}

	public void test18() throws Exception{
//		printTestDisabledMessage("test for bug#16314");		
//		if (true)
//			return;
		
		Object elem0= fCuA.getType("A").getMethod("fs", new String[0]);
		Object[] elems= new Object[]{elem0};
		SourceReferenceTestUtil.delete(elems);
		check();
	}
	
}
