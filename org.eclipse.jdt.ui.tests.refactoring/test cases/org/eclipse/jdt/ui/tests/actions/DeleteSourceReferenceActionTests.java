package org.eclipse.jdt.ui.tests.actions;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.reorg.DeleteSourceReferencesAction;

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

	private DeleteSourceReferencesAction createAction(Object[] elems){
		ISelectionProvider provider= new FakeSelectionProvider(elems);

		DeleteSourceReferencesAction deleteAction= new DeleteSourceReferencesAction(provider){
			protected boolean isOkToDeleteCus(ICompilationUnit[] cusToDelete) {
				return false;
			}
		};
		return deleteAction;
	}

	private void helper_delete(Object[] elems) throws IOException, JavaModelException {
		DeleteSourceReferencesAction deleteAction= createAction(elems);
		
		deleteAction.update();
		assertTrue("delete action enabled", deleteAction.isEnabled());
		deleteAction.run();
		
		fNewCuA= getPackageP().getCompilationUnit(CU_NAME + ".java");
		assertTrue("A.java does not exist", fNewCuA.exists());
		assertEquals("incorrect content of A.java", getFileContents(getOutputTestFileName(CU_NAME)), fNewCuA.getSource());	
	}
		
	public void test0() throws Exception{
		//test for bug#8405 Delete field action broken for multiple declarations
		Object elem0= fCuA.getType("A").getField("i");
		helper_delete(new Object[]{elem0});
	}

	public void test1() throws Exception{
		Object elem0= fCuA.getType("A");
		helper_delete(new Object[]{elem0});		
	}
	
	public void test2() throws Exception{
		Object elem0= fCuA.getType("A").getField("i");
		helper_delete(new Object[]{elem0});
	}

	public void test3() throws Exception{
		//test for bug#8405 Delete field action broken for multiple declarations
		Object elem0= fCuA.getType("A").getField("i");
		Object elem1= fCuA.getType("A").getField("j");
		helper_delete(new Object[]{elem0, elem1});
	}

	public void test4() throws Exception{
		//test for bug#8405 Delete field action broken for multiple declarations
		Object elem0= fCuA.getType("A").getField("i");
		Object elem1= fCuA.getType("A").getField("k");
		helper_delete(new Object[]{elem0, elem1});
	}

	public void test5() throws Exception{
		//test for bug#8405 Delete field action broken for multiple declarations
		Object elem0= fCuA.getType("A").getField("j");
		helper_delete(new Object[]{elem0});
	}

	public void test6() throws Exception{
		//test for bug#8405 Delete field action broken for multiple declarations
		//exposes bug#9382 IField::delete incorrect on multiple field declarations with 
		Object elem0= fCuA.getType("A").getField("j");
		helper_delete(new Object[]{elem0});
	}

	public void test7() throws Exception{
		//exposes bug#9381 IPackageDeclaration is not ISourceManipulation 
		Object elem0= fCuA.getPackageDeclaration("p");
		helper_delete(new Object[]{elem0});
	}
	
	public void test8() throws Exception{
		Object elem0= fCuA.getType("A").getMethod("m", new String[0]);
		helper_delete(new Object[]{elem0});
	}

	public void test9() throws Exception{
		Object elem0= fCuA.getType("A").getInitializer(1);
		helper_delete(new Object[]{elem0});
	}

	public void test10() throws Exception{
		Object elem0= fCuA.getType("A").getInitializer(1);
		helper_delete(new Object[]{elem0});
	}

	public void test11() throws Exception{
		Object elem0= fCuA.getImport("java.util.List");
		helper_delete(new Object[]{elem0});
	}

	public void test12() throws Exception{
		Object elem0= fCuA.getType("A").getType("B");
		helper_delete(new Object[]{elem0});
	}
}
