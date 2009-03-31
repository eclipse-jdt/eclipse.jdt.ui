/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.util.CoreUtility;


public class JavaModelUtilTest extends TestCase {

	private static final Class THIS= JavaModelUtilTest.class;

	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	private boolean fEnableAutoBuildAfterTesting;

	private static final IPath LIB= new Path("testresources/mylib.jar");

	public JavaModelUtilTest(String name) {
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
			suite.addTest(new JavaModelUtilTest("testFindType"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		IWorkspace workspace= JavaTestPlugin.getWorkspace();
		assertNotNull(workspace);

		// disable auto-build
		IWorkspaceDescription workspaceDesc= workspace.getDescription();
		if (workspaceDesc.isAutoBuilding()) {
			fEnableAutoBuildAfterTesting= true;
			CoreUtility.setAutoBuilding(false);
		}

		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");

		IPackageFragmentRoot jdk= JavaProjectHelper.addVariableRTJar(fJProject1, "JRE_LIB_TEST", null, null);
		assertTrue("jdk not found", jdk != null);

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

		File mylibJar= JavaTestPlugin.getDefault().getFileInPlugin(LIB);
		assertTrue("lib not found", junitSrcArchive != null && junitSrcArchive.exists());

		JavaProjectHelper.addLibraryWithImport(fJProject1, Path.fromOSString(mylibJar.getPath()), null, null);

		JavaProjectHelper.addVariableEntry(fJProject2, new Path("JRE_LIB_TEST"), null, null);

		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJProject2, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);

		ICompilationUnit cu1= pack1.getCompilationUnit("ReqProjType.java");
		cu1.createType("public class ReqProjType { static class Inner { static class InnerInner {} }\n}\n", null, true, null);

		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);

		if (fEnableAutoBuildAfterTesting)
			CoreUtility.setAutoBuilding(true);
	}


	private void assertElementName(String name, IJavaElement elem, int type) {
		assertNotNull(name, elem);
		assertEquals(name + "-name", name, elem.getElementName());
		assertTrue(name + "-type", type == elem.getElementType());
	}

	public void testFindType() throws Exception {
		IType type= fJProject1.findType("junit.extensions.ExceptionTestCase");
		assertElementName("ExceptionTestCase", type, IJavaElement.TYPE);

		type= fJProject1.findType("junit.samples.money.IMoney");
		assertElementName("IMoney", type, IJavaElement.TYPE);

		type= fJProject1.findType("junit.tests.framework.TestCaseTest.TornDown");
		assertElementName("TornDown", type, IJavaElement.TYPE);

		type= fJProject1.findType("mylib.Foo");
		assertElementName("Foo", type, IJavaElement.TYPE);

		type= fJProject1.findType("mylib.Foo.FooInner");
		assertElementName("FooInner", type, IJavaElement.TYPE);

		type= fJProject1.findType("mylib.Foo.FooInner.FooInnerInner");
		assertElementName("FooInnerInner", type, IJavaElement.TYPE);

		type= fJProject1.findType("pack1.ReqProjType");
		assertElementName("ReqProjType", type, IJavaElement.TYPE);

		type= fJProject1.findType("pack1.ReqProjType.Inner");
		assertElementName("Inner", type, IJavaElement.TYPE);

		type= fJProject1.findType("pack1.ReqProjType.Inner.InnerInner");
		assertElementName("InnerInner", type, IJavaElement.TYPE);
	}

	public void testFindTypeContainer() throws Exception {
		IJavaElement elem= JavaModelUtil.findTypeContainer(fJProject1, "junit.extensions");
		assertElementName("junit.extensions", elem, IJavaElement.PACKAGE_FRAGMENT);

		elem= JavaModelUtil.findTypeContainer(fJProject1, "junit.tests.framework.TestCaseTest");
		assertElementName("TestCaseTest", elem, IJavaElement.TYPE);

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
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/tests/framework/TestCaseTest.java"));
		assertElementName("TestCaseTest.java", cu, IJavaElement.COMPILATION_UNIT);

		IType type= JavaModelUtil.findTypeInCompilationUnit(cu, "TestCaseTest");
		assertElementName("TestCaseTest", type, IJavaElement.TYPE);

		type= JavaModelUtil.findTypeInCompilationUnit(cu, "TestCaseTest.TornDown");
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

	private void assertClasspathEntry(String name, IJavaElement elem, IPath path, int type) throws Exception {
		IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(elem);
		assertNotNull(name + "-noroot", root);
		IClasspathEntry entry= root.getRawClasspathEntry();
		assertNotNull(name + "-nocp", entry);
		assertEquals(name + "-wrongpath", entry.getPath(), path);
		assertTrue(name + "-wrongtype", type == entry.getEntryKind());
	}

	public void testGetRawClasspathEntry() throws Exception {
		IType type= fJProject1.findType("junit.extensions.ExceptionTestCase");
		assertElementName("ExceptionTestCase", type, IJavaElement.TYPE);
		IPath path= fJProject1.getProject().getFullPath().append("src");
		assertClasspathEntry("ExceptionTestCase", type, path, IClasspathEntry.CPE_SOURCE);

		type= fJProject1.findType("mylib.Foo");
		assertElementName("Foo", type, IJavaElement.TYPE);
		path= fJProject1.getProject().getFullPath().append(LIB.lastSegment());
		assertClasspathEntry("Foo", type, path, IClasspathEntry.CPE_LIBRARY);

		type= fJProject1.findType("java.lang.Object");
		assertElementName("Object", type, IJavaElement.TYPE);
		path= new Path("JRE_LIB_TEST");
		assertClasspathEntry("Object", type, path, IClasspathEntry.CPE_VARIABLE);

		type= fJProject1.findType("pack1.ReqProjType");
		assertElementName("ReqProjType", type, IJavaElement.TYPE);
		path= fJProject2.getProject().getFullPath().append("src");
		assertClasspathEntry("ReqProjType", type, path, IClasspathEntry.CPE_SOURCE);
	}

	private void assertFindMethod(String methName, String[] paramTypeNames, boolean isConstructor, IType type) throws Exception {
		String[] sig= new String[paramTypeNames.length];
		for (int i= 0; i < paramTypeNames.length; i++) {
			// create as unresolved
			String name= Signature.getSimpleName(paramTypeNames[i]);
			sig[i]= Signature.createTypeSignature(name, false);
			assertNotNull(methName + "-ts1" + i, sig[i]);
		}
		IMethod meth= JavaModelUtil.findMethod(methName, sig, isConstructor, type);
		assertElementName(methName, meth, IJavaElement.METHOD);
		assertTrue("methName-nparam1", meth.getParameterTypes().length == paramTypeNames.length);

		for (int i= 0; i < paramTypeNames.length; i++) {
			// create as resolved
			sig[i]= Signature.createTypeSignature(paramTypeNames[i], true);
			assertNotNull(methName + "-ts2" + i, sig[i]);
		}
		meth= JavaModelUtil.findMethod(methName, sig, isConstructor, type);
		assertElementName(methName, meth, IJavaElement.METHOD);
		assertTrue("methName-nparam2", meth.getParameterTypes().length == paramTypeNames.length);
	}

	public void testFindMethod() throws Exception {
		IType type= fJProject1.findType("junit.framework.Assert");
		assertElementName("Assert", type, IJavaElement.TYPE);

		assertFindMethod("assertNotNull", new String[] { "java.lang.Object" }, false, type);
		assertFindMethod("assertNotNull", new String[] { "java.lang.String", "java.lang.Object" }, false, type);
		assertFindMethod("assertEquals", new String[] { "java.lang.String", "double", "double", "double" }, false, type);
		assertFindMethod("assertEquals", new String[] { "java.lang.String", "long", "long" }, false, type);
		assertFindMethod("Assert", new String[0], true, type);

		type= fJProject1.findType("junit.samples.money.MoneyTest");
		assertElementName("MoneyTest", type, IJavaElement.TYPE);

		assertFindMethod("main", new String[] { "java.lang.String[]" }, false, type);
		assertFindMethod("setUp", new String[0] , false, type);

		type= fJProject1.findType("junit.samples.money.MoneyBag");
		assertElementName("MoneyBag", type, IJavaElement.TYPE);

		assertFindMethod("addMoneyBag", new String[] { "junit.samples.money.MoneyBag" }, false, type);
	}

	private void assertFindMethodInHierarchy(String methName, String[] paramTypeNames, boolean isConstructor, IType type, String declaringTypeName) throws Exception {
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);

		String[] sig= new String[paramTypeNames.length];
		for (int i= 0; i < paramTypeNames.length; i++) {
			// create as unresolved
			String name= Signature.getSimpleName(paramTypeNames[i]);
			sig[i]= Signature.createTypeSignature(name, false);
			assertNotNull(methName + "-ts1" + i, sig[i]);
		}
		IMethod meth= JavaModelUtil.findMethodInHierarchy(hierarchy, type, methName, sig, isConstructor);
		assertElementName(methName, meth, IJavaElement.METHOD);
		assertTrue("methName-nparam1", meth.getParameterTypes().length == paramTypeNames.length);
		assertEquals("methName-decltype", declaringTypeName, meth.getDeclaringType().getFullyQualifiedName('.'));

		for (int i= 0; i < paramTypeNames.length; i++) {
			// create as resolved
			sig[i]= Signature.createTypeSignature(paramTypeNames[i], true);
			assertNotNull(methName + "-ts2" + i, sig[i]);
		}
		meth= JavaModelUtil.findMethodInHierarchy(hierarchy, type, methName, sig, isConstructor);
		assertElementName(methName, meth, IJavaElement.METHOD);
		assertTrue("methName-nparam2", meth.getParameterTypes().length == paramTypeNames.length);
		assertEquals("methName-decltype", declaringTypeName, meth.getDeclaringType().getFullyQualifiedName('.'));
	}

	public void testFindMethodInHierarchy() throws Exception {
		IType type= fJProject1.findType("junit.extensions.TestSetup");
		assertElementName("TestSetup", type, IJavaElement.TYPE);

		assertFindMethodInHierarchy("run", new String[] { "junit.framework.TestResult" }, false, type, "junit.extensions.TestSetup");
		assertFindMethodInHierarchy("toString", new String[] {} , false, type, "junit.extensions.TestDecorator");
	}

	public void testHasMainMethod() throws Exception {
		IType type= fJProject1.findType("junit.samples.money.MoneyTest");
		assertElementName("MoneyTest", type, IJavaElement.TYPE);

		assertTrue("MoneyTest-nomain", JavaModelUtil.hasMainMethod(type));

		type= fJProject1.findType("junit.framework.TestResult");
		assertElementName("TestResult", type, IJavaElement.TYPE);

		assertTrue("TestResult-hasmain", !JavaModelUtil.hasMainMethod(type));

		type= fJProject1.findType("junit.samples.VectorTest");
		assertElementName("VectorTest", type, IJavaElement.TYPE);

		assertTrue("VectorTest-nomain", JavaModelUtil.hasMainMethod(type));
	}

}
