/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package com.ibm.jdt.ui.tests;

import java.util.ArrayList;import junit.framework.Test;import junit.framework.TestCase;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IWorkspaceRoot;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.search.IJavaSearchConstants;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.core.search.ITypeNameRequestor;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.testplugin.JavaTestPlugin;import org.eclipse.jdt.testplugin.JavaTestProject;import org.eclipse.jdt.testplugin.TestPluginLauncher;import org.eclipse.jdt.testplugin.ui.TestPluginUILauncher;import org.eclipse.jdt.internal.ui.util.TypeRef;import org.eclipse.jdt.internal.ui.util.TypeRefRequestor;

public class TypeRefTest extends TestCase {

	private static final IPath SOURCES= new Path("t:\\jabiru\\smoke\\junit32src");
	private static final IPath JARFILE= new Path("d:\\temp\\VectorFooLibrary.jar");

	private JavaTestProject fTestProject;

	public TypeRefTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), TypeRefTest.class, args);
	}

	public static Test suite() {
		return new TypeRefTest("doTest1");
	}

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	public void doTest1() throws Exception {
		IWorkspaceRoot workspaceRoot= JavaTestPlugin.getWorkspace().getRoot();

		// a junit project
		JavaTestProject proj2= new JavaTestProject(workspaceRoot, "TestProject2", "bin");
		proj2.addRTJar();
		proj2.addSourceContainerWithImport("junit", SOURCES);

		// our project
		JavaTestProject proj1= new JavaTestProject(workspaceRoot, "TestProject1", "bin");
		// external jar
		proj1.addRTJar();
		// required project
		proj1.addRequiredProject(proj2.getJavaProject());
		
		// source folder
		IPackageFragmentRoot root1= proj1.addSourceContainer("src");
		IPackageFragment pack1= root1.createPackageFragment("com.oti", true, null);
		ICompilationUnit cu1= pack1.getCompilationUnit("V.java");
		IType type1= cu1.createType("public class V {\n static class VInner {\n}\n}\n", null, true, null);

		// internal jar
		IPackageFragmentRoot root2= proj1.addLibraryWithImport(JARFILE, null, null);
		ArrayList result= new ArrayList();

		IResource[] resources= new IResource[] {proj1.getJavaProject().getProject()};
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
		ITypeNameRequestor requestor= new TypeRefRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			workspaceRoot.getWorkspace(), 
			null, 
			new char[] {'V'}, 
			IJavaSearchConstants.PREFIX_MATCH, 
			IJavaSearchConstants.CASE_INSENSITIVE, 
			IJavaSearchConstants.TYPE, 
			scope, 
			requestor, 
			IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, 
			null); 
		assert("Should find 45 elements", result.size() == 45);

		System.out.println("Elements found: " + result.size());
		for (int i= 0; i < result.size(); i++) {
			TypeRef ref= (TypeRef) result.get(i);
			IType resolvedType= ref.resolveType(scope);
			if (resolvedType == null) {
				assert("Could not be resolved: " + ref.toString(), false);
			} else {
				System.out.println(resolvedType.getFullyQualifiedName());
			}
		}

		proj1.remove();
	}
	
	public void doTest2() throws Exception {
		IWorkspaceRoot workspaceRoot= JavaTestPlugin.getWorkspace().getRoot();


		// our project
		JavaTestProject proj1= new JavaTestProject(workspaceRoot, "TestProject1", "bin");
		// external jar
		proj1.addRTJar();
		
		ArrayList result= new ArrayList();
		
		IResource[] resources= new IResource[] {proj1.getJavaProject().getProject()};
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(resources);
		ITypeNameRequestor requestor= new TypeRefRequestor(result);
		SearchEngine engine= new SearchEngine();

		engine.searchAllTypeNames(
			workspaceRoot.getWorkspace(), 
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
			TypeRef ref= (TypeRef) result.get(i);
			System.out.println(ref.getTypeName());
			IType resolvedType= ref.resolveType(scope);
			if (resolvedType == null) {
				assert("Could not be resolved: " + ref.toString(), false);
			} else {
				System.out.println(resolvedType.getFullyQualifiedName());
			}
		}

		proj1.remove();
	}

	

}
