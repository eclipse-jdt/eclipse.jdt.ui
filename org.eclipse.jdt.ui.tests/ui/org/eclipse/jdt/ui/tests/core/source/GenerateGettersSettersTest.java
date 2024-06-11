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

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Tests generation of getters and setters.
 *
 * @see org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation
 */
public class GenerateGettersSettersTest extends SourceTestCase {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private static final IField[] NOFIELDS= new IField[] {};

	private IType createNewType(String fQName) throws JavaModelException {
		final String pkg= fQName.substring(0, fQName.lastIndexOf('.'));
		final String typeName= fQName.substring(fQName.lastIndexOf('.') + 1);
		final IPackageFragment fragment= fRoot.createPackageFragment(pkg, true, null);
		final ICompilationUnit unit= fragment.getCompilationUnit(typeName + ".java");
		return unit.createType("public class " + typeName + " {\n}\n", null, true, null);
	}

	/**
	 * Create and run a new getter/setter operation; do not ask for anything
	 * (always skip setters for final fields; never overwrite existing methods).
	 * @param type the type
	 * @param getters fields to create getters for
	 * @param setters fields to create setters for
	 * @param gettersAndSetters fields to create getters and setters for
	 * @param sort enable sort
	 * @param visibility visibility for new methods
	 * @param sibling element to insert before
	 */
	private void runOperation(IType type, IField[] getters, IField[] setters, IField[] gettersAndSetters, boolean sort, int visibility, IJavaElement sibling) throws CoreException {

		IRequestQuery allYes= member -> IRequestQuery.YES_ALL;

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		AddGetterSetterOperation op= new AddGetterSetterOperation(type, getters, setters, gettersAndSetters, unit, allYes, sibling, fSettings, true, true);
		op.setSort(sort);
		op.setVisibility(visibility);

		op.run(new NullProgressMonitor());

		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	private void runOperation(IField[] getters, IField[] setters, IField[] gettersAndSetters) throws CoreException {
		runOperation(fClassA, getters, setters, gettersAndSetters, false, Modifier.PUBLIC, null);
	}

	private void runOperation(IType type, IField[] getters, IField[] setters, IField[] gettersAndSetters) throws CoreException {
		runOperation(type, getters, setters, gettersAndSetters, false, Modifier.PUBLIC, null);
	}

	private void runOperation(IField[] getters, IField[] setters, IField[] gettersAndSetters, boolean sort) throws CoreException {
		runOperation(fClassA, getters, setters, gettersAndSetters, sort, Modifier.PUBLIC, null);
	}

	// --------------------- Actual tests

	/**
	 * Tests normal getter/setter generation for one field.
	 */
	@Test
	public void test0() throws Exception {

		IField field1= fClassA.createField("String field1;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= """
			public class A {\r
			\r
				String field1;\r
			\r
				/**\r
				 * @return Returns the field1.\r
				 */\r
				public String getField1() {\r
					return field1;\r
				}\r
			\r
				/**\r
				 * @param field1 The field1 to set.\r
				 */\r
				public void setField1(String field1) {\r
					this.field1 = field1;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 */
	@Test
	public void doneWithSmartIs() throws Exception {

		IField field1= fClassA.createField("boolean done;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= """
			public class A {\r
			\r
				boolean done;\r
			\r
				/**\r
				 * @return Returns the done.\r
				 */\r
				public boolean isDone() {\r
					return done;\r
				}\r
			\r
				/**\r
				 * @param done The done to set.\r
				 */\r
				public void setDone(boolean done) {\r
					this.done = done;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 */
	@Test
	public void isDoneWithSmartIs() throws Exception {

		IField field1= fClassA.createField("boolean isDone;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= """
			public class A {\r
			\r
				boolean isDone;\r
			\r
				/**\r
				 * @return Returns the isDone.\r
				 */\r
				public boolean isDone() {\r
					return isDone;\r
				}\r
			\r
				/**\r
				 * @param isDone The isDone to set.\r
				 */\r
				public void setDone(boolean isDone) {\r
					this.isDone = isDone;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 */
	@Test
	public void doneWithoutSmartIs() throws Exception {
		final IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		try {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, false);
			IField field1= fClassA.createField("boolean done;", null, false, new NullProgressMonitor());
			runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

			String expected= """
				public class A {\r
				\r
					boolean done;\r
				\r
					/**\r
					 * @return Returns the done.\r
					 */\r
					public boolean getDone() {\r
						return done;\r
					}\r
				\r
					/**\r
					 * @param done The done to set.\r
					 */\r
					public void setDone(boolean done) {\r
						this.done = done;\r
					}\r
				}""";

			compareSource(expected, fClassA.getSource());
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, true);
		}
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 */
	@Test
	public void isDoneWithoutSmartIs() throws Exception {
		final IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		try {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, false);
			IField field1= fClassA.createField("boolean isDone;", null, false, new NullProgressMonitor());
			runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

			String expected= """
				public class A {\r
				\r
					boolean isDone;\r
				\r
					/**\r
					 * @return Returns the isDone.\r
					 */\r
					public boolean getIsDone() {\r
						return isDone;\r
					}\r
				\r
					/**\r
					 * @param isDone The isDone to set.\r
					 */\r
					public void setIsDone(boolean isDone) {\r
						this.isDone = isDone;\r
					}\r
				}""";

			compareSource(expected, fClassA.getSource());
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, true);
		}
	}

	/**
	 * No setter for final fields (if skipped by user, as per parameter)
	 */
	@Test
	public void test1() throws Exception {

		IField field1= fClassA.createField("final String field1 = null;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= """
			public class A {\r
			\r
				String field1 = null;\r
			\r
				/**\r
				 * @return Returns the field1.\r
				 */\r
				public String getField1() {\r
					return field1;\r
				}\r
			\r
				/**\r
				 * @param field1 The field1 to set.\r
				 */\r
				public void setField1(String field1) {\r
					this.field1 = field1;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests if full-qualified field declaration type is also full-qualified in setter parameter.
	 */
	@Test
	public void test2() throws Exception {

		createNewType("q.Other");
		IField field1= fClassA.createField("q.Other field1;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= """
			public class A {\r
			\r
				q.Other field1;\r
			\r
				/**\r
				 * @return Returns the field1.\r
				 */\r
				public q.Other getField1() {\r
					return field1;\r
				}\r
			\r
				/**\r
				 * @param field1 The field1 to set.\r
				 */\r
				public void setField1(q.Other field1) {\r
					this.field1 = field1;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}


	/**
	 * Test parameterized types in field declarations
	 */
	@Test
	public void test3() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", """
			package p;\r
			\r
			import java.util.HashMap;\r
			import java.util.Map;\r
			\r
			public class B {\r
			\r
				Map<String,String> a = new HashMap<String,String>();\r
			}""", true, null);

		IType classB= b.getType("B");
		IField field1= classB.getField("a");

		runOperation(classB, NOFIELDS,NOFIELDS, new IField[] { field1 });

		String expected= """
			public class B {\r
			\r
				Map<String,String> a = new HashMap<String,String>();\r
			\r
				/**\r
				 * @return Returns the a.\r
				 */\r
				public Map<String, String> getA() {\r
					return a;\r
				}\r
			\r
				/**\r
				 * @param a The a to set.\r
				 */\r
				public void setA(Map<String, String> a) {\r
					this.a = a;\r
				}\r
			}""";

		compareSource(expected, classB.getSource());
	}


	/**
	 * Tests enum typed fields
	 */
	@Test
	public void test4() throws Exception {

		IType theEnum= fClassA.createType("private enum ENUM { C,D,E };", null, false, null);
		IField field1= fClassA.createField("private ENUM someEnum;", theEnum, false, null);
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= """
			public class A {\r
			\r
				private ENUM someEnum;\r
			\r
				private enum ENUM { C,D,E }\r
			\r
				/**\r
				 * @return Returns the someEnum.\r
				 */\r
				public ENUM getSomeEnum() {\r
					return someEnum;\r
				}\r
			\r
				/**\r
				 * @param someEnum The someEnum to set.\r
				 */\r
				public void setSomeEnum(ENUM someEnum) {\r
					this.someEnum = someEnum;\r
				};\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Test generation for more than one field
	 */
	@Test
	public void test5() throws Exception {

		createNewType("q.Other");
		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", """
			package p;\r
			\r
			import q.Other;\r
			\r
			public class B {\r
			\r
				private String a;\r
				private String b;\r
				protected Other c;\r
				public String d;\r
			}""", true, null);

		IType classB= b.getType("B");
		IField field1= classB.getField("a");
		IField field2= classB.getField("b");
		IField field3= classB.getField("c");
		IField field4= classB.getField("d");
		runOperation(classB, NOFIELDS, NOFIELDS, new IField[] { field1, field2, field3, field4 });

		String expected= """
			public class B {\r
			\r
				private String a;\r
				private String b;\r
				protected Other c;\r
				public String d;\r
				/**\r
				 * @return Returns the a.\r
				 */\r
				public String getA() {\r
					return a;\r
				}\r
				/**\r
				 * @param a The a to set.\r
				 */\r
				public void setA(String a) {\r
					this.a = a;\r
				}\r
				/**\r
				 * @return Returns the b.\r
				 */\r
				public String getB() {\r
					return b;\r
				}\r
				/**\r
				 * @param b The b to set.\r
				 */\r
				public void setB(String b) {\r
					this.b = b;\r
				}\r
				/**\r
				 * @return Returns the c.\r
				 */\r
				public Other getC() {\r
					return c;\r
				}\r
				/**\r
				 * @param c The c to set.\r
				 */\r
				public void setC(Other c) {\r
					this.c = c;\r
				}\r
				/**\r
				 * @return Returns the d.\r
				 */\r
				public String getD() {\r
					return d;\r
				}\r
				/**\r
				 * @param d The d to set.\r
				 */\r
				public void setD(String d) {\r
					this.d = d;\r
				}\r
			}""";

		compareSource(expected, classB.getSource());
	}

	/**
	 * Some more fields, this time sorted
	 */
	@Test
	public void test6() throws Exception {

		IField field1= fClassA.createField("private String a;", null, false, new NullProgressMonitor());
		IField field2= fClassA.createField("private String b;", null, false, new NullProgressMonitor());
		IField field3= fClassA.createField("public String d;", null, false, new NullProgressMonitor());
		//Note that in sorted mode, the fields must be provided separately
		runOperation(new IField[] { field1, field2, field3 }, new IField[] { field1, field2, field3 }, NOFIELDS, true);

		String expected= """
			public class A {\r
			\r
				private String a;\r
				private String b;\r
				public String d;\r
				/**\r
				 * @return Returns the a.\r
				 */\r
				public String getA() {\r
					return a;\r
				}\r
				/**\r
				 * @return Returns the b.\r
				 */\r
				public String getB() {\r
					return b;\r
				}\r
				/**\r
				 * @return Returns the d.\r
				 */\r
				public String getD() {\r
					return d;\r
				}\r
				/**\r
				 * @param a The a to set.\r
				 */\r
				public void setA(String a) {\r
					this.a = a;\r
				}\r
				/**\r
				 * @param b The b to set.\r
				 */\r
				public void setB(String b) {\r
					this.b = b;\r
				}\r
				/**\r
				 * @param d The d to set.\r
				 */\r
				public void setD(String d) {\r
					this.d = d;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Test getter/setter generation in anonymous type
	 */
	@Test
	public void test7() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", """
			package p;\r
			\r
			public class B {\r
			\r
				{\r
					B someAnon = new B() {\r
						\r
						A innerfield;\r
						\r
					};\r
				}\r
			}""", true, null);

		IType anon= (IType)b.getElementAt(60); // This is the position of the constructor of the anonymous type
		IField field= anon.getField("innerfield");
		runOperation(anon, NOFIELDS, NOFIELDS, new IField[] { field } , false, Modifier.PUBLIC, null);

		String expected= """
			public class B {\r
			\r
				{\r
					B someAnon = new B() {\r
						\r
						A innerfield;\r
			\r
						/**\r
						 * @return Returns the innerfield.\r
						 */\r
						public A getInnerfield() {\r
							return innerfield;\r
						}\r
			\r
						/**\r
						 * @param innerfield The innerfield to set.\r
						 */\r
						public void setInnerfield(A innerfield) {\r
							this.innerfield = innerfield;\r
						}\r
						\r
					};\r
				}\r
			}""";
		compareSource(expected, b.getType("B").getSource());
	}

	/**
	 * Tests other modifiers for the generated getters
	 */
	@Test
	public void test8() throws Exception {

		IField field1= fClassA.createField("private Object o;", null, false, new NullProgressMonitor());
		runOperation(fClassA, new IField[] { field1 }, NOFIELDS, NOFIELDS, false, Modifier.NONE | Modifier.SYNCHRONIZED, null);

		String expected= """
			public class A {\r
			\r
				private Object o;\r
			\r
				/**\r
				 * @return Returns the o.\r
				 */\r
				synchronized Object getO() {\r
					return o;\r
				}\r
			}""";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Verify existing getters are not overwritten, and setters are created
	 */
	@Test
	public void test9() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", """
			package p;\r
			\r
			public class B {\r
			\r
				private Object o;\r
			\r
				/**\r
				 * @return Returns the o.\r
				 */\r
				synchronized Object getO() {\r
					return o;\r
				}\r
			}""", true, null);

		IType classB= b.getType("B");
		IField field= classB.getField("o");
		runOperation(classB, NOFIELDS, NOFIELDS, new IField[] { field }, false, Modifier.PUBLIC, field);

		String expected= """
			public class B {\r
			\r
				/**\r
				 * @param o The o to set.\r
				 */\r
				public void setO(Object o) {\r
					this.o = o;\r
				}\r
			\r
				private Object o;\r
			\r
				/**\r
				 * @return Returns the o.\r
				 */\r
				synchronized Object getO() {\r
					return o;\r
				}\r
			}""";

		compareSource(expected, classB.getSource());
	}

	/**
	 * Test creation for combined field declarations; creating a pair of accessors for
	 * one variable and only a setter for another.
	 */
	@Test
	public void test10() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"	private float c, d, e = 0;\r\n" +
				"}\r\n" +
				"", true, null);

		IType classB= b.getType("B");
		IField field1= classB.getField("d");
		IField field2= classB.getField("e");
		runOperation(classB, NOFIELDS, new IField[] { field2 }, new IField[] { field1 }, false, Modifier.PUBLIC, null);

		String expected= """
			public class B {\r
			\r
				private float c, d, e = 0;\r
			\r
				/**\r
				 * @return Returns the d.\r
				 */\r
				public float getD() {\r
					return d;\r
				}\r
			\r
				/**\r
				 * @param d The d to set.\r
				 */\r
				public void setD(float d) {\r
					this.d = d;\r
				}\r
			\r
				/**\r
				 * @param e The e to set.\r
				 */\r
				public void setE(float e) {\r
					this.e = e;\r
				}\r
			}\r
			""";

		compareSource(expected, classB.getSource());
	}

	/**
	 * Tests insertion of members before a certain method.
	 */
	@Test
	public void test11() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", """
			public class B {\r
				\r
				private float a;\r
				private float b;\r
				private float c;\r
				/**\r
				 * @return Returns the a.\r
				 */\r
				public float getA() {\r
					return a;\r
				}\r
				/**\r
				 * @param a The a to set.\r
				 */\r
				public void setA(float a) {\r
					this.a = a;\r
				}\r
				/**\r
				 * @return Returns the b.\r
				 */\r
				public float getB() {\r
					return b;\r
				}\r
				/**\r
				 * @param b The b to set.\r
				 */\r
				public void setB(float b) {\r
					this.b = b;\r
				}\r
			}\r
			""", true, null);

		IType classB= b.getType("B");
		IField field= classB.getField("c");
		IMethod getB= classB.getMethod("getB", new String[0]);

		//Insert before getB.
		runOperation(classB, NOFIELDS, NOFIELDS, new IField[] { field }, false, Modifier.PUBLIC, getB);

		String expected= """
			public class B {\r
				\r
				private float a;\r
				private float b;\r
				private float c;\r
				/**\r
				 * @return Returns the a.\r
				 */\r
				public float getA() {\r
					return a;\r
				}\r
				/**\r
				 * @param a The a to set.\r
				 */\r
				public void setA(float a) {\r
					this.a = a;\r
				}\r
				/**\r
				 * @return Returns the c.\r
				 */\r
				public float getC() {\r
					return c;\r
				}\r
				/**\r
				 * @param c The c to set.\r
				 */\r
				public void setC(float c) {\r
					this.c = c;\r
				}\r
				/**\r
				 * @return Returns the b.\r
				 */\r
				public float getB() {\r
					return b;\r
				}\r
				/**\r
				 * @param b The b to set.\r
				 */\r
				public void setB(float b) {\r
					this.b = b;\r
				}\r
			}""";

		compareSource(expected, classB.getSource());
	}

	@Test
	public void insertSetterAtLocation() throws Exception {
		String expectedGetter= """
			/**
				 * @return Returns the x.
				 */
				public Runnable getX() {
					return x;
				}""";

		assertInsertAt(expectedGetter, true);
	}

	@Test
	public void insertGetterAtLocation() throws Exception {
		String expectedSetter= """
			/**
				 * @param x The x to set.
				 */
				public void setX(Runnable x) {
					this.x = x;
				}""";

		assertInsertAt(expectedSetter, false);
	}

	@Test
	public void insertGetterAtLocation2() throws Exception {
		String expectedSetter= """
			/**
				 * @param x The x to set.
				 */
				public void setX(Runnable x) {
					this.x = x;
				}""";

		assertInsertAt2(expectedSetter, false);
	}

	private void assertInsertAt(String expectedMethod, boolean isGetter) throws CoreException {
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


		// try to insert the new delegate after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackageP.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				IField field= (IField) children[0];
				assertEquals(NUM_MEMBERS, children.length);

				IJavaElement insertBefore= i < NUM_MEMBERS ? children[i] : null;

				IField[] getters= isGetter ? new IField[] { field } : new IField[0];
				IField[] setters= !isGetter ? new IField[] { field } : new IField[0];
				runOperation(type, getters, setters, new IField[0], false, Flags.AccPublic, insertBefore);

				IJavaElement[] newChildren= type.getChildren();
				assertEquals(NUM_MEMBERS + 1, newChildren.length);
				String source= ((IMember) newChildren[i]).getSource(); // new element expected at index i
				assertEquals("Insert before " + insertBefore, expectedMethod, source);
			} finally {
				if (unit != null) {
					JavaProjectHelper.delete(unit);
				}
			}
		}
	}

	private void assertInsertAt2(String expectedMethod, boolean isGetter) throws CoreException {
		String originalContent= """
			package p;
			
			public class A  {
				Runnable x;
			\t
			 // begin
				/**
				 * Javadoc
				 */
				A() {
				} // end of line
			\t
			 // begin
				void foo() {
				} // end of line
			\t
			 // begin
				{
				} // end of line
			\t
			 // begin
				static {
				} // end of line
			\t
			 // begin
				class Inner {
				} // end of line
			}""";

		final int NUM_MEMBERS= 6;


		// try to insert the new delegate after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackageP.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				IField field= (IField) children[0];
				assertEquals(NUM_MEMBERS, children.length);

				IJavaElement insertBefore= i < NUM_MEMBERS ? children[i] : null;

				IField[] getters= isGetter ? new IField[] { field } : new IField[0];
				IField[] setters= !isGetter ? new IField[] { field } : new IField[0];
				runOperation(type, getters, setters, new IField[0], false, Flags.AccPublic, insertBefore);

				IJavaElement[] newChildren= type.getChildren();
				assertEquals(NUM_MEMBERS + 1, newChildren.length);
				String source= ((IMember) newChildren[i]).getSource(); // new element expected at index i
				assertEquals("Insert before " + insertBefore, expectedMethod, source);
			} finally {
				if (unit != null) {
					JavaProjectHelper.delete(unit);
				}
			}
		}
	}

}
