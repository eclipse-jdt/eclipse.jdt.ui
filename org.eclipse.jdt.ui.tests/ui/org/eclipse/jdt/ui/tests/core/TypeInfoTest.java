/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.TypeInfoRequestor;


public class TypeInfoTest extends TestCase {
	
	private static final Class THIS= TypeInfoTest.class;
	
	private IJavaProject fJProject1;
	private IJavaProject fJProject2;

	public TypeInfoTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(THIS);
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject1));
		
		fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
		assertNotNull("jre is null", JavaProjectHelper.addRTJar(fJProject2));
		
		// add Junit source to project 2
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
		assertTrue("Junit source", junitSrcArchive != null && junitSrcArchive.exists());
		ZipFile zipfile= new ZipFile(junitSrcArchive);
		JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", zipfile);
		
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);		
		
	}


	public void test1() throws Exception {
	
		// source folder
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("com.oti", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("V.java");
		IType type1= cu1.createType("public class V {\n static class VInner {\n}\n}\n", null, true, null);

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
			IJavaSearchConstants.PREFIX_MATCH, 
			IJavaSearchConstants.CASE_INSENSITIVE, 
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
		
		IJavaProject[] elements= new IJavaProject[] { fJProject2 };
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements);
		ITypeNameRequestor requestor= new TypeInfoRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			fJProject1.getJavaModel().getWorkspace(),
			null, 
			new char[] {'T'}, 
			IJavaSearchConstants.PREFIX_MATCH, 
			IJavaSearchConstants.CASE_INSENSITIVE, 
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
}
