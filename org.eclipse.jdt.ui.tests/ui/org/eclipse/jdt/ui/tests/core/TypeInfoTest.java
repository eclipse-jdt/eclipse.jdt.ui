/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.IFileTypeInfo;
import org.eclipse.jdt.internal.corext.util.JarFileEntryTypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoRequestor;


public class TypeInfoTest extends TestCase {
	
	private static final Class THIS= TypeInfoTest.class;
	
	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	public TypeInfoTest(String name) {
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
			suite.addTest(new TypeInfoTest("test1"));
			return new ProjectTestSetup(suite);
		}	
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject1));
		
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject2));		
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);		
		
	}


	public void test1() throws Exception {
		
		// add Junit source to project 2
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
		assertTrue("Junit source", junitSrcArchive != null && junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", junitSrcArchive);
	
		// source folder
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("com.oti", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("V.java");
		cu1.createType("public class V {\n static class VInner {\n}\n}\n", null, true, null);

		// proj1 has proj2 as prerequisit
		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);

		// internal jar
		//IPackageFragmentRoot root2= JavaProjectHelper.addLibraryWithImport(fJProject1, JARFILE, null, null);
		ArrayList result= new ArrayList();

		IJavaElement[] elements= new IJavaElement[] { fJProject1 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		ITypeNameRequestor requestor= new TypeInfoRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			fJProject1.getJavaModel().getWorkspace(),
			null, 
			new char[] {'V'}, 
			SearchPattern.R_PREFIX_MATCH, 
			false, 
			IJavaSearchConstants.TYPE, 
			scope, 
			requestor, 
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
			null); 
		findTypeRef(result, "com.oti.V");
		findTypeRef(result, "com.oti.V.VInner");
		findTypeRef(result, "java.lang.VerifyError");
		findTypeRef(result, "java.lang.Void");
		findTypeRef(result, "java.util.Vector");
		findTypeRef(result, "junit.samples.VectorTest");

		
		for (int i= 0; i < result.size(); i++) {
			TypeInfo ref= (TypeInfo) result.get(i);
			//System.out.println(ref.getTypeName());
			IType resolvedType= ref.resolveType(scope);
			if (resolvedType == null) {
				assertTrue("Could not be resolved: " + ref.toString(), false);
			}
		}
		assertTrue("Should find 9 elements, is " + result.size(), result.size() == 9);


	}
	
	private void findTypeRef(List refs, String fullname) {
		for (int i= 0; i <refs.size(); i++) {
			TypeInfo curr= (TypeInfo) refs.get(i);
			if (fullname.equals(curr.getFullyQualifiedName())) {
				return;
			}
		}
		assertTrue("Type not found: " + fullname, false);
	}
		
	
	public void test2() throws Exception {	
		ArrayList result= new ArrayList();
		
		// add Junit source to project 2
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
		assertTrue("Junit source", junitSrcArchive != null && junitSrcArchive.exists());
		JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", junitSrcArchive);
		
		
		IJavaProject[] elements= new IJavaProject[] { fJProject2 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		ITypeNameRequestor requestor= new TypeInfoRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			fJProject1.getJavaModel().getWorkspace(),
			null, 
			new char[] {'T'}, 
			SearchPattern.R_PREFIX_MATCH, 
			false, 
			IJavaSearchConstants.TYPE, 
			scope, 
			requestor, 
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
			null); 

		findTypeRef(result, "junit.extensions.TestDecorator");
		findTypeRef(result, "junit.framework.Test");
		findTypeRef(result, "junit.framework.TestListener");
		findTypeRef(result, "junit.tests.TestCaseTest.TornDown");

		assertTrue("Should find 37 elements, is " + result.size(), result.size() == 37);
		//System.out.println("Elements found: " + result.size());
		for (int i= 0; i < result.size(); i++) {
			TypeInfo ref= (TypeInfo) result.get(i);
			//System.out.println(ref.getTypeName());
			IType resolvedType= ref.resolveType(scope);
			if (resolvedType == null) {
				assertTrue("Could not be resolved: " + ref.toString(), false);
			}
		}
	}
	
	
	
	
	
	public void testNoSourceFolder() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJProject1, "");
		IPackageFragment pack1= root.createPackageFragment("", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);		

		IPackageFragment pack2= root.createPackageFragment("org.eclipse", true, null);
		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		cu2.createType("public class B {\n}\n", null, true, null);
		
		TypeInfo[] result= AllTypesCache.getAllTypes(new NullProgressMonitor());
		for (int i= 0; i < result.length; i++) {
			TypeInfo info= result[i];
			if (info.getElementType() == TypeInfo.IFILE_TYPE_INFO) {
				IFileTypeInfo fileInfo= (IFileTypeInfo)info;
				if (info.getTypeName().equals("A")) {
					assertEquals(info.getPackageName(), "");
					assertEquals(fileInfo.getProject(), "TestProject1");
					assertNull(fileInfo.getFolder());
					assertEquals(fileInfo.getFileName(), "A");
					assertEquals(fileInfo.getExtension(), "java");
				} else if (info.getTypeName().equals("B")) {
					assertEquals(info.getPackageName(), "org.eclipse");
					assertEquals(fileInfo.getProject(), "TestProject1");
					assertNull(fileInfo.getFolder());
					assertEquals(fileInfo.getFileName(), "B");
					assertEquals(fileInfo.getExtension(), "java");
				}
			}
		}
	}

	public void testSourceFolder() throws Exception {
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= root.createPackageFragment("", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);		

		IPackageFragment pack2= root.createPackageFragment("org.eclipse", true, null);
		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		cu2.createType("public class B {\n}\n", null, true, null);
		
		TypeInfo[] result= AllTypesCache.getAllTypes(new NullProgressMonitor());
		for (int i= 0; i < result.length; i++) {
			TypeInfo info= result[i];
			if (info.getElementType() == TypeInfo.IFILE_TYPE_INFO) {
				IFileTypeInfo fileInfo= (IFileTypeInfo)info;
				if (info.getTypeName().equals("A")) {
					assertEquals(info.getPackageName(), "");
					assertEquals(fileInfo.getProject(), "TestProject1");
					assertEquals(fileInfo.getFolder(), "src");
					assertEquals(fileInfo.getFileName(), "A");
					assertEquals(fileInfo.getExtension(), "java");
				} else if (info.getTypeName().equals("B")) {
					assertEquals(info.getPackageName(), "org.eclipse");
					assertEquals(fileInfo.getProject(), "TestProject1");
					assertEquals(fileInfo.getFolder(), "src");
					assertEquals(fileInfo.getFileName(), "B");
					assertEquals(fileInfo.getExtension(), "java");
				}
			}
		}
	}
	
	public void testJar() throws Exception {
		TypeInfo[] result= AllTypesCache.getAllTypes(new NullProgressMonitor());
		for (int i= 0; i < result.length; i++) {
			TypeInfo info= result[i];
			if (info.getElementType() == TypeInfo.JAR_FILE_ENTRY_TYPE_INFO) {
				JarFileEntryTypeInfo jarInfo= (JarFileEntryTypeInfo)info;
				if (info.getTypeName().equals("Object")) {
					assertEquals(info.getPackageName(), "java.lang");
					assertTrue(jarInfo.getJar().endsWith("rtstubs.jar"));
					assertEquals(jarInfo.getFileName(), "Object");
					assertEquals(jarInfo.getExtension(), "class");
				}
			}
		}		
	}

	public void test_bug44772() throws Exception {
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);

		JavaProjectHelper.addLibraryWithImport(fJProject1, new Path(lib.getPath()), null, null); // as internal 
		JavaProjectHelper.addLibrary(fJProject1, new Path(lib.getPath())); // and as external
		
		ArrayList result= new ArrayList();

		IJavaElement[] elements= new IJavaElement[] { fJProject1 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		ITypeNameRequestor requestor= new TypeInfoRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			fJProject1.getJavaModel().getWorkspace(),
			null, 
			"Foo".toCharArray(),
			SearchPattern.R_EXACT_MATCH, 
			true, 
			IJavaSearchConstants.TYPE, 
			scope, 
			requestor, 
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
			null); 
		assertEquals("result size", result.size(), 2);
		IType type1= ((TypeInfo) result.get(0)).resolveType(scope);
		IType type2= ((TypeInfo) result.get(1)).resolveType(scope);
		
		assertNotNull(type1);
		assertNotNull(type2);
		assertFalse(type1.equals(type2));

	}

	
}
