/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class BindingsNameTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private ICompilationUnit fCompilationUnit;

	@Before
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar13(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= fSourceFolder.createPackageFragment("", false, null);
		String str= """
			public class X {
			}
			""";
		pack0.createCompilationUnit("X.java", str, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			import X;
			public class E {
			    public class Inner {
			        public class InnerInner {
			        }
			    }
			    public void goo(int i, int[] j, Object o, Object[] p, Inner.InnerInner x, Inner.InnerInner[][] y, X a, X[][][] b) {
			    }
			}
			""";
		fCompilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	@Test
	public void getFullyQualifiedName() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List<SingleVariableDeclaration> params= methodDeclaration.parameters();
		String[] fullNames= new String[params.size()];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= params.get(i);
			IVariableBinding varBinding= elem.resolveBinding();
			fullNames[i]= Bindings.getFullyQualifiedName(varBinding.getType());
		}

		assertEquals("int", fullNames[0]);
		assertEquals("int[]", fullNames[1]);
		assertEquals("java.lang.Object", fullNames[2]);
		assertEquals("java.lang.Object[]", fullNames[3]);
		assertEquals("test1.ae.E.Inner.InnerInner", fullNames[4]);
		assertEquals("test1.ae.E.Inner.InnerInner[][]", fullNames[5]);
		assertEquals("X", fullNames[6]);
		assertEquals("X[][][]", fullNames[7]);
	}

	@Test
	public void getTypeQualifiedName() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List<SingleVariableDeclaration> params= methodDeclaration.parameters();
		String[] fullNames= new String[params.size()];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= params.get(i);
			IVariableBinding varBinding= elem.resolveBinding();
			fullNames[i]= Bindings.getTypeQualifiedName(varBinding.getType());
		}

		assertEquals("int", fullNames[0]);
		assertEquals("int[]", fullNames[1]);
		assertEquals("Object", fullNames[2]);
		assertEquals("Object[]", fullNames[3]);
		assertEquals("E.Inner.InnerInner", fullNames[4]);
		assertEquals("E.Inner.InnerInner[][]", fullNames[5]);
		assertEquals("X", fullNames[6]);
		assertEquals("X[][][]", fullNames[7]);
	}

	@Test
	public void getAllNameComponents() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List<SingleVariableDeclaration> params= methodDeclaration.parameters();
		String[][] fullNames= new String[params.size()][];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= params.get(i);
			IVariableBinding varBinding= elem.resolveBinding();
			fullNames[i]= Bindings.getAllNameComponents(varBinding.getType());
		}

		assertEqualArray(new String[] { "int" }, fullNames[0]);
		assertEqualArray(new String[] { "int[]" }, fullNames[1]);
		assertEqualArray(new String[] { "java", "lang", "Object" }, fullNames[2]);
		assertEqualArray(new String[] { "java", "lang", "Object[]" }, fullNames[3]);
		assertEqualArray(new String[] { "test1", "ae", "E", "Inner", "InnerInner" }, fullNames[4]);
		assertEqualArray(new String[] { "test1", "ae", "E", "Inner", "InnerInner[][]" }, fullNames[5]);
		assertEqualArray(new String[] { "X" }, fullNames[6]);
		assertEqualArray(new String[] { "X[][][]" }, fullNames[7]);
	}

	@Test
	public void getNameComponents() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List<SingleVariableDeclaration> params= methodDeclaration.parameters();
		String[][] fullNames= new String[params.size()][];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= params.get(i);
			IVariableBinding varBinding= elem.resolveBinding();
			fullNames[i]= Bindings.getNameComponents(varBinding.getType());
		}

		assertEqualArray(new String[] { "int" }, fullNames[0]);
		assertEqualArray(new String[] { "int[]" }, fullNames[1]);
		assertEqualArray(new String[] { "Object" }, fullNames[2]);
		assertEqualArray(new String[] { "Object[]" }, fullNames[3]);
		assertEqualArray(new String[] { "E", "Inner", "InnerInner" }, fullNames[4]);
		assertEqualArray(new String[] { "E", "Inner", "InnerInner[][]" }, fullNames[5]);
		assertEqualArray(new String[] { "X" }, fullNames[6]);
		assertEqualArray(new String[] { "X[][][]" }, fullNames[7]);
	}

	private void assertEqualArray(Object[] elements, Object[] list) {
		assertEquals("different length", list.length, elements.length);
		for (int i= 0; i < list.length; i++) {
			assertEquals(elements[i], list[i]);
		}
	}

	private CompilationUnit createAST(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}
}
