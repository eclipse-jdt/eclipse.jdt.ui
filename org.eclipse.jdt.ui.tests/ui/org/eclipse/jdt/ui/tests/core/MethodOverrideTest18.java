/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;

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

public class MethodOverrideTest18 extends MethodOverrideTest {

	private static final Class THIS= MethodOverrideTest18.class;

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSrc;

	public MethodOverrideTest18(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJProject1= Java18ProjectTestSetup.getProject();
		fSrc= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}


	public void testOverrideLambda1() throws Exception {
		IPackageFragment pack1= fSrc.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
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
