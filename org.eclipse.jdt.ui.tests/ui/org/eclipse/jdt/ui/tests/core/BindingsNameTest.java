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
package org.eclipse.jdt.ui.tests.core;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.Bindings;

/**
  */
public class BindingsNameTest extends TestCase {

	private static final Class THIS= BindingsNameTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private ICompilationUnit fCompilationUnit;

	public BindingsNameTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new BindingsNameTest("testFullyQualifiedNames"));
			return suite;
		}
	}
	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar13(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= fSourceFolder.createPackageFragment("", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("public class X {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("X.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("import X;\n");
		buf.append("public class E {\n");
		buf.append("    public class Inner {\n");
		buf.append("        public class InnerInner {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void goo(int i, int[] j, Object o, Object[] p, Inner.InnerInner x, Inner.InnerInner[][] y, X a, X[][][] b) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		fCompilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	public void testGetFullyQualifiedName() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List params= methodDeclaration.parameters();
		String[] fullNames= new String[params.size()];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
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

	public void testGetTypeQualifiedName() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List params= methodDeclaration.parameters();
		String[] fullNames= new String[params.size()];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
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

	public void testGetAllNameComponents() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List params= methodDeclaration.parameters();
		String[][] fullNames= new String[params.size()][];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
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

	public void testGetNameComponents() throws Exception {
		CompilationUnit astRoot= createAST(fCompilationUnit);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration typeDeclaration= (TypeDeclaration) astRoot.types().get(0);
		MethodDeclaration methodDeclaration= typeDeclaration.getMethods()[0];

		List params= methodDeclaration.parameters();
		String[][] fullNames= new String[params.size()][];

		for (int i= 0; i < params.size(); i++) {
			SingleVariableDeclaration elem= (SingleVariableDeclaration) params.get(i);
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
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

}
