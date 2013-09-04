/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageHover;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.junit.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.corext.CorextMessages;

import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.text.java.hover.JavadocBrowserInformationControlInput;
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover;


/**
 * Tests for fetching package Javadoc.
 * 
 * @since 3.9
 */
public class PackageJavadocTests extends CoreTests {


	public PackageJavadocTests(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(PackageJavadocTests.class));
	}

	private IJavaProject fJProject1;


	public void testGetDocFromPackageHtml_src() throws Exception {

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));

		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		IJavaElement[] elements= new JavaElement[1];
		elements[0]= pack;
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(elements, cu, new Region(21, 16), null);
		String actualHtmlContent= hoverInfo.getHtml();

		//checking for some part of the expected Javadoc which could be retrieved.
		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("Test package documentation in package.html"));
	}


	public void testGetDocFromPackageInfoJava_src() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));

		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochover/Activator.java"));
		assertNotNull("Activator.java", cu);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		IJavaElement[] elements= new JavaElement[1];
		elements[0]= pack;
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(elements, cu, new Region(21, 12), null);
		String actualHtmlContent= hoverInfo.getHtml();

		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("This is the test content"));
	}

	public void testGetDocFromPackageHtml_archive() throws Exception {

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));

		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		IJavaElement[] elements= new JavaElement[1];
		elements[0]= pack;
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(elements, cu, new Region(67, 10), null);
		String actualHtmlContent= hoverInfo.getHtml();

		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("Test package documentation in package.html"));
	}

	public void testGetDocFromPackageInfoJava_archive() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);

		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/PackageJavadocTests/testData.zip"));
		File srcJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/PackageJavadocTests/testData_src.zip"));


		JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), new Path(srcJarPath.getAbsolutePath()), null);
		fJProject1.open(null);
		IPackageFragmentRoot jarRoot= this.fJProject1.getPackageFragmentRoot(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/TestSetupProject/testData.zip")));
		IPackageFragment packageFragment= jarRoot.getPackageFragment("org.eclipse.jdt.ui.tests");
		assertNotNull(packageFragment);
		IPath sourceAttachmentPath= jarRoot.getSourceAttachmentPath();
		Assert.assertNotNull(sourceAttachmentPath);

		int offset= cu.getSource().indexOf("org.eclipse.jdt.ui.tests");
		int length= "org.eclipse.jdt.ui.tests".length();
		IJavaElement[] codeSelect= cu.codeSelect(offset, length);
		Assert.assertNotNull(codeSelect);
		Assert.assertTrue("No package found !", codeSelect.length > 0);
		packageFragment= (IPackageFragment)codeSelect[0];
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(new IJavaElement[] { packageFragment }, cu, new Region(offset, length), null);
		String actualHtmlContent= hoverInfo.getHtml();

		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("This is the package documentation for org.eclipse.jdt.ui.tests"));

	}


	public void testGetDocFromSourceAttachmentRootPath() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);

		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/PackageJavadocTests/testData.zip"));
		File srcJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/PackageJavadocTests/testData_src.zip"));


		JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), new Path(srcJarPath.getAbsolutePath()), new Path("src"));
		fJProject1.open(null);
		IPackageFragmentRoot jarRoot= this.fJProject1.getPackageFragmentRoot(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/TestSetupProject/testData.zip")));
		Assert.assertTrue(jarRoot != null && jarRoot.exists());
		IPackageFragment packageFragment= jarRoot.getPackageFragment("org.eclipse.jdt.ui.tests.html");
		Assert.assertTrue(packageFragment != null && packageFragment.exists());
		IPath sourceAttachmentPath= jarRoot.getSourceAttachmentPath();
		Assert.assertNotNull(sourceAttachmentPath);

		int offset= cu.getSource().indexOf("org.eclipse.jdt.ui.tests.html");
		int length= "org.eclipse.jdt.ui.tests.html".length();
		IJavaElement[] codeSelect= cu.codeSelect(offset, length);
		Assert.assertNotNull(codeSelect);
		Assert.assertTrue("No package found !", codeSelect.length > 0);
		packageFragment= (IPackageFragment)codeSelect[0];
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(new IJavaElement[] { packageFragment }, cu, new Region(offset, length), null);
		String actualHtmlContent= hoverInfo.getHtml();


		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("This is the package documentation for org.eclipse.jdt.ui.tests.html."));

	}


	public void testGetPackageAttacheddoc() throws Exception {
		//  http://download.oracle.com/javase/6/docs/api/
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));

		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);


		IClasspathAttribute attribute=
				JavaCore.newClasspathAttribute(
						IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
						"url:http://download.oracle.com/javase/6/docs/api/");
		IClasspathEntry[] rawClasspath= fJProject1.getRawClasspath();
		IClasspathEntry newEntry= JavaCore.newLibraryEntry(new Path(JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/rtstubs15.jar")).getAbsolutePath()), null, null, null,
				new IClasspathAttribute[] { attribute }, false);
		rawClasspath[0]= newEntry;
		IClasspathEntry[] newPathEntry= new IClasspathEntry[] { rawClasspath[0], rawClasspath[1] };
		this.fJProject1.setRawClasspath(newPathEntry, null);
		this.fJProject1.getResolvedClasspath(false);

		int offset= cu.getSource().indexOf("java.math");
		int length= "java.math".length();
		IJavaElement[] codeSelect= cu.codeSelect(offset, length);
		Assert.assertNotNull(codeSelect);
		Assert.assertTrue("No package found !", codeSelect.length > 0);

		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(codeSelect, cu, new Region(offset, length), null);
		String actualHtmlContent= hoverInfo.getHtml();
		Assert.assertNotNull(actualHtmlContent, actualHtmlContent);

		try {
			//trying to connect to the internet. Exception will be thrown if there is no net connection.
			new URL("url:http://download.oracle.com/").openConnection().connect();
			Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("Provides classes for performing arbitrary-precision integer arithmetic"));
		} catch (Exception e) {
			//there is no internet connection, so the Javadoc cannot be retrieved.
			Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains(CorextMessages.JavaDocLocations_noAttachedSource) || actualHtmlContent.contains(CorextMessages.JavaDocLocations_error_gettingJavadoc)
					|| actualHtmlContent.contains(CorextMessages.JavaDocLocations_error_gettingAttachedJavadoc));
		}

	}

	/**
	 * Package Javadoc hover throws NullPointerException if package-info.java contains references.
	 * This test case is to test the fix.
	 * 
	 * @throws Exception when the test case fails
	 */
	public void testPackageInfoWithReferenceLinks() throws Exception {

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));

		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/checkPackageInfo/TestNPEHover.java"));
		assertNotNull("TestNPEHover.java", cu);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		IJavaElement[] elements= new JavaElement[1];
		elements[0]= pack;
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(elements, cu, new Region(21, 12), null);
		String actualHtmlContent= hoverInfo.getHtml();

		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains("The pack is a test package. This doc contains references"));
	}

	public void testPackageWithNoJavadoc() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);

		File clsJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/PackageJavadocTests/testData.zip"));
		File srcJarPath= JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/PackageJavadocTests/testData_src.zip"));


		JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(clsJarPath.getAbsolutePath()), new Path(srcJarPath.getAbsolutePath()), new Path("src"));
		fJProject1.open(null);
		IPackageFragmentRoot jarRoot= this.fJProject1.getPackageFragmentRoot(ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/TestSetupProject/testData.zip")));
		Assert.assertTrue(jarRoot != null && jarRoot.exists());
		IPackageFragment packageFragment= jarRoot.getPackageFragment("org.eclipse.jdt.ui.tests.noJavadoc");
		Assert.assertTrue(packageFragment != null && packageFragment.exists());
		IPath sourceAttachmentPath= jarRoot.getSourceAttachmentPath();
		Assert.assertNotNull(sourceAttachmentPath);

		int offset= cu.getSource().indexOf("org.eclipse.jdt.ui.tests.noJavadoc");
		int length= "org.eclipse.jdt.ui.tests.noJavadoc".length();
		IJavaElement[] codeSelect= cu.codeSelect(offset, length);
		Assert.assertNotNull(codeSelect);
		Assert.assertTrue("No package found !", codeSelect.length > 0);
		packageFragment= (IPackageFragment)codeSelect[0];
		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(new IJavaElement[] { packageFragment }, cu, new Region(offset, length), null);
		String actualHtmlContent= hoverInfo.getHtml();
		
		Assert.assertTrue(actualHtmlContent, actualHtmlContent.contains(CorextMessages.JavaDocLocations_noAttachedJavadoc));
	}

	public void testFailToAccessAttachedJavadoc() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/PackageJavadocTests/JavadocHover_src.zip"));

		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		ICompilationUnit cu= (ICompilationUnit)fJProject1.findElement(new Path("junit/javadochoverhtml/JavaDocHoverTest.java"));
		assertNotNull("JavaDocHoverTest.java", cu);

		//set a wrong Javadoc location URL
		IClasspathAttribute attribute=
				JavaCore.newClasspathAttribute(
						IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
						"url:http://download.oracle.com/javase/6/docs/apii/");
		IClasspathEntry[] rawClasspath= fJProject1.getRawClasspath();
		IClasspathEntry newEntry= JavaCore.newLibraryEntry(new Path(JavaTestPlugin.getDefault().getFileInPlugin(new Path("/testresources/rtstubs15.jar")).getAbsolutePath()), null, null, null,
				new IClasspathAttribute[] { attribute }, false);
		rawClasspath[0]= newEntry;
		IClasspathEntry[] newPathEntry= new IClasspathEntry[] { rawClasspath[0], rawClasspath[1] };
		this.fJProject1.setRawClasspath(newPathEntry, null);
		this.fJProject1.getResolvedClasspath(false);

		int offset= cu.getSource().indexOf("java.math");
		int length= "java.math".length();
		IJavaElement[] codeSelect= cu.codeSelect(offset, length);
		Assert.assertNotNull(codeSelect);
		Assert.assertTrue("No package found !", codeSelect.length > 0);

		JavadocBrowserInformationControlInput hoverInfo= JavadocHover.getHoverInfo(codeSelect, cu, new Region(offset, length), null);
		String actualHtmlContent= hoverInfo.getHtml();
		Assert.assertNotNull(actualHtmlContent);

		// Need to check both conditions for now. See https://bugs.eclipse.org/403036 and https://bugs.eclipse.org/403154 for details.
		Assert.assertTrue(actualHtmlContent,
				actualHtmlContent.contains(CorextMessages.JavaDocLocations_error_gettingAttachedJavadoc) || actualHtmlContent.contains(CorextMessages.JavaDocLocations_noAttachedSource));

	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();

	}


	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

}
