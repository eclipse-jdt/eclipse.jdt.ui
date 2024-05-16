/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.team.core.RepositoryProvider;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

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
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;

import org.eclipse.jdt.ui.tests.refactoring.ccp.MockReorgQueries;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestRepositoryProvider;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d5Setup;


public class ValidateEditTests extends GenericRefactoringTest {

	public ValidateEditTests() {
		rts= new Java1d5Setup();
	}

	@Before
	public void setUp() throws Exception {
		RepositoryProvider.map(getRoot().getJavaProject().getProject(), RefactoringTestRepositoryProvider.PROVIDER_ID);
	}

	@After
	public void tearDown() throws Exception {
		RepositoryProvider.unmap(getRoot().getJavaProject().getProject());
	}

	@Test
	public void testPackageRename1() throws Exception {
		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		String str= """
			package org.test;
			class A {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("A.java", str, true, null);
		setReadOnly(cu1);

		String str1= """
			package org.test;
			class B {
			}
			""";
		ICompilationUnit cu2= fragment.createCompilationUnit("B.java", str1, true, null);
		setReadOnly(cu2);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(fragment);
		descriptor.setNewName("org.test2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu2.getPath()));
	}

	@Test
	public void testPackageRename2() throws Exception {
		// A readonly and moved, B moved, C changes

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		String str= """
			package org.test;
			public class A {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("A.java", str, true, null);
		setReadOnly(cu1);

		String str1= """
			package org.test;
			public class B {
			}
			""";
		fragment.createCompilationUnit("B.java", str1, true, null);
		// not read only

		IPackageFragment fragment2= getRoot().createPackageFragment("org.other", true, null);

		String str2= """
			package org.other;
			public class C extends org.test.A {
			}
			""";
		ICompilationUnit cu3= fragment2.createCompilationUnit("C.java", str2, true, null);
		setReadOnly(cu3);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_PACKAGE);
		descriptor.setJavaElement(fragment);
		descriptor.setNewName("org.test2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu3.getPath()));
	}

	@Test
	public void testPackageRenameWithResource() throws Exception {
		// MyClass readonly and moved, x.properties readonly moved

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
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

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(1, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
	}


	@Test
	public void testPackageRenameWithResource2() throws Exception {
		// MyClass readonly and moved, x.properties readonly moved

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
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

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(file.getFullPath()));
	}

	@Test
	public void testCURename() throws Exception {

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
		setReadOnly(cu1);

		String str1= """
			package org.test;
			public class C extends MyClass {
			}
			""";
		ICompilationUnit cu2= fragment.createCompilationUnit("C.java", str1, true, null);
		setReadOnly(cu2);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		descriptor.setJavaElement(cu1);
		descriptor.setNewName("MyClass2.java");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu2.getPath()));
	}

	@Test
	public void testTypeRename() throws Exception {

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
		setReadOnly(cu1);

		String str1= """
			package org.test;
			public class C extends MyClass {
			}
			""";
		ICompilationUnit cu2= fragment.createCompilationUnit("C.java", str1, true, null);
		setReadOnly(cu2);

		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_TYPE);
		descriptor.setJavaElement(cu1.findPrimaryType());
		descriptor.setNewName("MyClass2");
		descriptor.setUpdateReferences(true);
		RefactoringStatus status= performRefactoring(descriptor);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath()));
		assertTrue(validatedEditPaths.contains(cu2.getPath()));
	}

	@Test
	public void testMoveCU2() throws Exception {
		// Move CU and file: Only CU be validated as file doesn't change

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
		setReadOnly(cu1);

		IFile file= ((IFolder) fragment.getResource()).getFile("x.properties");
		String content= "A file with no references";
		file.create(getStream(content), true, null);
		setReadOnly(file);

		String str1= """
			package org.test;
			public class C extends MyClass {
			}
			""";
		ICompilationUnit cu2= fragment.createCompilationUnit("C.java", str1, true, null);
		setReadOnly(cu2);

		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(new IResource[] { file } , new IJavaElement[] { cu1 });
		assertTrue(policy.canEnable());

		JavaMoveProcessor javaMoveProcessor= new JavaMoveProcessor(policy);
		javaMoveProcessor.setDestination(ReorgDestinationFactory.createDestination(otherFragment));
		javaMoveProcessor.setReorgQueries(new MockReorgQueries());
		RefactoringStatus status= performRefactoring(new MoveRefactoring(javaMoveProcessor));
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath())); // moved and changed
		assertTrue(validatedEditPaths.contains(cu2.getPath())); // changed
	}

	@Test
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

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(1, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(file2.getFullPath())); // replaced
	}

	@Test
	public void testMoveCuWithReplace() throws Exception {
		// Move CU to an existing location

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
		setReadOnly(cu1);


		String str1= """
			package org.test1;
			public class MyClass {
			}
			""";
		ICompilationUnit cu2= otherFragment.createCompilationUnit("MyClass.java", str1, true, null);
		setReadOnly(cu2);


		IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(new IResource[0], new IJavaElement[] { cu1 });
		assertTrue(policy.canEnable());

		JavaMoveProcessor javaMoveProcessor= new JavaMoveProcessor(policy);
		javaMoveProcessor.setDestination(ReorgDestinationFactory.createDestination(otherFragment));
		javaMoveProcessor.setReorgQueries(new MockReorgQueries());
		RefactoringStatus status= performRefactoring(new MoveRefactoring(javaMoveProcessor), false);
		if (status != null)
			assertTrue(status.toString(), status.isOK());

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
		assertEquals(2, validatedEditPaths.size());
		assertTrue(validatedEditPaths.contains(cu1.getPath())); // moved and changed
		assertTrue(validatedEditPaths.contains(cu2.getPath())); // replaced
	}

	@Test
	public void testCopyCuWithReplace() throws Exception {
		// Copy CU to a existing location

		IPackageFragment fragment= getRoot().createPackageFragment("org.test", true, null);
		IPackageFragment otherFragment= getRoot().createPackageFragment("org.test1", true, null);

		String str= """
			package org.test;
			public class MyClass {
			}
			""";
		ICompilationUnit cu1= fragment.createCompilationUnit("MyClass.java", str, true, null);
		setReadOnly(cu1);

		String str1= """
			package org.test1;
			public class MyClass {
			}
			""";
		ICompilationUnit cu2= otherFragment.createCompilationUnit("MyClass.java", str1, true, null);
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

		Collection<IPath> validatedEditPaths= RefactoringTestRepositoryProvider.getValidatedEditPaths(getRoot().getJavaProject().getProject());
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
