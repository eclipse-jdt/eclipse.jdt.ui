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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.DeleteRefactoring2;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.IConfirmQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.IReorgQueries;


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

	private void verifyDisabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		assertTrue("delete should be disabled", ! DeleteRefactoring2.isAvailable(resources, javaElements));
	}

	private void verifyEnabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		assertTrue("delete should be enabled", DeleteRefactoring2.isAvailable(resources, javaElements));
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
			DeleteRefactoring2 refactoring= DeleteRefactoring2.create(new IResource[0], elems);
			assertNotNull(refactoring);
			refactoring.setQueries(createReorgQueries());
			RefactoringStatus status= performRefactoring(refactoring);
			assertEquals("precondition was supposed to pass", null, status);

			newCuA= getPackageP().getCompilationUnit(CU_NAME + ".java");
			assertTrue("A.java does not exist", newCuA.exists() == !deleteCu);
			if (! deleteCu)
				assertEquals("incorrect content of A.java", getFileContents(getOutputTestFileName(CU_NAME)), newCuA.getSource());
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
	
	public void testDisabled_emptySelection() throws Exception{
		IJavaElement[] javaElements= {};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_projectAndNonProject() throws Exception{
		IJavaElement[] javaElements= {MySetup.getProject(), getPackageP()};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_nonExistingResource() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		
		IJavaElement[] javaElements= {};
		IResource[] resources= {file};
		verifyDisabled(resources, javaElements);			
	}
	
	public void testDisabled_nonExistingJavaElement() throws Exception{
		IJavaElement notExistingCu= getPackageP().getCompilationUnit("V.java");
		
		IJavaElement[] javaElements= {notExistingCu};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);			
	}

	public void testDisabled_nullResource() throws Exception{
		IJavaElement[] javaElements= {MySetup.getProject()};
		IResource[] resources= {null};
		verifyDisabled(resources, javaElements);
	}
	
	public void testDisabled_nullJavaElement() throws Exception{
		IJavaElement[] javaElements= {getPackageP(), null};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_archiveElement() throws Exception{		
		IPackageFragmentRoot archive= getArchiveRoot();
		assertNotNull(archive);
		
		IJavaElement[] javaElements= archive.getChildren();
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_externalArchive() throws Exception{
		IPackageFragmentRoot archive= getArchiveRoot();
		assertNotNull(archive);
		
		IJavaElement[] javaElements= {archive};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
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
		
			IJavaElement[] javaElements= {superPackage};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
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
		IJavaElement[] javaElements= {MySetup.getProject()};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_simpleProject() throws Exception{
		IJavaElement[] javaElements= {};
		IResource[] resources= {MySetup.getProject().getProject()};
		verifyDisabled(resources, javaElements);
	}

	public void testEnabled_cu() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		
		try{		
			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			verifyEnabled(resources, javaElements);
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
			
			IJavaElement[] javaElements= {packageD, importD, type};
			IResource[] resources= {};
			verifyEnabled(resources, javaElements);			
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
			
			IJavaElement[] javaElements= {field, method, initializer, innerType};
			IResource[] resources= {};
			verifyEnabled(resources, javaElements);			
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
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			verifyEnabled(resources, javaElements);			
		} finally{
			performDummySearch();
			file.delete(true, false, null);
		}
	}	

	public void testEnabled_folder() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		
		IJavaElement[] javaElements= {};
		IResource[] resources= {folder};
		verifyEnabled(resources, javaElements);			
	}	

	public void testEnabled_readOnlyCu() throws Exception{
		//TODO implement me
	}	

	public void testEnabled_readOnlyFile() throws Exception{
		//TODO implement me
	}	
	
	public void testEnabled_package() throws Exception{
		IJavaElement[] javaElements= {getPackageP()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);
	}

	public void testEnabled_sourceFolder() throws Exception{
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);
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

	private IReorgQueries createReorgQueries() {
		final IConfirmQuery yesQuery= new IConfirmQuery(){
			public boolean confirm(String question) throws OperationCanceledException {
				return true;
			}
			public boolean confirm(String question, Object[] elements) throws OperationCanceledException {
				return true;
			}
		};
		return new IReorgQueries(){
			public IConfirmQuery createYesNoQuery(String queryTitle, int queryID) {
				return yesQuery;
			}
			public IConfirmQuery createYesYesToAllNoNoToAllQuery(String queryTitle, int queryID) {
				return yesQuery;
			}
		};
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
	
	public void testDeleteFile() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		assertTrue("file does not exist", file.exists());
		IJavaElement[] javaElements= {};
		IResource[] resources= {file};
		verifyEnabled(resources, javaElements);			
		performDummySearch();			

		DeleteRefactoring2 ref= DeleteRefactoring2.create(resources, javaElements);
		ref.setQueries(createReorgQueries());
		RefactoringStatus status= performRefactoring(ref);
		assertEquals("expected to pass", null, status);
		assertTrue("file not deleted", ! file.exists());
	}

	public void testDeleteFolder() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFolder subFolder= folder.getFolder("subFolder");
		subFolder.create(true, true, null);

		assertTrue("folder does not exist", subFolder.exists());
		IJavaElement[] javaElements= {};
		IResource[] resources= {subFolder};
		verifyEnabled(resources, javaElements);			
		performDummySearch();			

		DeleteRefactoring2 ref= DeleteRefactoring2.create(resources, javaElements);
		ref.setQueries(createReorgQueries());
		RefactoringStatus status= performRefactoring(ref);
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
		IJavaElement[] javaElements= {};
		IResource[] resources= {subFolder, subsubFolder};
		verifyEnabled(resources, javaElements);			
		performDummySearch();			

		DeleteRefactoring2 ref= DeleteRefactoring2.create(resources, javaElements);
		ref.setQueries(createReorgQueries());
		RefactoringStatus status= performRefactoring(ref);
		assertEquals("expected to pass", null, status);
		assertTrue("folder not deleted", ! subFolder.exists());
		assertTrue("folder not deleted", ! subsubFolder.exists());
	}
	
	public void testDeletePackage() throws Exception{
		IPackageFragment newPackage= getRoot().createPackageFragment("newPackage", true, new NullProgressMonitor());
		assertTrue("package not created", newPackage.exists());

		IJavaElement[] javaElements= {newPackage};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);			
		performDummySearch();			
		
		DeleteRefactoring2 ref= DeleteRefactoring2.create(resources, javaElements);
		ref.setQueries(createReorgQueries());
		RefactoringStatus status= performRefactoring(ref);
		assertEquals("expected to pass", null, status);
		assertTrue("package not deleted", ! newPackage.exists());
	}

	public void testDeleteCu() throws Exception{
		ICompilationUnit newCU= getPackageP().createCompilationUnit("X.java", "package p; class X{}", true, new NullProgressMonitor());
		assertTrue("cu not created", newCU.exists());

		IJavaElement[] javaElements= {newCU};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);			
		performDummySearch();			
		
		DeleteRefactoring2 ref= DeleteRefactoring2.create(resources, javaElements);
		ref.setQueries(createReorgQueries());
		RefactoringStatus status= performRefactoring(ref);
		assertEquals("expected to pass", null, status);
		assertTrue("cu not deleted", ! newCU.exists());
	}
	
	public void testDeleteSourceFolder() throws Exception{
		IPackageFragmentRoot fredRoot= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "fred");
		assertTrue("not created", fredRoot.exists());

		IJavaElement[] javaElements= {fredRoot};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);			
		performDummySearch();			

		DeleteRefactoring2 ref= DeleteRefactoring2.create(resources, javaElements);
		ref.setQueries(createReorgQueries());
		RefactoringStatus status= performRefactoring(ref);
		assertEquals("expected to pass", null, status);
		assertTrue("not deleted", ! fredRoot.exists());
	}
}
