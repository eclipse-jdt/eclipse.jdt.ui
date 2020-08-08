/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class MethodOverrideTest1d8 extends MethodOverrideTest {
	@Rule
	public Java1d8ProjectTestSetup j18p= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSrc;

	@Before
	public void before() throws Exception {
		fJProject1= j18p.getProject();
		fSrc= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);
	}

	@After
	public void after() throws Exception {
		JavaProjectHelper.clear(fJProject1, j18p.getDefaultClasspath());
	}

	@Test
	public void overrideLambda1() throws Exception {
		IPackageFragment pack1= fSrc.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public interface MyFunction<T, R> {\n");
		buf.append("    R apply(T t);\n");
		buf.append("    default <V> MyFunction<V, R> compose(MyFunction<? super V, ? extends T> before) {\n");
		buf.append("        return (V v) -> apply(before.apply(v));\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("MyFunction.java", buf.toString(), false, null);

		CompilationUnit root= assertNoCompilationError(cu);
		IType focusType= cu.getTypes()[0];

		int vStart= buf.indexOf("v))");
		ILocalVariable v= (ILocalVariable) cu.codeSelect(vStart, 1)[0];
		IType overridingType= v.getDeclaringMember().getDeclaringType();

		LambdaExpression lambda= (LambdaExpression) NodeFinder.perform(root, buf.indexOf("->"), 2);
		ITypeBinding overridingTypeBinding= lambda.resolveTypeBinding();

		IType overriddenType= focusType;
		ITypeBinding overriddenTypeBinding= ((TypeDeclaration) root.types().get(0)).resolveBinding();

		doOverrideTests(root, focusType, overridingType, overridingTypeBinding, overriddenType, overriddenTypeBinding);
	}
}
