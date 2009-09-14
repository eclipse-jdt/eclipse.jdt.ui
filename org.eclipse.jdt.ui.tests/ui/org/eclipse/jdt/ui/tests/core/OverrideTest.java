/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Arrays;

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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.Bindings;

public class OverrideTest extends TestCase {

	private static final Class THIS= OverrideTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragment fPackage;

	public OverrideTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new OverrideTest("testFullyQualifiedNames"));
			return suite;
		}
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackage= fSourceFolder.createPackageFragment("override.test", false, null);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	private static CompilationUnit createAST(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	public void test14Overloaded() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("public class Top {\n");
		buf.append("    void m(Integer i) {}\n");
		buf.append("}\n");
		buf.append("class Sub extends Top {\n");
		buf.append("    void m(Integer arg) {}\n");
		buf.append("    void m(String string) {}\n");
		buf.append("    void m(Object o) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("Top.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration top= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding topInteger= top.getMethods()[0].resolveBinding();

		TypeDeclaration sub= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding subInteger= sub.getMethods()[0].resolveBinding();
		IMethodBinding subString= sub.getMethods()[1].resolveBinding();
		IMethodBinding subObject= sub.getMethods()[2].resolveBinding();

		assertSame(topInteger, Bindings.findOverriddenMethod(subInteger, true));
		assertNull(Bindings.findOverriddenMethod(subString, true));
		assertNull(Bindings.findOverriddenMethod(subObject, true));
	}

	public void test14Overloaded2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("public interface ITop {\n");
		buf.append("    void m(Integer i);\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Middle1 implements ITop {\n");
		buf.append("    public void m(Integer arg) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("abstract class Middle2 implements ITop {\n");
		buf.append("}\n");
		buf.append("class Sub1 extends Middle1 {\n");
		buf.append("    public void m(Integer arg) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class Sub2 extends Middle2 {\n");
		buf.append("    public void m(Integer arg) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("ITop.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration iTop= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding iTopInteger= iTop.getMethods()[0].resolveBinding();

		TypeDeclaration middle1= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding middle1Integer= middle1.getMethods()[0].resolveBinding();

		TypeDeclaration sub1= (TypeDeclaration) astRoot.types().get(3);
		IMethodBinding sub1Integer= sub1.getMethods()[0].resolveBinding();

		TypeDeclaration sub2= (TypeDeclaration) astRoot.types().get(4);
		IMethodBinding sub2Integer= sub2.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(iTopInteger, true));
		assertSame(iTopInteger, Bindings.findOverriddenMethod(middle1Integer, true));
		assertSame(middle1Integer, Bindings.findOverriddenMethod(sub1Integer, true));
		assertSame(iTopInteger, Bindings.findOverriddenMethod(sub2Integer, true));
	}

	public void test15Bug100233() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("abstract class A<T> {\n");
		buf.append("  void g1 (T t) {\n");
		buf.append("    System.out.println(\"g1 base: \" + t);\n");
		buf.append("  }\n");
		buf.append("  void g2 (T t) {\n");
		buf.append("    System.out.println(\"g2 base: \" + t);\n");
		buf.append("  }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("public class B extends A<java.util.List<Number>> {\n");
		buf.append("  void g1 (java.util.List<?> t) {\n");
		buf.append("    System.out.println(\"g1 derived: \" + t);\n");
		buf.append("  }\n");
		buf.append("  void g2 (java.util.List<Number> t) {\n");
		buf.append("    System.out.println(\"g2 derived: \" + t);\n");
		buf.append("  }\n");
		buf.append("  public static void main (String[] args) {\n");
		buf.append("    B b = new B();\n");
		buf.append("    b.g1(new java.util.ArrayList<Number>());\n");
		buf.append("    b.g2(new java.util.ArrayList<Number>());\n");
		buf.append("  }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("B.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding ag2= a.getMethods()[1].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bg1= b.getMethods()[0].resolveBinding();
		IMethodBinding bg2= b.getMethods()[1].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bg1, true));
		assertSame(ag2, Bindings.findOverriddenMethod(bg2, true).getMethodDeclaration()); // found method is from parameterized superclass
	}

	public void test15Bug97027() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class AA<T> {\n");
		buf.append("    public AA<Object> test() { return null; }\n");
		buf.append("}\n");
		buf.append("class BB extends AA<CC> {\n");
		buf.append("    public <T> BB test() { return null; }\n");
		buf.append("}\n");
		buf.append("class CC {}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("AA.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 1);
		assertTrue(problems[0].isError());
		assertTrue(problems[0].getID() == IProblem.MethodNameClash);

		TypeDeclaration bb= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bbtest= bb.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bbtest, true));
	}

	public void test15JLS3_842() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.util.Collection;\n");
		buf.append("import java.util.List;\n");
		buf.append("class CollectionConverter {\n");
		buf.append("    <T> List<T> toList(Collection<T> c) { return null; }\n");
		buf.append("}\n");
		buf.append("class Overrider extends CollectionConverter {\n");
		buf.append("    List toList(Collection c) { return null; }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("CollectionConverter.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertTrue(problems[0].isWarning());
		assertTrue(problems[0].getID() == IProblem.UnsafeReturnTypeOverride);

		TypeDeclaration collectionConverter= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding collectionConverter_toList= collectionConverter.getMethods()[0].resolveBinding();

		TypeDeclaration overrider= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding overrider_toList= overrider.getMethods()[0].resolveBinding();

		assertSame(collectionConverter_toList, Bindings.findOverriddenMethod(overrider_toList, true));
	}

	public void test15JLS3_848_1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class C implements Cloneable {\n");
		buf.append("    C copy() { return (C)clone(); }\n");
		buf.append("}\n");
		buf.append("class D extends C implements Cloneable {\n");
		buf.append("    D copy() { return (D)clone(); }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(2, problems.length);
		assertEquals(IProblem.UnhandledException, problems[0].getID());
		assertEquals(IProblem.UnhandledException, problems[1].getID());

		TypeDeclaration c= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding ccopy= c.getMethods()[0].resolveBinding();

		TypeDeclaration d= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding dcopy= d.getMethods()[0].resolveBinding();

		assertSame(ccopy, Bindings.findOverriddenMethod(dcopy, true));
	}

	public void test15JLS3_848_2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.Collection;\n");
		buf.append("import java.util.List;\n");
		buf.append("class StringSorter {\n");
		buf.append("    List<String> toList(Collection<String> c) { return new ArrayList<String>(c); }\n");
		buf.append("}\n");
		buf.append("class Overrider extends StringSorter {\n");
		buf.append("    List toList(Collection c) { return new ArrayList(c); }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("StringSorter.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(2, problems.length);
		assertEquals(IProblem.UnsafeReturnTypeOverride, problems[0].getID());
		assertEquals(IProblem.UnsafeRawConstructorInvocation, problems[1].getID());

		TypeDeclaration stringSorter= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding stringSorter_toList= stringSorter.getMethods()[0].resolveBinding();

		TypeDeclaration overrider= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding overrider_toList= overrider.getMethods()[0].resolveBinding();

		assertSame(stringSorter_toList, Bindings.findOverriddenMethod(overrider_toList, true));
	}

	public void test15JLS3_848_3() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class C<T> {\n");
		buf.append("    T id(T x) { return x; }\n");
		buf.append("}\n");
		buf.append("class D extends C<String> {\n");
		buf.append("    Object id(Object x) { return x; }\n");
		buf.append("    String id(String x) { return x; }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertEquals(IProblem.MethodNameClash, problems[0].getID());

		TypeDeclaration c= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding cid= c.getMethods()[0].resolveBinding();

		TypeDeclaration d= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding didObject= d.getMethods()[0].resolveBinding();
		IMethodBinding didString= d.getMethods()[1].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(didObject, true));
		assertSame(cid, Bindings.findOverriddenMethod(didString, true).getMethodDeclaration());
	}

	public void test15JLS3_848_4() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class C<T> {\n");
		buf.append("    public T id (T x) { return x; }\n");
		buf.append("}\n");
		buf.append("interface I<T> {\n");
		buf.append("    public T id(T x);\n");
		buf.append("}\n");
		buf.append("class D extends C<String> implements I<Integer> {\n");
		buf.append("    public String id(String x) { return x; }\n");
		buf.append("    public Integer id(Integer x) { return x; }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("C.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertEquals(IProblem.MethodNameClash, problems[0].getID());

		TypeDeclaration c= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding cid= c.getMethods()[0].resolveBinding();

		TypeDeclaration i= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding iid= i.getMethods()[0].resolveBinding();

		TypeDeclaration d= (TypeDeclaration) astRoot.types().get(2);
		IMethodBinding didString= d.getMethods()[0].resolveBinding();
		IMethodBinding didInteger= d.getMethods()[1].resolveBinding();

		assertEquals(cid, Bindings.findOverriddenMethod(didString, true).getMethodDeclaration());
		assertEquals(iid, Bindings.findOverriddenMethod(didInteger, true).getMethodDeclaration());
	}

	public void test15ClassTypeVars1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class A<E extends Number, F> {\n");
		buf.append("    void take(E e, F f) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B<S extends Number, T> extends A<S, T> {\n");
		buf.append("    void take(S e, T f) {}\n");
		buf.append("    void take(T f, S e) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding atake= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding btakeST= b.getMethods()[0].resolveBinding();
		IMethodBinding btakeTS= b.getMethods()[1].resolveBinding();

		assertEquals(atake, Bindings.findOverriddenMethod(btakeST, true).getMethodDeclaration());
		assertNull(Bindings.findOverriddenMethod(btakeTS, true));
	}

	public void test15ClassTypeVars2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class A<T extends Number> {\n");
		buf.append("    void m(T t) {}\n");
		buf.append("}\n");
		buf.append("class B<S extends Integer> extends A<S> {\n");
		buf.append("    @Override\n");
		buf.append("     void m(S t) { System.out.println(\"B: \" + t); }\n");
		buf.append("}\n");
		buf.append("class C extends A/*raw*/ {\n");
		buf.append("    @Override\n");
		buf.append("    void m(Number t) { System.out.println(\"C: \" + t); }\n");
		buf.append("}\n");
		buf.append("class D extends B/*raw*/ {\n");
		buf.append("    @Override\n");
		buf.append("    void m(Number t) { System.out.println(\"C#m(Number): \" + t); }\n");
		buf.append("    @Override\n");
		buf.append("    void m(Integer t) { System.out.println(\"C#m(Integer): \" + t); }\n");
		buf.append("}\n");
		buf.append("class E extends B<Integer> {\n");
		buf.append("    //illegal:\n");
		buf.append("    void m(Number t) { System.out.println(\"D#m(Number): \" + t); }\n");
		buf.append("    @Override\n");
		buf.append("    void m(Integer t) { System.out.println(\"D#m(Integer): \" + t); }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
//		IProblem[] problems= astRoot.getProblems();
//		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding am= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		TypeDeclaration c= (TypeDeclaration) astRoot.types().get(2);
		IMethodBinding cm= c.getMethods()[0].resolveBinding();

		TypeDeclaration d= (TypeDeclaration) astRoot.types().get(3);
		IMethodBinding dmNumber= d.getMethods()[0].resolveBinding();
		IMethodBinding dmInteger= d.getMethods()[1].resolveBinding();

		TypeDeclaration e= (TypeDeclaration) astRoot.types().get(4);
		IMethodBinding emNumber= e.getMethods()[0].resolveBinding();
		IMethodBinding emInteger= e.getMethods()[1].resolveBinding();

		assertEquals(am, Bindings.findOverriddenMethod(bm, true).getMethodDeclaration());
		assertEquals(am, Bindings.findOverriddenMethod(bm, true).getMethodDeclaration());
		assertEquals(am, Bindings.findOverriddenMethod(cm, true).getMethodDeclaration());
		assertEquals(am, Bindings.findOverriddenMethod(dmNumber, true).getMethodDeclaration());
		assertEquals(bm, Bindings.findOverriddenMethod(dmInteger, true).getMethodDeclaration());
		assertNull(Bindings.findOverriddenMethod(emNumber, true));
		assertEquals(bm, Bindings.findOverriddenMethod(emInteger, true).getMethodDeclaration());
	}

	public void test15MethodTypeVars1() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class A {\n");
		buf.append("    <E extends Number, F> void take(E e, F f) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B extends A {\n");
		buf.append("    <S extends Number, T> void take(S e, T f) {}\n");
		buf.append("    <S extends Number, T> void take(T f, S e) {}\n");
		buf.append("    <S extends Number, T extends S> void take(S e, T f) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding atake= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding btakeST= b.getMethods()[0].resolveBinding();
		IMethodBinding btakeTS= b.getMethods()[1].resolveBinding();
		IMethodBinding btakeSTS= b.getMethods()[2].resolveBinding();

		assertEquals(atake, Bindings.findOverriddenMethod(btakeST, true).getMethodDeclaration());
		assertNull(Bindings.findOverriddenMethod(btakeTS, true));
		assertNull(Bindings.findOverriddenMethod(btakeSTS, true));
	}

	public void test15MethodTypeVars2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class A {\n");
		buf.append("    <E extends Number, F> void take(E e, F f) {}\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B extends A {\n");
		buf.append("    <S extends Number, T extends Object> void take(S e, T f) {}\n");
		buf.append("    <S extends Integer, T> void take(S e, T f) {}\n");
		buf.append("    <S extends Number, T> void take(T f, S e) {}\n");
		buf.append("    <S extends Number, T extends S> void take(T f, S e) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertEquals(IProblem.FinalBoundForTypeVariable, problems[0].getID());

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding atake= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding btake1= b.getMethods()[0].resolveBinding();
		IMethodBinding btake2= b.getMethods()[1].resolveBinding();
		IMethodBinding btake3= b.getMethods()[2].resolveBinding();

		assertEquals(atake, Bindings.findOverriddenMethod(btake1, true).getMethodDeclaration());
		assertNull(Bindings.findOverriddenMethod(btake2, true));
		assertNull(Bindings.findOverriddenMethod(btake3, true));
	}

	public void test15MethodTypeVars3() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class A {\n");
		buf.append("    void take(Object t) {}\n");
		buf.append("    <T> void take2(T t) {}\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    <T> void take(T t) {}\n");
		buf.append("    <T, S> void take2(T t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(2, problems.length);
		assertEquals(IProblem.MethodNameClash, problems[0].getID());
		assertEquals(IProblem.MethodNameClash, problems[1].getID());

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding btake= b.getMethods()[0].resolveBinding();
		IMethodBinding btake2= b.getMethods()[1].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(btake, true));
		assertNull(Bindings.findOverriddenMethod(btake2, true));
	}

	public void test15MethodTypeVars4() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("public class A {\n");
		buf.append("    <T, U extends T> void m(T t, U u) { }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    @Override\n");
		buf.append("    <X, Y extends X> void m(X t, Y u) { }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding am= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		assertEquals(am, Bindings.findOverriddenMethod(bm, true));
	}

	public void test15MethodTypeVars5() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.util.List;\n");
		buf.append("class A {\n");
		buf.append("    <T extends List<Number>> void m(T t) {}\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    <S extends List<Integer>> void m(S t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertEquals(IProblem.MethodNameClash, problems[0].getID());

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bm, true));
	}

	public void test15MethodTypeVars6() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.util.List;\n");
		buf.append("class A {\n");
		buf.append("    <T extends List<T>> void m(T t) {}\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    @Override\n");
		buf.append("    <S extends List<S>> void m(S t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding am= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		assertEquals(am, Bindings.findOverriddenMethod(bm, true));
	}

	public void test15Bug99608() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class Top<E> {\n");
		buf.append("    void add(E[] e) {}\n");
		buf.append("    void remove(E... e) {}\n");
		buf.append("}\n");
		buf.append("class Sub extends Top<String> {\n");
		buf.append("    void add(String... s) {}\n");
		buf.append("    void remove(String[] s) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("Top.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(2, problems.length);
		assertEquals(IProblem.VarargsConflict, problems[0].getID());
		assertEquals(IProblem.VarargsConflict, problems[1].getID());

		TypeDeclaration top= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding topAdd= top.getMethods()[0].resolveBinding();
		IMethodBinding topRemove= top.getMethods()[1].resolveBinding();

		TypeDeclaration sub= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding subAdd= sub.getMethods()[0].resolveBinding();
		IMethodBinding subRemove= sub.getMethods()[1].resolveBinding();

		assertEquals(topAdd, Bindings.findOverriddenMethod(subAdd, true).getMethodDeclaration());
		assertEquals(topRemove, Bindings.findOverriddenMethod(subRemove, true).getMethodDeclaration());
	}

	public void test15Bug90114() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("class SuperX {\n");
		buf.append("    static void notOverridden() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("public class X extends SuperX {\n");
		buf.append("    static void notOverridden() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("} \n");
		ICompilationUnit cu= fPackage.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration x= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding xnotOverridden= x.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(xnotOverridden, true));
	}

	public void test15Bug89516primitive() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class Test extends ArrayList<String> {\n");
		buf.append("    static final long serialVersionUID= 1L;\n");
		buf.append("    public boolean add(int i) {\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("Test.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration test= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding testAdd= test.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(testAdd, true));
	}

	public void test15Bug105669() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.util.*;\n");
		buf.append("class I extends Vector<Number> {\n");
		buf.append("    static final long serialVersionUID= 1L;\n");
		buf.append("    @Override\n");
		buf.append("    public synchronized boolean addAll(Collection c) {\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("I.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration i= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding iaddAll= i.getMethods()[0].resolveBinding();

		IMethodBinding overridden= Bindings.findOverriddenMethod(iaddAll, true).getMethodDeclaration();
		ITypeBinding vector= i.getSuperclassType().resolveBinding().getTypeDeclaration();

		assertTrue(Arrays.asList(vector.getDeclaredMethods()).contains(overridden));
	}

	public void test15Bug107105() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package override.test;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    <S extends Number & Serializable & Runnable > void foo2(S s) { }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class B extends A {\n");
		buf.append("    @Override // should error\n");
		buf.append("    <S extends Number & Runnable> void foo2(S s) { }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class C extends A {\n");
		buf.append("    @Override // should error\n");
		buf.append("    <S extends Number & Runnable & Cloneable> void foo2(S s) { }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("class D extends A {\n");
		buf.append("    @Override // correct\n");
		buf.append("    <S extends Number & Runnable & Serializable> void foo2(S s) { }\n");
		buf.append("}\n");
		buf.append("interface I extends Runnable, Serializable { }\n");
		buf.append("class E extends A {\n");
		buf.append("    @Override //should error\n");
		buf.append("    <S extends Number & I> void foo2(S s) { }\n");
		buf.append("}\n");
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(cu);
//		IProblem[] problems= astRoot.getProblems();
//		assertEquals(3, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding afoo= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bfoo= b.getMethods()[0].resolveBinding();

		TypeDeclaration c= (TypeDeclaration) astRoot.types().get(2);
		IMethodBinding cfoo= c.getMethods()[0].resolveBinding();

		TypeDeclaration d= (TypeDeclaration) astRoot.types().get(3);
		IMethodBinding dfoo= d.getMethods()[0].resolveBinding();

		TypeDeclaration e= (TypeDeclaration) astRoot.types().get(5);
		IMethodBinding efoo= e.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bfoo, true));
		assertNull(Bindings.findOverriddenMethod(cfoo, true));
		assertEquals(afoo, Bindings.findOverriddenMethod(dfoo, true));
		assertNull(Bindings.findOverriddenMethod(efoo, true));
	}

}
