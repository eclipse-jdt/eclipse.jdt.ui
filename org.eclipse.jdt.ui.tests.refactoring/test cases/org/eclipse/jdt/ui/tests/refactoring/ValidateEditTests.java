/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.core.RepositoryProvider;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;

import org.eclipse.jdt.ui.tests.refactoring.ccp.MockReorgQueries;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestRepositoryProvider;


public class ValidateEditTests extends RefactoringTest {

	private static final Class clazz= ValidateEditTests.class;

	public ValidateEditTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java15Setup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java15Setup(someTest);
	}

	protected void setUp() throws Exception {
		super.setUp();
		RepositoryProvider.map(getRoot().getJavaProject().getProject(), RefactoringTestRepositoryProvider.PROVIDER_ID);
	}

	protected void tearDown() throws Exception {
		RepositoryProvider.unmap(getRoot().getJavaProject().getProject());
		super.tearDown();
	}

	public void testPackageRename1() throws Exception {
		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("class A {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("A.java", buf.toString(), true, null);
		setReadOnly(cu1);

		buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("class B {\n");
		buf.append("}\n");
		ICompilationUnit cu2= fragment.createCompilationUnit("B.java", buf.toString(), true, null);
		setReadOnly(cu2);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(fragment);
		descriptor.setNewName("org.test2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu2.getPath()));
	}

	public void testPackageRename2() throws Exception {
		// A readonly and moved, B moved, C changes

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("A.java", buf.toString(), true, null);
		setReadOnly(cu1);

		buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		fragment.createCompilationUnit("B.java", buf.toString(), true, null);
		// not read only

		IPackageFragment fragment2= getRoot().createPackageFragment("org.other", true, null);

		buf= new StringBuffer();
		buf.append("package org.other;\n");
		buf.append("public class C extends org.test.A {\n");
		buf.append("}\n");
		ICompilationUnit cu3= fragment2.createCompilationUnit("C.java", buf.toString(), true, null);
		setReadOnly(cu3);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(fragment);
		descriptor.setNewName("org.test2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu3.getPath()));
	}

	public void testPackageRenameWithResource() throws Exception {
		// MyClass readonly and moved, x.properties readonly moved

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);

		IFile file= ((IFolder) fragment.getResource()).getFile("x.properties");
		String content= "A file with no references";
		file.create(getStream(content), true, null);
		setReadOnly(file);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(fragment);
		descriptor.setNewName("org.test2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(1, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
	}


	public void testPackageRenameWithResource2() throws Exception {
		// MyClass readonly and moved, x.properties readonly moved

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);

		IFile file= ((IFolder) fragment.getResource()).getFile("x.properties");
		byte[] content= "This is about 'org.test' and more".getBytes();
		file.create(new ByteArrayInputStream(content), true, null);
		file.refreshLocal( IResource.DEPTH_ONE, null);
		setReadOnly(file);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(fragment);
		descriptor.setNewName("org.test2");
		descriptor.setUpdateReferences(true);
		descriptor.setUpdateQualifiedNames(true);
		descriptor.setFileNamePatterns("*.properties");
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(file.getFullPath()));
	}

	public void testCURename() throws Exception {

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);

		buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class C extends MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu2= fragment.createCompilationUnit("C.java", buf.toString(), true, null);
		setReadOnly(cu2);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		descriptor.setJavaElement(cu1);
		descriptor.setNewName("MyClass2.java");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu2.getPath()));
	}

	public void testTypeRename() throws Exception {

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);

		buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class C extends MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu2= fragment.createCompilationUnit("C.java", buf.toString(), true, null);
		setReadOnly(cu2);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(cu1.findPrimaryType());
		descriptor.setNewName("MyClass2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu2.getPath()));
	}

	public void testMoveCU2() throws Exception {
		// Move CU and file: Only CU be validated as file doesn't change

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);

		IFile file= ((IFolder) fragment.getResource()).getFile("x.properties");
		String content= "A file with no references";
		file.create(getStream(content), true, null);
		setReadOnly(file);

		buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class C extends MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu2= fragment.createCompilationUnit("C.java", buf.toString(), true, null);
		setReadOnly(cu2);

		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(new IResource[] { file } , new IJavaElement[] { cu1 });
		assertTrue(policy.canEnable());

		JavaMoveProcessor javaMoveProcessor= new JavaMoveProcessor(policy);
		javaMoveProcessor.setDestination(ReorgDestinationFactory.createDestination(otherFragment));
		javaMoveProcessor.setReorgQueries(new MockReorgQueries());
		RefactoringStatus status= performRefactoring(new MoveRefactoring(javaMoveProcessor));
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath())); // moved and changed
		assertTrue(validatedEditPaths.contains(cu2.getPath())); // changed
	}

	public void testMoveFileWithReplace() throws Exception {
		// Move file to a existing location

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		IFile file= ((IFolder) fragment.getResource()).getFile("x.properties");
		String content= "A file with no references";
		file.create(getStream(content), true, null);
		setReadOnly(file);

		IFile file2= ((IFolder) otherFragment.getResource()).getFile("x.properties");
		file2.create(getStream(content), true, null);
		setReadOnly(file2);


		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(new IResource[] { file }, new IJavaElement[] {});
		assertTrue(policy.canEnable());

		JavaMoveProcessor javaMoveProcessor= new JavaMoveProcessor(policy);
		javaMoveProcessor.setDestination(ReorgDestinationFactory.createDestination(otherFragment));
		javaMoveProcessor.setReorgQueries(new MockReorgQueries());
		RefactoringStatus status= performRefactoring(new MoveRefactoring(javaMoveProcessor), true);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(1, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(file2.getFullPath())); // replaced
	}

	public void testMoveCuWithReplace() throws Exception {
		// Move CU to an existing location

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);


		buf= new StringBuffer();
		buf.append("package org.test1;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu2= otherFragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu2);


		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(new IResource[0], new IJavaElement[] { cu1 });
		assertTrue(policy.canEnable());

		JavaMoveProcessor javaMoveProcessor= new JavaMoveProcessor(policy);
		javaMoveProcessor.setDestination(ReorgDestinationFactory.createDestination(otherFragment));
		javaMoveProcessor.setReorgQueries(new MockReorgQueries());
		RefactoringStatus status= performRefactoring(new MoveRefactoring(javaMoveProcessor), false);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath())); // moved and changed
		assertTrue(validatedEditPaths.contains(cu2.getPath())); // replaced
	}

	public void testCopyCuWithReplace() throws Exception {
		// Copy CU to a existing location

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu1);

		buf= new StringBuffer();
		buf.append("package org.test1;\n");
		buf.append("public class MyClass {\n");
		buf.append("}\n");
		ICompilationUnit cu2= otherFragment.createCompilationUnit("MyClass.java", buf.toString(), true, null);
		setReadOnly(cu2);

		ICopyPolicy policy= ReorgPolicyFactory.createCopyPolicy(new IResource[0], new IJavaElement[] { cu1 });
		assertTrue(policy.canEnable());

		JavaCopyProcessor javaCopyProcessor= new JavaCopyProcessor(policy);
		javaCopyProcessor.setDestination(ReorgDestinationFactory.createDestination(otherFragment));
		javaCopyProcessor.setReorgQueries(new MockReorgQueries());
		javaCopyProcessor.setNewNameQueries(new MockReorgQueries());
		RefactoringStatus status= performRefactoring(new CopyRefactoring(javaCopyProcessor), false);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(1, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu2.getPath())); // replaced
	}


	private static void setReadOnly(ICompilationUnit cu) throws CoreException {
		setReadOnly(cu.getResource());
	}


	private static void setReadOnly(IResource resource) throws CoreException {
		ResourceAttributes resourceAttributes = resource.getResourceAttributes();
		if (resourceAttributes != null) {
			resourceAttributes.setReadOnly(true);
			resource.setResourceAttributes(resourceAttributes);
		}
	}

}
