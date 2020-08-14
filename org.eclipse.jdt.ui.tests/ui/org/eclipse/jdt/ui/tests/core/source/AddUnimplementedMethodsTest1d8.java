/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class AddUnimplementedMethodsTest1d8 {
	@Rule
	public Java1d8ProjectTestSetup j18p= new Java1d8ProjectTestSetup();

	private IJavaProject fJavaProject;

	private IPackageFragment fPackage;

	@Before
	public void setUp() throws Exception {
		fJavaProject= j18p.getProject();
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJavaProject, j18p.getDefaultClasspath());
		fJavaProject= null;
		fPackage= null;
	}

	@Test
	public void testBug460521() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("interface I {\n");
		buf.append("}\n");

		ICompilationUnit cu= fPackage.createCompilationUnit("I.java", buf.toString(), true, null);
		IType testClass= cu.createType(buf.toString(), null, true, null);

		testHelper(testClass, -1, true);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "hashCode", "equals", "clone", "toString", "finalize" }, methods);
	}

	private void testHelper(IType testClass, int insertionPos, boolean implementAllOverridable) throws JavaModelException, CoreException {
		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(testClass.getCompilationUnit(), true);
		AbstractTypeDeclaration declaration= ASTNodes.getParent(NodeFinder.perform(unit, testClass.getNameRange()), AbstractTypeDeclaration.class);
		assertNotNull("Could not find type declaration node", declaration);
		ITypeBinding binding= declaration.resolveBinding();
		assertNotNull("Binding for type declaration could not be resolved", binding);

		IMethodBinding[] overridableMethods= implementAllOverridable ? StubUtility2Core.getOverridableMethods(unit.getAST(), binding, false) : null;

		AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(unit, binding, overridableMethods, insertionPos, true, true, true);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());
	}

	private void checkMethods(String[] expected, IMethod[] methods) {
		int nMethods= methods.length;
		int nExpected= expected.length;
		assertEquals("" + nExpected + " methods expected, is " + nMethods, nExpected, nMethods);
		for (int i= 0; i < nExpected; i++) {
			String methName= expected[i];
			assertTrue("method " + methName + " expected", nameContained(methName, methods));
		}
	}

	private boolean nameContained(String methName, IJavaElement[] methods) {
		for (IJavaElement method : methods) {
			if (method.getElementName().equals(methName)) {
				return true;
			}
		}
		return false;
	}
}
