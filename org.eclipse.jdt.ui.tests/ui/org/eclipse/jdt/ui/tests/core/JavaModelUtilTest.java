/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;


import java.io.File;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

public class JavaModelUtilTest extends TestCase {
	
	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	private static final IPath SOURCES= new Path("test-resources/junit32-noUI.zip");
	private static final IPath LIB= new Path("test-resources/mylib.jar");

	public JavaModelUtilTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), JavaModelUtilTest.class, args);
	}


	public static Test suite() {
		return new TestSuite(JavaModelUtilTest.class);
	}


	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");

		IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJProject1);
		assertTrue("jdk not found", jdk != null);


		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(SOURCES);
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());
		ZipFile zipfile= new ZipFile(junitSrcArchive);
		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", zipfile);

		File mylibJar= JavaTestPlugin.getDefault().getFileInPlugin(LIB);
		assertTrue("lib not found", junitSrcArchive != null && junitSrcArchive.exists());
		
		JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(mylibJar.getPath()), null, null);

		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		ICompilationUnit cu1= pack1.getCompilationUnit("ReqProjType.java");
		IType type1= cu1.createType("public class ReqProjType { static class Inner { static class InnerInner {} }\n}\n", null, true, null);

		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);
	}


	private void assertElementName(String name, IJavaElement elem, int type) {
		assertNotNull(name, elem);
		assertEquals(name + "-1", name, elem.getElementName());
		assertTrue(name + "-2", type == elem.getElementType());
	}

	public void testFindType() throws Exception {
		IType type= JavaModelUtil.findType(fJProject1, "junit.extensions.ExceptionTestCase");
		assertElementName("ExceptionTestCase", type, IJavaElement.TYPE);

		type= JavaModelUtil.findType(fJProject1, "junit.samples.money.IMoney");
		assertElementName("IMoney", type, IJavaElement.TYPE);	

		type= JavaModelUtil.findType(fJProject1, "junit.tests.TestTest.TornDown");
		assertElementName("TornDown", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "mylib.Foo");
		assertElementName("Foo", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "mylib.Foo.FooInner");
		assertElementName("FooInner", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "mylib.Foo.FooInner.FooInnerInner");
		assertElementName("FooInnerInner", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "pack1.ReqProjType");
		assertElementName("ReqProjType", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "pack1.ReqProjType.Inner");
		assertElementName("Inner", type, IJavaElement.TYPE);	

		type= JavaModelUtil.findType(fJProject1, "pack1.ReqProjType.Inner.InnerInner");
		assertElementName("InnerInner", type, IJavaElement.TYPE);	
	}
	
	public void testFindType2() throws Exception {
		IType type= JavaModelUtil.findType(fJProject1, "junit.extensions", "ExceptionTestCase");
		assertElementName("ExceptionTestCase", type, IJavaElement.TYPE);

		type= JavaModelUtil.findType(fJProject1, "junit.samples.money" , "IMoney");
		assertElementName("IMoney", type, IJavaElement.TYPE);	

		type= JavaModelUtil.findType(fJProject1, "junit.tests", "TestTest.TornDown");
		assertElementName("TornDown", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "mylib" , "Foo");
		assertElementName("Foo", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "mylib", "Foo.FooInner");
		assertElementName("FooInner", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "mylib", "Foo.FooInner.FooInnerInner");
		assertElementName("FooInnerInner", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "pack1", "ReqProjType");
		assertElementName("ReqProjType", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findType(fJProject1, "pack1", "ReqProjType.Inner");
		assertElementName("Inner", type, IJavaElement.TYPE);	

		type= JavaModelUtil.findType(fJProject1, "pack1", "ReqProjType.Inner.InnerInner");
		assertElementName("InnerInner", type, IJavaElement.TYPE);				
	}
	
	public void testFindTypeContainer() throws Exception {
		IJavaElement elem= JavaModelUtil.findTypeContainer(fJProject1, "junit.extensions");
		assertElementName("junit.extensions", elem, IJavaElement.PACKAGE_FRAGMENT);

		elem= JavaModelUtil.findTypeContainer(fJProject1, "junit.tests.TestTest");
		assertElementName("TestTest", elem, IJavaElement.TYPE);
		
		elem= JavaModelUtil.findTypeContainer(fJProject1, "mylib" );
		assertElementName("mylib", elem, IJavaElement.PACKAGE_FRAGMENT);
		
		elem= JavaModelUtil.findTypeContainer(fJProject1, "mylib.Foo");
		assertElementName("Foo", elem, IJavaElement.TYPE);
		
		elem= JavaModelUtil.findTypeContainer(fJProject1, "mylib.Foo.FooInner");
		assertElementName("FooInner", elem, IJavaElement.TYPE);
		
		elem= JavaModelUtil.findTypeContainer(fJProject1, "pack1");
		assertElementName("pack1", elem, IJavaElement.PACKAGE_FRAGMENT);
		
		elem= JavaModelUtil.findTypeContainer(fJProject1, "pack1.ReqProjType");
		assertElementName("ReqProjType", elem, IJavaElement.TYPE);	

		elem= JavaModelUtil.findTypeContainer(fJProject1, "pack1.ReqProjType.Inner");
		assertElementName("Inner", elem, IJavaElement.TYPE);				
	}
	
	public void testFindTypeInCompilationUnit() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/tests/TestTest.java"));
		assertElementName("TestTest.java", cu, IJavaElement.COMPILATION_UNIT);
		
		IType type= JavaModelUtil.findTypeInCompilationUnit(cu, "TestTest");
		assertElementName("TestTest", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findTypeInCompilationUnit(cu, "TestTest.TornDown");
		assertElementName("TornDown", type, IJavaElement.TYPE);
		
		cu= (ICompilationUnit) fJProject1.findElement(new Path("pack1/ReqProjType.java"));
		assertElementName("ReqProjType.java", cu, IJavaElement.COMPILATION_UNIT);
		
		type= JavaModelUtil.findTypeInCompilationUnit(cu, "ReqProjType");
		assertElementName("ReqProjType", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findTypeInCompilationUnit(cu, "ReqProjType.Inner");
		assertElementName("Inner", type, IJavaElement.TYPE);
		
		type= JavaModelUtil.findTypeInCompilationUnit(cu, "ReqProjType.Inner.InnerInner");
		assertElementName("InnerInner", type, IJavaElement.TYPE);		
	}	
	

}
