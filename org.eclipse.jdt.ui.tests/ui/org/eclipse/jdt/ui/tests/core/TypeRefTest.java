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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
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
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoRequestor;

public class TypeRefTest extends TestCase {
	
	private IJavaProject fJProject1;
	private IJavaProject fJProject2;
	private static final IPath SOURCES= new Path("test-resources/junit32-noUI.zip");
	public TypeRefTest(String name) {
		super(name);
	}
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), TypeRefTest.class, args);
	}

	public static Test suite() {
		TestSuite suite= new TestSuite(TestSuite.class.getName());
		suite.addTest(new TypeRefTest("doTest1"));
		suite.addTest(new TypeRefTest("doTest2"));
		return suite;
	}

	protected void setUp() throws Exception {
			fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
			fJProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
		JavaProjectHelper.delete(fJProject2);		
		
	}

	public void doTest1() throws Exception {
		
		// a junit project
		IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJProject2);
		assertTrue("jdk not found", jdk != null);
		
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(SOURCES);
		assertTrue(junitSrcArchive != null && junitSrcArchive.exists());
		ZipFile zipfile= new ZipFile(junitSrcArchive);
		JavaProjectHelper.addSourceContainerWithImport(fJProject2, "src", zipfile);
		

		// external jar
		JavaProjectHelper.addRTJar(fJProject1);
		// required project
		JavaProjectHelper.addRequiredProject(fJProject1, fJProject2);
		
		// source folder
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("com.oti", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("V.java");
		IType type1= cu1.createType("public class V {\n static class VInner {\n}\n}\n", null, true, null);

		// internal jar
		//IPackageFragmentRoot root2= JavaProjectHelper.addLibraryWithImport(fJProject1, JARFILE, null, null);
		ArrayList result= new ArrayList();

		IResource[] resources= new IResource[] {fJProject1.getJavaProject().getProject()};
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
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
			IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, 
			null); 
		findTypeRef(result, "com.oti.V");
		findTypeRef(result, "com.oti.V.VInner");
		findTypeRef(result, "java.lang.VerifyError");
		findTypeRef(result, "java.lang.Void");
		findTypeRef(result, "java.util.Vector");
		findTypeRef(result, "junit.samples.VectorTest");
		findTypeRef(result, "junit.util.Version");
		//System.out.println("Elements found: " + result.size());
		for (int i= 0; i < result.size(); i++) {
			TypeInfo ref= (TypeInfo) result.get(i);
			IType resolvedType= ref.resolveType(scope);
			if (resolvedType == null) {
				assertTrue("Could not be resolved: " + ref.toString(), false);
			} else {
				//System.out.println(resolvedType.getFullyQualifiedName());
			}
		}


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
		
	
	
	public void doTest2() throws Exception {
		// our project
		IJavaProject fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		// external jar
		JavaProjectHelper.addRTJar(fJProject1);
		
		ArrayList result= new ArrayList();
		
		IResource[] resources= new IResource[] {fJProject1.getProject()};
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
		ITypeNameRequestor requestor= new TypeInfoRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			fJProject1.getJavaModel().getWorkspace(),
			null, 
			new char[] {'A', 'c', 't', 'i', 'v', 'a'}, 
			IJavaSearchConstants.PREFIX_MATCH, 
			IJavaSearchConstants.CASE_INSENSITIVE, 
			IJavaSearchConstants.TYPE, 
			scope, 
			requestor, 
			IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, 
			null); 

		System.out.println("Elements found: " + result.size());
		for (int i= 0; i < result.size(); i++) {
			TypeInfo ref= (TypeInfo) result.get(i);
			System.out.println(ref.getTypeName());
			IType resolvedType= ref.resolveType(scope);
			if (resolvedType == null) {
				assertTrue("Could not be resolved: " + ref.toString(), false);
			} else {
				System.out.println(resolvedType.getFullyQualifiedName());
			}
		}
	}
}
