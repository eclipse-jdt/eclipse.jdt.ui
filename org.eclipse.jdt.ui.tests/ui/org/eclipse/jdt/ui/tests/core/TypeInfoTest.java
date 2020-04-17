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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.manipulation.TypeNameMatchCollector;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatch;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class TypeInfoTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	@Before
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject1));

		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject2));
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);
	}

	@Test
	public void test1() throws Exception {

		// add Junit source to project 2
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("Junit source", junitSrcArchive);
		assertTrue("Junit source", junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);
		// source folder
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("com.oti", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("V.java");
		cu1.createType("public class V {\n static class VInner {\n}\n}\n", null, true, null);

		// proj1 has proj2 as prerequisit
		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);

		// internal jar
		//IPackageFragmentRoot root2= JavaProjectHelper.addLibraryWithImport(fJProject1, JARFILE, null, null);
		ArrayList<TypeNameMatch> result= new ArrayList<>();

		IJavaElement[] elements= new IJavaElement[] { fJProject1 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		TypeNameMatchRequestor requestor= new TypeNameMatchCollector(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			null,
			0,
			new char[] {'V'},
			SearchPattern.R_PREFIX_MATCH,
			IJavaSearchConstants.TYPE,
			scope,
			requestor,
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			null);
		findTypeRef(result, "com.oti.V");
		findTypeRef(result, "com.oti.V.VInner");

		findTypeRef(result, "java.lang.VerifyError");
		findTypeRef(result, "java.lang.VirtualMachineError");
		findTypeRef(result, "java.lang.Void");
		findTypeRef(result, "java.util.Vector");

		findTypeRef(result, "junit.samples.VectorTest");
		findTypeRef(result, "junit.runner.Version");


		for (TypeNameMatch ref : result) {
			//System.out.println(ref.getTypeName());
			assertResolve(ref);

		}
		assertEquals("Should find 8 elements, is " + result.size(), 8, result.size());


	}

	private void assertResolve(TypeNameMatch ref) {
		IType resolvedType= ref.getType();
		assertNotNull("Could not be resolved: " + ref.toString(), resolvedType);
		assertTrue("Resolved type does not exist: " + ref.toString(), resolvedType.exists());
		StringAsserts.assertEqualString(resolvedType.getFullyQualifiedName('.'), ref.getFullyQualifiedName());
	}

	private void findTypeRef(List<TypeNameMatch> refs, String fullname) {
		for (TypeNameMatch curr : refs) {
			if (fullname.equals(curr.getFullyQualifiedName())) {
				return;
			}
		}
		fail("Type not found: " + fullname);
	}


	@Test
	public void test2() throws Exception {
		ArrayList<TypeNameMatch> result= new ArrayList<>();

		// add Junit source to project 2
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("Junit source", junitSrcArchive);
		assertTrue("Junit source", junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);


		IJavaProject[] elements= new IJavaProject[] { fJProject2 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		TypeNameMatchRequestor requestor= new TypeNameMatchCollector(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			null,
			0,
			new char[] {'T'},
			SearchPattern.R_PREFIX_MATCH,
			IJavaSearchConstants.TYPE,
			scope,
			requestor,
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			null);

		findTypeRef(result, "junit.extensions.TestDecorator");
		findTypeRef(result, "junit.framework.Test");
		findTypeRef(result, "junit.framework.TestListener");
		findTypeRef(result, "junit.tests.framework.TestCaseTest.TornDown");

		assertEquals("wrong element count", 51, result.size());
		//System.out.println("Elements found: " + result.size());
		for (TypeNameMatch ref : result) {
			//System.out.println(ref.getTypeName());
			assertResolve(ref);
		}
	}


	@Test
	public void bug44772() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);

		JavaProjectHelper.addLibraryWithImport(fJProject1, Path.fromOSString(lib.getPath()), null, null); // as internal
		JavaProjectHelper.addLibrary(fJProject1, Path.fromOSString(lib.getPath())); // and as external

		ArrayList<TypeNameMatch> result= new ArrayList<>();

		IJavaElement[] elements= new IJavaElement[] { fJProject1 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		TypeNameMatchRequestor requestor= new TypeNameMatchCollector(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			null,
			0,
			"Foo".toCharArray(),
			SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE,
			IJavaSearchConstants.TYPE,
			scope,
			requestor,
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			null);
		assertEquals("result size", 2, result.size());
		IType type1= result.get(0).getType();
		IType type2= result.get(1).getType();

		assertNotNull(type1);
		assertNotNull(type2);
		assertNotEquals(type1, type2);
	}
}
