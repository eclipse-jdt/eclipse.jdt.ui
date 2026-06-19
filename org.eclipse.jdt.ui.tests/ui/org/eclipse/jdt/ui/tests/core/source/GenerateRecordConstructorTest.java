/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.core.source;

import java.lang.reflect.Modifier;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class GenerateRecordConstructorTest extends SourceTestCase {

	@Before
	@Override
	public void setUp() throws CoreException {
		super.setUp();
		JavaProjectHelper.set17CompilerOptions(fJavaProject, false);
	}

	public void runOperation(IType type, boolean createComments, boolean omitSuper, int visibility) throws CoreException {

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);
		ITypeBinding typeBinding= ASTNodes.getTypeBinding(unit, type);
		IMethodBinding constructorToInvoke= GenerateConstructorUsingFieldsTest.getObjectConstructor(unit);

		fSettings.createComments= createComments;

		AddCustomConstructorOperation op= new AddCustomConstructorOperation(unit, typeBinding, new IVariableBinding[] {}, constructorToInvoke, null, fSettings, true, true);
		op.setOmitSuper(omitSuper);
		op.setVisibility(visibility);

		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	@Test
	public void testGenerateCompactConstructorForRecord() throws Exception {

		ICompilationUnit cu= fPackageP.createCompilationUnit(
				"InnerApp4.java",
				"""
						package p;

						public record InnerApp4(int p) {
						}
						""",
				true,
				null);

		IType type= cu.getType("InnerApp4");

		runOperation(type, true, true, Modifier.PUBLIC);

		compareSource("""
				package p;

				public record InnerApp4(int p) {

					/**
					 *\s
					 */
					public InnerApp4 {
					}
				}
				""", cu.getSource());
	}
}
