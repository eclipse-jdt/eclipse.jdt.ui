/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class AddUnimplementedMethodsTest extends TestCase {

	private static final Class THIS= AddUnimplementedMethodsTest.class;

	private IJavaProject fJavaProject;
	private IPackageFragment fPackage;
	private IType fClassA, fInterfaceB, fClassC, fClassD, fInterfaceE;


	public AddUnimplementedMethodsTest(String name) {
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
			suite.addTest(new AddUnimplementedMethodsTest("test1"));
			return new ProjectTestSetup(suite);
		}
	}

	protected void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		fJavaProject.setOptions(options);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "${body_statement}\n// TODO", null);

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);

		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public abstract class A {\n}\n", null, true, null);
		fClassA.createMethod("public abstract void a();\n", null, true, null);
		fClassA.createMethod("public abstract void b(java.util.Vector v);\n", null, true, null);

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


	protected void tearDown () throws Exception {
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
	public void test1() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu.createType("public class Test1 extends A implements B {\n}\n", null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "a", "b", "c", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.Vector" }, imports);
	}

	/*
	 * method c() of interface B is already implemented by class C
	 */
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
	public void test4() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test4.java");
		IType testClass= cu.createType("public class Test4 implements B, E {\n}\n", null, true, null);

		testHelper(testClass);

		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "c", "e", "equals", "clone", "toString", "finalize", "hashCode" }, methods);

		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.NoSuchElementException" }, imports);
	}

	public void testBug119171() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package ibm.util;\n");
		buf.append("import java.util.Properties;\n");
		buf.append("public interface F {\n");
		buf.append("    public void b(Properties p);\n");
		buf.append("}\n");
		fPackage.createCompilationUnit("F.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package ibm.util;\n");
		buf.append("public class Properties {\n");
		buf.append("    public int get() {return 0;}\n");
		buf.append("}\n");
		fPackage.createCompilationUnit("Properties.java", buf.toString(), false, null);

		buf= new StringBuffer();
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

	public void testBug297183() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package ibm.util;\n");
		buf.append("interface Shape {\r\n");
		buf.append("  int getX();\r\n");
		buf.append("  int getY();\r\n");
		buf.append("  int getEdges();\r\n");
		buf.append("  int getArea();\r\n");
		buf.append("}\r\n");
		fPackage.createCompilationUnit("Shape.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package ibm.util;\n");
		buf.append("interface Circle extends Shape {\r\n");
		buf.append("  int getR();\r\n");
		buf.append("}\r\n");
		buf.append("\r\n");
		fPackage.createCompilationUnit("Circle.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
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
	
	public void testInsertAt() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("p", true, null);

		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public abstract class B  {\n");
		buf.append("	public abstract void foo() {\n");
		buf.append("	}\n");
		buf.append("}");
		fPackage.createCompilationUnit("B.java", buf.toString(), true, null);


		buf= new StringBuffer();
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

		buf= new StringBuffer();
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

	private void testHelper(IType testClass) throws JavaModelException, CoreException {
		testHelper(testClass, -1, true);
	}

	private void testHelper(IType testClass, int insertionPos, boolean implementAllOverridable) throws JavaModelException, CoreException {
		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		CompilationUnit unit= parser.parse(testClass.getCompilationUnit(), true);
		AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) ASTNodes.getParent(NodeFinder.perform(unit, testClass.getNameRange()), AbstractTypeDeclaration.class);
		assertNotNull("Could not find type declaration node", declaration);
		ITypeBinding binding= declaration.resolveBinding();
		assertNotNull("Binding for type declaration could not be resolved", binding);

		IMethodBinding[] overridableMethods= implementAllOverridable ? StubUtility2.getOverridableMethods(unit.getAST(), binding, false) : null;

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
		assertTrue("" + nExpected + " methods expected, is " + nMethods, nMethods == nExpected);
		for (int i= 0; i < nExpected; i++) {
			String methName= expected[i];
			assertTrue("method " + methName + " expected", nameContained(methName, methods));
		}
	}

	private void checkImports(String[] expected, IImportDeclaration[] imports) {
		int nImports= imports.length;
		int nExpected= expected.length;
		if (nExpected != nImports) {
			StringBuffer buf= new StringBuffer();
			buf.append(nExpected).append(" imports expected, is ").append(nImports).append("\n");
			buf.append("expected:\n");
			for (int i= 0; i < expected.length; i++) {
				buf.append(expected[i]).append("\n");
			}
			buf.append("actual:\n");
			for (int i= 0; i < imports.length; i++) {
				buf.append(imports[i]).append("\n");
			}
			assertTrue(buf.toString(), false);
		}
		for (int i= 0; i < nExpected; i++) {
			String impName= expected[i];
			assertTrue("import " + impName + " expected", nameContained(impName, imports));
		}
	}

	private boolean nameContained(String methName, IJavaElement[] methods) {
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].getElementName().equals(methName)) {
				return true;
			}
		}
		return false;
	}
}
