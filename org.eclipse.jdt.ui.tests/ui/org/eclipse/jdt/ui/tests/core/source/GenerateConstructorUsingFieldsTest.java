/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import java.lang.reflect.Modifier;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests generation of constructors using fields
 *
 * @see org.eclipse.jdt.internal.corext.codemanipulation.AddCustomConstructorOperation
 */
public class GenerateConstructorUsingFieldsTest extends SourceTestCase {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	public void runOperation(IType type, IField[] fields, IMethod superConstructor, IJavaElement insertBefore, boolean createComments, boolean omitSuper, int visibility) throws CoreException {

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		ITypeBinding typeBinding= ASTNodes.getTypeBinding(unit, type);

		IVariableBinding[] bindings= new IVariableBinding[fields.length];
		for (int i= 0; i < fields.length; i++) {
			assertTrue(fields[i].exists());
			VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fields[i], unit);
			bindings[i]= frag.resolveBinding();
		}

		IMethodBinding constructorToInvoke;
		if (superConstructor != null) {
			CompilationUnit mUnit= parser.parse(superConstructor.getCompilationUnit(), true);
			MethodDeclaration mdl= ASTNodeSearchUtil.getMethodDeclarationNode(superConstructor, mUnit);
			constructorToInvoke= mdl.resolveBinding();
		} else
			constructorToInvoke= getObjectConstructor(unit);

		fSettings.createComments= createComments;

		AddCustomConstructorOperation op= new AddCustomConstructorOperation(unit, typeBinding, bindings, constructorToInvoke, insertBefore, fSettings, true, true);
		op.setOmitSuper(omitSuper);
		op.setVisibility(visibility);

		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	public void runOperation(IType type, IField[] fields, IMethod superConstructor) throws CoreException {
		runOperation(type, fields, superConstructor, null, true, false, Modifier.PUBLIC);
	}

	private IMethodBinding getObjectConstructor(CompilationUnit compilationUnit) {
		final ITypeBinding binding= compilationUnit.getAST().resolveWellKnownType("java.lang.Object"); //$NON-NLS-1$
		return Bindings.findMethodInType(binding, "Object", new ITypeBinding[0]); //$NON-NLS-1$
	}

	private void runIt(String topLevelTypeName, String[] fieldNames, IMethod superConstructor, String source, String destination) throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit(topLevelTypeName + ".java", source, true, null);
		IType type= a.getType(topLevelTypeName);
		IField[] fields= new IField[fieldNames.length];
		for (int i= 0; i < fieldNames.length; i++)
			fields[i]= type.getField(fieldNames[i]);

		runOperation(type, fields, superConstructor);

		compareSource(destination, a.getSource());
	}

	private void runIt(String topLevelTypeName, String[] fieldNames, String source, String destination) throws Exception {
		runIt(topLevelTypeName, fieldNames, null, source, destination);
	}


	// ----------------------- actual tests --------------------------

	/**
	 * Tests simple constructor generation with one field
	 */
	@Test
	public void test01() throws Exception {

		runIt("A",
				new String[] { "field1" },
				"""
					package p;\r
					\r
					public class A {\r
						String field1;\r
					\r
					}""",

				"""
					package p;\r
					\r
					public class A {\r
						String field1;\r
					\r
						/**\r
						 * @param field1\r
						 */\r
						public A(String field1) {\r
							super();\r
							this.field1 = field1;\r
						}\r
					\r
					}""");
	}

	/**
	 * Tests adding two fields with identically named classes from different packages
	 */
	@Ignore("see bug 113052 (import issue)")
	@Test
	public void test02() throws Exception {

//		ICompilationUnit pB= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
//		"\r\n" +
//		"public class B {\r\n" +
//		"\r\n" +
//		"}\r\n" +
//		"", true, null);

		IPackageFragment packageQ= fRoot.createPackageFragment("q", true, null);
		packageQ.createCompilationUnit("B.java", "package p;\r\n" +
		"\r\n" +
		"public class B {\r\n" +
		"\r\n" +
		"}\r\n" +
		"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
		"\r\n" +
		"public class A {\r\n" +
		"	q.B field1;\r\n" +
		"	B field2;\r\n" +
		"}\r\n" +
		"", true, null);

		IField f1= a.getType("A").getField("field1");
		IField f2= a.getType("A").getField("field2");

		runOperation(a.getType("A"), new IField[] { f1, f2 }, null);

		compareSource("package p;\r\n" +
		"\r\n" +
		"public class A {\r\n" +
		"	q.B field1;\r\n" +
		"	B field2;\r\n" +
		"	/**\r\n" +
		"	 * @param field1\r\n" +
		"	 * @param field2\r\n" +
		"	 */\r\n" +
		"	public A(q.B field1, B field2) {\r\n" +
		"		super();\r\n" +
		"		// TODO Auto-generated constructor stub\r\n" +
		"		this.field1 = field1;\r\n" +
		"		this.field2 = field2;\r\n" +
		"	}\r\n" +
		"}\r\n" +
		"", a.getSource());
	}

	/**
	 * Ensure field ordering stays constant
	 */
	@Test
	public void test03() throws Exception {

		runIt("A", new String[] { "firstField", "secondField", "beforeHandThirdField" },
				"""
					package p;\r
					\r
					import java.util.List;\r
					\r
					public class A {\r
						String firstField;\r
						List secondField;\r
						A beforeHandThirdField;\r
					}""",

				"package p;\r\n" +
				"\r\n" +
				"import java.util.List;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	String firstField;\r\n" +
				"	List secondField;\r\n" +
				"	A beforeHandThirdField;\r\n" +
				"	/**\r\n" +
				"	 * @param firstField\r\n" +
				"	 * @param secondField\r\n" +
				"	 * @param beforeHandThirdField\r\n" +
				"	 */\r\n" +
				"	public A(String firstField, List secondField, A beforeHandThirdField) {\r\n" +
				"		super();\r\n" +
				"		this.firstField = firstField;\r\n" +
				"		this.secondField = secondField;\r\n" +
				"		this.beforeHandThirdField = beforeHandThirdField;\r\n" +
				"	}\r\n" +
				"}\r\n" +
		"");
	}

	/**
	 * Test insertion between two methods
	 */
	@Test
	public void test04() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				String field;\r
			\r
				public void method1() {}\r
				public void method2() {}\r
			}""", true, null);
		IType typeA= a.getType("A");
		IField firstField= typeA.getField("field");
		IMethod method= typeA.getMethod("method2", new String[0]);

		runOperation(typeA, new IField[] { firstField }, null, method, true, false, Modifier.PUBLIC);

		compareSource("""
			package p;\r
			\r
			public class A {\r
				String field;\r
			\r
				public void method1() {}\r
				/**\r
				 * @param field\r
				 */\r
				public A(String field) {\r
					super();\r
					this.field = field;\r
				}\r
				public void method2() {}\r
			}""", a.getSource());
	}

	/**
	 * Without comments
	 */
	@Test
	public void test05() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				String field;\r
			}""", true, null);
		IType typeA= a.getType("A");
		IField field= typeA.getField("field");

		runOperation(typeA, new IField[] { field }, null, null, false, false, Modifier.PUBLIC);

		compareSource("""
			package p;\r
			\r
			public class A {\r
				String field;\r
			\r
				public A(String field) {\r
					super();\r
					this.field = field;\r
				}\r
			}""", a.getSource());
	}

	/**
	 * With a different modifier
	 */
	@Test
	public void test06() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				String field;\r
			}""", true, null);
		IType typeA= a.getType("A");
		IField field= typeA.getField("field");

		runOperation(typeA, new IField[] { field }, null, null, false, false, Modifier.PROTECTED);

		compareSource("""
			package p;\r
			\r
			public class A {\r
				String field;\r
			\r
				protected A(String field) {\r
					super();\r
					this.field = field;\r
				}\r
			}""", a.getSource());
	}

	/**
	 * Omitting the super constructor
	 */
	@Test
	public void test07() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				String field;\r
			}""", true, null);
		IType typeA= a.getType("A");
		IField field= typeA.getField("field");

		runOperation(typeA, new IField[] { field }, null, null, false, true, Modifier.PUBLIC);

		compareSource("""
			package p;\r
			\r
			public class A {\r
				String field;\r
			\r
				public A(String field) {\r
					this.field = field;\r
				}\r
			}""", a.getSource());
	}

	/**
	 * Type variables; generic types in fields
	 */
	@Test
	public void test08() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Map;\r
			\r
			public class A<E> {\r
				Map<String, B> field1;\r
				E field2;\r
			}""", true, null);

		IType typeA= a.getType("A");
		IField field1= typeA.getField("field1");
		IField field2= typeA.getField("field2");

		runOperation(typeA, new IField[] { field1, field2 }, null);

		compareSource("""
			package p;\r
			\r
			import java.util.Map;\r
			\r
			public class A<E> {\r
				Map<String, B> field1;\r
				E field2;\r
				/**\r
				 * @param field1\r
				 * @param field2\r
				 */\r
				public A(Map<String, B> field1, E field2) {\r
					super();\r
					this.field1 = field1;\r
					this.field2 = field2;\r
				}\r
			}""", a.getSource());
	}

	/**
	 * Enums
	 */
	@Test
	public void test09() throws Exception {

		runIt("A", new String[] { "field1" },
				"""
					package p;\r
					\r
					public class A {\r
						public enum B { C, D };\r
						B field1;\r
					}""",

				"""
					package p;\r
					\r
					public class A {\r
						public enum B { C, D };\r
						B field1;\r
						/**\r
						 * @param field1\r
						 */\r
						public A(B field1) {\r
							super();\r
							this.field1 = field1;\r
						}\r
					}""");
	}

	/**
	 * Final uninitialized fields
	 */
	@Test
	public void test10() throws Exception {

		runIt("A", new String[] { "field1" },
				"""
					package p;\r
					\r
					public class A {\r
						final A field1;\r
					}""",

				"""
					package p;\r
					\r
					public class A {\r
						final A field1;\r
					\r
						/**\r
						 * @param field1\r
						 */\r
						public A(A field1) {\r
							super();\r
							this.field1 = field1;\r
						}\r
					}""");
	}

	/**
	 * Verify JDT code conventions are followed, see bug 111801
	 */
	@Test
	public void test11() throws Exception {

		runIt("A", new String[] { "startDate", "endDate" },
				"""
					package p;\r
					\r
					import java.util.Date;\r
					\r
					public class A {\r
						private Date startDate;\r
						private Date endDate;\r
					}""",

				"""
					package p;\r
					\r
					import java.util.Date;\r
					\r
					public class A {\r
						private Date startDate;\r
						private Date endDate;\r
						/**\r
						 * @param startDate\r
						 * @param endDate\r
						 */\r
						public A(Date startDate, Date endDate) {\r
							super();\r
							this.startDate = startDate;\r
							this.endDate = endDate;\r
						}\r
					}""");
	}


	/**
	 * Name clashing fun with super constructors
	 */
	@Test
	public void test12() throws Exception {

		ICompilationUnit unit= fPackageP.createCompilationUnit("SuperA.java", "package p;\r\n" +
				"\r\n" +
				"public class SuperA {\r\n" +
				"	\r\n" +
				"	public SuperA() {};\r\n" +
				"	public SuperA(String a, String b) {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IMethod superConstructor= unit.getType("SuperA").getMethod("SuperA", new String[] { "QString;", "QString;" });

		runIt("A", new String[] { "a", "b" }, superConstructor,
				"package p;\r\n" +
				"\r\n" +
				"public class A extends SuperA {\r\n" +
				"	\r\n" +
				"	String a;\r\n" +
				"	String b;\r\n" +
				"}\r\n" +
				"",

				"""
					package p;\r
					\r
					public class A extends SuperA {\r
						\r
						String a;\r
						String b;\r
						/**\r
						 * @param a\r
						 * @param b\r
						 * @param a2\r
						 * @param b2\r
						 */\r
						public A(String a, String b, String a2, String b2) {\r
							super(a, b);\r
							a = a2;\r
							b = b2;\r
						}\r
					}""");
	}

	/**
	 * Generic types in parameters of super constructor
	 */
	@Test
	public void test13() throws Exception {

		ICompilationUnit unit= fPackageP.createCompilationUnit("SuperA.java", "package p;\r\n" +
				"\r\n" +
				"import java.util.Map;\r\n" +
				"\r\n" +
				"public class SuperA {\r\n" +
				"	\r\n" +
				"	private Map<String, A> someMap;\r\n" +
				"\r\n" +
				"	public SuperA() {}\r\n" +
				"\r\n" +
				"	public SuperA(Map<String, A> someMap) {\r\n" +
				"		this.someMap = someMap;\r\n" +
				"	};\r\n" +
				"}\r\n" +
				"", true, null);

		IMethod superConstructor= unit.getType("SuperA").getMethod("SuperA", new String[] { "QMap<QString;QA;>;" });

		runIt("A", new String[] { "field" }, superConstructor,
				"package p;\r\n" +
				"\r\n" +
				"public class A extends SuperA {\r\n" +
				"	String field;\r\n" +
				"}\r\n" +
				"",

				"""
					package p;\r
					\r
					import java.util.Map;\r
					\r
					public class A extends SuperA {\r
						String field;\r
					\r
						/**\r
						 * @param someMap\r
						 * @param field\r
						 */\r
						public A(Map<String, A> someMap, String field) {\r
							super(someMap);\r
							this.field = field;\r
						}\r
					}""");
	}

	/**
	 * Inner types
	 */
	@Test
	public void test14() throws Exception {

		ICompilationUnit unit= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A  {\r\n" +
				"	\r\n" +
				"	class B {\r\n" +
				"		A b;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", true, null);

		IType innerType= unit.getType("A").getType("B");
		IField field= innerType.getField("b");

		runOperation(innerType, new IField[] { field }, null);

		compareSource("""
			package p;\r
			\r
			public class A  {\r
				\r
				class B {\r
					A b;\r
			\r
					/**\r
					 * @param b\r
					 */\r
					public B(A b) {\r
						super();\r
						this.b = b;\r
					}\r
				}\r
			}""", unit.getSource());
	}

	@Test
	public void test15() throws Exception {

		ICompilationUnit unit= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A  {\r
			}\r
			\r
			class B {\r
				A foo;\r
			}""", true, null);

		IType secondary= (IType)unit.getElementAt(40); // get secondary type
		IField foo= secondary.getField("foo");

		runOperation(secondary, new IField[] { foo }, null);

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A  {\r\n" +
				"}\r\n" +
				"\r\n" +
				"class B {\r\n" +
				"	A foo;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param foo\r\n" +
				"	 */\r\n" +
				"	public B(A foo) {\r\n" +
				"		super();\r\n" +
				"		this.foo = foo;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", unit.getSource());
	}
	@Test
	public void test16() throws Exception {  // https://bugs.eclipse.org/bugs/show_bug.cgi?id=552556

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			}""", true, null);
		IType typeA= a.getType("A");

		runOperation(typeA, new IField[] {}, null, null, false, false, Modifier.PUBLIC);

		compareSource("""
			package p;\r
			\r
			public class A {\r
			\r
				public A() {\r
					super();\r
				}\r
			}""", a.getSource());
	}

	@Test
	public void insertAt1() throws Exception {
		String originalContent= """
			package p;
			
			public class A  {
				int x;
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
			public A(int x) {
					this.x = x;
				}""";

		// try to insert the new constructor after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackageP.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				IField foo= (IField) children[0];
				assertEquals(NUM_MEMBERS, children.length);

				IJavaElement insertBefore= i < NUM_MEMBERS ? children[i] : null;

				runOperation(type, new IField[] { foo }, null, insertBefore, false, true, Flags.AccPublic);

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
}
