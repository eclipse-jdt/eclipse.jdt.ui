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
package org.eclipse.jdt.ui.tests.core.source;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.util.ASTHelper;

public class AddUnimplementedMethodsTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJavaProject;
	private IPackageFragment fPackage;
	private IType fClassA, fInterfaceB, fClassC, fClassD, fInterfaceE;

	@Before
	public void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		fJavaProject.setOptions(options);

		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "${body_statement}\n// TODO", null);

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);

		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public abstract class A {\n}\n", null, true, null);
		fClassA.createMethod("public abstract void a();\n", null, true, null);
		fClassA.createMethod("public abstract void b(java.util.Vector<java.util.Date> v);\n", null, true, null);

		cu= fPackage.getCompilationUnit("B.java");
		fInterfaceB= cu.createType("public interface B {\n}\n", null, true, null);
		fInterfaceB.createMethod("void c(java.util.Hashtable h);\n", null, true, null);

		cu= fPackage.getCompilationUnit("C.java");
		fClassC= cu.createType("public abstract class C {\n}\n", null, true, null);
		fClassC.createMethod("public void c(java.util.Hashtable h) {\n}\n", null, true, null);
		fClassC.createMethod("public abstract java.util.Enumeration d(java.util.Hashtable h) {\n}\n", null, true, null);


		cu= fPackage.getCompilationUnit("D.java");
		fClassD= cu.createType("public abstract class D extends C {\n}\n", null, true, null);
		fClassD.createMethod("public abstract void c(java.util.Hashtable h);\n", null, true, null);

		cu= fPackage.getCompilationUnit("E.java");
		fInterfaceE= cu.createType("public interface E {\n}\n", null, true, null);
		fInterfaceE.createMethod("void c(java.util.Hashtable h);\n", null, true, null);
		fInterfaceE.createMethod("void e() throws java.util.NoSuchElementException;\n", null, true, null);

		IEclipsePreferences node= new ProjectScope(fJavaProject.getProject()).getNode(JavaUI.ID_PLUGIN);
		node.putBoolean(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, false);
		node.putBoolean(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		node.flush();
	}


	@After
	public void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
		fPackage= null;
		fClassA= null;
		fInterfaceB= null;
		fClassC= null;
		fClassD= null;
		fInterfaceE= null;
	}

	/*
	 * basic test: extend an abstract class and an interface
	 */
	@Test
	public void test1() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu.createType("public class Test1 extends A implements B {\n}\n", null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "a", "b", "c", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Date", "java.util.Hashtable", "java.util.Vector" }, imports);
	}

	/*
	 * method c() of interface B is already implemented by class C
	 */
	@Test
	public void test2() throws Exception {

		ICompilationUnit cu= fPackage.getCompilationUnit("Test2.java");
		IType testClass= cu.createType("public class Test2 extends C implements B {\n}\n", null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "c", "d", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Enumeration", "java.util.Hashtable" }, imports);
	}


	/*
	 * method c() is implemented in C but made abstract again in class D
	 */
	@Test
	public void test3() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test3.java");
		IType testClass= cu.createType("public class Test3 extends D {\n}\n", null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "c", "d", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.Enumeration" }, imports);
	}

	/*
	 * method c() defined in both interfaces B and E
	 */
	@Test
	public void test4() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test4.java");
		IType testClass= cu.createType("public class Test4 implements B, E {\n}\n", null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "c", "e", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.NoSuchElementException" }, imports);
	}

	@Test
	public void bug119171() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("package ibm.util;\n");
		buf.append("import java.util.Properties;\n");
		buf.append("public interface F {\n");
		buf.append("    public void b(Properties p);\n");
		buf.append("}\n");
		fPackage.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package ibm.util;\n");
		buf.append("public class Properties {\n");
		buf.append("    public int get() {return 0;}\n");
		buf.append("}\n");
		fPackage.createCompilationUnit("Properties.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("public class Test5 implements F {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Properties p= new Properties();\n");
		buf.append("        p.get();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.getCompilationUnit("Test5.java");
		IType testClass= cu.createType(buf.toString(), null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethodsInOrder(new String[] { "foo", "b", "clone", "equals", "finalize", "hashCode", "toString"}, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[0], imports);
	}

	@Test
	public void bug297183() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("package ibm.util;\n");
		buf.append("interface Shape {\r\n");
		buf.append("  int getX();\r\n");
		buf.append("  int getY();\r\n");
		buf.append("  int getEdges();\r\n");
		buf.append("  int getArea();\r\n");
		buf.append("}\r\n");
		fPackage.createCompilationUnit("Shape.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package ibm.util;\n");
		buf.append("interface Circle extends Shape {\r\n");
		buf.append("  int getR();\r\n");
		buf.append("}\r\n");
		buf.append("\r\n");
		fPackage.createCompilationUnit("Circle.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package ibm.util;\n");
		buf.append("public class DefaultCircle implements Circle {\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.getCompilationUnit("DefaultCircle.java");
		IType testClass= cu.createType(buf.toString(), null, true, null);

		testHelper(testClass, -1, false);

		IMethod[] methods= testClass.getMethods();
		checkMethodsInOrder(new String[] { "getX", "getY", "getEdges", "getArea", "getR"}, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[0], imports);
	}

	@Test
	public void insertAt() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("p", true, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public abstract class B  {\n");
		buf.append("	public abstract void foo() {\n");
		buf.append("	}\n");
		buf.append("}");
		fPackage.createCompilationUnit("B.java", buf.toString(), true, null);


		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class A extends B {\n");
		buf.append("    int x;\n");
		buf.append("\n");
		buf.append("    A() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    void bar() {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    {\n"); // initializer
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    static {\n"); // static initializer
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    class Inner {\n"); // inner class
		buf.append("    }\n");
		buf.append("}");
		String originalContent= buf.toString();

		final int NUM_MEMBERS= 6;

		buf= new StringBuilder();
		buf.append("public void foo() {\n");
		buf.append("        // TODO\n");
		buf.append("    }");
		String expectedConstructor= buf.toString();

		// try to insert the new constructor after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackage.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				assertEquals(NUM_MEMBERS, children.length);

				int insertIndex= i < NUM_MEMBERS ? ((IMember) children[i]).getSourceRange().getOffset() : -1;

				testHelper(type, insertIndex, false);

				IJavaElement[] newChildren= type.getChildren();
				assertEquals(NUM_MEMBERS + 1, newChildren.length);
				String source= ((IMember) newChildren[i]).getSource(); // new element expected at index i
				assertEquals(expectedConstructor, source);
			} finally {
				if (unit != null) {
					JavaProjectHelper.delete(unit);
				}
			}
		}
	}

	@Test
	public void bug480682() throws Exception {
		StringBuilder buf= new StringBuilder();
		buf.append("public class Test480682 extends Base {\n");
		buf.append("}\n");
		buf.append("abstract class Base implements I {\n");
		buf.append("    @Override\n");
		buf.append("    public final void method1() {}\n");
		buf.append("}\n");
		buf.append("interface I {\n");
		buf.append("    void method1();\n");
		buf.append("    void method2();\n");
		buf.append("}\n");

		ICompilationUnit cu= fPackage.createCompilationUnit("Test480682.java", buf.toString(), true, null);
		IType testClass= cu.createType(buf.toString(), null, true, null);

		testHelper(testClass, -1, false);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "method2" }, methods);
	}

	/**
	 * @throws Exception
	 * @deprecated tests deprecated API
	 */
	@Deprecated
	@Test
	public void jLS3() throws Exception {
		doTestOldAstLevel(ASTHelper.JLS3);
	}

	/**
	 * @throws Exception
	 * @deprecated tests deprecated API
	 */
	@Deprecated
	@Test
	public void jLS4() throws Exception {
		doTestOldAstLevel(ASTHelper.JLS4);
	}

	@Test
	public void jLS8() throws Exception {
		doTestOldAstLevel(ASTHelper.JLS8);
	}

	/**
	 * @param astLevel AST.JLS*
	 * @throws Exception
	 * @deprecated tests deprecated API
	 */
	@Deprecated
	public void doTestOldAstLevel(int astLevel) throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu.createType(
				  "public class Test1 extends A implements B {\n"
				+ "    @Deprecated\n"
				+ "    java.util.List<String>[][] getArray() throws RuntimeException {\n"
				+ "        return (ArrayList<String>[][]) new ArrayList<?>[1][2];\n"
				+ "    }\n"
				+ "}\n", null, true, null);
		cu.createImport("java.util.ArrayList", null, null);

		RefactoringASTParser parser= new RefactoringASTParser(astLevel);
		CompilationUnit unit= parser.parse(cu, true);
		AbstractTypeDeclaration declaration= ASTNodes.getParent(NodeFinder.perform(unit, testClass.getNameRange()), AbstractTypeDeclaration.class);
		assertNotNull("Could not find type declaration node", declaration);
		ITypeBinding binding= declaration.resolveBinding();
		assertNotNull("Binding for type declaration could not be resolved", binding);

		IMethodBinding[] overridableMethods= StubUtility2Core.getOverridableMethods(unit.getAST(), binding, false);

		AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(unit, binding, overridableMethods, -1, true, true, true);
		op.run(new NullProgressMonitor());

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "a", "b", "c", "getArray", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Date", "java.util.Hashtable", "java.util.Vector", "java.util.ArrayList" }, imports);

		IProblem[] problems= parser.parse(cu, true).getProblems();
		assertArrayEquals(new IProblem[0], problems);
	}

	private void testHelper(IType testClass) throws JavaModelException, CoreException {
		testHelper(testClass, -1, true);
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

	private void checkMethodsInOrder(String[] expected, IMethod[] methods) {
		String[] actualNames= new String[methods.length];
		for (int i= 0; i < actualNames.length; i++) {
			actualNames[i]= methods[i].getElementName();
		}
		assertEquals(Strings.concatenate(expected, ", "), Strings.concatenate(actualNames, ", "));
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

	private void checkImports(String[] expected, IImportDeclaration[] imports) {
		int nImports= imports.length;
		int nExpected= expected.length;
		if (nExpected != nImports) {
			StringBuilder buf= new StringBuilder();
			buf.append(nExpected).append(" imports expected, is ").append(nImports).append("\n");
			buf.append("expected:\n");
			for (String e : expected) {
				buf.append(e).append("\n");
			}
			buf.append("actual:\n");
			for (IImportDeclaration i : imports) {
				buf.append(i).append("\n");
			}
			fail(buf.toString());
		}
		for (int i= 0; i < nExpected; i++) {
			String impName= expected[i];
			assertTrue("import " + impName + " expected", nameContained(impName, imports));
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
