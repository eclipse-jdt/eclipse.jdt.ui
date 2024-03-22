/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;


/**
 * Tests the AST provider.
 *
 * @since 3.3
 */
public class ASTProviderTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private static final class AddFieldRefactoring extends Refactoring {

		private final ICompilationUnit fCu;
		private final int fFieldNumber;

		private AddFieldRefactoring(ICompilationUnit cu, int fieldNumber) {
			fCu= cu;
			fFieldNumber= fieldNumber;
		}

		@Override
		public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			return new RefactoringStatus();
		}

		@Override
		public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			return new RefactoringStatus();
		}

		@Override
		public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
			CompilationUnitChange result= new CompilationUnitChange("", fCu);

			String text= "  private int " + getFieldName(fFieldNumber) + "=1;\n";
			int position= 33 + (fFieldNumber * text.length());
			result.setEdit(new ReplaceEdit(position, 0, text));

			return result;
		}

		@Override
		public String getName() {
			return "Add field";
		}

		private static String getFieldName(int number) {
			return "a"+getNormalizeNumber(number);
		}

	}

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testBug181257() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			}
			""";
		final ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		JavaUI.openInEditor(cu);

		for (int i= 0; i < 100; i++) {
			String expected= cu.getBuffer().getContents();
			CompilationUnit ast= SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_ACTIVE_ONLY, null);
			assertNotNull(ast);
			assertEquals(expected, ast.toString());

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
