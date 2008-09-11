/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

public class ExtractMethodTestSetup extends RefactoringTestSetup {

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
	private IPackageFragment fGenericsPackage;
	private IPackageFragment fEnumsPackage;
	private IPackageFragment fVarargsPackage;
	private IPackageFragment fFieldInitializerPackage;

	public ExtractMethodTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();

		RefactoringCore.getUndoManager().flush();

		IPackageFragmentRoot root= getDefaultSourceFolder();
		fSelectionPackage= root.createPackageFragment("selection", true, null);
		fInvalidSelectionPackage= root.createPackageFragment("invalidSelection", true, null);
		fValidSelectionPackage= root.createPackageFragment("validSelection", true, null);
		fValidSelectionCheckedPackage= root.createPackageFragment("validSelection_in", true, null);
		fSemicolonPackage= root.createPackageFragment("semicolon_in", true, null);
		fTryPackage= root.createPackageFragment("try_in", true, null);
		fLocalsPackage= root.createPackageFragment("locals_in", true, null);
		fExpressionPackage= root.createPackageFragment("expression_in", true, null);
		fNestedPackage= root.createPackageFragment("nested_in", true, null);
		fReturnPackage= root.createPackageFragment("return_in", true, null);
		fBranchPackage= root.createPackageFragment("branch_in", true, null);
		fErrorPackage= root.createPackageFragment("error_in", true, null);
		fWikiPackage= root.createPackageFragment("wiki_in", true, null);
		fParameterNamePackage= root.createPackageFragment("parameterName_in", true, null);
		fDuplicatesPackage= root.createPackageFragment("duplicates_in", true, null);
		fInitializerPackage= root.createPackageFragment("initializer_in", true, null);
		fDestinationPackage= root.createPackageFragment("destination_in", true, null);
		fGenericsPackage= root.createPackageFragment("generics_in", true, null);
		fEnumsPackage= root.createPackageFragment("enums_in", true, null);
		fVarargsPackage= root.createPackageFragment("varargs_in", true, null);
		fFieldInitializerPackage= root.createPackageFragment("fieldInitializer_in", true, null);

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

	public IPackageFragment getGenericsPackage() {
		return fGenericsPackage;
	}

	public IPackageFragment getEnumsPackage() {
		return fEnumsPackage;
	}

	public IPackageFragment getVarargsPackage() {
		return fVarargsPackage;
	}

	public IPackageFragment getFieldInitializerPackage() {
		return fFieldInitializerPackage;
	}
}

