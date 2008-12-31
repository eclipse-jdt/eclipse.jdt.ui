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
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;

import org.eclipse.jdt.ui.tests.refactoring.ParticipantTesting;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;


public class MultiMoveTest extends RefactoringTest {

	private static final Class clazz= MultiMoveTest.class;
	private static final String REFACTORING_PATH= "MultiMove/";

	public MultiMoveTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fIsPreDeltaTest= true;
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

	private void delete(IPackageFragmentRoot root) throws Exception {
		performDummySearch();
		try {
			if (root != null && root.exists())
				root.delete(IResource.FORCE, IPackageFragmentRoot.ORIGINATING_PROJECT_CLASSPATH, null);
		} catch(JavaModelException e) {
			//ignore, we should keep going
			e.printStackTrace();
		}
	}

	//--------
	public void test0() throws Exception{
		ParticipantTesting.reset();
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

			String[] handles= ParticipantTesting.createHandles(new Object[] {
				p1A, p1A.getTypes()[0],
				p1B, p1B.getTypes()[0],
				p1A.getResource(), p1B.getResource()});

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A, p1B};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(packP2));
			processor.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, true);

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
			ParticipantTesting.testMove(
				handles,
				new MoveArguments[] {
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2.getResource(), processor.getUpdateReferences()),
					new MoveArguments(packP2.getResource(), processor.getUpdateReferences())
				});

		} finally {
			delete(packP1);
			delete(packP2);
		}
	}


	public void test1() throws Exception{
		ParticipantTesting.reset();
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

			String[] handles= ParticipantTesting.createHandles(new Object[] {
				p1A, p1A.getTypes()[0],
				p1B, p1B.getTypes()[0],
				p1A.getResource(), p1B.getResource()});

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A, p1B};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(packP2));
			processor.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, true);

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
			ParticipantTesting.testMove(
				handles,
				new MoveArguments[] {
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2.getResource(), processor.getUpdateReferences()),
					new MoveArguments(packP2.getResource(), processor.getUpdateReferences())
				});
		} finally {
			delete(packP1);
			delete(packP2);
		}
	}

	public void test2() throws Exception{
		ParticipantTesting.reset();
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

			String[] handles= ParticipantTesting.createHandles(new Object[] {
				p1A, p1A.getTypes()[0],
				p1A.getResource()});

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(packP2));
			processor.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, true);

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

			ParticipantTesting.testMove(
				handles,
				new MoveArguments[] {
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2.getResource(), processor.getUpdateReferences()),
				});
		} finally {
			delete(packP1);
			delete(packP2);
		}
	}

	public void test3() throws Exception{
		ParticipantTesting.reset();
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

			String[] handles= ParticipantTesting.createHandles(new Object[] {
				p1A, p1A.getTypes()[0],
				p1A.getResource()});

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1A};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(packP2));
			processor.setUpdateReferences(true);
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, true);

			//-- checks
			assertEquals("status should be ok here", null, status);

			assertEquals("p1 files", 0, packP1.getChildren().length);
			assertEquals("p2 files", 1, packP2.getChildren().length);
			assertEquals("p1 files", 1, packP3.getChildren().length);

			String expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p2Name + "/Outer.java");
			assertEqualLines("incorrect update of Outer", expectedSource, packP2.getCompilationUnit("Outer.java").getSource());

			expectedSource= getFileContents(getRefactoringPath() + getName() + outDir + p3Name + "/Test.java");
			assertEqualLines("incorrect update of Test", expectedSource, packP3.getCompilationUnit("Test.java").getSource());
			ParticipantTesting.testMove(
				handles,
				new MoveArguments[] {
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2, processor.getUpdateReferences()),
					new MoveArguments(packP2.getResource(), processor.getUpdateReferences()),
				});

		} finally {
			delete(packP1);
			delete(packP2);
			delete(packP3);
		}
	}

	public void testPackageMoveParticipants() throws Exception {
		ParticipantTesting.reset();
		IPackageFragmentRoot r1= null;
		IPackageFragmentRoot r2= null;
		try {
			r1= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "src1");
			r2= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "src2");
			IPackageFragment p1= r1.createPackageFragment("p1", true, null);
			p1.createCompilationUnit("A.java", "public class A {}", true, null);
			p1.createCompilationUnit("B.java", "public class B {}", true, null);

			String[] moveHandes= ParticipantTesting.createHandles(new Object[] {p1, p1.getResource()});

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(r2));
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			ParticipantTesting.testMove(
				moveHandes,
				new MoveArguments[] {
					new MoveArguments(r2, processor.getUpdateReferences()),
					new MoveArguments(r2.getResource(), processor.getUpdateReferences()),
				});
		} finally {
			delete(r1);
			delete(r2);
		}
	}

	public void testPackageMoveParticipants2() throws Exception {
		fIsPreDeltaTest= false;
		ParticipantTesting.reset();
		IPackageFragmentRoot r1= null;
		IPackageFragmentRoot r2= null;
		try {
			r1= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "src1");
			r2= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "src2");
			IPackageFragment p1= r1.createPackageFragment("p1", true, null);
			r1.createPackageFragment("p1.p2", true, null);
			ICompilationUnit c1= p1.createCompilationUnit("A.java", "public class A {}", true, null);
			IFile file= ((IContainer)p1.getResource()).getFile(new Path("Z.txt"));
			file.create(getStream("123"), true, null);

			String[] moveHandles= ParticipantTesting.createHandles(new Object[] {
				p1, c1.getResource(), file });

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(r2));
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			IPath path= r2.getResource().getFullPath();
			path= path.append(p1.getElementName().replace('.', '/'));
			IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
			String[] createHandles= ParticipantTesting.createHandles(new Object[] {target});

			ParticipantTesting.testCreate(createHandles);

			ParticipantTesting.testMove(
				moveHandles,
				new MoveArguments[] {
					new MoveArguments(r2, processor.getUpdateReferences()),
					new MoveArguments(target, processor.getUpdateReferences()),
					new MoveArguments(target, processor.getUpdateReferences()),
				});
		} finally {
			delete(r1);
			delete(r2);
		}
	}

	public void testPackageMoveParticipants3() throws Exception {
		ParticipantTesting.reset();
		IPackageFragmentRoot r1= null;
		IPackageFragmentRoot r2= null;
		try {
			r1= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "src1");
			r2= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), "src2");
			IPackageFragment p1= r1.createPackageFragment("p1", true, null);
			r2.createPackageFragment("p1", true, null);
			ICompilationUnit c1= p1.createCompilationUnit("A.java", "public class A {}", true, null);

			String[] moveHandles= ParticipantTesting.createHandles(new Object[] {
				p1, c1.getResource()});
			String[] deleteHandles= ParticipantTesting.createHandles(new Object[] {p1.getResource()});

			IResource[] resources= {};
			IJavaElement[] javaElements= {p1};
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			JavaMoveProcessor processor= (policy.canEnable() ? new JavaMoveProcessor(policy) : null);
			processor.setReorgQueries(new MockReorgQueries());
			processor.setDestination(ReorgDestinationFactory.createDestination(r2));
		    performDummySearch();
			RefactoringStatus status= performRefactoring(processor, false);

			//-- checks
			assertEquals("status should be ok here", null, status);

			IPath path= r2.getResource().getFullPath();
			path= path.append(p1.getElementName().replace('.', '/'));
			IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);

			ParticipantTesting.testDelete(deleteHandles);

			ParticipantTesting.testMove(
				moveHandles,
				new MoveArguments[] {
					new MoveArguments(r2, processor.getUpdateReferences()),
					new MoveArguments(target, processor.getUpdateReferences()),
				});
		} finally {
			delete(r1);
			delete(r2);
		}
	}

	private RefactoringStatus performRefactoring(JavaMoveProcessor processor, boolean providesUndo) throws Exception {
		return performRefactoring(new MoveRefactoring(processor), providesUndo);
	}
}
