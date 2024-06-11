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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class OverrideTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	private IPackageFragment fPackage;

	@Before
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fPackage= fSourceFolder.createPackageFragment("override.test", false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	private static CompilationUnit createAST(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}

	@Test
	public void test14Overloaded() throws Exception {
		String str= """
			package override.test;
			public class Top {
			    void m(Integer i) {}
			}
			class Sub extends Top {
			    void m(Integer arg) {}
			    void m(String string) {}
			    void m(Object o) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("Top.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

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

	@Test
	public void test14Overloaded2() throws Exception {
		String str= """
			package override.test;
			public interface ITop {
			    void m(Integer i);
			}
			
			class Middle1 implements ITop {
			    public void m(Integer arg) {}
			}
			
			abstract class Middle2 implements ITop {
			}
			class Sub1 extends Middle1 {
			    public void m(Integer arg) {}
			}
			
			class Sub2 extends Middle2 {
			    public void m(Integer arg) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("ITop.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

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

	@Test
	public void test15Bug100233() throws Exception {
		String str= """
			package override.test;
			abstract class A<T> {
			  void g1 (T t) {
			    System.out.println("g1 base: " + t);
			  }
			  void g2 (T t) {
			    System.out.println("g2 base: " + t);
			  }
			}
			
			public class B extends A<java.util.List<Number>> {
			  void g1 (java.util.List<?> t) {
			    System.out.println("g1 derived: " + t);
			  }
			  void g2 (java.util.List<Number> t) {
			    System.out.println("g2 derived: " + t);
			  }
			  public static void main (String[] args) {
			    B b = new B();
			    b.g1(new java.util.ArrayList<Number>());
			    b.g2(new java.util.ArrayList<Number>());
			  }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("B.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding ag2= a.getMethods()[1].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bg1= b.getMethods()[0].resolveBinding();
		IMethodBinding bg2= b.getMethods()[1].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bg1, true));
		assertSame(ag2, Bindings.findOverriddenMethod(bg2, true).getMethodDeclaration()); // found method is from parameterized superclass
	}

	@Test
	public void test15Bug97027() throws Exception {
		String str= """
			package override.test;
			class AA<T> {
			    public AA<Object> test() { return null; }
			}
			class BB extends AA<CC> {
			    public <T> BB test() { return null; }
			}
			class CC {}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("AA.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertTrue(problems[0].isError());
		assertEquals(IProblem.MethodNameClash, problems[0].getID());

		TypeDeclaration bb= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bbtest= bb.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bbtest, true));
	}

	@Test
	public void test15JLS3_842() throws Exception {
		String str= """
			package override.test;
			import java.util.Collection;
			import java.util.List;
			class CollectionConverter {
			    <T> List<T> toList(Collection<T> c) { return null; }
			}
			class Overrider extends CollectionConverter {
			    List toList(Collection c) { return null; }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("CollectionConverter.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertTrue(problems[0].isWarning());
		assertEquals(IProblem.UnsafeReturnTypeOverride, problems[0].getID());

		TypeDeclaration collectionConverter= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding collectionConverter_toList= collectionConverter.getMethods()[0].resolveBinding();

		TypeDeclaration overrider= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding overrider_toList= overrider.getMethods()[0].resolveBinding();

		assertSame(collectionConverter_toList, Bindings.findOverriddenMethod(overrider_toList, true));
	}

	@Test
	public void test15JLS3_848_1() throws Exception {
		String str= """
			package override.test;
			class C implements Cloneable {
			    C copy() { return (C)clone(); }
			}
			class D extends C implements Cloneable {
			    D copy() { return (D)clone(); }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("C.java", str, false, null);

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

	@Test
	public void test15JLS3_848_2() throws Exception {
		String str= """
			package override.test;
			import java.util.ArrayList;
			import java.util.Collection;
			import java.util.List;
			class StringSorter {
			    List<String> toList(Collection<String> c) { return new ArrayList<String>(c); }
			}
			class Overrider extends StringSorter {
			    List toList(Collection c) { return new ArrayList(c); }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("StringSorter.java", str, false, null);

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

	@Test
	public void test15JLS3_848_3() throws Exception {
		String str= """
			package override.test;
			class C<T> {
			    T id(T x) { return x; }
			}
			class D extends C<String> {
			    Object id(Object x) { return x; }
			    String id(String x) { return x; }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("C.java", str, false, null);

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

	@Test
	public void test15JLS3_848_4() throws Exception {
		String str= """
			package override.test;
			class C<T> {
			    public T id (T x) { return x; }
			}
			interface I<T> {
			    public T id(T x);
			}
			class D extends C<String> implements I<Integer> {
			    public String id(String x) { return x; }
			    public Integer id(Integer x) { return x; }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("C.java", str, false, null);

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

	@Test
	public void test15ClassTypeVars1() throws Exception {
		String str= """
			package override.test;
			class A<E extends Number, F> {
			    void take(E e, F f) {}
			}
			
			class B<S extends Number, T> extends A<S, T> {
			    void take(S e, T f) {}
			    void take(T f, S e) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

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

	@Test
	public void test15ClassTypeVars2() throws Exception {
		String str= """
			package override.test;
			class A<T extends Number> {
			    void m(T t) {}
			}
			class B<S extends Integer> extends A<S> {
			    @Override
			     void m(S t) { System.out.println("B: " + t); }
			}
			class C extends A/*raw*/ {
			    @Override
			    void m(Number t) { System.out.println("C: " + t); }
			}
			class D extends B/*raw*/ {
			    @Override
			    void m(Number t) { System.out.println("C#m(Number): " + t); }
			    @Override
			    void m(Integer t) { System.out.println("C#m(Integer): " + t); }
			}
			class E extends B<Integer> {
			    //illegal:
			    void m(Number t) { System.out.println("D#m(Number): " + t); }
			    @Override
			    void m(Integer t) { System.out.println("D#m(Integer): " + t); }
			}
			
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

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

	@Test
	public void test15MethodTypeVars1() throws Exception {
		String str= """
			package override.test;
			class A {
			    <E extends Number, F> void take(E e, F f) {}
			}
			
			class B extends A {
			    <S extends Number, T> void take(S e, T f) {}
			    <S extends Number, T> void take(T f, S e) {}
			    <S extends Number, T extends S> void take(S e, T f) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

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

	@Test
	public void test15MethodTypeVars2() throws Exception {
		String str= """
			package override.test;
			class A {
			    <E extends Number, F> void take(E e, F f) {}
			}
			
			class B extends A {
			    <S extends Number, T extends Object> void take(S e, T f) {}
			    <S extends Integer, T> void take(S e, T f) {}
			    <S extends Number, T> void take(T f, S e) {}
			    <S extends Number, T extends S> void take(T f, S e) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

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

	@Test
	public void test15MethodTypeVars3() throws Exception {
		String str= """
			package override.test;
			class A {
			    void take(Object t) {}
			    <T> void take2(T t) {}
			}
			class B extends A {
			    <T> void take(T t) {}
			    <T, S> void take2(T t) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

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

	@Test
	public void test15MethodTypeVars4() throws Exception {
		String str= """
			package override.test;
			public class A {
			    <T, U extends T> void m(T t, U u) { }
			}
			class B extends A {
			    @Override
			    <X, Y extends X> void m(X t, Y u) { }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding am= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		assertEquals(am, Bindings.findOverriddenMethod(bm, true));
	}

	@Test
	public void test15MethodTypeVars5() throws Exception {
		String str= """
			package override.test;
			import java.util.List;
			class A {
			    <T extends List<Number>> void m(T t) {}
			}
			class B extends A {
			    <S extends List<Integer>> void m(S t) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(1, problems.length);
		assertEquals(IProblem.MethodNameClash, problems[0].getID());

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(bm, true));
	}

	@Test
	public void test15MethodTypeVars6() throws Exception {
		String str= """
			package override.test;
			import java.util.List;
			class A {
			    <T extends List<T>> void m(T t) {}
			}
			class B extends A {
			    @Override
			    <S extends List<S>> void m(S t) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration a= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding am= a.getMethods()[0].resolveBinding();

		TypeDeclaration b= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding bm= b.getMethods()[0].resolveBinding();

		assertEquals(am, Bindings.findOverriddenMethod(bm, true));
	}

	@Test
	public void test15Bug99608() throws Exception {
		String str= """
			package override.test;
			class Top<E> {
			    void add(E[] e) {}
			    void remove(E... e) {}
			}
			class Sub extends Top<String> {
			    void add(String... s) {}
			    void remove(String[] s) {}
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("Top.java", str, false, null);

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

	@Test
	public void test15Bug90114() throws Exception {
		String str= """
			package override.test;
			class SuperX {
			    static void notOverridden() {
			        return;
			    }
			}
			public class X extends SuperX {
			    static void notOverridden() {
			        return;
			    }
			}\s
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("X.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration x= (TypeDeclaration) astRoot.types().get(1);
		IMethodBinding xnotOverridden= x.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(xnotOverridden, true));
	}

	@Test
	public void test15Bug89516primitive() throws Exception {
		String str= """
			package override.test;
			import java.util.ArrayList;
			public class Test extends ArrayList<String> {
			    static final long serialVersionUID= 1L;
			    public boolean add(int i) {
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("Test.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration test= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding testAdd= test.getMethods()[0].resolveBinding();

		assertNull(Bindings.findOverriddenMethod(testAdd, true));
	}

	@Test
	public void test15Bug105669() throws Exception {
		String str= """
			package override.test;
			import java.util.*;
			class I extends Vector<Number> {
			    static final long serialVersionUID= 1L;
			    @Override
			    public synchronized boolean addAll(Collection c) {
			        return false;
			    }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("I.java", str, false, null);

		CompilationUnit astRoot= createAST(cu);
		IProblem[] problems= astRoot.getProblems();
		assertEquals(0, problems.length);

		TypeDeclaration i= (TypeDeclaration) astRoot.types().get(0);
		IMethodBinding iaddAll= i.getMethods()[0].resolveBinding();

		IMethodBinding overridden= Bindings.findOverriddenMethod(iaddAll, true).getMethodDeclaration();
		ITypeBinding vector= i.getSuperclassType().resolveBinding().getTypeDeclaration();

		assertTrue(Arrays.asList(vector.getDeclaredMethods()).contains(overridden));
	}

	@Test
	public void test15Bug107105() throws Exception {
		String str= """
			package override.test;
			import java.io.Serializable;
			
			class A {
			    <S extends Number & Serializable & Runnable > void foo2(S s) { }
			}
			
			class B extends A {
			    @Override // should error
			    <S extends Number & Runnable> void foo2(S s) { }
			}
			
			class C extends A {
			    @Override // should error
			    <S extends Number & Runnable & Cloneable> void foo2(S s) { }
			}
			
			class D extends A {
			    @Override // correct
			    <S extends Number & Runnable & Serializable> void foo2(S s) { }
			}
			interface I extends Runnable, Serializable { }
			class E extends A {
			    @Override //should error
			    <S extends Number & I> void foo2(S s) { }
			}
			""";
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", str, false, null);

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
