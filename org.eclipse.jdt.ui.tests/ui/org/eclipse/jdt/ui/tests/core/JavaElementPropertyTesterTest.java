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
package org.eclipse.jdt.ui.tests.core;


import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.manipulation.JavaElementPropertyTester;

public class JavaElementPropertyTesterTest extends TestCase {

	private static final Class THIS= JavaElementPropertyTesterTest.class;

	private IJavaProject fJProject1;
	private IJavaProject fOtherProject,  fOtherClosedProject;

	private static final IPath LIB= new Path("testresources/mylib.jar");

	private IPackageFragmentRoot fJDK;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragmentRoot fLocalArchive;
	private IPackageFragmentRoot fFolder;
	private IPackageFragment fPack;
	private ICompilationUnit fCU;

	public JavaElementPropertyTesterTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new JavaElementPropertyTesterTest("testFindType"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		fOtherProject= JavaCore.create(createSimpleProject("SimpleProject", true));
		fOtherClosedProject= JavaCore.create(createSimpleProject("SimpleProject", false));

		fJProject1= JavaProjectHelper.createJavaProject("Test", "bin");

		fJDK= JavaProjectHelper.addRTJar(fJProject1);
		assertTrue("jdk not found", fJDK != null);
		assertTrue(fJDK.exists());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPack= fSourceFolder.createPackageFragment("org.test", true, null);
		fCU= fPack.createCompilationUnit("A.java", "package org.test; class A { }", true, null);

		File file= JavaTestPlugin.getDefault().getFileInPlugin(LIB);
		assertTrue("lib not found", file != null && file.exists());

		fLocalArchive= JavaProjectHelper.addLibraryWithImport(fJProject1, Path.fromOSString(file.getPath()), null, null);

		IFolder folder= fJProject1.getProject().getFolder("doc");
		folder.create(true, true, null);

		fFolder= fJProject1.getPackageFragmentRoot(folder);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		fOtherProject.getProject().delete(true, null);
		fOtherClosedProject.getProject().delete(true, null);
	}

	private static IProject createSimpleProject(String projectName, boolean open) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		if (open && !project.isOpen()) {
			project.open(null);
		}
		return project;
	}


	public void testJavaElementPropertyTester() {
		JavaElementPropertyTester tester= new JavaElementPropertyTester();

		assertEquals(true, tester.test(fJProject1, JavaElementPropertyTester.NAME, new Object[0], "Test"));
		assertEquals(true, tester.test(fJProject1, JavaElementPropertyTester.NAME, new Object[0], "T.*"));
		assertEquals(false, tester.test(fJProject1, JavaElementPropertyTester.NAME, new Object[0], "Tests"));


		IJavaElement[] allElements=
			{ fJProject1, fOtherProject, fOtherClosedProject, fJProject1.getJavaModel(), fJDK, fSourceFolder, fLocalArchive, fFolder, fPack, fCU };
		boolean[] expectedResult= {false, false, false, false, true, true, true, false, true, true };

		for (int i= 0; i < expectedResult.length; i++) {
			boolean actual= tester.test(allElements[i], JavaElementPropertyTester.IS_ON_CLASSPATH, new Object[0], null);
			assertEquals(allElements[i].getElementName(), expectedResult[i], actual);
		}

		boolean[] expectedResult2= {false, false, false, false, false, true, false, false, true, true };
		for (int i= 0; i < expectedResult2.length; i++) {
			boolean actual= tester.test(allElements[i], JavaElementPropertyTester.IN_SOURCE_FOLDER, new Object[0], null);
			assertEquals(allElements[i].getElementName(), expectedResult2[i], actual);
		}

		boolean[] expectedResult3= {false, false, false, false, true, false, true, false, false, false };
		for (int i= 0; i < expectedResult3.length; i++) {
			boolean actual= tester.test(allElements[i], JavaElementPropertyTester.IN_ARCHIVE, new Object[0], null);
			assertEquals(allElements[i].getElementName(), expectedResult3[i], actual);
		}

		boolean[] expectedResult4= {false, false, false, false, true, false, false, false, false, false };
		for (int i= 0; i < expectedResult4.length; i++) {
			boolean actual= tester.test(allElements[i], JavaElementPropertyTester.IN_EXTERNAL_ARCHIVE, new Object[0], null);
			assertEquals(allElements[i].getElementName(), expectedResult4[i], actual);
		}

		Object[] args= { JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5 };
		assertEquals(true, tester.test(fJProject1, JavaElementPropertyTester.PROJECT_OPTION, args , null));
		assertEquals(false, tester.test(fOtherProject, JavaElementPropertyTester.PROJECT_OPTION, args , null));
		assertEquals(false, tester.test(fOtherClosedProject, JavaElementPropertyTester.PROJECT_OPTION, args , null));

		fJProject1.setOption(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);

		assertEquals(false, tester.test(fJProject1, JavaElementPropertyTester.PROJECT_OPTION, args, null));

		assertEquals(true, tester.test(fJProject1, JavaElementPropertyTester.HAS_TYPE_ON_CLASSPATH, args, "org.test.A"));
		assertEquals(false, tester.test(fJProject1, JavaElementPropertyTester.HAS_TYPE_ON_CLASSPATH, args, "junit.framework.Test"));
		assertEquals(false, tester.test(fOtherClosedProject, JavaElementPropertyTester.HAS_TYPE_ON_CLASSPATH, args, "org.test.A"));

		assertEquals(true, tester.test(fSourceFolder, JavaElementPropertyTester.HAS_TYPE_ON_CLASSPATH, args, "java.util.ArrayList"));

	}


}
