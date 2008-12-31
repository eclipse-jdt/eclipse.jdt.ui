/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CreateChangeOperation;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;


import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.SharedASTProvider;


/**
 * Tests the AST provider.
 *
 * @since 3.3
 */
public class ASTProviderTest extends CoreTests {


	private static final class AddFieldRefactoring extends Refactoring {

		private final ICompilationUnit fCu;
		private final int fFieldNumber;

		private AddFieldRefactoring(ICompilationUnit cu, int fieldNumber) {
			fCu= cu;
			fFieldNumber= fieldNumber;
		}

		public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			return new RefactoringStatus();
		}

		public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			return new RefactoringStatus();
		}

		public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			CompilationUnitChange result= new CompilationUnitChange("", fCu);

			String text= "  private int " + getFieldName(fFieldNumber) + "=1;\n";
			int position= 35 + (fFieldNumber * text.length());
			result.setEdit(new ReplaceEdit(position, 0, text));

			return result;
		}

		public String getName() {
			return "Add field";
		}

		private static String getFieldName(int number) {
			return "a"+getNormalizeNumber(number);
		}

	}

	private static final Class THIS= ASTProviderTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTProviderTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}


	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testBug181257() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		final ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		JavaUI.openInEditor(cu);

		for (int i= 0; i < 100; i++) {
			String expected= cu.getBuffer().getContents();
			CompilationUnit ast= SharedASTProvider.getAST(cu, SharedASTProvider.WAIT_NO, null);
			if (ast != null) {
				String actual= ast.toString();
				System.out.println("Cached AST:");
				System.out.println(actual);
				System.out.println("CU:");
				System.out.println(expected);
				assertEquals(expected, actual);
			}

			Refactoring refactoring= new AddFieldRefactoring(cu, i);

			refactoring.checkAllConditions(new NullProgressMonitor());
			PerformChangeOperation operation= new PerformChangeOperation(new CreateChangeOperation(refactoring));
			operation.run(new NullProgressMonitor());
		}

		cu.getBuffer().save(null, true);
	}

	private static String getNormalizeNumber(int number) {
		if (number < 10) {
			return "000" + number;
		} else if (number < 100) {
			return "00" + number;
		} else if (number < 1000) {
			return "0" + number;
		} else {
			return "" + number;
		}
	}
}
