/*******************************************************************************
 * Copyright (c) 2017, 2020 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.ClasspathAttribute;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;

/**
 * Tests for the PackageExplorerContentProvider.
 * <ul>
 * <li>Bug 501507: [1.9] Consider displaying the module-info.java file in the source folder root
 * </ul>
 *
 * @since 3.14
 */
public class ContentProviderTests7 {

	private static Object[] NO_CHILDREN= new Object[0];

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSrcFolder;

	// test matrix:
	//	 Criterion (true/false):														NameSegment:
	// 		no ordinary CU in dlft package	/ with ordinary CU in dflt package	=		Module1 		/ Module2
	//		hierarchical layout				/ flat layout		 				= 		Hierarchical	/ Flat
	// 		empty module-info				/ real module						= 		Empty			/
	//   Checking in each test:
	// 		- children of fSrcFolder
	//		- children of the default package
	private Object[][] runTest(boolean withClassInDfltPack, boolean isFlatLayout, boolean withModuleContent) throws JavaModelException {
		IPackageFragment defaultPackage= fSrcFolder.getPackageFragment("");
		defaultPackage.createCompilationUnit("module-info.java", withModuleContent ? "module M {}" : "", true, null);
		IPackageFragment p= fSrcFolder.createPackageFragment("p", true, null);
		createEmptyClass(p, "PC", "p");
		IPackageFragment q= fSrcFolder.createPackageFragment("p.q", true, null);
		createEmptyClass(q, "QC", "p.q");
		if (withClassInDfltPack) {
			createEmptyClass(fSrcFolder.getPackageFragment(""), "DefC", null);
		}
		return new Object[][] {
			getChildren(fSrcFolder, isFlatLayout),
			getChildren(fSrcFolder.getPackageFragment(""), isFlatLayout)
		};
	}

	private void createEmptyClass(IPackageFragment fragment, String name, String packageName) throws JavaModelException {
		String packDecl= packageName != null ? "package "+packageName+";\n" : "";
		fragment.createCompilationUnit(name+".java", packDecl+"public class "+name+" {}\n", true, null);
	}

	private IJavaElement getModuleInfo() {
		return fSrcFolder.getPackageFragment("").getCompilationUnit("module-info.java");
	}

	private Object[] getChildren(IJavaElement src, boolean isFlatLayout) {
		PackageExplorerContentProvider provider= new PackageExplorerContentProvider(false);
		provider.setIsFlatLayout(isFlatLayout);
		return provider.getChildren(src);
	}

	private void assertResults(Object[][] actual, Object[] expectedSrcFolderChildren, Object[] expectedDefaultPkgChildren) {
		assertTrue("Wrong children found for source folder", compareArrays(actual[0], expectedSrcFolderChildren));
		assertTrue("Wrong children found for default package", compareArrays(actual[1], expectedDefaultPkgChildren));//$NON-NLS-1$
	}

	@Test
	public void testModule1EmptyHierarchical() throws Exception {
		Object[][] actualResult= runTest(false, false, false);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment("p"),
		};
		assertResults(actualResult, expectedChildren, NO_CHILDREN);
	}

	@Test
	public void testModule1Hierarchical() throws Exception {
		Object[][] actualResult= runTest(false, false, true);
		Object[] expectedChildren= new Object[]{
				getModuleInfo(),
				fSrcFolder.getPackageFragment("p"),
		};
		assertResults(actualResult, expectedChildren, NO_CHILDREN);
	}

	@Test
	public void testModule1EmptyFlat() throws Exception {
		Object[][] actualResult= runTest(false, true, false);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment("p"),
				fSrcFolder.getPackageFragment("p.q")
		};
		assertResults(actualResult, expectedChildren, NO_CHILDREN);
	}

	@Test
	public void testModule1Flat() throws Exception {
		Object[][] actualResult= runTest(false, true, true);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment("p"),
				fSrcFolder.getPackageFragment("p.q")
		};
		assertResults(actualResult, expectedChildren, NO_CHILDREN);
	}

	@Test
	public void testModule2EmptyHierarchical() throws Exception {
		Object[][] actualResult= runTest(true, false, false);
		createEmptyClass(fSrcFolder.getPackageFragment(""), "DefC", null);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment(""),
				fSrcFolder.getPackageFragment("p"),
		};
		assertResults(actualResult, expectedChildren, new Object[] {fSrcFolder.getPackageFragment("").getCompilationUnit("DefC.java") } );
	}

	@Test
	public void testModule2Hierarchical() throws Exception {
		Object[][] actualResult= runTest(true, false, true);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment(""),
				fSrcFolder.getPackageFragment("p"),
		};
		assertResults(actualResult, expectedChildren, new Object[] {fSrcFolder.getPackageFragment("").getCompilationUnit("DefC.java") } );
	}

	@Test
	public void testModule2EmptyFlat() throws Exception {
		Object[][] actualResult= runTest(true, true, false);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment(""),
				fSrcFolder.getPackageFragment("p"),
				fSrcFolder.getPackageFragment("p.q")
		};
		assertResults(actualResult, expectedChildren, new Object[] {fSrcFolder.getPackageFragment("").getCompilationUnit("DefC.java") } );
	}

	@Test
	public void testModule2Flat() throws Exception {
		Object[][] actualResult= runTest(true, true, true);
		Object[] expectedChildren= new Object[] {
				getModuleInfo(),
				fSrcFolder.getPackageFragment(""),
				fSrcFolder.getPackageFragment("p"),
				fSrcFolder.getPackageFragment("p.q")
		};
		assertResults(actualResult, expectedChildren, new Object[] {fSrcFolder.getPackageFragment("").getCompilationUnit("DefC.java") } );
	}

	/*
	 * @see TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		fJProject= JavaProjectHelper.createJavaProject("TestProject", "bin");//$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("project null", fJProject);//$NON-NLS-1$
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS_9);
		JavaProjectHelper.set9CompilerOptions(fJProject);
		IClasspathAttribute[] attributes= { new ClasspathAttribute(IClasspathAttribute.MODULE, "true") };
		IClasspathEntry cpe= JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], null, attributes, false);
		JavaProjectHelper.addToClasspath(fJProject, cpe);


		//set up project : Add classFolder
		fSrcFolder= JavaProjectHelper.addSourceContainer(fJProject, "src", null, null); //$NON-NLS-1$
		assertNotNull("source folder null", fSrcFolder); //$NON-NLS-1$
	}

	private boolean compareArrays(Object[] children, Object[] expectedChildren) {
		if(children.length!=expectedChildren.length)
			return false;
		for (Object child : children) {
			if (child instanceof IJavaElement) {
				IJavaElement el= (IJavaElement) child;
				if(!contains(el, expectedChildren))
					return false;
			} else if(child instanceof IResource){
				IResource res= (IResource) child;
				if(!contains(res, expectedChildren)){
					return false;
				}
			}
		}
		return true;
	}
	private boolean contains(IResource res, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IResource) {
				IResource expres= (IResource) object;
				if(expres.equals(res))
					return true;
			}
		}
		return false;
	}
	private boolean contains(IJavaElement fragment, Object[] expectedChildren) {
		for (Object object : expectedChildren) {
			if (object instanceof IJavaElement) {
				IJavaElement expfrag= (IJavaElement) object;
				if(expfrag.equals(fragment))
					return true;
			}
		}
		return false;
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);
	}
}