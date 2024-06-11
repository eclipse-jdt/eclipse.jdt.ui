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
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * Tests for the PackageExplorerContentProvider. Bugs:
 * <ul>
 * <li>66694 PackageExplorer shows elements twice</li>
 * <li>35851 Content of folders with illegal package name are no shown in package explorer</li>
 * <li>35851 Content of folders with illegal package name are no shown in package explorer</li>
 * <li>35851 Content of folders with illegal package name are no shown in package explorer</li>
 * </ul>
 *
 * @since 3.0+
 */
public class ContentProviderTests5{
	private boolean fEnableAutoBuildAfterTesting;
	private ITreeContentProvider fProvider;

	private IJavaProject fJProject;
	private IFile fDotClasspath;
	private IFolder dotSettings;
	private IFile fDotProject;
	private IPackageFragmentRoot jdk;

	@Before
	public void setUp() throws Exception {

		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		assertNotNull(workspace);
		IWorkspaceDescription workspaceDesc= workspace.getDescription();
		fEnableAutoBuildAfterTesting= workspaceDesc.isAutoBuilding();
		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(false);

		//create project
		fJProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		assertNotNull(fJProject);

		for (Object object : fJProject.getNonJavaResources()) {
			if (object instanceof IFile) {
				IFile file = (IFile) object;
				if (".classpath".equals(file.getName()))
					fDotClasspath = file;
				else if (".project".equals(file.getName()))
					fDotProject = file;
			} else if (object instanceof IFolder) {
				IFolder folder= (IFolder) object;
				if(".settings".equals(folder.getName())) {
					dotSettings= folder;
				}
			}
		}
		assertNotNull(fDotClasspath);
		assertNotNull(fDotProject);
		assertNotNull(dotSettings);

		//add rt.jar
		jdk= JavaProjectHelper.addVariableRTJar(fJProject, "JRE_LIB_TEST", null, null);
		assertNotNull("jdk not found", jdk);

		setUpView();
	}

	private void setUpView() throws PartInitException {
		IWorkbench workbench= PlatformUI.getWorkbench();
		assertNotNull(workbench);

		IWorkbenchPage page= workbench.getActiveWorkbenchWindow().getActivePage();
		assertNotNull(page);

		IViewPart myPart= page.showView("org.eclipse.jdt.ui.PackageExplorer");
		if (myPart instanceof PackageExplorerPart) {
			PackageExplorerPart packageExplorerPart= (PackageExplorerPart) myPart;
			packageExplorerPart.setShowLibrariesNode(false);
			fProvider= (ITreeContentProvider) packageExplorerPart.getTreeViewer().getContentProvider();
			setFolding(false);
		} else {
			fail("Unable to get view");
		}
		assertNotNull(fProvider);
	}

	private void setFolding(boolean fold) {
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.APPEARANCE_FOLD_PACKAGES_IN_PACKAGE_EXPLORER, fold);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject);

		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);
	}

	private ByteArrayInputStream asInputStream(String string) throws UnsupportedEncodingException {
		return new ByteArrayInputStream(string.getBytes(ResourcesPlugin.getEncoding()));
	}

	@Test
	public void testProjectSource1() throws Exception { //bug 35851, 66694
		IPath[] inclusionFilters= {new Path("**"), new Path("excl/incl/")};
		IPath[] exclusionFilters= {new Path("excl/*"), new Path("x/*.java"), new Path("y/")};
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJProject, "", inclusionFilters, exclusionFilters);

		IPackageFragment defaultPackage= root.createPackageFragment("", true, null);

		IFolder ab= fJProject.getProject().getFolder("a-b");
		CoreUtility.createFolder(ab, true, true, null);
		IFile description= ab.getFile("description.txt");
		description.create(asInputStream("description"), true, null);

		IPackageFragment exclInclPackage= root.createPackageFragment("excl.incl", true, null);
		ICompilationUnit In= exclInclPackage.createCompilationUnit("In.java", """
			package excl.incl;\r
			public class In {\r
			}\r
			""", true, null);

		IFolder excl= fJProject.getProject().getFolder("excl");
		IFile Ex= excl.getFile("Ex.java");
		Ex.create(asInputStream("package excl;\npublic class Ex{}"), false, null);

		IPackageFragment xPackage= root.createPackageFragment("x", true, null);
		IFolder x= fJProject.getProject().getFolder("x");
		IFile xhidden= x.getFile(".hidden");
		xhidden.create(asInputStream(""), true, null);
		IFile X= x.getFile("X.java");
		X.create(asInputStream("package x;\r\npublic class X {\r\n\t\r\n}\r\n"), true, null);

		x.copy(new Path("y"), true, null);
		IFolder y= fJProject.getProject().getFolder("y");
		IFile yX= y.getFile("X.java");
		IFile yhidden= y.getFile(".hidden");

		IPackageFragment zPackage= root.createPackageFragment("z", true, null);
		ICompilationUnit Z= zPackage.createCompilationUnit("Z.java", "package z;public class Z{}", true, null);

		assertEqualElements(new Object[] {defaultPackage, exclInclPackage, xPackage, zPackage, jdk, ab, excl, y, fDotClasspath, dotSettings, fDotProject},
				fProvider.getChildren(fJProject));
		assertEqualElements(new Object[0], fProvider.getChildren(defaultPackage));
		assertEqualElements(new Object[] {In},	fProvider.getChildren(exclInclPackage));
		assertEqualElements(new Object[] {Ex},	fProvider.getChildren(excl));
		assertEqualElements(new Object[] {X, xhidden},	fProvider.getChildren(xPackage));
		assertEquals(xPackage,	fProvider.getParent(X));
		assertEquals(xPackage,	fProvider.getParent(xhidden));
		assertEqualElements(new Object[] {Z},	fProvider.getChildren(zPackage));
		assertEqualElements(new Object[] {description},	fProvider.getChildren(ab));
		assertEqualElements(new Object[] {Ex},	fProvider.getChildren(excl));
		assertEqualElements(new Object[] {yX, yhidden},	fProvider.getChildren(y));
	}

	@Test
	public void testNestedSource1() throws Exception { //bug 35851, 66694
//		<classpathentry excluding="a-b/a/b/" kind="src" path="src"/>
//		<classpathentry kind="src" path="src/a-b/a/b"/>
		IPath[] inclusionFilters= {};
		IPath[] exclusionFilters= {new Path("a-b/a/b/")};
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJProject, "src", inclusionFilters, exclusionFilters);
		IPackageFragmentRoot srcabab= JavaProjectHelper.addSourceContainer(fJProject, "src/a-b/a/b", new IPath[0], new IPath[0]);

		IPackageFragment defaultSrc= src.createPackageFragment("", true, null);
		IPackageFragment p= src.createPackageFragment("p", true, null);
		IFile file= ((IFolder) p.getCorrespondingResource()).getFile("file.txt");
		file.create(asInputStream("f"), true, null);

		IFolder ab= ((IFolder) src.getUnderlyingResource()).getFolder("a-b");
		CoreUtility.createFolder(ab, true, true, null);
		IFolder aba= ab.getFolder("a");
		CoreUtility.createFolder(aba, true, true, null);
		IFile abaTxt= aba.getFile("aba.txt");
		abaTxt.create(asInputStream("x"), true, null);

		IPackageFragment defaultAbab= srcabab.createPackageFragment("", true, null);
		ICompilationUnit b= defaultAbab.createCompilationUnit("B.java", "public class B {}", true, null);


		assertEqualElements(new Object[] {src, srcabab, jdk, fDotClasspath, dotSettings, fDotProject}, fProvider.getChildren(fJProject));
		assertEqualElements(new Object[] {defaultSrc, p, ab}, fProvider.getChildren(src));
		assertEqualElements(new Object[] {}, fProvider.getChildren(defaultSrc));
		assertEqualElements(new Object[] {file}, fProvider.getChildren(p));
		assertEqualElements(new Object[] {aba}, fProvider.getChildren(ab));
		assertEqualElements(new Object[] {abaTxt}, fProvider.getChildren(aba));

		assertEqualElements(new Object[] {defaultAbab}, fProvider.getChildren(srcabab));
		assertEqualElements(new Object[] {b}, fProvider.getChildren(defaultAbab));
	}

	@Test
	public void testInclExcl1() throws Exception { //bug 35851, 66694
//		<classpathentry including="a/b/c/" excluding="a/b/c/d/" kind="src" path="src2"/>
		IPath[] inclusionFilters= {new Path("a/b/c/")};
		IPath[] exclusionFilters= {new Path("a/b/c/d/")};
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJProject, "src", inclusionFilters, exclusionFilters);

		IPackageFragment abc= src.createPackageFragment("a.b.c", true, null);
		ICompilationUnit x= abc.createCompilationUnit("X.java", "", true, null);
		IFolder d= ((IFolder) abc.getUnderlyingResource()).getFolder("d");
		CoreUtility.createFolder(d, false, true, null);
		IFile dTxt= d.getFile("d.txt");
		dTxt.create(asInputStream(""), true, null);

		IContainer b= d.getParent().getParent();
		IContainer a= b.getParent();

		assertEqualElements(new Object[] {src, jdk, fDotClasspath, dotSettings, fDotProject}, fProvider.getChildren(fJProject));
		assertEqualElements(new Object[] {abc, a}, fProvider.getChildren(src));
		assertEqualElements(new Object[] {x, d}, fProvider.getChildren(abc));
		assertEqualElements(new Object[] {dTxt}, fProvider.getChildren(d));
		assertEqualElements(new Object[] {b}, fProvider.getChildren(a));
	}

	private void assertEqualElements(Object[] expected, Object[] actual) {
		assertEquals("array length", expected.length, actual.length);
		exp: for (int i= 0; i < expected.length; i++) {
			Object e= expected[i];
			for (Object a : actual) {
				if (e.equals(a))
					continue exp;
			}
			fail("expected[" + i + "] not found in actual:" + Arrays.asList(actual).toString());
		}
	}
}
