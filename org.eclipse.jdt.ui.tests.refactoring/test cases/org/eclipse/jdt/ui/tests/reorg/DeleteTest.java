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

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;


public class DeleteTest extends RefactoringTest{

	private static final Class clazz= DeleteTest.class;
	private static final String REFACTORING_PATH= "Delete/";

	public DeleteTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void verifyDisabled(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring ref= new DeleteRefactoring(processor);
		assertTrue("delete should be disabled", !ref.isAvailable());
	}

	private void verifyEnabled(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring ref= new DeleteRefactoring(processor);
		assertTrue("delete should be enabled", ref.isAvailable());
	}

	private IPackageFragmentRoot getArchiveRoot() throws JavaModelException, Exception {
		IPackageFragmentRoot[] roots= MySetup.getProject().getPackageFragmentRoots();
		IPackageFragmentRoot archive= null;
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (root.isArchive() && root.isExternal())
				archive= root;
		}
		return archive;
	}

	private ICompilationUnit fCuA;
	private static final String CU_NAME= "A";

	private void loadFileSetup() throws Exception{
		fCuA= createCUfromTestFile(getPackageP(), CU_NAME);
		assertTrue("A.java does not exist", fCuA.exists());
	}
	
	private void checkDelete(IJavaElement[] elems, boolean deleteCu) throws JavaModelException, Exception {
		ICompilationUnit newCuA= null;
		try {
			DeleteRefactoring refactoring= createRefactoring(elems);
			assertNotNull(refactoring);
			RefactoringStatus status= performRefactoring(refactoring, false);
			assertEquals("precondition was supposed to pass", null, status);

			newCuA= getPackageP().getCompilationUnit(CU_NAME + ".java");
			assertTrue("A.java does not exist", newCuA.exists() == !deleteCu);
			if (! deleteCu)
				assertEqualLines("incorrect content of A.java", getFileContents(getOutputTestFileName(CU_NAME)), newCuA.getSource());
		} finally {
			performDummySearch();
			if (newCuA != null && newCuA.exists())
				newCuA.delete(true, null);	
			if (fCuA != null && fCuA.exists()){
				fCuA.delete(true, null);		
				fCuA= null;
			}
		}	
	}

	//---- tests
	
	private IReorgQueries createReorgQueries() {
		return new MockReorgQueries();
	}

	public void testDisabled_emptySelection() throws Exception{
		verifyDisabled(new Object[] {});
	}

	public void testDisabled_projectAndNonProject() throws Exception{
		IJavaElement[] javaElements= {MySetup.getProject(), getPackageP()};
		verifyDisabled(javaElements);
	}

	public void testDisabled_nonExistingResource() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		
		IResource[] resources= {file};
		verifyDisabled(resources);			
	}
	
	public void testDisabled_nonExistingJavaElement() throws Exception{
		IJavaElement notExistingCu= getPackageP().getCompilationUnit("V.java");
		
		IJavaElement[] javaElements= {notExistingCu};
		verifyDisabled(javaElements);			
	}

	public void testDisabled_nullResource() throws Exception{
		Object[] elements= {MySetup.getProject(), null};
		verifyDisabled(elements);
	}
	
	public void testDisabled_nullJavaElement() throws Exception{
		Object[] elements= {getPackageP(), null};
		verifyDisabled(elements);
	}

	public void testDisabled_archiveElement() throws Exception{		
		IPackageFragmentRoot archive= getArchiveRoot();
		assertNotNull(archive);
		
		Object[] elements= archive.getChildren();
		verifyDisabled(elements);
	}

	public void testDisabled_externalArchive() throws Exception{
		IPackageFragmentRoot archive= getArchiveRoot();
		assertNotNull(archive);
		
		Object[] elements= {archive};
		verifyDisabled(elements);
	}

	public void testDisabled_archiveFromAnotherProject() throws Exception{
		//TODO implement me
	}

	public void testDisabled_emptySuperPackage() throws Exception{
		IPackageFragment superPackage= getRoot().createPackageFragment("superPackage", false, new NullProgressMonitor());
		IPackageFragment subPackage= getRoot().createPackageFragment("superPackage.subPackage", false, new NullProgressMonitor());
		try{
			assertTrue(superPackage.exists());
			assertTrue(subPackage.exists());
			assertTrue(superPackage.hasSubpackages());
		
			Object[] elements= {superPackage};
			verifyDisabled(elements);
		} finally {
			performDummySearch();
			subPackage.delete(true, new NullProgressMonitor());
			superPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testDisabled_binaryMember() throws Exception{
		//TODO implement me
	}

	public void testDisabled_javaProject() throws Exception{
		Object[] elements= {MySetup.getProject()};
		verifyDisabled(elements);
	}

	public void testEnabled_defaultPackage() throws Exception{
//		printTestDisabledMessage("enable this case once 38450 is fixed");
		
		IPackageFragment defaultPackage= getRoot().getPackageFragment("");
		ICompilationUnit cu= defaultPackage.createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		
		try{
			Object[] elements= {defaultPackage};
			verifyEnabled(elements);		
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}
	
	public void testDisabled_simpleProject() throws Exception{
		Object[] elements= {MySetup.getProject().getProject()};
		verifyDisabled(elements);
	}

	public void testEnabled_cu() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		
		try{		
			Object[] elements= {cu};
			verifyEnabled(elements);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}	

	public void testEnabled_sourceReferences1() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		try{
			IJavaElement importD= cu.createImport("java.lang.*", null, new NullProgressMonitor());
			IJavaElement packageD= cu.createPackageDeclaration("p", new NullProgressMonitor());
			IJavaElement type= cu.createType("class A{}", null, false, new NullProgressMonitor());
			
			Object[] elements= {packageD, importD, type};
			verifyEnabled(elements);			
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}
	
	public void testEnabled_sourceReferences2() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		try{
			IType type= cu.createType("class A{}", null, false, new NullProgressMonitor());
			IJavaElement field= type.createField("int i;", null, false, new NullProgressMonitor());
			IJavaElement method= type.createMethod("void f(){}", null, false, new NullProgressMonitor());
			IJavaElement initializer= type.createInitializer("{ int k= 0;}", null, new NullProgressMonitor());
			IJavaElement innerType= type.createType("class Inner{}", null, false,  new NullProgressMonitor());
			
			Object[] elements= {field, method, initializer, innerType};
			verifyEnabled(elements);			
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}	

	
	public void testEnabled_file() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		try{
			Object[] elements= {file};
			verifyEnabled(elements);			
		} finally{
			performDummySearch();
			file.delete(true, false, null);
		}
	}	

	public void testEnabled_folder() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		
		Object[] elements= {folder};
		verifyEnabled(elements);			
	}	

	public void testEnabled_readOnlyCu() throws Exception{
		//TODO implement me
	}	

	public void testEnabled_readOnlyFile() throws Exception{
		//TODO implement me
	}	
	
	public void testEnabled_package() throws Exception{
		Object[] elements= {getPackageP()};
		verifyEnabled(elements);
	}

	public void testEnabled_sourceFolder() throws Exception{
		Object[] elements= {getRoot()};
		verifyEnabled(elements);
	}	

	public void testEnabled_linkedFile() throws Exception{
		//TODO implement me
	}	
	
	public void testEnabled_linkedFolder() throws Exception{
		//TODO implement me
	}	

	public void testEnabled_linkedPackage() throws Exception{
		//TODO implement me
	}	

	public void testEnabled_linkedSourceFolder() throws Exception{
		//TODO implement me
	}

	public void testDeleteWithinCu0() throws Exception{
//		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu1() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, true);
	}
	
	public void testDeleteWithinCu2() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu3() throws Exception{
//		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
//		if (true)
//			return;
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement elem1= fCuA.getType("A").getField("j");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu4() throws Exception{
//		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
//		if (true)
//			return;
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement elem1= fCuA.getType("A").getField("k");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu5() throws Exception{
//		printTestDisabledMessage("bug#15305 incorrect deletion of fields (multi-declaration case)");
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
//		if (true)
//			return;
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("j");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu6() throws Exception{
//		printTestDisabledMessage("test for bug#8405 Delete field action broken for multiple declarations");		
//		printTestDisabledMessage("test for bug#9382 IField::delete incorrect on multiple field declarations with initializers");		
//		if (true)
//			return;
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("j");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu7() throws Exception{
		//exposes bug#9381 IPackageDeclaration is not ISourceManipulation 
		loadFileSetup();
		IJavaElement elem0= fCuA.getPackageDeclaration("p");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}
	
	public void testDeleteWithinCu8() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getMethod("m", new String[0]);
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu9() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getInitializer(1);
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu10() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getInitializer(1);
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu11() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getImport("java.util.List");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu12() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getType("B");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}
	
	public void testDeleteWithinCu13() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getType("B");
		IJavaElement elem1= fCuA.getType("A");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1};

		checkDelete(elems, true);
	}

	public void testDeleteWithinCu14() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getType("B");
		IJavaElement elem1= fCuA.getType("A");
		IJavaElement elem2= fCuA.getPackageDeclaration("p");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1, elem2};

		checkDelete(elems, true);
	}

	public void testDeleteWithinCu15() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("field");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu16() throws Exception{
		if (true) {
			printTestDisabledMessage("testDeleteWithinCu16 desaibled for bug#55221");		
			return;
		}
		
		
//		printTestDisabledMessage("test for bug#15412 deleting type removes too much from editor");		
//		if (true)
//			return;
		
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("Test");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu17() throws Exception{
//		printTestDisabledMessage("test for bug#15936 delete methods deletes too much (indent messing)");		
//		if (true)
//			return;
		
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getMethod("f", new String[0]);
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu18() throws Exception{
//		printTestDisabledMessage("test for bug#16314");		
//		if (true)
//			return;
		
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getMethod("fs", new String[0]);
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}
	
	public void testDeleteWithinCu19() throws Exception{
		loadFileSetup();
		IJavaElement elem0= fCuA.getImportContainer();
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu20() throws Exception{
//		printTestDisabledMessage("regression test for bug 38480");
		
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("fEmpty");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}

	public void testDeleteWithinCu21() throws Exception{
//		printTestDisabledMessage("regression test for bug 39195");
		
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("var11");
		IJavaElement[] elems= new IJavaElement[]{elem0};

		checkDelete(elems, false);
	}
	
	public void testDeleteFile() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		assertTrue("file does not exist", file.exists());
		Object[] elem= {file};
		verifyEnabled(elem);			
		performDummySearch();			
		
		DeleteRefactoring ref= createRefactoring(elem);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("file not deleted", ! file.exists());
	}

	public void testDeleteFolder() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFolder subFolder= folder.getFolder("subFolder");
		subFolder.create(true, true, null);

		assertTrue("folder does not exist", subFolder.exists());
		Object[] elements= {subFolder};
		verifyEnabled(elements);			
		performDummySearch();			

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("folder not deleted", ! subFolder.exists());
	}

	public void testDeleteNestedFolders() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFolder subFolder= folder.getFolder("subFolder");
		subFolder.create(true, true, null);
		IFolder subsubFolder= subFolder.getFolder("subSubFolder");
		subsubFolder.create(true, true, null);

		assertTrue("folder does not exist", subFolder.exists());
		assertTrue("folder does not exist", subsubFolder.exists());
		Object[] elements= {subFolder, subsubFolder};
		verifyEnabled(elements);			
		performDummySearch();			

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("folder not deleted", ! subFolder.exists());
		assertTrue("folder not deleted", ! subsubFolder.exists());
	}
	
	public void testDeletePackage() throws Exception{
		IPackageFragment newPackage= getRoot().createPackageFragment("newPackage", true, new NullProgressMonitor());
		assertTrue("package not created", newPackage.exists());

		Object[] elements= {newPackage};
		verifyEnabled(elements);			
		performDummySearch();			
		
		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("package not deleted", ! newPackage.exists());
	}

	public void testDeleteCu() throws Exception{
		ICompilationUnit newCU= getPackageP().createCompilationUnit("X.java", "package p; class X{}", true, new NullProgressMonitor());
		assertTrue("cu not created", newCU.exists());

		Object[] elements= {newCU};
		verifyEnabled(elements);			
		performDummySearch();			
		
		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("cu not deleted", ! newCU.exists());
	}
	
	public void testDeleteSourceFolder() throws Exception{
		IPackageFragmentRoot fredRoot= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "fred");
		assertTrue("not created", fredRoot.exists());

		Object[] elements= {fredRoot};
		verifyEnabled(elements);			
		performDummySearch();			

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("not deleted", ! fredRoot.exists());
	}
	
	public void testDeleteInternalJAR() throws Exception{
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		assertTrue("lib does not exist",  lib != null && lib.exists());
		IPackageFragmentRoot internalJAR= JavaProjectHelper.addLibraryWithImport(MySetup.getProject(), new Path(lib.getPath()), null, null);

		Object[] elements= {internalJAR};
		verifyEnabled(elements);			
		performDummySearch();			

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, false);
		assertEquals("expected to pass", null, status);
		assertTrue("not deleted", ! internalJAR.exists());		
	}
	
	public void testDeleteClassFile() throws Exception{
		//TODO implement me - how do i get a handle to a class file?
	}
	
	private DeleteRefactoring createRefactoring(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring result= new DeleteRefactoring(processor);
		processor.setQueries(createReorgQueries());
		return result;		
	}	
}
