package org.eclipse.jdt.ui.tests.actions;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

public class PasteSourceReferenceActionTests extends RefactoringTest {

	private static final Class clazz= PasteSourceReferenceActionTests.class;
	private static final String REFACTORING_PATH= "PasteSourceReference/";
	
	public PasteSourceReferenceActionTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
		
	private ICompilationUnit fCuA;
	private ICompilationUnit fCuB;
	private ICompilationUnit fNewCuA;
	private ICompilationUnit fNewCuB;
	private static final String CU_A_NAME= "A";
	private static final String CU_B_NAME= "B";
	private Clipboard fClipboard;
		
	protected void setUp() throws Exception{
		super.setUp();
		fClipboard= new Clipboard(Display.getDefault());
		fCuA= createCUfromTestFile(getPackageP(), CU_A_NAME);
		fCuB= createCUfromTestFile(getPackageP(), CU_B_NAME);
		assertTrue("A.java does not exist", fCuA.exists());
		assertTrue("B.java does not exist", fCuB.exists());
	}
	
	protected void tearDown() throws Exception{
		super.tearDown();
		fClipboard.dispose();
		if (fNewCuA != null && fNewCuA.exists())
			fNewCuA.delete(false, null);		
		if (fCuA != null && fCuA.exists())
			fCuA.delete(false, null);		
			
		if (fNewCuB != null && fNewCuB.exists())
			fNewCuB.delete(false, null);		
		if (fCuB != null && fCuB.exists())
			fCuB.delete(false, null);		
	}

	private void check() throws IOException, JavaModelException {
		fNewCuA= getPackageP().getCompilationUnit(CU_A_NAME + ".java");
		assertEquals("incorrect paste in A", getFileContents(getOutputTestFileName(CU_A_NAME)), fNewCuA.getSource());	
		
		fNewCuB= getPackageP().getCompilationUnit(CU_B_NAME + ".java");
		assertEquals("incorrect paste in B", getFileContents(getOutputTestFileName("B")), fNewCuB.getSource());	
	}
		
	//---- tests 
	
	public void test0() throws Exception{
		IType typeA= fCuA.getType("A");
		assertTrue("A does not exist", typeA.exists());

		SourceReferenceTestUtil.copy(new IType[]{typeA}, fClipboard);
		
		IType typeB= fCuB.getType("B");
		SourceReferenceTestUtil.paste(new IType[]{typeB}, fClipboard);
		
		check();
	}

	public void test1() throws Exception{
		IType typeA= fCuA.getType("A");
		assertTrue("A does not exist", typeA.exists());
		
		SourceReferenceTestUtil.copy(new IType[]{typeA}, fClipboard);
		SourceReferenceTestUtil.delete(new IType[]{typeA});
		
		IType typeB= fCuB.getType("B");
		SourceReferenceTestUtil.paste(new IType[]{typeB}, fClipboard);

		check();
	}
	
	public void test2() throws Exception{
		Object elem0= fCuA.getType("A").getField("y");
				
		SourceReferenceTestUtil.copy(new Object[]{elem0}, fClipboard);
		SourceReferenceTestUtil.delete(new Object[]{elem0});
		
		IType typeB= fCuB.getType("B");
		SourceReferenceTestUtil.paste(new IType[]{typeB}, fClipboard);

		check();
	}

	public void test3() throws Exception{
//		if (true){
//			printTestDisabledMessage("test for bug#19007");
//			return;
//		}	
		Object elem0= fCuA.getImport("java.lang.*");
				
		SourceReferenceTestUtil.copy(new Object[]{elem0}, fClipboard);
		
		ISourceReference container= fCuB.getImportContainer();
		SourceReferenceTestUtil.paste(new ISourceReference[]{container}, fClipboard);

		check();
	}
	
}

