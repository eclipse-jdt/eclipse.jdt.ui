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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.MoveRefactoring2;


public class MoveTest extends RefactoringTest {

	public MoveTest(String name) {
		super(name);
	}

	private static final Class clazz= MoveTest.class;
	private static final String REFACTORING_PATH= "Move/";

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void verifyDisabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		assertTrue("move should be disabled", ! MoveRefactoring2.isAvailable(resources, javaElements, settings));
		MoveRefactoring2 refactoring2= MoveRefactoring2.create(resources, javaElements, settings);
		assertTrue(refactoring2 == null);
	}
	
	private MoveRefactoring2 verifyEnabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
		assertTrue("move should be enabled", MoveRefactoring2.isAvailable(resources, javaElements, settings));
		MoveRefactoring2 refactoring2= MoveRefactoring2.create(resources, javaElements, settings);
		assertNotNull(refactoring2);
		return refactoring2;
	}
	
	private void verifyInvalidDestination(MoveRefactoring2 ref, Object destination) throws Exception {
		RefactoringStatus status= null;
		if (destination instanceof IResource)
			status= ref.setDestination((IResource)destination);
		else if (destination instanceof IJavaElement)
			status= ref.setDestination((IJavaElement)destination);
		else assertTrue(false);
		
		assertEquals("destination was expected to be not valid",  RefactoringStatus.FATAL, status.getSeverity());
	}


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
			IMethod	methodFoo= classA.getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { methodFoo, classA};
			IResource[] resources= {};
			verifyDisabled(resources, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_no_fileToItsef() throws Exception {
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			file.delete(true, false, null);
		}
	}
	
	public void testDestination_no_fileToSiblingFile() throws Exception {
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file1= superFolder.getFile("a.txt");
		file1.create(getStream("123"), true, null);
		IFile file2= superFolder.getFile("b.txt");
		file2.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file1};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file2;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			file1.delete(true, false, null);
			file2.delete(true, false, null);
		}
	}

	public void testDestination_no_folderToItsef() throws Exception {
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, false, null);
		}
	}

	public void testDestination_no_cuToItsef() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
		}
	}

	public void testDestination_no_cuToSiblingCu() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		ICompilationUnit cu1= getPackageP().createCompilationUnit("B.java", "package p;class A{}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu1;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
			cu1.delete(true, null);
		}
	}

	public void testDestination_no_cuToSiblingFile() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file1= superFolder.getFile("a.txt");
		file1.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file1;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
			file1.delete(true, null);
		}
	}
	
	public void testDestination_no_packageToItsef() throws Exception {
		IJavaElement[] javaElements= {getPackageP()};
		IResource[] resources= {};
		MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getPackageP();
		verifyInvalidDestination(ref, destination);
	}
	
	public void testDestination_no_sourceFolderToItsef() throws Exception {
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getRoot();
		verifyInvalidDestination(ref, destination);
	}
	
	public void testDestination_no_methodToItsef() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= method;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
		}
	}

	public void testDestination_no_fileToParentFolder() throws Exception {
		IProject superFolder= MySetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		IFile file= folder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, false, null);
			file.delete(true, false, null);
		}
	}

	public void testDestination_no_fileToParentPackage() throws Exception {
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getPackageP();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			file.delete(true, false, null);
		}
	}

	public void testDestination_no_fileToParentSourceFolder() throws Exception {
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= getRoot();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			file.delete(true, false, null);
		}
	}

	public void testDestination_no_folderToParentFolder() throws Exception {
		IProject superFolder= MySetup.getProject().getProject();

		IFolder parentFolder= superFolder.getFolder("folder");
		parentFolder.create(true, true, null);
		IFolder folder= parentFolder.getFolder("subfolder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {parentFolder};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= parentFolder;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			parentFolder.delete(true, false, null);
		}
	}

	public void testDestination_no_cuToParentPackage() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu.getParent();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
		}
	}
	
	public void testDestination_no_packageToParentSourceFolder() throws Exception {
		IJavaElement[] javaElements= {getPackageP()};
		IResource[] resources= {};
		MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getRoot();
		verifyInvalidDestination(ref, destination);
	}
	
	public void testDestination_no_sourceFolderToParentProject() throws Exception {
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

		Object destination= getRoot().getParent();
		verifyInvalidDestination(ref, destination);
	}
	
	public void testDestination_no_methodToParentType() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu.getType("A");
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
		}
	}

	public void testDestination_no_fileToMethod() throws Exception {
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= method;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			file.delete(true, false, null);
			cu.delete(true, null);
		}
	}

	public void testDestination_no_cuToMethod() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		ICompilationUnit cu1= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {cu1};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= method;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, null);
			cu1.delete(true, null);
		}
	}
	
	public void testDestination_no_packageToCu() throws Exception {
		IPackageFragment pack1= getRoot().createPackageFragment("q", true, new NullProgressMonitor());
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {pack1};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= cu;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			pack1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_no_packageToFile() throws Exception {
		IPackageFragment pack1= getRoot().createPackageFragment("q", true, new NullProgressMonitor());
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {pack1};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			pack1.delete(true, new NullProgressMonitor());
			file.delete(true, false, null);	
		}
	}

	public void testDestination_no_packageToFolder() throws Exception {
		IPackageFragment pack1= getRoot().createPackageFragment("q", true, new NullProgressMonitor());
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {pack1};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			pack1.delete(true, new NullProgressMonitor());
			folder.delete(true, false, null);	
		}
	}

	public void testDestination_no_packageToSimpleProject() throws Exception {
		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);
		
		IPackageFragment pack1= getRoot().createPackageFragment("q", true, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {pack1};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= simpleProject;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			pack1.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, true, null);	
		}
	}

//	public void testDestination_no_packageToJavaProjectWithNoSourceFolders() throws Exception {
//		IJavaProject otherProject= JavaProjectHelper.createJavaProject("otherProject", null);
//		JavaProjectHelper.addSourceContainer(otherProject, null);
//		IPackageFragment pack1= getRoot().createPackageFragment("q", true, new NullProgressMonitor());
//		try{
//			IJavaElement[] javaElements= {pack1};
//			IResource[] resources= {};
//			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
//
//			Object destination= otherProject;
//			verifyInvalidDestination(ref, destination);
//		} finally{
//			performDummySearch();
//			pack1.delete(true, new NullProgressMonitor());
//			JavaProjectHelper.delete(otherProject);
//		}
//	}
	
	public void testDestination_no_packageToPackage() throws Exception {
		IPackageFragment pack1= getRoot().createPackageFragment("q", true, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {getPackageP()};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);

			Object destination= pack1;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			pack1.delete(true, new NullProgressMonitor());
		}
	}
	
	public void testDestination_no_sourceFolderToCu() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= cu;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			sourceFolder.delete(0, 0, new NullProgressMonitor());
		}
	}

	public void testDestination_no_sourceFolderToPackage() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= getPackageP();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			sourceFolder.delete(0, 0, new NullProgressMonitor());
		}
	}

	public void testDestination_no_sourceFolderToFile() throws Exception {
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			sourceFolder.delete(0, 0, new NullProgressMonitor());
			file.delete(true, false, null);	
		}
	}

	public void testDestination_no_sourceFolderToFolder() throws Exception {
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			sourceFolder.delete(0, 0, new NullProgressMonitor());
			folder.delete(true, false, null);	
		}
	}

	public void testDestination_no_sourceFolderToSourceFolder() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= getRoot();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			sourceFolder.delete(0, 0, new NullProgressMonitor());
		}
	}

	public void testDestination_no_sourceFolderToSimpleProject() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);

		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= simpleProject;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			sourceFolder.delete(0, 0, new NullProgressMonitor());
			simpleProject.delete(true, true, null);
		}
	}

	public void testDestination_no_sourceFolderToJavaProjecteWithNoSourceFolder() throws Exception {
		IJavaProject otherProject= JavaProjectHelper.createJavaProject("otherProject", null);
		JavaProjectHelper.addSourceContainer(otherProject, null);
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(MySetup.getProject(), "src2");
		
		try{
			IJavaElement[] javaElements= {sourceFolder};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= otherProject;
			verifyInvalidDestination(ref, destination);
		}finally{
			performDummySearch();
			sourceFolder.delete(0, 0, new NullProgressMonitor());
			JavaProjectHelper.delete(otherProject);
		} 
	}
	
	public void testDestination_no_methodToCu() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		ICompilationUnit cu1= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= cu1;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			cu1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_no_methodToFile() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			file.delete(true, false, null);
		}
	}

	public void testDestination_no_methodToFolder() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IFolder superFolder= (IFolder)getRoot().getResource();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			folder.delete(true, false, null);
		}
	}
	
	public void testDestination_no_methodToPackage() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= getPackageP();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_no_methodToSourceFolder() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= getRoot();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_no_methodToJavaProject() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= MySetup.getProject();
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_no_methodToSimpleProject() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);
		try{
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= {method};
			IResource[] resources= {};
			MoveRefactoring2 ref= verifyEnabled(resources, javaElements);
	
			Object destination= simpleProject;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, true, null);
		}
	}
}
