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
package org.eclipse.jdt.ui.tests.core.source;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation.DelegateEntry;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
 * Tests generation of delegate methods
 *
 */
public class GenerateDelegateMethodsTest extends SourceTestCase {

	static final Class THIS= GenerateDelegateMethodsTest.class;

	public GenerateDelegateMethodsTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.tests.core.source.SourceTestCase#setUp()
	 */
	protected void setUp() throws CoreException {
		super.setUp();

		StringBuffer comment= new StringBuffer();
		comment.append("/* (non-Javadoc)\n");
		comment.append(" * ${see_to_target}\n");
		comment.append(" */");
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, comment.toString(), null);

	}

	public void runOperation(IType type, IField[] fields, IMethod[] methods, IJavaElement insertBefore, boolean createComments) throws CoreException {

		Assert.assertEquals(fields.length, methods.length);

		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		DelegateEntry[] entries= new DelegateEntry[fields.length];

		for (int i= 0; i < fields.length; i++) {

			IField field= fields[i];
			IMethod method= methods[i];

			Assert.assertTrue(field.exists());
			Assert.assertTrue(method.exists());

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
	 *
	 * @throws Exception
	 */
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

		String expected= "package p;\r\n" +
		"\r\n" +
		"public class A {\r\n" +
		"\r\n" +
		"	B b1;\r\n" +
		"	B b2;\r\n" +
		"	C c1;\r\n" +
		"	C c2;\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.B#foo_b()\r\n" +
		"	 */\r\n" +
		"	public void foo_b() {\r\n" +
		"		b1.foo_b();\r\n" +
		"	}\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.B#bar_b()\r\n" +
		"	 */\r\n" +
		"	public void bar_b() {\r\n" +
		"		b2.bar_b();\r\n" +
		"	}\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.C#foo_c()\r\n" +
		"	 */\r\n" +
		"	public void foo_c() {\r\n" +
		"		c1.foo_c();\r\n" +
		"	}\r\n" +
		"	/* (non-Javadoc)\r\n" +
		"	 * @see p.C#bar_c()\r\n" +
		"	 */\r\n" +
		"	public void bar_c() {\r\n" +
		"		c2.bar_c();\r\n" +
		"	}\r\n" +
		"}";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test insertion in-between two existing methods (before foo3).
	 *
	 * @throws Exception
	 */
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
	 *
	 * @throws Exception
	 */
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
	 *
	 * @throws Exception
	 */
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

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B<A> someField;\r\n" +
				"\r\n" +
				"}", true, null);

		IField field= a.getType("A").getField("someField");
		IMethod method= b.getType("B").getMethod("get", new String[0]);
		IMethod method2= b.getType("B").getMethod("set", new String[] { "QE;" });

		runOperation(a.getType("A"), new IField[] { field, field } , new IMethod[] { method, method2 });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B<A> someField;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.B#get()\r\n" +
				"	 */\r\n" +
				"	public A get() {\r\n" +
				"		return someField.get();\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.B#set(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	public void set(A e) {\r\n" +
				"		someField.set(e);\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}", a.getSource());
	}

	/**
	 * Tests generic methods
	 *
	 * @throws Exception
	 */
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
	 *
	 * @throws Exception
	 */
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

		compareSource("package p;\r\n" +
				"\r\n" +
				"import p.B.SomeEnum;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	B someField;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.B#getIt()\r\n" +
				"	 */\r\n" +
				"	public SomeEnum getIt() {\r\n" +
				"		return someField.getIt();\r\n" +
				"	}\r\n" +
				"}", a.getSource());

	}

	/**
	 * Test generation in inner types.
	 *
	 * @throws Exception
	 */
	public void test07() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"\r\n" +
				"	class C {\r\n" +
				"		\r\n" +
				"		A some;\r\n" +
				"	}\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"\r\n" +
				"}", true, null);

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
	 *
	 * @throws Exception
	 */
	public void test08() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public void foo() {\r\n" +
				"		A a = new A() {\r\n" +
				"			A someA;\r\n" +
				"		};\r\n" +
				"	}\r\n" +
				"}", true, null);

		IType anonType= (IType)a.getElementAt(70);
		IField theField= anonType.getField("someA");
		IMethod theMethod= a.getType("A").getMethod("foo", new String[0]);

		runOperation(anonType, new IField[] { theField }, new IMethod[] { theMethod });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public void foo() {\r\n" +
				"		A a = new A() {\r\n" +
				"			A someA;\r\n" +
				"\r\n" +
				"			/* (non-Javadoc)\r\n" +
				"			 * @see p.A#foo()\r\n" +
				"			 */\r\n" +
				"			public void foo() {\r\n" +
				"				someA.foo();\r\n" +
				"			}\r\n" +
				"		};\r\n" +
				"	}\r\n" +
				"}", a.getSource());
	}

	/**
	 * Test delegate generation on type variable typed field
	 *
	 * @throws Exception
	 */
	public void test09() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A<E extends B> {\r\n" +
				"\r\n" +
				"	E someField;\r\n" +
				"	\r\n" +
				"}", true, null);

		IType typeA= a.getType("A");
		IField someField= typeA.getField("someField");
		IMethod theMethod= b.getType("B").getMethod("foo", new String[0]);

		runOperation(typeA, new IField[] { someField }, new IMethod[] { theMethod });

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A<E extends B> {\r\n" +
				"\r\n" +
				"	E someField;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.B#foo()\r\n" +
				"	 */\r\n" +
				"	public void foo() {\r\n" +
				"		someField.foo();\r\n" +
				"	}\r\n" +
				"	\r\n" +
				"}", a.getSource());
	}

	/**
	 * Test delegate generation in secondary types
	 *
	 * @throws Exception
	 */
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
	 *
	 * @throws Exception
	 */
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

	public void testInsertAt() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class A  {\n");
		buf.append("	Runnable x;\n");
		buf.append("	\n");
		buf.append("	A() {\n");
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	void foo() {\n");
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	{\n"); // initializer
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	static {\n"); // static initializer
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	class Inner {\n"); // inner class
		buf.append("	}\n");
		buf.append("}");
		String originalContent= buf.toString();

		final int NUM_MEMBERS= 6;

		buf= new StringBuffer();
		buf.append("public void run() {\n");
		buf.append("		x.run();\n");
		buf.append("	}");
		String expectedConstructor= buf.toString();


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
