/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class ExtractMethodTestSetup extends TestSetup {
	
	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	private IPackageFragment fSelectionPackage;
	private IPackageFragment fInvalidSelectionPackage;
	private IPackageFragment fValidSelectionPackage;
	private IPackageFragment fSemicolonPackage;
	private IPackageFragment fTryPackage;
	private IPackageFragment fLocalsPackage;
	private IPackageFragment fExpressionPackage;
	private IPackageFragment fNestedPackage;
	private IPackageFragment fReturnPackage;
	private IPackageFragment fBranchPackage;
	private IPackageFragment fWikiPackage;
	
	public ExtractMethodTestSetup(Test test) {
		super(test);
	}	
	
	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}
		
	protected void setUp() throws Exception {
		super.setUp();
		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);
		
		Refactoring.getUndoManager().flush();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		
		fSelectionPackage= getRoot().createPackageFragment("selection", true, null);
		fInvalidSelectionPackage= fRoot.createPackageFragment("invalidSelection", true, null);
		fValidSelectionPackage= fRoot.createPackageFragment("validSelection", true, null);
		fSemicolonPackage= getRoot().createPackageFragment("semicolon_in", true, null);
		fTryPackage= getRoot().createPackageFragment("try_in", true, null);
		fLocalsPackage= getRoot().createPackageFragment("locals_in", true, null);
		fExpressionPackage= getRoot().createPackageFragment("expression_in", true, null);
		fNestedPackage= getRoot().createPackageFragment("nested_in", true, null);
		fReturnPackage= getRoot().createPackageFragment("return_in", true, null);
		fBranchPackage= getRoot().createPackageFragment("branch_in", true, null);
		fWikiPackage= getRoot().createPackageFragment("wiki_in", true, null);
		
		ICompilationUnit cu= fExpressionPackage.createCompilationUnit(
			"A.java", 
			"package expression_in; import java.io.File; class A { public File getFile() { return null; } public void useFile(File file) { } }",
			true, null);
		cu.save(null, true);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		super.tearDown();
	}
	
	public IPackageFragment getExpressionPackage() {
		return fExpressionPackage;
	}
	
	public IPackageFragment getInvalidSelectionPackage() {
		return fInvalidSelectionPackage;
	}
	
	public IPackageFragment getLocalsPackage() {
		return fLocalsPackage;
	}
	
	public IPackageFragment getNestedPackage() {
		return fNestedPackage;
	}
	
	public IPackageFragment getReturnPackage() {
		return fReturnPackage;
	}
	
	public IPackageFragment getSelectionPackage() {
		return fSelectionPackage;
	}
	
	public IPackageFragment getSemicolonPackage() {
		return fSemicolonPackage;
	}
	
	public IPackageFragment getTryPackage() {
		return fTryPackage;
	}
	
	public IPackageFragment getValidSelectionPackage() {
		return fValidSelectionPackage;
	}

	public IPackageFragment getBranchPackage() {
		return fBranchPackage;
	}

	public IPackageFragment getWikiPackage() {
		return fWikiPackage;
	}	
}

