package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class MultiMoveTests extends RefactoringTest {

	private static final Class clazz= MultiMoveTests.class;
	private static final String REFACTORING_PATH= "MultiMove/";

	public MultiMoveTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//---
	private IPackageFragment createPackage(String name) throws Exception{
		return getRoot().createPackageFragment(name, true, null);
	}
	
	private ICompilationUnit createCu(IPackageFragment pack, String cuPath, String cuName) throws Exception{
		return createCU(pack, cuName, getFileContents(getRefactoringPath() + cuPath));
	}
	
	private void delete(IPackageFragment pack) throws Exception {
		performDummySearch();
		try {
			if (pack.exists())
				pack.delete(true, null);
		} catch(JavaModelException e) {
			//ignore
		}
	}
	
	
	//--------
	public void test0() throws Exception{		
		final String p1Name= "p1";
		final String inDir= "/in/";
		final String outDir= "/out/";
		
		IPackageFragment packP1= createPackage(p1Name);
		ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
		ICompilationUnit p1B= createCu(packP1, getName() + inDir + p1Name + "/B.java", "B.java");
		
		String p2Name= "p2";
		IPackageFragment packP2= createPackage(p2Name);
		ICompilationUnit p2C= createCu(packP2, getName() + inDir + p2Name + "/C.java", "C.java");
		
		List elems= new ArrayList();
		elems.add(p1A);
		elems.add(p1B);
		MoveRefactoring ref= new MoveRefactoring(elems, JavaPreferencesSettings.getCodeGenerationSettings());
		ref.setDestination(packP2);
		ref.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(ref);
		
		//-- checks
		assertEquals("status should be ok here", null, status);		
		
		assertEquals("p1 files", 0, packP1.getChildren().length);
		assertEquals("p2 files", 3, packP2.getChildren().length);

		String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/A.java");
		assertEquals("incorrect update of A", expectedSource, packP2.getCompilationUnit("A.java").getSource());		
		
		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/B.java");
		assertEquals("incorrect update of B", expectedSource, packP2.getCompilationUnit("B.java").getSource());
		
		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/C.java");
		assertEquals("incorrect update of C", expectedSource ,p2C.getSource());
		
		delete(packP1);
		delete(packP2);
	}

	
	public void test1() throws Exception{		
		final String p1Name= "p1";
		final String p3Name= "p3";
		final String inDir= "/in/";
		final String outDir= "/out/";
		
		IPackageFragment packP1= createPackage(p1Name);
		IPackageFragment packP3= createPackage(p3Name);
		ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
		ICompilationUnit p3B= createCu(packP3, getName() + inDir + p3Name + "/B.java", "B.java");
		
		String p2Name= "p2";
		IPackageFragment packP2= createPackage(p2Name);
		ICompilationUnit p2C= createCu(packP2, getName() + inDir + p2Name + "/C.java", "C.java");
		
		List elems= new ArrayList();
		elems.add(p1A);
		elems.add(p3B);
		MoveRefactoring ref= new MoveRefactoring(elems, JavaPreferencesSettings.getCodeGenerationSettings());
		ref.setDestination(packP2);
		ref.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(ref);
		
		//-- checks
		assertEquals("status should be ok here", null, status);		
		
		assertEquals("p1 files", 0, packP1.getChildren().length);
		assertEquals("p2 files", 3, packP2.getChildren().length);
		assertEquals("p1 files", 0, packP3.getChildren().length);
				
		String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/A.java");
		assertEquals("incorrect update of A", expectedSource, packP2.getCompilationUnit("A.java").getSource());
		
		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/B.java");
		assertEquals("incorrect update of B", expectedSource, packP2.getCompilationUnit("B.java").getSource());
		
		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/C.java");
		assertEquals("incorrect update of C", expectedSource ,p2C.getSource());
		
		delete(packP1);
		delete(packP2);
		delete(packP3);
	}
	
	public void test2() throws Exception{
		final String p1Name= "p1";
		final String inDir= "/in/";
		final String outDir= "/out/";
		
		IPackageFragment packP1= createPackage(p1Name);
		ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
		createCu(packP1, getName() + inDir + p1Name + "/B.java", "B.java");
		
		String p2Name= "p2";
		IPackageFragment packP2= createPackage(p2Name);
		ICompilationUnit p2C= createCu(packP2, getName() + inDir + p2Name + "/C.java", "C.java");
		
		List elems= new ArrayList();
		elems.add(p1A);
		MoveRefactoring ref= new MoveRefactoring(elems, JavaPreferencesSettings.getCodeGenerationSettings());
		ref.setDestination(packP2);
		ref.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(ref);
		
		//-- checks
		assertEquals("status should be ok here", null, status);		
		
		assertEquals("p1 files", 1, packP1.getChildren().length);
		assertEquals("p2 files", 2, packP2.getChildren().length);

		String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/A.java");
		assertEquals("incorrect update of A", expectedSource, packP2.getCompilationUnit("A.java").getSource());		
		
		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/B.java");
		assertEquals("incorrect update of B", expectedSource, packP1.getCompilationUnit("B.java").getSource());
		
		expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/C.java");
		assertEquals("incorrect update of C", expectedSource, p2C.getSource());
		
		delete(packP1);
		delete(packP2);
	}
	
}

