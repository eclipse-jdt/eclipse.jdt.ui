/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.reorg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
;

public class MultiMoveTest extends RefactoringTest {

	private static final Class clazz= MultiMoveTest.class;
	private static final String REFACTORING_PATH= "MultiMove/";

	public MultiMoveTest(String name) {
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
			if (pack != null && pack.exists())
				pack.delete(true, null);
		} catch(JavaModelException e) {
			//ignore, we should keep going
			e.printStackTrace();
		}
	}
	
	
	//--------
	public void test0() throws Exception{		
		IPackageFragment packP1= null;
		IPackageFragment packP2= null;
		try {
			final String p1Name= "p1";
			final String inDir= "/in/";
			final String outDir= "/out/";

			packP1= createPackage(p1Name);
			ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
			ICompilationUnit p1B= createCu(packP1, getName() + inDir + p1Name + "/B.java", "B.java");

			String p2Name= "p2";
			packP2= createPackage(p2Name);
			ICompilationUnit p2C= createCu(packP2, getName() + inDir + p2Name + "/C.java", "C.java");

//			List elems= new ArrayList();
//			elems.add(p1A);
//			elems.add(p1B);
			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A, p1B};
			JavaMoveProcessor processor= JavaMoveProcessor.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(packP2);
			processor.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			assertEquals("p1 files", 0, packP1.getChildren().length);
			assertEquals("p2 files", 3, packP2.getChildren().length);

			String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/A.java");
			assertEqualLines("incorrect update of A", expectedSource, packP2.getCompilationUnit("A.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/B.java");
			assertEqualLines("incorrect update of B", expectedSource, packP2.getCompilationUnit("B.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/C.java");
			assertEqualLines("incorrect update of C", expectedSource, p2C.getSource());
		} finally {
			delete(packP1);
			delete(packP2);		
		}
	}

	
	public void test1() throws Exception{		
		IPackageFragment packP1= null;
		IPackageFragment packP2= null;
		try {
			final String p1Name= "p1";
			final String inDir= "/in/";
			final String outDir= "/out/";

			packP1= createPackage(p1Name);
			ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
			ICompilationUnit p1B= createCu(packP1, getName() + inDir + p1Name + "/B.java", "B.java");

			String p2Name= "p2";
			packP2= createPackage(p2Name);
			ICompilationUnit p2C= createCu(packP2, getName() + inDir + p2Name + "/C.java", "C.java");

//			List elems= new ArrayList();
//			elems.add(p1A);
//			elems.add(p3B);
			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A, p1B};
			JavaMoveProcessor ref= JavaMoveProcessor.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
			ref.setReorgQueries(new MockReorgQueries());
			ref.setDestination(packP2);
			ref.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(ref, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			assertEquals("p1 files", 0, packP1.getChildren().length);
			assertEquals("p2 files", 3, packP2.getChildren().length);

			String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/A.java");
			assertEqualLines("incorrect update of A", expectedSource, packP2.getCompilationUnit("A.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/B.java");
			assertEqualLines("incorrect update of B", expectedSource, packP2.getCompilationUnit("B.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/C.java");
			assertEqualLines("incorrect update of C", expectedSource, p2C.getSource());
		} finally {
			delete(packP1);
			delete(packP2);
		}		
	}
	
	public void test2() throws Exception{
		IPackageFragment packP1= null;
		IPackageFragment packP2= null;
		try {
			final String p1Name= "p1";
			final String inDir= "/in/";
			final String outDir= "/out/";

			packP1= createPackage(p1Name);
			ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/A.java", "A.java");
			createCu(packP1, getName() + inDir + p1Name + "/B.java", "B.java");

			String p2Name= "p2";
			packP2= createPackage(p2Name);
			ICompilationUnit p2C= createCu(packP2, getName() + inDir + p2Name + "/C.java", "C.java");

//			List elems= new ArrayList();
//			elems.add(p1A);
			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A};
			JavaMoveProcessor ref= JavaMoveProcessor.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
			ref.setReorgQueries(new MockReorgQueries());
			ref.setDestination(packP2);
			ref.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(ref, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			assertEquals("p1 files", 1, packP1.getChildren().length);
			assertEquals("p2 files", 2, packP2.getChildren().length);

			String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/A.java");
			assertEqualLines("incorrect update of A", expectedSource, packP2.getCompilationUnit("A.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p1Name + "/B.java");
			assertEqualLines("incorrect update of B", expectedSource, packP1.getCompilationUnit("B.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/C.java");
			assertEqualLines("incorrect update of C", expectedSource, p2C.getSource());

		} finally {
			delete(packP1);
			delete(packP2);	
		}		
	}

	public void test3() throws Exception{		
		IPackageFragment packP1= null;
		IPackageFragment packP3= null;
		IPackageFragment packP2= null;
		try {
			final String p1Name= "p1";
			final String p3Name= "p3";
			final String inDir= "/in/";
			final String outDir= "/out/";

			packP1= createPackage(p1Name);
			packP3= createPackage(p3Name);
			ICompilationUnit p1A= createCu(packP1, getName() + inDir + p1Name + "/Outer.java", "Outer.java");
			createCu(packP3, getName() + inDir + p3Name + "/Test.java", "Test.java");

			String p2Name= "p2";
			packP2= createPackage(p2Name);

//			List elems= new ArrayList();
//			elems.add(p1A);
			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A};
			JavaMoveProcessor ref= JavaMoveProcessor.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
			ref.setReorgQueries(new MockReorgQueries());
			ref.setDestination(packP2);
			ref.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(ref, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			assertEquals("p1 files", 0, packP1.getChildren().length);
			assertEquals("p2 files", 1, packP2.getChildren().length);
			assertEquals("p1 files", 1, packP3.getChildren().length);

			String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/Outer.java");
			assertEqualLines("incorrect update of Outer", expectedSource, packP2.getCompilationUnit("Outer.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p3Name + "/Test.java");
			assertEqualLines("incorrect update of Test", expectedSource, packP3.getCompilationUnit("Test.java").getSource());

		} finally {
			delete(packP1);
			delete(packP2);
			delete(packP3);		
		}
	}
	
	private RefactoringStatus performRefactoring(JavaMoveProcessor processor, boolean providesUndo) throws Exception {
		return performRefactoring(new MoveRefactoring(processor), providesUndo);
	}	
}

