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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

public class ExtractMethodTestSetup extends TestSetup {
	
	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fRoot;
	private static final String CONTAINER= "src";

	private IPackageFragment fSelectionPackage;
	private IPackageFragment fInvalidSelectionPackage;
	private IPackageFragment fValidSelectionPackage;
	private IPackageFragment fValidSelectionCheckedPackage;
	private IPackageFragment fSemicolonPackage;
	private IPackageFragment fTryPackage;
	private IPackageFragment fLocalsPackage;
	private IPackageFragment fExpressionPackage;
	private IPackageFragment fNestedPackage;
	private IPackageFragment fReturnPackage;
	private IPackageFragment fBranchPackage;
	private IPackageFragment fErrorPackage;
	private IPackageFragment fWikiPackage;
	private IPackageFragment fParameterNamePackage;
	private IPackageFragment fDuplicatesPackage;
	private IPackageFragment fInitializerPackage;
	private IPackageFragment fDestinationPackage;
	
	public ExtractMethodTestSetup(Test test) {
		super(test);
	}	
	
	public IPackageFragmentRoot getRoot() {
		return fRoot;
	}
		
	protected void setUp() throws Exception {
		super.setUp();
		
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "0");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);
		
		fJavaProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fJavaProject);
		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, CONTAINER);
		
		RefactoringCore.getUndoManager().flush();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription description= workspace.getDescription();
		description.setAutoBuilding(false);
		workspace.setDescription(description);
		
		fSelectionPackage= getRoot().createPackageFragment("selection", true, null);
		fInvalidSelectionPackage= fRoot.createPackageFragment("invalidSelection", true, null);
		fValidSelectionPackage= fRoot.createPackageFragment("validSelection", true, null);
		fValidSelectionCheckedPackage= fRoot.createPackageFragment("validSelection_in", true, null);
		fSemicolonPackage= getRoot().createPackageFragment("semicolon_in", true, null);
		fTryPackage= getRoot().createPackageFragment("try_in", true, null);
		fLocalsPackage= getRoot().createPackageFragment("locals_in", true, null);
		fExpressionPackage= getRoot().createPackageFragment("expression_in", true, null);
		fNestedPackage= getRoot().createPackageFragment("nested_in", true, null);
		fReturnPackage= getRoot().createPackageFragment("return_in", true, null);
		fBranchPackage= getRoot().createPackageFragment("branch_in", true, null);
		fErrorPackage= getRoot().createPackageFragment("error_in", true, null);
		fWikiPackage= getRoot().createPackageFragment("wiki_in", true, null);
		fParameterNamePackage= getRoot().createPackageFragment("parameterName_in", true, null);
		fDuplicatesPackage= getRoot().createPackageFragment("duplicates_in", true, null);
		fInitializerPackage= getRoot().createPackageFragment("initializer_in", true, null);
		fDestinationPackage= getRoot().createPackageFragment("destination_in", true, null);
		
		ICompilationUnit cu= fExpressionPackage.createCompilationUnit(
			"A.java", 
			"package expression_in; import java.io.File; class A { public File getFile() { return null; } public void useFile(File file) { } }",
			true, null);
			
		cu= fExpressionPackage.createCompilationUnit(
			"B.java", 
			"package expression_in; import java.util.List; public class B { public List[] foo() { return null; } }",
			true, null);
		cu.save(null, true);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RefactoringTest.performDummySearch(fJavaProject);
		JavaProjectHelper.delete(fJavaProject);
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

	public IPackageFragment getValidSelectionCheckedPackage() {
		return fValidSelectionCheckedPackage;
	}
	
	public IPackageFragment getBranchPackage() {
		return fBranchPackage;
	}

	public IPackageFragment getErrorPackage() {
		return fErrorPackage;
	}

	public IPackageFragment getWikiPackage() {
		return fWikiPackage;
	}	

	public IPackageFragment getParameterNamePackage() {
		return fParameterNamePackage;
	}

	public IPackageFragment getDuplicatesPackage() {
		return fDuplicatesPackage;
	}

	public IPackageFragment getInitializerPackage() {
		return fInitializerPackage;
	}

	public IPackageFragment getDestinationPackage() {
		return fDestinationPackage;
	}
}

