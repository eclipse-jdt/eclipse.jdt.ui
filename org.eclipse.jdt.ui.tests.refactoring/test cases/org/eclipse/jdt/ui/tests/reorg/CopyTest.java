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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.CopyRefactoring2;


public class CopyTest extends RefactoringTest {

	private static final Class clazz= CopyTest.class;
	private static final String REFACTORING_PATH= "Copy/";

	public CopyTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void verifyDisabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		assertTrue("delete should be disabled", ! CopyRefactoring2.isAvailable(resources, javaElements));
		CopyRefactoring2 refactoring2= CopyRefactoring2.create(resources, javaElements);
		assertTrue(refactoring2 == null);
	}

	private CopyRefactoring2 verifyEnabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		assertTrue("delete should be enabled", CopyRefactoring2.isAvailable(resources, javaElements));
		CopyRefactoring2 refactoring2= CopyRefactoring2.create(resources, javaElements);
		assertNotNull(refactoring2);
		return refactoring2;
	}

	private void verifyInvalidDestination(CopyRefactoring2 ref, Object destination) throws Exception {
		RefactoringStatus status= null;
		if (destination instanceof IResource)
			status= ref.setDestination((IResource)destination);
		else if (destination instanceof IJavaElement)
			status= ref.setDestination((IJavaElement)destination);
		else assertTrue(false);
		
		assertEquals("destination was expected to be not valid",  RefactoringStatus.FATAL, status.getSeverity());
	}
	
	private void verifyValidDestination(CopyRefactoring2 ref, Object destination) throws Exception {
		RefactoringStatus status= null;
		if (destination instanceof IResource)
			status= ref.setDestination((IResource)destination);
		else if (destination instanceof IJavaElement)
			status= ref.setDestination((IJavaElement)destination);
		else assertTrue(false);
		
		assertEquals("destination was expected to be valid: " + status.getFirstMessage(status.getSeverity()), RefactoringStatus.OK, status.getSeverity());
	}

	//---------------
	public void testDisabled_empty() throws Exception {
		IJavaElement[] javaElements= {};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}
	
	public void testDisabled_null_element() throws Exception {
		IJavaElement[] javaElements= {null};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_null_resource() throws Exception {
		IJavaElement[] javaElements= {};
		IResource[] resources= {null};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_javaProject() throws Exception {
		IJavaElement[] javaElements= {MySetup.getProject()};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_defaultPackage() throws Exception {
		IPackageFragment defaultPackage= getRoot().getPackageFragment("");
		assertTrue(defaultPackage.exists());
		IJavaElement[] javaElements= {defaultPackage};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_project() throws Exception {
		IJavaElement[] javaElements= {};
		IResource[] resources= {MySetup.getProject().getProject()};
		verifyDisabled(resources, javaElements);
	}

	public void testDisabled_notExistingElement() throws Exception {
		ICompilationUnit notExistingCu= getPackageP().getCompilationUnit("NotMe.java");
		assertTrue(! notExistingCu.exists());
		IJavaElement[] javaElements= {notExistingCu};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);		
	}

	public void testDisabled_notExistingResource() throws Exception {
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile notExistingFile= folder.getFile("a.txt");
		
		IJavaElement[] javaElements= {};
		IResource[] resources= {notExistingFile};
		verifyDisabled(resources, javaElements);
	}
	
	public void testDisabled_noCommonParent0() throws Exception {
		IJavaElement[] javaElements= {getPackageP(), getRoot()};
		IResource[] resources= {};
		verifyDisabled(resources, javaElements);		
	}
	
	public void testDisabled_noCommonParent1() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
		try {
			IType classA= cu.getType("A");
			IMethod	methodFoo= classA.getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { classA, methodFoo };
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}		
	}

	public void testDisabled_noCommonParent2() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
		try {
			IType classA= cu.getType("A");
			IJavaElement[] javaElements= { classA, cu};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}		
	}

	public void testDisabled_noCommonParent3() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= {cu, getPackageP()};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}		
	}

	public void testDisabled_noCommonParent5() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= {cu, getRoot()};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}		
	}

	public void testDisabled_noCommonParent6() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= {cu, getRoot()};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}		
	}

	public void testDisabled_noCommonParent7() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{class Inner{}}", false, new NullProgressMonitor());
		try {
			IType classA= cu.getType("A");
			IType classInner= classA.getType("Inner");
			IJavaElement[] javaElements= { classA, classInner};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}
	
	public void testDisabled_noCommonParent8() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try {
			IType classA= cu.getType("A");
			IType classInner= classA.getType("Inner");
			IMethod	methodFoo= classA.getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { methodFoo, classInner};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testEnabled_cu() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= { cu};
			IResource[] resources= {};
			verifyEnabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}		
	}
	
	public void testEnabled_package() throws Exception {
		IJavaElement[] javaElements= { getPackageP()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);
	}	
	
	public void testEnabled_packageRoot() throws Exception {
		IJavaElement[] javaElements= { getRoot()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements);
	}	

	public void testEnabled_file() throws Exception {
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

	public void testEnabled_fileFolder() throws Exception {
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file, folder};
			verifyEnabled(resources, javaElements);			
		} finally{
			performDummySearch();
			file.delete(true, false, null);
			folder.delete(true, false, null);
		}
	}	

	public void testEnabled_fileFolderCu() throws Exception {
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file, folder};
			verifyEnabled(resources, javaElements);			
		} finally{
			performDummySearch();
			file.delete(true, false, null);
			folder.delete(true, false, null);
			cu.delete(true, new NullProgressMonitor());
		}
	}
	
	public void testDestination_package_no_0() throws Exception{
		IJavaElement[] javaElements= { getPackageP()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);
		verifyInvalidDestination(ref, getPackageP());
	}

	public void testDestination_package_no_1() throws Exception{
		IJavaElement[] javaElements= { getPackageP()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);
		verifyInvalidDestination(ref, MySetup.getProject());
	}

	public void testDestination_package_no_2() throws Exception{		
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination=cu;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_package_no_3() throws Exception{		
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			file.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_package_no_4() throws Exception{		
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_package_no_5() throws Exception{		
		IPackageFragment otherPackage= getRoot().createPackageFragment("other.pack", true, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= otherPackage;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			otherPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_no_0() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		ICompilationUnit cu2= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			IType classB= cu2.getType("B");
			Object destination= classB;
			verifyInvalidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			cu2.delete(true, new NullProgressMonitor());
		}
	}
	
	public void testDestination_file_no_0() throws Exception{
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		ICompilationUnit cu2= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			IType classB= cu2.getType("B");			
			Object destination= classB;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			file.delete(true, new NullProgressMonitor());			
			cu2.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_no_0() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;//same folder
			verifyInvalidDestination(ref, destination);	
		}finally{
			performDummySearch();
			folder.delete(true, false, null);
		}
	}

	public void testDestination_folder_no_1() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFolder childFolder= folder.getFolder("folder");
		childFolder.create(true, true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= childFolder;
			verifyInvalidDestination(ref, destination);			
		}finally{
			performDummySearch();
			folder.delete(true, false, null);
			childFolder.delete(true, false, null);
		}
	}
	
	public void testDestination_folder_no_2() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFile childFile= folder.getFile("a.txt");
		childFile.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= childFile;
			verifyInvalidDestination(ref, destination);			
		}finally{
			performDummySearch();
			folder.delete(true, false, null);
			childFile.delete(true, false, null);
		}
	}
	
	public void testDestination_root_no_0() throws Exception{
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getPackageP();
		verifyInvalidDestination(ref, destination);			
	}

	public void testDestination_root_no_1() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		
		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}			
	}

	public void testDestination_root_no_2() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		
		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			IType classB= cu.getType("B");
			Object destination= classB;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}			
	}
	
	public void testDestination_root_no_3() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}			
	}

	public void testDestination_root_no_4() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_root_no_5() throws Exception{
		IJavaElement[] javaElements= { getRoot()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getRoot();
		verifyInvalidDestination(ref, destination);
	}
	
	public void testDestination_cu_yes_0() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);
			Object destination= cu1;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_yes_1() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getPackageP();
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_yes_2() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IPackageFragment otherPackage= getRoot().createPackageFragment("otherPackage", true, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= otherPackage;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			otherPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_yes_3() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getRoot();
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_yes_4() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= MySetup.getProject();
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_yes_5() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= simpleProject;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, true, null);
		}
	}

	public void testDestination_cu_yes_6() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			file.delete(true, false, null);
		}
	}

	public void testDestination_cu_yes_7() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			folder.delete(true, false, null);
		}
	}

	public void testDestination_file_yes_0() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}		

	public void testDestination_file_yes_1() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		IFile otherFile= superFolder.getFile("b.txt");
		otherFile.create(getStream("123"), true, null);
		
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= otherFile;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			otherFile.delete(true, new NullProgressMonitor());
		}
	}		

	public void testDestination_file_yes_3() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			folder.delete(true, new NullProgressMonitor());
		}
	}		

	public void testDestination_file_yes_4() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());			
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu1;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			cu1.delete(true, new NullProgressMonitor());
		}
	}		
	
	public void testDestination_file_yes_5() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getPackageP();
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_6() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getRoot();
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}		

	public void testDestination_file_yes_7() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= MySetup.getProject();
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}		

	public void testDestination_file_yes_8() throws Exception{
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		IFile file= parentFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= parentFolder;
			verifyValidDestination(ref, destination);			
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}
			
	public void testDestination_folder_yes_0() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFolder otherFolder= superFolder.getFolder("otherfolder");
		otherFolder.create(true, true, null);
		
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= otherFolder;
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
			otherFolder.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_folder_yes_1() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= MySetup.getProject();
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_folder_yes_2() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getRoot();
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_folder_yes_3() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getPackageP();
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
		}
	}
	
	public void testDestination_folder_yes_4() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFolder otherFolder= superFolder.getFolder("otherfolder");
		otherFolder.create(true, true, null);
		IFile fileInAnotherFolder= otherFolder.getFile("f.tex");
		fileInAnotherFolder.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= fileInAnotherFolder;
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
		}
	}

	public void testDestination_folder_yes_5() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu;
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_6() throws Exception{
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= simpleProject;
			verifyValidDestination(ref, destination);						
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());			
			simpleProject.delete(true, true, new NullProgressMonitor());
		}
	}

	public void testDestination_package_yes_0() throws Exception{
		IJavaElement[] javaElements= {getPackageP()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getRoot();
		verifyValidDestination(ref, destination);						
	}

	public void testDestination_root_yes_0() throws Exception{
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getRoot().getJavaProject();
		verifyValidDestination(ref, destination);
	}
	
	public void testDestination_root_yes_1() throws Exception{
		//TODO implement me
		if (true) return;
		IJavaProject otherJavaProject= null;
		
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		CopyRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= otherJavaProject;
		verifyValidDestination(ref, destination);						
	}

}