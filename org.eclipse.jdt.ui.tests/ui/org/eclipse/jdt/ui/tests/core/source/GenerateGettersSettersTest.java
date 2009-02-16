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
package org.eclipse.jdt.ui.tests.core.source;

import junit.framework.Test;
import junit.framework.TestSuite;

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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Tests generation of getters and setters.
 *
 * @see org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation
 *
 */
public class GenerateGettersSettersTest extends SourceTestCase {

	static final Class THIS= GenerateGettersSettersTest.class;

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	private static final IField[] NOFIELDS= new IField[] {};

	public GenerateGettersSettersTest(String name) {
		super(name);
	}

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
	 * @throws CoreException
	 *
	 */
	private void runOperation(IType type, IField[] getters, IField[] setters, IField[] gettersAndSetters, boolean sort, int visibility, IJavaElement sibling) throws CoreException {

		IRequestQuery allYes= new IRequestQuery() {

			public int doQuery(IMember member) {
				return IRequestQuery.YES_ALL;
			}
		};

		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
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
	 *
	 * @throws Exception
	 */
	public void test0() throws Exception {

		IField field1= fClassA.createField("String field1;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	String field1;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the field1.\r\n" +
				"	 */\r\n" +
				"	public String getField1() {\r\n" +
				"		return field1;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param field1 The field1 to set.\r\n" +
				"	 */\r\n" +
				"	public void setField1(String field1) {\r\n" +
				"		this.field1 = field1;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 * 
	 * @throws Exception
	 */
	public void testDoneWithSmartIs() throws Exception {

		IField field1= fClassA.createField("boolean done;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	boolean done;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the done.\r\n" +
				"	 */\r\n" +
				"	public boolean isDone() {\r\n" +
				"		return done;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param done The done to set.\r\n" +
				"	 */\r\n" +
				"	public void setDone(boolean done) {\r\n" +
				"		this.done = done;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 * 
	 * @throws Exception
	 */
	public void testIsDoneWithSmartIs() throws Exception {

		IField field1= fClassA.createField("boolean isDone;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	boolean isDone;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the isDone.\r\n" +
				"	 */\r\n" +
				"	public boolean isDone() {\r\n" +
				"		return isDone;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param isDone The isDone to set.\r\n" +
				"	 */\r\n" +
				"	public void setDone(boolean isDone) {\r\n" +
				"		this.isDone = isDone;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 * 
	 * @throws Exception
	 */
	public void testDoneWithoutSmartIs() throws Exception {
		final IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		try {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, false);
			IField field1= fClassA.createField("boolean done;", null, false, new NullProgressMonitor());
			runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

			String expected= "public class A {\r\n" +
				"\r\n" +
				"	boolean done;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the done.\r\n" +
				"	 */\r\n" +
					"	public boolean getDone() {\r\n" +
				"		return done;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param done The done to set.\r\n" +
				"	 */\r\n" +
				"	public void setDone(boolean done) {\r\n" +
				"		this.done = done;\r\n" +
				"	}\r\n" +
				"}";

			compareSource(expected, fClassA.getSource());
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, true);
		}
	}

	/**
	 * Tests normal getter/setter generation for one field.
	 * 
	 * @throws Exception
	 */
	public void testIsDoneWithoutSmartIs() throws Exception {
		final IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		try {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, false);
			IField field1= fClassA.createField("boolean isDone;", null, false, new NullProgressMonitor());
			runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

			String expected= "public class A {\r\n" +
				"\r\n" +
				"	boolean isDone;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the isDone.\r\n" +
				"	 */\r\n" +
					"	public boolean getIsDone() {\r\n" +
				"		return isDone;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param isDone The isDone to set.\r\n" +
				"	 */\r\n" +
					"	public void setIsDone(boolean isDone) {\r\n" +
				"		this.isDone = isDone;\r\n" +
				"	}\r\n" +
				"}";

			compareSource(expected, fClassA.getSource());
		} finally {
			store.setValue(PreferenceConstants.CODEGEN_IS_FOR_GETTERS, true);
		}
	}

	/**
	 * No setter for final fields (if skipped by user, as per parameter)
	 * @throws Exception
	 */
	public void test1() throws Exception {

		IField field1= fClassA.createField("final String field1 = null;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	String field1 = null;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the field1.\r\n" +
				"	 */\r\n" +
				"	public String getField1() {\r\n" +
				"		return field1;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param field1 The field1 to set.\r\n" +
				"	 */\r\n" +
				"	public void setField1(String field1) {\r\n" +
				"		this.field1 = field1;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Tests if full-qualified field declaration type is also full-qualified in setter parameter.
	 * @throws Exception
	 */
	public void test2() throws Exception {

		createNewType("q.Other");
		IField field1= fClassA.createField("q.Other field1;", null, false, new NullProgressMonitor());
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	q.Other field1;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the field1.\r\n" +
				"	 */\r\n" +
				"	public q.Other getField1() {\r\n" +
				"		return field1;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param field1 The field1 to set.\r\n" +
				"	 */\r\n" +
				"	public void setField1(q.Other field1) {\r\n" +
				"		this.field1 = field1;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}


	/**
	 * Test parameterized types in field declarations
	 * @throws Exception
	 */
	public void test3() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"import java.util.HashMap;\r\n" +
				"import java.util.Map;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"	Map<String,String> a = new HashMap<String,String>();\r\n" +
				"}", true, null);

		IType classB= b.getType("B");
		IField field1= classB.getField("a");

		runOperation(classB, NOFIELDS,NOFIELDS, new IField[] { field1 });

		String expected= "public class B {\r\n" +
				"\r\n" +
				"	Map<String,String> a = new HashMap<String,String>();\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the a.\r\n" +
				"	 */\r\n" +
				"	public Map<String, String> getA() {\r\n" +
				"		return a;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param a The a to set.\r\n" +
				"	 */\r\n" +
				"	public void setA(Map<String, String> a) {\r\n" +
				"		this.a = a;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, classB.getSource());
	}


	/**
	 * Tests enum typed fields
	 * @throws Exception
	 */
	public void test4() throws Exception {

		IType theEnum= fClassA.createType("private enum ENUM { C,D,E };", null, false, null);
		IField field1= fClassA.createField("private ENUM someEnum;", theEnum, false, null);
		runOperation(NOFIELDS, NOFIELDS, new IField[] { field1 });

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	private ENUM someEnum;\r\n" +
				"\r\n" +
				"	private enum ENUM { C,D,E }\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the someEnum.\r\n" +
				"	 */\r\n" +
				"	public ENUM getSomeEnum() {\r\n" +
				"		return someEnum;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param someEnum The someEnum to set.\r\n" +
				"	 */\r\n" +
				"	public void setSomeEnum(ENUM someEnum) {\r\n" +
				"		this.someEnum = someEnum;\r\n" +
				"	};\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Test generation for more than one field
	 * @throws Exception
	 */
	public void test5() throws Exception {

		createNewType("q.Other");
		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"import q.Other;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"	private String a;\r\n" +
				"	private String b;\r\n" +
				"	protected Other c;\r\n" +
				"	public String d;\r\n" +
				"}", true, null);

		IType classB= b.getType("B");
		IField field1= classB.getField("a");
		IField field2= classB.getField("b");
		IField field3= classB.getField("c");
		IField field4= classB.getField("d");
		runOperation(classB, NOFIELDS, NOFIELDS, new IField[] { field1, field2, field3, field4 });

		String expected= "public class B {\r\n" +
				"\r\n" +
				"	private String a;\r\n" +
				"	private String b;\r\n" +
				"	protected Other c;\r\n" +
				"	public String d;\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the a.\r\n" +
				"	 */\r\n" +
				"	public String getA() {\r\n" +
				"		return a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param a The a to set.\r\n" +
				"	 */\r\n" +
				"	public void setA(String a) {\r\n" +
				"		this.a = a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the b.\r\n" +
				"	 */\r\n" +
				"	public String getB() {\r\n" +
				"		return b;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param b The b to set.\r\n" +
				"	 */\r\n" +
				"	public void setB(String b) {\r\n" +
				"		this.b = b;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the c.\r\n" +
				"	 */\r\n" +
				"	public Other getC() {\r\n" +
				"		return c;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param c The c to set.\r\n" +
				"	 */\r\n" +
				"	public void setC(Other c) {\r\n" +
				"		this.c = c;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the d.\r\n" +
				"	 */\r\n" +
				"	public String getD() {\r\n" +
				"		return d;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param d The d to set.\r\n" +
				"	 */\r\n" +
				"	public void setD(String d) {\r\n" +
				"		this.d = d;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, classB.getSource());
	}

	/**
	 * Some more fields, this time sorted
	 * @throws Exception
	 */
	public void test6() throws Exception {

		IField field1= fClassA.createField("private String a;", null, false, new NullProgressMonitor());
		IField field2= fClassA.createField("private String b;", null, false, new NullProgressMonitor());
		IField field3= fClassA.createField("public String d;", null, false, new NullProgressMonitor());
		//Note that in sorted mode, the fields must be provided separately
		runOperation(new IField[] { field1, field2, field3 }, new IField[] { field1, field2, field3 }, NOFIELDS, true);

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	private String a;\r\n" +
				"	private String b;\r\n" +
				"	public String d;\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the a.\r\n" +
				"	 */\r\n" +
				"	public String getA() {\r\n" +
				"		return a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the b.\r\n" +
				"	 */\r\n" +
				"	public String getB() {\r\n" +
				"		return b;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the d.\r\n" +
				"	 */\r\n" +
				"	public String getD() {\r\n" +
				"		return d;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param a The a to set.\r\n" +
				"	 */\r\n" +
				"	public void setA(String a) {\r\n" +
				"		this.a = a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param b The b to set.\r\n" +
				"	 */\r\n" +
				"	public void setB(String b) {\r\n" +
				"		this.b = b;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param d The d to set.\r\n" +
				"	 */\r\n" +
				"	public void setD(String d) {\r\n" +
				"		this.d = d;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Test getter/setter generation in anonymous type
	 * @throws Exception
	 */
	public void test7() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"	{\r\n" +
				"		B someAnon = new B() {\r\n" +
				"			\r\n" +
				"			A innerfield;\r\n" +
				"			\r\n" +
				"		};\r\n" +
				"	}\r\n" +
				"}", true, null);

		IType anon= (IType)b.getElementAt(60); // This is the position of the constructor of the anonymous type
		IField field= anon.getField("innerfield");
		runOperation(anon, NOFIELDS, NOFIELDS, new IField[] { field } , false, Modifier.PUBLIC, null);

		String expected= "public class B {\r\n" +
				"\r\n" +
				"	{\r\n" +
				"		B someAnon = new B() {\r\n" +
				"			\r\n" +
				"			A innerfield;\r\n" +
				"\r\n" +
				"			/**\r\n" +
				"			 * @return Returns the innerfield.\r\n" +
				"			 */\r\n" +
				"			public A getInnerfield() {\r\n" +
				"				return innerfield;\r\n" +
				"			}\r\n" +
				"\r\n" +
				"			/**\r\n" +
				"			 * @param innerfield The innerfield to set.\r\n" +
				"			 */\r\n" +
				"			public void setInnerfield(A innerfield) {\r\n" +
				"				this.innerfield = innerfield;\r\n" +
				"			}\r\n" +
				"			\r\n" +
				"		};\r\n" +
				"	}\r\n" +
				"}";
		compareSource(expected, b.getType("B").getSource());
	}

	/**
	 * Tests other modifiers for the generated getters
	 * @throws Exception
	 */
	public void test8() throws Exception {

		IField field1= fClassA.createField("private Object o;", null, false, new NullProgressMonitor());
		runOperation(fClassA, new IField[] { field1 }, NOFIELDS, NOFIELDS, false, Modifier.NONE | Modifier.SYNCHRONIZED, null);

		String expected= "public class A {\r\n" +
				"\r\n" +
				"	private Object o;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the o.\r\n" +
				"	 */\r\n" +
				"	synchronized Object getO() {\r\n" +
				"		return o;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, fClassA.getSource());
	}

	/**
	 * Verify existing getters are not overwritten, and setters are created
	 * @throws Exception
	 */
	public void test9() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"	private Object o;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the o.\r\n" +
				"	 */\r\n" +
				"	synchronized Object getO() {\r\n" +
				"		return o;\r\n" +
				"	}\r\n" +
				"}", true, null);

		IType classB= b.getType("B");
		IField field= classB.getField("o");
		runOperation(classB, NOFIELDS, NOFIELDS, new IField[] { field }, false, Modifier.PUBLIC, field);

		String expected= "public class B {\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param o The o to set.\r\n" +
				"	 */\r\n" +
				"	public void setO(Object o) {\r\n" +
				"		this.o = o;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	private Object o;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the o.\r\n" +
				"	 */\r\n" +
				"	synchronized Object getO() {\r\n" +
				"		return o;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, classB.getSource());
	}

	/**
	 * Test creation for combined field declarations; creating a pair of accessors for
	 * one variable and only a setter for another.
	 * @throws Exception
	 */
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

		String expected= "public class B {\r\n" +
				"\r\n" +
				"	private float c, d, e = 0;\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the d.\r\n" +
				"	 */\r\n" +
				"	public float getD() {\r\n" +
				"		return d;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param d The d to set.\r\n" +
				"	 */\r\n" +
				"	public void setD(float d) {\r\n" +
				"		this.d = d;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/**\r\n" +
				"	 * @param e The e to set.\r\n" +
				"	 */\r\n" +
				"	public void setE(float e) {\r\n" +
				"		this.e = e;\r\n" +
				"	}\r\n" +
				"}\r\n";

		compareSource(expected, classB.getSource());
	}

	/**
	 * Tests insertion of members before a certain method.
	 * @throws Exception
	 */
	public void test11() throws Exception {

		ICompilationUnit b= fPackageP.createCompilationUnit("B.java", "public class B {\r\n" +
				"	\r\n" +
				"	private float a;\r\n" +
				"	private float b;\r\n" +
				"	private float c;\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the a.\r\n" +
				"	 */\r\n" +
				"	public float getA() {\r\n" +
				"		return a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param a The a to set.\r\n" +
				"	 */\r\n" +
				"	public void setA(float a) {\r\n" +
				"		this.a = a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the b.\r\n" +
				"	 */\r\n" +
				"	public float getB() {\r\n" +
				"		return b;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param b The b to set.\r\n" +
				"	 */\r\n" +
				"	public void setB(float b) {\r\n" +
				"		this.b = b;\r\n" +
				"	}\r\n" +
				"}\r\n", true, null);

		IType classB= b.getType("B");
		IField field= classB.getField("c");
		IMethod getB= classB.getMethod("getB", new String[0]);

		//Insert before getB.
		runOperation(classB, NOFIELDS, NOFIELDS, new IField[] { field }, false, Modifier.PUBLIC, getB);

		String expected= "public class B {\r\n" +
				"	\r\n" +
				"	private float a;\r\n" +
				"	private float b;\r\n" +
				"	private float c;\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the a.\r\n" +
				"	 */\r\n" +
				"	public float getA() {\r\n" +
				"		return a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param a The a to set.\r\n" +
				"	 */\r\n" +
				"	public void setA(float a) {\r\n" +
				"		this.a = a;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the c.\r\n" +
				"	 */\r\n" +
				"	public float getC() {\r\n" +
				"		return c;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param c The c to set.\r\n" +
				"	 */\r\n" +
				"	public void setC(float c) {\r\n" +
				"		this.c = c;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @return Returns the b.\r\n" +
				"	 */\r\n" +
				"	public float getB() {\r\n" +
				"		return b;\r\n" +
				"	}\r\n" +
				"	/**\r\n" +
				"	 * @param b The b to set.\r\n" +
				"	 */\r\n" +
				"	public void setB(float b) {\r\n" +
				"		this.b = b;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, classB.getSource());
	}

	public void testInsertSetterAtLocation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append("	 * @return Returns the x.\n");
		buf.append("	 */\n");
		buf.append("	public Runnable getX() {\n");
		buf.append("		return x;\n");
		buf.append("	}");
		String expectedGetter= buf.toString();

		assertInsertAt(expectedGetter, true);
	}

	public void testInsertGetterAtLocation() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append("	 * @param x The x to set.\n");
		buf.append("	 */\n");
		buf.append("	public void setX(Runnable x) {\n");
		buf.append("		this.x = x;\n");
		buf.append("	}");
		String expectedSetter= buf.toString();

		assertInsertAt(expectedSetter, false);
	}

	public void testInsertGetterAtLocation2() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append("	 * @param x The x to set.\n");
		buf.append("	 */\n");
		buf.append("	public void setX(Runnable x) {\n");
		buf.append("		this.x = x;\n");
		buf.append("	}");
		String expectedSetter= buf.toString();

		assertInsertAt2(expectedSetter, false);
	}

	private void assertInsertAt(String expectedMethod, boolean isGetter) throws CoreException {
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
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class A  {\n");
		buf.append("	Runnable x;\n");
		buf.append("	\n");
		buf.append(" // begin\n");
		buf.append("	/**\n");
		buf.append("	 * Javadoc\n");
		buf.append("	 */\n");
		buf.append("	A() {\n");
		buf.append("	} // end of line\n");
		buf.append("	\n");
		buf.append(" // begin\n");
		buf.append("	void foo() {\n");
		buf.append("	} // end of line\n");
		buf.append("	\n");
		buf.append(" // begin\n");
		buf.append("	{\n"); // initializer
		buf.append("	} // end of line\n");
		buf.append("	\n");
		buf.append(" // begin\n");
		buf.append("	static {\n"); // static initializer
		buf.append("	} // end of line\n");
		buf.append("	\n");
		buf.append(" // begin\n");
		buf.append("	class Inner {\n"); // inner class
		buf.append("	} // end of line\n");
		buf.append("}");
		String originalContent= buf.toString();

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
