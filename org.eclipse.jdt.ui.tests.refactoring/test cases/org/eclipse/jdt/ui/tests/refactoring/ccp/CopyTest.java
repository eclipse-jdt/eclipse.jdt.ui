/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.INewNameQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestination;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.util.JavaElementResourceMapping;

import org.eclipse.jdt.ui.tests.refactoring.ParticipantTesting;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.TestModelProvider;


public class CopyTest extends RefactoringTest {

	private static final Class clazz= CopyTest.class;
	private static final String REFACTORING_PATH= "Copy/";

	public CopyTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
	}

	protected void executePerformOperation(PerformChangeOperation perform, IWorkspace workspace) throws CoreException {
		if (fIsPreDeltaTest) {
			try {
				TestModelProvider.IS_COPY_TEST= true;
				super.executePerformOperation(perform, workspace);
			} finally {
				TestModelProvider.IS_COPY_TEST= false;
			}
		} else {
			super.executePerformOperation(perform, workspace);
		}
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void verifyDisabled(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		assertTrue("copy should be disabled", ! RefactoringAvailabilityTester.isCopyAvailable(resources, javaElements));
		assertTrue(!ReorgPolicyFactory.createCopyPolicy(resources, javaElements).canEnable());
	}

	private JavaCopyProcessor verifyEnabled(IResource[] resources, IJavaElement[] javaElements, INewNameQueries newNameQueries, IReorgQueries reorgQueries) throws JavaModelException {
		assertTrue("copy should be enabled", RefactoringAvailabilityTester.isCopyAvailable(resources, javaElements));
		ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements);
		assertTrue(copyPolicy.canEnable());
		JavaCopyProcessor processor= new JavaCopyProcessor(copyPolicy);
		if (newNameQueries != null)
			processor.setNewNameQueries(newNameQueries);
		if (reorgQueries != null)
			processor.setReorgQueries(reorgQueries);
		return processor;
	}

	private IReorgQueries createReorgQueries(){
		return new MockReorgQueries();
	}

	private void verifyInvalidDestination(JavaCopyProcessor processor, Object destination) throws Exception {
		RefactoringStatus status= processor.setDestination(ReorgDestinationFactory.createDestination(destination));

		assertEquals("destination was expected to be not valid",  RefactoringStatus.FATAL, status.getSeverity());
	}

	private void verifyValidDestination(JavaCopyProcessor processor, Object destination) throws Exception {
		verifyValidDestination(processor, destination, IReorgDestination.LOCATION_ON);
	}

	private void verifyValidDestination(JavaCopyProcessor processor, Object destination, int location) throws Exception {
		RefactoringStatus status= processor.setDestination(ReorgDestinationFactory.createDestination(destination, location));

		assertEquals("destination was expected to be valid: " + status.getMessageMatchingSeverity(status.getSeverity()), RefactoringStatus.OK, status.getSeverity());
	}

	private void verifyCopyingOfSubCuElements(ICompilationUnit[] cus, Object destination, IJavaElement[] javaElements) throws JavaModelException, Exception, IOException {
		verifyCopyingOfSubCuElements(cus, destination, IReorgDestination.LOCATION_ON, javaElements);
	}

	private void verifyCopyingOfSubCuElements(ICompilationUnit[] cus, Object destination, int location, IJavaElement[] javaElements) throws JavaModelException, Exception, IOException {
		JavaCopyProcessor processor= verifyEnabled(new IResource[0], javaElements, new MockNewNameQueries(), createReorgQueries());
		verifyValidDestination(processor, destination, location);
		RefactoringStatus status= performRefactoring(new CopyRefactoring(processor), false);
		assertNull("failed precondition", status);
		for (int i= 0; i < cus.length; i++) {
			assertEqualLines("different source in " + cus[i].getElementName(), getFileContents(getOutputTestFileName(removeExtension(cus[i].getElementName()))), cus[i].getSource());
		}
	}

	private final RefactoringStatus performRefactoring(JavaCopyProcessor processor, boolean providesUndo) throws Exception {
		return performRefactoring(new CopyRefactoring(processor), providesUndo);
	}

	private static class MockNewNameQueries implements INewNameQueries{

		private static final String NEW_PACKAGE_NAME= "unusedName";
		private static final String NEW_PACKAGE_FOLDER_NAME= "unusedName";
		private static final String NEW_PACKAGE_FRAGMENT_ROOT_NAME= "UnusedName";
		private static final String NEW_FILE_NAME= "UnusedName.gif";
		private static final String NEW_FOLDER_NAME= "UnusedName";
		private static final String NEW_CU_NAME= "UnusedName";

		public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu, String s) {
			return createStaticQuery(NEW_CU_NAME);
		}

		public INewNameQuery createNewResourceNameQuery(IResource res, String s) {
			if (res instanceof IFile)
				return createStaticQuery(NEW_FILE_NAME);
			else
				return createStaticQuery(NEW_FOLDER_NAME);
		}

		public INewNameQuery createNewPackageNameQuery(IPackageFragment pack, String s) {
			return createStaticQuery(NEW_PACKAGE_NAME);
		}

		public INewNameQuery createNullQuery() {
			return createStaticQuery(null);
		}

		public INewNameQuery createStaticQuery(final String newName) {
			return new INewNameQuery(){
				public String getNewName() {
					return newName;
				}
			};
		}

		public INewNameQuery createNewPackageFragmentRootNameQuery(IPackageFragmentRoot root, String initialSuggestedName) {
			return createStaticQuery(NEW_PACKAGE_FRAGMENT_ROOT_NAME);
		}
	}

	private static class MockCancelNameQueries implements INewNameQueries{
		public INewNameQuery createNewCompilationUnitNameQuery(ICompilationUnit cu, String s) {
			return createNullQuery();
		}
		public INewNameQuery createNewResourceNameQuery(IResource res, String s) {
			return createNullQuery();
		}
		public INewNameQuery createNewPackageNameQuery(IPackageFragment pack, String s) {
			return createNullQuery();
		}
		public INewNameQuery createNullQuery() {
			return new INewNameQuery() {
				public String getNewName() {
					throw new OperationCanceledException();
				}
			};
		}
		public INewNameQuery createStaticQuery(final String newName) {
			return createNullQuery();
		}
		public INewNameQuery createNewPackageFragmentRootNameQuery(IPackageFragmentRoot root, String initialSuggestedName) {
			return createNullQuery();
		}
	}

	//---------------

	public void test_field_declared_in_multi_yes_type() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IField field= cu.getType("A").getField("bar");
			IType type= cu.getType("A");
			IJavaElement[] javaElements= { field };
			Object destination= type;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_fields_declared_in_multi_yes_type() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IField field1= cu.getType("A").getField("bar");
			IField field2= cu.getType("A").getField("baz");
			IType type= cu.getType("A");
			IJavaElement[] javaElements= { field1, field2 };
			Object destination= type;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_fields_declared_in_multi_yes_type_1() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IField field1= cu.getType("A").getField("var11");
			IField field2= cu.getType("A").getField("var2");
			IField field3= cu.getType("A").getField("var3");
			IType type= cu.getType("A");
			IJavaElement[] javaElements= { field1, field2, field3};
			Object destination= type;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
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
		IJavaElement[] javaElements= {RefactoringTestSetup.getProject()};
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
		IResource[] resources= {RefactoringTestSetup.getProject().getProject()};
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

//	public void testDisabled_noCommonParent2() throws Exception {
//		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
//		try {
//			IType classA= cu.getType("A");
//			IJavaElement[] javaElements= { classA, cu};
//			IResource[] resources= {};
//			verifyDisabled(resources, javaElements);
//		} finally {
//			performDummySearch();
//			cu.delete(true, new NullProgressMonitor());
//		}
//	}

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

	public void testEnabled_cu() throws Exception {
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= { cu};
			IResource[] resources= {};
			verifyEnabled(resources, javaElements, null, createReorgQueries());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testEnabled_package() throws Exception {
		IJavaElement[] javaElements= { getPackageP()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements, null, createReorgQueries());
	}

	public void testEnabled_packageRoot() throws Exception {
		IJavaElement[] javaElements= { getRoot()};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements, null, createReorgQueries());
	}

	public void testEnabled_archivePackageRoot() throws Exception {
		IJavaProject project= RefactoringTestSetup.getProject();
		IProject projectFolder= project.getProject();
		IFile archiveFile= projectFolder.getFile("archive.jar");
		archiveFile.create(getStream(""), true, null);

		IPackageFragmentRoot root= JavaProjectHelper.addLibrary(project, archiveFile.getFullPath());
		IJavaElement[] javaElements= { root};
		IResource[] resources= {};
		verifyEnabled(resources, javaElements, null, createReorgQueries());
	}

	public void testEnabled_file() throws Exception {
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			verifyEnabled(resources, javaElements, null, createReorgQueries());
		} finally{
			performDummySearch();
			file.delete(true, false, null);
		}
	}

	public void testEnabled_folder() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();

		IJavaElement[] javaElements= {};
		IResource[] resources= {folder};
		verifyEnabled(resources, javaElements, null, createReorgQueries());
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
			verifyEnabled(resources, javaElements, null, createReorgQueries());
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
			verifyEnabled(resources, javaElements, null, createReorgQueries());
		} finally{
			performDummySearch();
			file.delete(true, false, null);
			folder.delete(true, false, null);
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_package_no_1() throws Exception{
		IJavaElement[] javaElements= { getPackageP()};
		IResource[] resources= {};
		JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());
		verifyInvalidDestination(ref, RefactoringTestSetup.getProject());
	}

	public void testDestination_package_no_2() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_package_no_4() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= folder;
			verifyValidDestination(ref, destination);
		} finally {
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_no_1() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IProject closedProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		closedProject.create(null);
		assertTrue(! closedProject.isOpen());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= closedProject;
			verifyInvalidDestination(ref, destination);
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			closedProject.delete(true, true, null);
		}
	}

	public void testDestination_folder_no_0() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= folder;//same folder
			verifyInvalidDestination(ref, destination);
		}finally{
			performDummySearch();
			folder.delete(true, false, null);
		}
	}

	public void testDestination_folder_no_1() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFolder childFolder= folder.getFolder("folder");
		childFolder.create(true, true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= childFolder;
			verifyInvalidDestination(ref, destination);
		}finally{
			performDummySearch();
			folder.delete(true, false, null);
			childFolder.delete(true, false, null);
		}
	}

	public void testDestination_folder_no_2() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFile childFile= folder.getFile("a.txt");
		childFile.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= childFile;
			verifyInvalidDestination(ref, destination);
		}finally{
			performDummySearch();
			folder.delete(true, false, null);
			childFile.delete(true, false, null);
		}
	}

	public void testDestination_folder_no_3() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		assertTrue(! simpleProject.isOpen());
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= simpleProject;
			verifyInvalidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, true, new NullProgressMonitor());
		}
	}


	public void testDestination_root_no_0() throws Exception{
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

		Object destination= getPackageP();
		verifyInvalidDestination(ref, destination);
	}

	public void testDestination_root_no_1() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());

		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			IType classB= cu.getType("B");
			Object destination= classB;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_root_no_3() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_root_no_4() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= folder;
			verifyValidDestination(ref, destination);
		} finally {
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_cu_yes_0() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());
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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
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
		simpleProject.open(null);
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= folder;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			folder.delete(true, false, null);
		}
	}

	public void testDestination_cu_yes_8() throws Exception{
		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		ICompilationUnit cu2= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= { cu1};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= cu2;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			cu1.delete(true, new NullProgressMonitor());
			cu2.delete(true, new NullProgressMonitor());
		}
	}


	public void testDestination_file_yes_0() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= file;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_1() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		IFile otherFile= superFolder.getFile("b.txt");
		otherFile.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= otherFile;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			otherFile.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_3() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= folder;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_4() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		ICompilationUnit cu1= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= cu1;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			cu1.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_5() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getPackageP();
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_6() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getRoot();
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_7() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
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
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= parentFolder;
			verifyValidDestination(ref, destination);
		}finally{
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_file_yes_9() throws Exception{
		IFolder superFolder= (IFolder)getPackageP().getResource();
		IFile file= superFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		ICompilationUnit cu2= getPackageP().createCompilationUnit("B.java", "package p;class B{}", false, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= {};
			IResource[] resources= {file};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= cu2;
			verifyValidDestination(ref, destination);
		} finally {
			performDummySearch();
			file.delete(true, new NullProgressMonitor());
			cu2.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_0() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFolder otherFolder= superFolder.getFolder("otherfolder");
		otherFolder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= otherFolder;
			verifyValidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			otherFolder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_1() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
			verifyValidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_2() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getRoot();
			verifyValidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_3() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getPackageP();
			verifyValidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_4() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IFolder otherFolder= superFolder.getFolder("otherfolder");
		otherFolder.create(true, true, null);
		IFile fileInAnotherFolder= otherFolder.getFile("f.tex");
		fileInAnotherFolder.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= fileInAnotherFolder;
			verifyValidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			otherFolder.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_5() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= cu;
			verifyValidDestination(ref, destination);
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_folder_yes_6() throws Exception{
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder folder= superFolder.getFolder("folder");
		folder.create(true, true, null);

		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

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
		JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

		Object destination= getRoot();
		verifyValidDestination(ref, destination);
	}

	public void testDestination_package_yes_1() throws Exception{
		IJavaElement[] javaElements= { getPackageP()};
		IResource[] resources= {};
		JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());
		verifyValidDestination(ref, getPackageP());
	}

	public void testDestination_package_yes_2() throws Exception{
		IPackageFragment otherPackage= getRoot().createPackageFragment("other.pack", true, new NullProgressMonitor());
		try {
			IJavaElement[] javaElements= { getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= otherPackage;
			verifyValidDestination(ref, destination);
		} finally {
			performDummySearch();
			otherPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_root_yes_0() throws Exception{
		IJavaElement[] javaElements= {getRoot()};
		IResource[] resources= {};
		JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

		Object destination= getRoot().getJavaProject();
		verifyValidDestination(ref, destination);
	}

	public void testDestination_root_yes_1() throws Exception{
		IJavaProject otherJavaProject= JavaProjectHelper.createJavaProject("other", "bin");

		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= otherJavaProject;
			verifyValidDestination(ref, destination);
		} finally {
			performDummySearch();
			JavaProjectHelper.delete(otherJavaProject);
		}
	}

	public void testDestination_method_no_package() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getPackageP();
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_method_no_file() throws Exception{
		ICompilationUnit cu= null;
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		try {
			cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= file;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			file.delete(true, false, null);
		}
	}

	public void testDestination_method_no_folder() throws Exception{
		ICompilationUnit cu= null;
		IProject parentFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= parentFolder.getFolder(folderName);
		folder.create(true, true, null);

		try {
			cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= folder;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			folder.delete(true, false, null);
		}
	}

	public void testDestination_method_no_root() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getRoot();
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_method_no_java_project() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_method_no_simple_project() throws Exception{
		ICompilationUnit cu= null;
		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);
		try {
			cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= simpleProject;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, true, null);
		}
	}

	public void testDestination_method_no_import_container() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "import java.util.*;package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			IImportContainer importContainer= cu.getImportContainer();
			Object destination= importContainer;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_method_no_import_declaration() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "import java.util.*;package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			IImportDeclaration importDeclaration= cu.getImport("java.util.*");
			Object destination= importDeclaration;
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_method_no_package_declaration() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "import java.util.*;package p;class A{void foo(){}}", false, new NullProgressMonitor());
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= cu.getPackageDeclaration("p");
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_itself() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= method;

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_cu_with_main_type() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit otherCu= createCUfromTestFile(getPackageP(), "C");
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= otherCu;

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu, otherCu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			otherCu.delete(true, new NullProgressMonitor());
		}
	}
	
	public void test_method_yes_other_method() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IMethod otherMethod= cu.getType("A").getMethod("bar", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= otherMethod;

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_AFTER, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_other_method_back() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IMethod otherMethod= cu.getType("A").getMethod("bar", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= otherMethod;

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_BEFORE, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_field() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IField field= cu.getType("A").getField("bar");
			IJavaElement[] javaElements= { method };
			Object destination= field;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_AFTER, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_field_back() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IField field= cu.getType("A").getField("bar");
			IJavaElement[] javaElements= { method };
			Object destination= field;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_BEFORE, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_type() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= cu.getType("A");

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_initializer() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= cu.getType("A").getInitializer(1);

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_AFTER, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_method_yes_initializer_back() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IMethod method= cu.getType("A").getMethod("foo", new String[0]);
			IJavaElement[] javaElements= { method };
			Object destination= cu.getType("A").getInitializer(1);

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_BEFORE, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_field_yes_field() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IField field= cu.getType("A").getField("bar");
			IField otherField= cu.getType("A").getField("baz");
			IJavaElement[] javaElements= { field };
			Object destination= otherField;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_AFTER, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_field_yes_field_back() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IField field= cu.getType("A").getField("bar");
			IField otherField= cu.getType("A").getField("baz");
			IJavaElement[] javaElements= { field };
			Object destination= otherField;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_BEFORE, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_inner_type_yes_cu() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit otherCu= createCUfromTestFile(getPackageP(), "C");

		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IType type= cu.getType("A").getType("Inner");
			IJavaElement[] javaElements= { type };
			Object destination= otherCu;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{otherCu, cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			otherCu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_type_yes_package() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{}", true, new NullProgressMonitor());
		ICompilationUnit newCu= null;
		try {
			ParticipantTesting.reset();
			IType type= cu.getType("A");
			IJavaElement[] javaElements= { type };
			IResource[] resources=  {};
			ResourceMapping mapping= JavaElementResourceMapping.create(type.getCompilationUnit());
			String[] handles= ParticipantTesting.createHandles(type.getCompilationUnit(), mapping);
			IPackageFragment destination= getPackageP();
			INewNameQueries queries= new MockNewNameQueries();
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			verifyValidDestination(ref, destination);
			assertTrue("source cu does not exist before copying", cu.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);
			assertTrue("source cu does not exist after copying", cu.exists());
			newCu= getPackageP().getCompilationUnit(MockNewNameQueries.NEW_CU_NAME + ".java");
			assertTrue("new cu does not exist after copying", newCu.exists());
			ReorgExecutionLog executionLog= new ReorgExecutionLog();
			executionLog.setNewName(type.getCompilationUnit(), MockNewNameQueries.NEW_CU_NAME + ".java");
			executionLog.setNewName(mapping, MockNewNameQueries.NEW_CU_NAME + ".java");
			executionLog.markAsProcessed(type.getCompilationUnit());
			executionLog.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, executionLog),
				new CopyArguments(destination.getResource(), executionLog)
			});
		} finally {
			performDummySearch();
			if (newCu != null && newCu.exists())
				newCu.delete(true, new NullProgressMonitor());
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_type_canel_package() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{}", true, new NullProgressMonitor());
		try {
			ParticipantTesting.reset();
			IType type= cu.getType("A");
			IJavaElement[] javaElements= { type };
			IResource[] resources=  {};
			ResourceMapping mapping= JavaElementResourceMapping.create(type.getCompilationUnit());
			String[] handles= ParticipantTesting.createHandles(type.getCompilationUnit(), mapping);
			IPackageFragment destination= getPackageP();
			INewNameQueries queries= new MockCancelNameQueries();
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			verifyValidDestination(ref, destination);
			assertTrue("source cu does not exist before copying", cu.exists());
			try {
				performRefactoring(ref, false);
			} catch (OperationCanceledException e) {
			}
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsCanceled();
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log),
				new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_type_yes_other_package() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{}", true, new NullProgressMonitor());
		IPackageFragment otherPackage= getRoot().createPackageFragment("other", true, new NullProgressMonitor());
		ICompilationUnit newCu= null;
		try {
			ParticipantTesting.reset();

			IType type= cu.getType("A");
			IJavaElement[] javaElements= { type };
			IResource[] resources=  {};
			IPackageFragment destination= otherPackage;
			ResourceMapping mapping= JavaElementResourceMapping.create(type.getCompilationUnit());
			String[] handles= ParticipantTesting.createHandles(type.getCompilationUnit(), mapping);

			INewNameQueries queries= new MockNewNameQueries();
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			verifyValidDestination(ref, destination);
			assertTrue("source cu does not exist before copying", cu.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);
			assertTrue("source cu does not exist after copying", cu.exists());
			newCu= otherPackage.getCompilationUnit(cu.getElementName());
			assertTrue("new cu does not exist after copying", newCu.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(type.getCompilationUnit());
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log),
				new CopyArguments(destination.getResource(), log)
			});

		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			if (newCu != null && newCu.exists())
				newCu.delete(true, new NullProgressMonitor());
			otherPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_initializer_no_package() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "import java.util.*;package p;class A{void foo(){}{}}", false, new NullProgressMonitor());
			IInitializer initializer= cu.getType("A").getInitializer(1);
			IJavaElement[] javaElements= { initializer };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getPackageP();
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_initializer_yes_type() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IInitializer initializer= cu.getType("A").getInitializer(1);
			IJavaElement[] javaElements= { initializer };
			Object destination= cu.getType("A");
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_initializer_yes_method() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IInitializer initializer= cu.getType("A").getInitializer(1);
			IJavaElement[] javaElements= { initializer };
			Object destination= cu.getType("A").getMethod("foo", new String[0]);

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_AFTER, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_initializer_yes_method_back() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IInitializer initializer= cu.getType("A").getInitializer(1);
			IJavaElement[] javaElements= { initializer };
			Object destination= cu.getType("A").getMethod("foo", new String[0]);

			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu}, destination, IReorgDestination.LOCATION_BEFORE, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testDestination_import_container_no_package() throws Exception{
		ICompilationUnit cu= null;
		try {
			cu= getPackageP().createCompilationUnit("A.java", "import java.util.*;package p;class A{void foo(){}{}}", false, new NullProgressMonitor());
			IImportContainer container= cu.getImportContainer();
			IJavaElement[] javaElements= { container };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, null, createReorgQueries());

			Object destination= getPackageP();
			verifyInvalidDestination(ref, destination);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_import_container_yes_type_in_different_cu() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit otherCu= createCUfromTestFile(getPackageP(), "C");

		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IImportContainer container= cu.getImportContainer();
			IJavaElement[] javaElements= { container };
			Object destination= otherCu;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu, otherCu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			otherCu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_import_container_yes_method_in_different_cu() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit otherCu= createCUfromTestFile(getPackageP(), "C");

		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IImportContainer container= cu.getImportContainer();
			IJavaElement[] javaElements= { container };
			Object destination= otherCu;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu, otherCu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			otherCu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_import_container_yes_cu() throws Exception{
		ICompilationUnit cu= null;
		ICompilationUnit otherCu= createCUfromTestFile(getPackageP(), "C");

		try {
			cu= createCUfromTestFile(getPackageP(), "A");
			IImportContainer container= cu.getImportContainer();
			IJavaElement[] javaElements= { container };
			Object destination= otherCu;
			verifyCopyingOfSubCuElements(new ICompilationUnit[]{cu, otherCu}, destination, javaElements);
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			otherCu.delete(true, new NullProgressMonitor());
		}
	}


	public void testCopy_File_to_Folder() throws Exception {
		ParticipantTesting.reset();
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder destinationFolder= superFolder.getFolder("folder");
		destinationFolder.create(true, true, null);

		IFile newFile= null;
		try {

			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			String[] handles= ParticipantTesting.createHandles(file);

			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= destinationFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= destinationFolder.getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(file);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log)
			});
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			destinationFolder.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_Same_Folder() throws Exception {
		ParticipantTesting.reset();
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFolder destinationFolder= parentFolder;

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			String[] handles= ParticipantTesting.createHandles(file);
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= destinationFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= destinationFolder.getFile(MockNewNameQueries.NEW_FILE_NAME);
			assertTrue("new file does not exist after copying", newFile.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.setNewName(file, MockNewNameQueries.NEW_FILE_NAME);
			log.markAsProcessed(file);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log)
			});
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_Same_Folder_Cancel() throws Exception {
		ParticipantTesting.reset();
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFolder destinationFolder= parentFolder;

		try {
			INewNameQueries queries= new MockCancelNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			String[] handles= ParticipantTesting.createHandles(file);
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= destinationFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			try {
				performRefactoring(ref, false);
			} catch(OperationCanceledException e) {
			}
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsCanceled();
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log)
			});
		} finally {
			performDummySearch();
			file.delete(true, false, null);
		}
	}

	public void testCopy_File_to_Itself() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= file;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= parentFolder.getFile(MockNewNameQueries.NEW_FILE_NAME);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_AnotherFile() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFile otherFile= RefactoringTestSetup.getProject().getProject().getFile("b.txt");
		otherFile.create(getStream("123"), true, null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= otherFile;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= RefactoringTestSetup.getProject().getProject().getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			otherFile.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_Package() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IPackageFragment otherPackage= getRoot().createPackageFragment("other.pack", true, new NullProgressMonitor());
		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= otherPackage;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= ((IFolder)otherPackage.getResource()).getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			otherPackage.delete(true, new NullProgressMonitor());
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_DefaultPackage() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IPackageFragment defaultPackage= getRoot().getPackageFragment("");
		assertTrue(defaultPackage.exists());
		assertTrue(defaultPackage.isDefaultPackage());
		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= defaultPackage;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= ((IFolder)defaultPackage.getResource()).getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_SourceFolder() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= getRoot();
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= ((IFolder)getRoot().getResource()).getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_JavaProject() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= RefactoringTestSetup.getProject().getProject().getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_Cu() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFile newFile= null;
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= cu;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= parentFolder.getFile(MockNewNameQueries.NEW_FILE_NAME);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			cu.delete(true, new NullProgressMonitor());
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_File_to_SimpleProject() throws Exception {
		IFolder parentFolder= (IFolder) getPackageP().getResource();
		String fileName= "a.txt";
		IFile file= parentFolder.getFile(fileName);
		file.create(getStream("123"), true, null);

		IFile newFile= null;
		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {};
			IResource[] resources= { file };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= simpleProject;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", file.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", file.exists());

			newFile= simpleProject.getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			file.delete(true, false, null);
			simpleProject.delete(true, new NullProgressMonitor());
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_Cu_to_Folder() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder destinationFolder= superFolder.getFolder("folder");
		destinationFolder.create(true, true, null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= { };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= destinationFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source cu does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source cu does not exist after copying", cu.exists());

			newFile= destinationFolder.getFile(fileName);
			assertTrue("new cu does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			destinationFolder.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_Bug67124() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		IPath cuPath= cu.getResource().getFullPath();

		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		IFolder destinationFolder= superFolder.getFolder("folder");
		destinationFolder.create(true, true, null);

		IFile newFile= null;
		try {
			FileBuffers.getTextFileBufferManager().connect(cuPath, LocationKind.IFILE, null);
			ITextFileBuffer buffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(cuPath, LocationKind.IFILE);
			buffer.getDocument().replace(0, 0, "Dirty");

			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= { };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= destinationFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source cu does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source cu does not exist after copying", cu.exists());

			newFile= destinationFolder.getFile(fileName);
			assertTrue("new cu does not exist after copying", newFile.exists());

			IPath newFilePath= newFile.getFullPath();
			try {
				FileBuffers.getTextFileBufferManager().connect(newFilePath, LocationKind.IFILE, null);
				ITextFileBuffer newBuffer= FileBuffers.getTextFileBufferManager().getTextFileBuffer(newFilePath, LocationKind.IFILE);
				assertEquals(buffer.getDocument().get(), newBuffer.getDocument().get());
			} finally {
				FileBuffers.getTextFileBufferManager().disconnect(newFilePath, LocationKind.IFILE, null);
			}
		} finally {
			performDummySearch();

			FileBuffers.getTextFileBufferManager().disconnect(cuPath, LocationKind.IFILE, null);

			cu.delete(true, new NullProgressMonitor());
			destinationFolder.delete(true, false, null);
			if (newFile != null)
				newFile.delete(true, false, null);

		}
	}

	public void testCopy_Cu_to_Same_Package() throws Exception {
		ParticipantTesting.reset();
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		ICompilationUnit newCu= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= { };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(cu);
			String[] handles= ParticipantTesting.createHandles(cu, mapping);

			IPackageFragment destination= getPackageP();
			verifyValidDestination(ref, destination);

			assertTrue("source cu does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source cu does not exist after copying", cu.exists());

			String newName= MockNewNameQueries.NEW_CU_NAME + ".java";
			newCu= getPackageP().getCompilationUnit(newName);
			assertTrue("new cu does not exist after copying", newCu.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.setNewName(cu, newName);
			log.setNewName(mapping, newName);
			log.markAsProcessed(cu);
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			if (newCu != null && newCu.exists()){
				newCu.delete(true, new NullProgressMonitor());
			}
		}
	}

	public void testCopy_Cu_to_Same_Package_Cancel() throws Exception {
		ParticipantTesting.reset();
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		try {
			INewNameQueries queries= new MockCancelNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= { };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(cu);
			String[] handles= ParticipantTesting.createHandles(cu, mapping);

			IPackageFragment destination= getPackageP();
			verifyValidDestination(ref, destination);

			assertTrue("source cu does not exist before copying", cu.exists());

			try {
				performRefactoring(ref, false);
			} catch (OperationCanceledException e) {
			}
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsCanceled();
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void test_type_yes_type() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit otherCu= createCUfromTestFile(getPackageP(), "C");

		ICompilationUnit newCu= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= { };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= otherCu;
			verifyValidDestination(ref, destination);

			assertTrue("source cu does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source cu does not exist after copying", cu.exists());

			newCu= getPackageP().getCompilationUnit(MockNewNameQueries.NEW_CU_NAME + ".java");
			assertTrue("new cu does not exist after copying", newCu.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			if (newCu != null && newCu.exists()){
				newCu.delete(true, new NullProgressMonitor());
			}
			if (otherCu != null && otherCu.exists()){
				otherCu.delete(true, new NullProgressMonitor());
			}
		}
	}

	public void testCopy_Cu_to_Itself() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		ICompilationUnit newCu= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= { };
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= cu;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			newCu= getPackageP().getCompilationUnit(MockNewNameQueries.NEW_CU_NAME + ".java");
			assertTrue("new file does not exist after copying", newCu.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			if (newCu != null && newCu.exists()){
				newCu.delete(true, new NullProgressMonitor());
			}
		}
	}

	public void testCopy_Cu_to_OtherPackage() throws Exception {
		ParticipantTesting.reset();
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IPackageFragment otherPackage= getRoot().createPackageFragment("other.pack", true, new NullProgressMonitor());
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(cu);
			String[] handles= ParticipantTesting.createHandles(cu, mapping);

			IPackageFragment destination= otherPackage;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			ICompilationUnit newCu= otherPackage.getCompilationUnit(fileName);
			assertTrue("new file does not exist after copying", newCu.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(cu);
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			otherPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_Cu_to_DefaultPackage() throws Exception {
		ParticipantTesting.reset();
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IPackageFragment defaultPackage= getRoot().getPackageFragment("");
		assertTrue(defaultPackage.exists());
		assertTrue(defaultPackage.isDefaultPackage());
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(cu);
			String[] handles= ParticipantTesting.createHandles(cu, mapping);

			IPackageFragment destination= defaultPackage;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			ICompilationUnit newCu= defaultPackage.getCompilationUnit(fileName);
			assertTrue("new file does not exist after copying", newCu.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(cu);
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_Cu_to_SourceFolder() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		ICompilationUnit newCu= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= getRoot();
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			newCu= getRoot().getPackageFragment("").getCompilationUnit(fileName);
			assertTrue("new file does not exist after copying", newCu.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_Cu_to_JavaProject() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			newFile= RefactoringTestSetup.getProject().getProject().getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			if (newFile != null)
				newFile.delete(true, false, null);
		}
	}

	public void testCopy_Cu_to_File_In_Package() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IFolder parentFolder= (IFolder) getPackageP().getResource();
		IFile file= parentFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		assertTrue(file.exists());

		ICompilationUnit newCu= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= file;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			newCu= getPackageP().getCompilationUnit(MockNewNameQueries.NEW_CU_NAME + ".java");
			assertTrue("new file does not exist after copying", newCu.exists());

			String expectedSource= "package p;class "+ MockNewNameQueries.NEW_CU_NAME +"{void foo(){}class Inner{}}";
			assertEqualLines("source compare failed", expectedSource, newCu.getSource());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			file.delete(true, false, null);
			if (newCu != null && newCu.exists()){
				newCu.delete(true, new NullProgressMonitor());
			}
		}
	}

	public void testCopy_Cu_to_File_In_Resource_Folder() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IProject parentFolder= RefactoringTestSetup.getProject().getProject();
		IFile file= parentFolder.getFile("a.txt");
		file.create(getStream("123"), true, null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= file;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			newFile= RefactoringTestSetup.getProject().getProject().getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			file.delete(true, false, null);
			newFile.delete(true, false, null);
		}
	}

	public void testCopy_Cu_to_SimpleProject() throws Exception {
		String fileName= "A.java";
		ICompilationUnit cu= getPackageP().createCompilationUnit(fileName, "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());

		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);

		IFile newFile= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {cu};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= simpleProject;
			verifyValidDestination(ref, destination);

			assertTrue("source file does not exist before copying", cu.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source file does not exist after copying", cu.exists());

			newFile= simpleProject.getFile(fileName);
			assertTrue("new file does not exist after copying", newFile.exists());
		} finally {
			performDummySearch();
			cu.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, false, null);
			newFile.delete(true, false, null);
		}
	}

	public void testCopy_Package_to_Its_Root() throws Exception {
		ParticipantTesting.reset();
		IPackageFragment newPackage= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(getPackageP());
			String[] handles= ParticipantTesting.createHandles(getPackageP(), mapping);

			IPackageFragmentRoot destination= getRoot();
			verifyValidDestination(ref, destination);

			assertTrue("source package does not exist before copying", getPackageP().exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source package does not exist after copying", getPackageP().exists());

			newPackage= getRoot().getPackageFragment(MockNewNameQueries.NEW_PACKAGE_NAME);
			assertTrue("new package does not exist after copying", newPackage.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.setNewName(getPackageP(), MockNewNameQueries.NEW_PACKAGE_NAME);
			log.setNewName(mapping, MockNewNameQueries.NEW_PACKAGE_NAME);
			log.markAsProcessed(getPackageP());
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log),
				new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			if (newPackage != null)
				newPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_Package_to_Its_Root_Cancel() throws Exception {
		ParticipantTesting.reset();
		try {
			INewNameQueries queries= new MockCancelNameQueries();

			IJavaElement[] javaElements= {getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(getPackageP());
			String[] handles= ParticipantTesting.createHandles(getPackageP(), mapping);

			IPackageFragmentRoot destination= getRoot();
			verifyValidDestination(ref, destination);

			assertTrue("source package does not exist before copying", getPackageP().exists());

			try {
				performRefactoring(ref, false);
			} catch(OperationCanceledException e) {
			}
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsCanceled();
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log),
				new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
		}
	}

	public void testCopy_Package_to_Itself() throws Exception {
		ParticipantTesting.reset();
		IPackageFragment newPackage= null;
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(getPackageP());
			String[] handles= ParticipantTesting.createHandles(getPackageP(), mapping);

			IPackageFragment destination= getPackageP();
			verifyValidDestination(ref, destination);

			assertTrue("source package does not exist before copying", getPackageP().exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source package does not exist after copying", getPackageP().exists());

			newPackage= getRoot().getPackageFragment(MockNewNameQueries.NEW_PACKAGE_NAME);
			assertTrue("new package does not exist after copying", newPackage.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(getPackageP());
			log.markAsProcessed(mapping);
			log.setNewName(getPackageP(), MockNewNameQueries.NEW_PACKAGE_NAME);
			log.setNewName(mapping, MockNewNameQueries.NEW_PACKAGE_FOLDER_NAME);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination.getParent(), log),
				new CopyArguments(destination.getParent().getResource(), log)
			});
		} finally {
			performDummySearch();
			if (newPackage != null && newPackage.exists())
				newPackage.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_Package_to_Another_Root() throws Exception {
		IPackageFragmentRoot otherRoot= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "otherRoot");
		IPackageFragment newPackage= null;
		String packageName= getPackageP().getElementName();
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= otherRoot;
			verifyValidDestination(ref, destination);

			assertTrue("source package does not exist before copying", getPackageP().exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source package does not exist after copying", getPackageP().exists());

			newPackage= otherRoot.getPackageFragment(packageName);
			assertTrue("new package does not exist after copying", newPackage.exists());
		} finally {
			performDummySearch();
			newPackage.delete(true, new NullProgressMonitor());
			otherRoot.delete(0, 0, new NullProgressMonitor());
		}
	}

	public void testCopy_Package_to_JavaProject_That_Is_Root() throws Exception {
		IJavaProject otherProject= JavaProjectHelper.createJavaProject("otherProject", null);
		JavaProjectHelper.addSourceContainer(otherProject, null);
		try {
			INewNameQueries queries= new MockNewNameQueries();

			IJavaElement[] javaElements= {getPackageP()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, queries, createReorgQueries());

			Object destination= otherProject;
			verifyValidDestination(ref, destination);

			assertTrue("source package does not exist before copying", getPackageP().exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source package does not exist after copying", getPackageP().exists());

			IPackageFragment newPackage= null;
			IPackageFragmentRoot[] roots= otherProject.getAllPackageFragmentRoots();
			for (int i= 0; i < roots.length; i++) {
				if (ReorgUtils.isSourceFolder(roots[i])){
					newPackage= roots[i].getPackageFragment(getPackageP().getElementName());
					assertTrue("new package does not exist after copying", newPackage.exists());
				}
			}
			assertNotNull(newPackage);
		} finally {
			performDummySearch();
			JavaProjectHelper.delete(otherProject);
		}
	}

	public void testCopy_folder_to_other_folder() throws Exception {
		ParticipantTesting.reset();
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		IFolder otherFolder= superFolder.getFolder("otherfolder");
		otherFolder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());
			String[] handles= ParticipantTesting.createHandles(folder);

			Object destination= otherFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source does not exist before copying", folder.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());
			assertTrue("copied folder does not exist after copying", otherFolder.getFolder(folderName).exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(folder);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log)
			});

		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			otherFolder.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_to_same_container() throws Exception {
		ParticipantTesting.reset();
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);
		IFolder newFolder= null;

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());
			String[] handles= ParticipantTesting.createHandles(folder);

			Object destination= superFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source does not exist before copying", folder.exists());

			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());
			newFolder= superFolder.getFolder(MockNewNameQueries.NEW_FOLDER_NAME);
			assertTrue("copied folder does not exist after copying", newFolder.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(folder);
			log.setNewName(folder, MockNewNameQueries.NEW_FOLDER_NAME);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log)
			});

		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			newFolder.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_to_same_container_cancel() throws Exception {
		ParticipantTesting.reset();
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockCancelNameQueries(), createReorgQueries());
			String[] handles= ParticipantTesting.createHandles(folder);

			Object destination= superFolder;
			verifyValidDestination(ref, destination);

			assertTrue("source does not exist before copying", folder.exists());

			try {
				performRefactoring(ref, false);
			} catch (OperationCanceledException e) {
			}
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsCanceled();
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log)
			});

		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_Java_project() throws Exception {
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());

			Object destination= RefactoringTestSetup.getProject();
			verifyValidDestination(ref, destination);

			assertTrue("source does not exist before copying", folder.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());

			assertTrue("copied folder does not exist after copying", RefactoringTestSetup.getProject().getProject().getFolder(folderName).exists());
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_to_source_folder() throws Exception {
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());

			Object destination= getRoot();
			verifyValidDestination(ref, destination);

			assertTrue("source does not exist before copying", folder.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());

			assertTrue("copied folder does not exist after copying", ((IFolder)getRoot().getResource()).getFolder(folderName).exists());
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_to_package() throws Exception {
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());

			IPackageFragment destination= getPackageP();
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", folder.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());
			assertTrue("copied folder does not exist after copying", ((IFolder)destination.getResource()).getFolder(folderName).exists());
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
		}

	}
	public void testCopy_folder_to_file_in_another_folder() throws Exception {
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		IFolder otherFolder= superFolder.getFolder("otherfolder");
		otherFolder.create(true, true, null);
		IFile fileInAnotherFolder= otherFolder.getFile("f.tex");
		fileInAnotherFolder.create(getStream("123"), true, null);

		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());

			Object destination= fileInAnotherFolder;
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", folder.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());
			assertTrue("copied folder does not exist after copying", otherFolder.getFolder(folderName).exists());
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			otherFolder.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_to_cu() throws Exception {
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "package p;class A{void foo(){}class Inner{}}", false, new NullProgressMonitor());
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());

			ICompilationUnit destination= cu;
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", folder.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());
			assertTrue("copied folder does not exist after copying", ((IFolder)getPackageP().getResource()).getFolder(folderName).exists());
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			cu.delete(true, new NullProgressMonitor());
		}
	}

	public void testCopy_folder_to_simple_project() throws Exception {
		IProject superFolder= RefactoringTestSetup.getProject().getProject();
		String folderName= "folder";
		IFolder folder= superFolder.getFolder(folderName);
		folder.create(true, true, null);

		IProject simpleProject= ResourcesPlugin.getWorkspace().getRoot().getProject("mySImpleProject");
		simpleProject.create(null);
		simpleProject.open(null);
		try{
			IJavaElement[] javaElements= {};
			IResource[] resources= {folder};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());

			Object destination= simpleProject;
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", folder.exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", folder.exists());
			assertTrue("copied folder does not exist after copying", simpleProject.getFolder(folderName).exists());
		} finally{
			performDummySearch();
			folder.delete(true, new NullProgressMonitor());
			simpleProject.delete(true, true, new NullProgressMonitor());
		}
	}

	public void testCopy_root_to_same_Java_project() throws Exception {
		ParticipantTesting.reset();
		IPackageFragmentRoot newRoot= null;
		try {
			// Delete the unnamed folder so that the delta is a ADD not a CHANGED
			IResource folder= RefactoringTestSetup.getProject().getProject().findMember(MockNewNameQueries.NEW_PACKAGE_FRAGMENT_ROOT_NAME);
			if (folder != null) {
				folder.delete(IResource.FORCE, null);
			}

			IJavaElement[] javaElements= { getRoot() };
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(getRoot());
			String[] handles= ParticipantTesting.createHandles(getRoot(), mapping);

			IJavaProject destination= getRoot().getJavaProject();
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", getRoot().exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", getRoot().exists());
			String newName= MockNewNameQueries.NEW_PACKAGE_FRAGMENT_ROOT_NAME;
			newRoot= getSourceFolder(RefactoringTestSetup.getProject(), newName);
			assertNotNull("copied folder does not exist after copying", newRoot);
			assertTrue("copied folder does not exist after copying", newRoot.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.setNewName(getRoot(), newName);
			log.setNewName(mapping, newName);
			log.markAsProcessed(getRoot());
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
			if (newRoot != null && newRoot.exists())
				newRoot.delete(0, 0, new NullProgressMonitor());
		}
	}

	public void testCopy_root_to_same_Java_project_cancel() throws Exception {
		ParticipantTesting.reset();
		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockCancelNameQueries(), createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(getRoot());
			String[] handles= ParticipantTesting.createHandles(getRoot(), mapping);

			IJavaProject destination= getRoot().getJavaProject();
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", getRoot().exists());
			try {
				performRefactoring(ref, false);
			} catch (OperationCanceledException e) {
			}
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsCanceled();
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getResource(), log)
			});
		} finally {
			performDummySearch();
		}
	}

	public void testCopy_root_to_other_Java_project() throws Exception {
		ParticipantTesting.reset();
		IJavaProject otherJavaProject= JavaProjectHelper.createJavaProject("other", "bin");
		try {
			IJavaElement[] javaElements= { getRoot()};
			IResource[] resources= {};
			JavaCopyProcessor ref= verifyEnabled(resources, javaElements, new MockNewNameQueries(), createReorgQueries());
			ResourceMapping mapping= JavaElementResourceMapping.create(getRoot());
			String[] handles= ParticipantTesting.createHandles(getRoot(), mapping);

			IJavaProject destination= otherJavaProject;
			verifyValidDestination(ref, destination);
			assertTrue("source does not exist before copying", getRoot().exists());
			RefactoringStatus status= performRefactoring(ref, false);
			assertEquals(null, status);

			assertTrue("source does not exist after copying", getRoot().exists());
			String newName= getRoot().getElementName();
			IPackageFragmentRoot newRoot= getSourceFolder(otherJavaProject, newName);
			assertNotNull("copied folder does not exist after copying", newRoot);
			assertTrue("copied folder does not exist after copying", newRoot.exists());
			ReorgExecutionLog log= new ReorgExecutionLog();
			log.markAsProcessed(getRoot());
			log.markAsProcessed(mapping);
			ParticipantTesting.testCopy(handles, new CopyArguments[] {
				new CopyArguments(destination, log), new CopyArguments(destination.getProject(), log)
			});
		} finally {
			performDummySearch();
			JavaProjectHelper.delete(otherJavaProject);
		}
	}
}
