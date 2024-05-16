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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation.DelegateEntry;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests generation of delegate methods
 */
public class GenerateDelegateMethodsTest extends SourceTestCase {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Before
	public void before() {
		String str= """
			/* (non-Javadoc)
			 * ${see_to_target}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, str, null);
	}

	public void runOperation(IType type, IField[] fields, IMethod[] methods, IJavaElement insertBefore, boolean createComments) throws CoreException {

		assertEquals(fields.length, methods.length);

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		DelegateEntry[] entries= new DelegateEntry[fields.length];

		for (int i= 0; i < fields.length; i++) {

			IField field= fields[i];
			IMethod method= methods[i];

			assertTrue(field.exists());
			assertTrue(method.exists());

			// Fields
			VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, unit);
			IVariableBinding b= frag.resolveBinding();

			IMethodBinding delegate= Bindings.findMethodInHierarchy(b.getType(), method.getElementName(), (String[]) null);
			entries[i]= new DelegateEntry(delegate, b);
		}

		fSettings.createComments= createComments;

		AddDelegateMethodsOperation op= new AddDelegateMethodsOperation(unit, entries, insertBefore, fSettings, true, true);

		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	public void runOperation(IType type, IField[] fields, IMethod[] methods) throws CoreException {
		runOperation(type, fields, methods, null, true);
	}

	// ------------- Actual tests

	/**
	 * Tests normal delegate method generation.
	 */
	@Test
	public void test01() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	public void foo_b() {}\r\n" +
				"	public void bar_b() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IMethod foo_b= b.getType("B").getMethod("foo_b", new String[0]);
		IMethod bar_b= b.getType("B").getMethod("bar_b", new String[0]);

		ICompilationUnit c= fPackageP.createCompilationUnit("C.java", "package p;\r\n" +
				"\r\n" +
				"public class C {\r\n" +
				"	\r\n" +
				"	public void foo_c() {}\r\n" +
				"	public void bar_c() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IMethod foo_c= c.getType("C").getMethod("foo_c", new String[0]);
		IMethod bar_c= c.getType("C").getMethod("bar_c", new String[0]);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"\r\n" +
				"	B b1;\r\n" +
				"	B b2;\r\n" +
				"	C c1;\r\n" +
				"	C c2;\r\n" +
				"}\r\n" +
				"", true, null);

		IField b1= a.getType("A").getField("b1");
		IField b2= a.getType("A").getField("b2");
		IField c1= a.getType("A").getField("c1");
		IField c2= a.getType("A").getField("c2");

		runOperation(a.getType("A"), new IField[] { b1, b2, c1, c2 }, new IMethod[] { foo_b, bar_b, foo_c, bar_c } );

		String expected= """
			package p;\r
			\r
			public class A {\r
			\r
				B b1;\r
				B b2;\r
				C c1;\r
				C c2;\r
				/* (non-Javadoc)\r
				 * @see p.B#foo_b()\r
				 */\r
				public void foo_b() {\r
					b1.foo_b();\r
				}\r
				/* (non-Javadoc)\r
				 * @see p.B#bar_b()\r
				 */\r
				public void bar_b() {\r
					b2.bar_b();\r
				}\r
				/* (non-Javadoc)\r
				 * @see p.C#foo_c()\r
				 */\r
				public void foo_c() {\r
					c1.foo_c();\r
				}\r
				/* (non-Javadoc)\r
				 * @see p.C#bar_c()\r
				 */\r
				public void bar_c() {\r
					c2.bar_c();\r
				}\r
			}""";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test insertion in-between two existing methods (before foo3).
	 */
	@Test
	public void test02() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	public void foo_b() {}\r\n" +
				"	public void bar_b() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IMethod foo_b= b.getType("B").getMethod("foo_b", new String[0]);
		IMethod bar_b= b.getType("B").getMethod("bar_b", new String[0]);

		ICompilationUnit c= fPackageP.createCompilationUnit("C.java", "package p;\r\n" +
				"\r\n" +
				"public class C {\r\n" +
				"	\r\n" +
				"	public void foo_c() {}\r\n" +
				"	public void bar_c() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IMethod foo_c= c.getType("C").getMethod("foo_c", new String[0]);
		IMethod bar_c= c.getType("C").getMethod("bar_c", new String[0]);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"\r\n" +
				"	B b1;\r\n" +
				"	B b2;\r\n" +
				"	C c1;\r\n" +
				"	C c2;\r\n" +
				"	\r\n" +
				"	public void foo2() {}\r\n" +
				"	\r\n" +
				"	public void foo3() {}\r\n" +
				"}\r\n" +
				"", true, null);

		IField b1= a.getType("A").getField("b1");
		IField b2= a.getType("A").getField("b2");
		IField c1= a.getType("A").getField("c1");
		IField c2= a.getType("A").getField("c2");

		IMethod insertBefore= a.getType("A").getMethod("foo3", new String[0]);

		runOperation(a.getType("A"), new IField[] { b1, b2, c1, c2 }, new IMethod[] { foo_b, bar_b, foo_c, bar_c } , insertBefore, true);

		String expected= "package p;\r\n" +
		"\r\n" +
		"public class A {\r\n" +
		"\r\n" +
		"	B b1;\r\n" +
		"	B b2;\r\n" +
		"	C c1;\r\n" +
		"	C c2;\r\n" +
		"	\r\n" +
		"	public void foo2() {}\r\n" +
		"	\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.B#foo_b()\r\n" +
		"	 */\r\n" +
		"	public void foo_b() {\r\n" +
		"		b1.foo_b();\r\n" +
		"	}\r\n" +
		"\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.B#bar_b()\r\n" +
		"	 */\r\n" +
		"	public void bar_b() {\r\n" +
		"		b2.bar_b();\r\n" +
		"	}\r\n" +
		"\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.C#foo_c()\r\n" +
		"	 */\r\n" +
		"	public void foo_c() {\r\n" +
		"		c1.foo_c();\r\n" +
		"	}\r\n" +
		"\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.C#bar_c()\r\n" +
		"	 */\r\n" +
		"	public void bar_c() {\r\n" +
		"		c2.bar_c();\r\n" +
		"	}\r\n" +
		"\r\n" +
		"	public void foo3() {}\r\n" +
		"}\r\n" +
		"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test insertion of imports
	 */
	@Test
	public void test03() throws Exception {

		IPackageFragment packageSomeOtherPackage= fRoot.createPackageFragment("someOtherPackage", true, null);
		packageSomeOtherPackage.createCompilationUnit("OtherClass.java", "package someOtherPackage;\r\n" +
				"\r\n" +
				"public class OtherClass {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"import someOtherPackage.OtherClass;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	public OtherClass returnOtherClass() {\r\n" +
				"		return null;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B b;\r\n" +
				"}\r\n" +
				"", true, null);

		IField field1= a.getType("A").getField("b");
		IMethod method1= b.getType("B").getMethod("returnOtherClass", new String[0]);

		runOperation(a.getType("A"), new IField[] { field1 }, new IMethod[] { method1 });

		compareSource("package p;\r\n" +
				"\r\n" +
				"import someOtherPackage.OtherClass;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B b;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.B#returnOtherClass()\r\n" +
				"	 */\r\n" +
				"	public OtherClass returnOtherClass() {\r\n" +
				"		return b.returnOtherClass();\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", a.getSource());
	}

	/**
	 * Tests generic types
	 */
	@Test
	public void test04() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B<E> {\r\n" +
				"	\r\n" +
				"	public E get() {\r\n" +
				"		return null;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	public void set(E e) {\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				B<A> someField;\r
			\r
			}""", true, null);

		IField field= a.getType("A").getField("someField");
		IMethod method= b.getType("B").getMethod("get", new String[0]);
		IMethod method2= b.getType("B").getMethod("set", new String[] { "QE;" });

		runOperation(a.getType("A"), new IField[] { field, field } , new IMethod[] { method, method2 });

		compareSource("""
			package p;\r
			\r
			public class A {\r
				\r
				B<A> someField;\r
			\r
				/* (non-Javadoc)\r
				 * @see p.B#get()\r
				 */\r
				public A get() {\r
					return someField.get();\r
				}\r
			\r
				/* (non-Javadoc)\r
				 * @see p.B#set(java.lang.Object)\r
				 */\r
				public void set(A e) {\r
					someField.set(e);\r
				}\r
			\r
			}""", a.getSource());
	}

	/**
	 * Tests generic methods
	 */
	@Test
	public void test05() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	public <E> E getSome(E e) {\r\n" +
				"		return e;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B someField;\r\n" +
				"}\r\n" +
				"", true, null);

		IField field= a.getType("A").getField("someField");
		IMethod sMethod= b.getType("B").getMethod("getSome", new String[] { "QE;" });

		runOperation(a.getType("A"), new IField[] { field } , new IMethod[] { sMethod });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B someField;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.B#getSome(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	public <E> E getSome(E e) {\r\n" +
				"		return someField.getSome(e);\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", a.getSource());
	}

	/**
	 *
	 * Test enum types
	 */
	@Test
	public void test06() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	static enum SomeEnum { X, Y };\r\n" +
				"	\r\n" +
				"	public SomeEnum getIt() {\r\n" +
				"		return SomeEnum.X;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B someField;\r\n" +
				"}\r\n" +
				"", true, null);

		IField field= a.getType("A").getField("someField");
		IMethod sMethod= b.getType("B").getMethod("getIt", new String[0]);

		runOperation(a.getType("A"), new IField[] { field } , new IMethod[] { sMethod });

		compareSource("""
			package p;\r
			\r
			import p.B.SomeEnum;\r
			\r
			public class A {\r
				\r
				B someField;\r
			\r
				/* (non-Javadoc)\r
				 * @see p.B#getIt()\r
				 */\r
				public SomeEnum getIt() {\r
					return someField.getIt();\r
				}\r
			}""", a.getSource());

	}

	/**
	 * Test generation in inner types.
	 */
	@Test
	public void test07() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			\r
				class C {\r
					\r
					A some;\r
				}\r
				\r
				public void foo() {}\r
			\r
			}""", true, null);

		IField fieldSome= a.getType("A").getType("C").getField("some");
		IMethod method= a.getType("A").getMethod("foo", new String[0]);

		runOperation(a.getType("A").getType("C"), new IField[] { fieldSome }, new IMethod[] { method });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"\r\n" +
				"	class C {\r\n" +
				"		\r\n" +
				"		A some;\r\n" +
				"\r\n" +
				"		/* (non-Javadoc)\r\n" +
				"		 * @see p.A#foo()\r\n" +
				"		 */\r\n" +
				"		public void foo() {\r\n" +
				"			some.foo();\r\n" +
				"		}\r\n" +
				"	}\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", a.getSource());
	}

	/**
	 * Test generation in anonymous types
	 *
	 * See also bug 112440 (bug only affects gui, however).
	 */
	@Test
	public void test08() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				public void foo() {\r
					A a = new A() {\r
						A someA;\r
					};\r
				}\r
			}""", true, null);

		IType anonType= (IType)a.getElementAt(70);
		IField theField= anonType.getField("someA");
		IMethod theMethod= a.getType("A").getMethod("foo", new String[0]);

		runOperation(anonType, new IField[] { theField }, new IMethod[] { theMethod });

		compareSource("""
			package p;\r
			\r
			public class A {\r
				public void foo() {\r
					A a = new A() {\r
						A someA;\r
			\r
						/* (non-Javadoc)\r
						 * @see p.A#foo()\r
						 */\r
						public void foo() {\r
							someA.foo();\r
						}\r
					};\r
				}\r
			}""", a.getSource());
	}

	/**
	 * Test delegate generation on type variable typed field
	 */
	@Test
	public void test09() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A<E extends B> {\r
			\r
				E someField;\r
				\r
			}""", true, null);

		IType typeA= a.getType("A");
		IField someField= typeA.getField("someField");
		IMethod theMethod= b.getType("B").getMethod("foo", new String[0]);

		runOperation(typeA, new IField[] { someField }, new IMethod[] { theMethod });

		compareSource("""
			package p;\r
			\r
			public class A<E extends B> {\r
			\r
				E someField;\r
			\r
				/* (non-Javadoc)\r
				 * @see p.B#foo()\r
				 */\r
				public void foo() {\r
					someField.foo();\r
				}\r
				\r
			}""", a.getSource());
	}

	/**
	 * Test delegate generation in secondary types
	 */
	@Test
	public void test10() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public void foo() {}\r\n" +
				"}\r\n" +
				"\r\n" +
				"class SecondaryClass {\r\n" +
				"	A someField;\r\n" +
				"}\r\n" +
				"", true, null);

		IType secondaryType= (IType)a.getElementAt(70);
		IField someField= secondaryType.getField("someField");
		IMethod theMethod= a.getType("A").getMethod("foo", new String[0]);

		runOperation(secondaryType, new IField[] { someField }, new IMethod[] { theMethod });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public void foo() {}\r\n" +
				"}\r\n" +
				"\r\n" +
				"class SecondaryClass {\r\n" +
				"	A someField;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.A#foo()\r\n" +
				"	 */\r\n" +
				"	public void foo() {\r\n" +
				"		someField.foo();\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", a.getSource());
	}

	/**
	 * Test delegate generation in secondary types with final methods
	 */
	@Test
	public void test11() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public void foo() {\r\n" +
				"	}\r\n" +
				"	public final void bar() {\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"\r\n" +
				"class SecondardClass {\r\n" +
				"	A someField;\r\n" +
				"}\r\n" +
				"", true, null);

		IType secondaryType= (IType)a.getElementAt(110);
		IField someField= secondaryType.getField("someField");
		IMethod firstMethod= a.getType("A").getMethod("foo", new String[0]);
		IMethod secondMethod= a.getType("A").getMethod("bar", new String[0]);

		runOperation(secondaryType, new IField[] { someField, someField }, new IMethod[] { firstMethod, secondMethod });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public void foo() {\r\n" +
				"	}\r\n" +
				"	public final void bar() {\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"\r\n" +
				"class SecondardClass {\r\n" +
				"	A someField;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.A#foo()\r\n" +
				"	 */\r\n" +
				"	public void foo() {\r\n" +
				"		someField.foo();\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.A#bar()\r\n" +
				"	 */\r\n" +
				"	public final void bar() {\r\n" +
				"		someField.bar();\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", a.getSource());
	}

	/**
	 * Test generation from interface both with and without default methods
	 *
	 * @throws Exception CoreException, JavaModelException or IOException
	 */
	@Test
	public void test12() throws Exception {

		/*ICompilationUnit i= */fPackageP.createCompilationUnit("I.java", """
			package p;\r
			\r
			public interface I {\r
				public void foo();\r
				public default void bar(String s) {\r
					if(s == null) {\r
						return;\r
					};\r
				}\r
			}""", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				I i;\r
			}""", true, null);

		IType anonType= (IType) a.getElementAt(32);
		IField theField= anonType.getField("i");

		IType iType= fPackageP.getJavaProject().findType("p.I");
		IMethod fooMethod= iType.getMethod("foo", new String[0]);
		IMethod barMethod= iType.getMethod("bar", new String[] { "QString;" });

		runOperation(anonType, new IField[] { theField, theField }, new IMethod[] { fooMethod, barMethod });

		compareSource("""
			package p;
			
			public class A {
				I i;
			
				/* (non-Javadoc)
				 * @see p.I#foo()
				 */
				public void foo() {
					i.foo();
				}
			
				/* (non-Javadoc)
				 * @see p.I#bar(java.lang.String)
				 */
				public void bar(String s) {
					i.bar(s);
				}
			}""", a.getSource());
	}

	@Test
	public void insertAt() throws Exception {
		String originalContent= """
			package p;
			
			public class A  {
				Runnable x;
			\t
				A() {
				}
			\t
				void foo() {
				}
			\t
				{
				}
			\t
				static {
				}
			\t
				class Inner {
				}
			}""";

		final int NUM_MEMBERS= 6;

		String expectedConstructor= """
			public void run() {
					x.run();
				}""";


		IType runnableType= fPackageP.getJavaProject().findType("java.lang.Runnable");
		IMethod runMethod= runnableType.getMethod("run", new String[0]);

		// try to insert the new delegate after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackageP.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				IField foo= (IField) children[0];
				assertEquals(NUM_MEMBERS, children.length);

				IJavaElement insertBefore= i < NUM_MEMBERS ? children[i] : null;

				runOperation(type, new IField[] { foo }, new IMethod[] { runMethod }, insertBefore, false);

				IJavaElement[] newChildren= type.getChildren();
				assertEquals(NUM_MEMBERS + 1, newChildren.length);
				String source= ((IMember) newChildren[i]).getSource(); // new element expected at index i
				assertEquals("Insert before " + insertBefore, expectedConstructor, source);
			} finally {
				if (unit != null) {
					JavaProjectHelper.delete(unit);
				}
			}
		}
	}
}
