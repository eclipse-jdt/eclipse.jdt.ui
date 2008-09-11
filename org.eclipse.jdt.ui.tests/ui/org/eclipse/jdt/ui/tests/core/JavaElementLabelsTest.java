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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;


public class JavaElementLabelsTest extends CoreTests {

	private static final Class THIS= JavaElementLabelsTest.class;

	private IJavaProject fJProject1;

	public JavaElementLabelsTest(String name) {
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
			suite.addTest(new JavaElementLabelsTest("testOrganizeImportOnRange2"));
			return new ProjectTestSetup(suite);
		}
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testTypeLabelOuter() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("public class Outer {\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Outer"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "Outer - org.test");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer");
	}

	public void testTypeLabelInner() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class Outer {\n");
		buf.append("    public void foo(Vector vec) {\n");
		buf.append("    }\n");
		buf.append("    public class Inner {\n");
		buf.append("        public int inner(Vector vec) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Inner"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.Inner");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.Inner");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "Inner - org.test.Outer");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.Inner - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.Inner");
	}

	public void testTypeLabelLocal() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class Outer {\n");
		buf.append("    public void foo(Vector vec) {\n");
		buf.append("        public class Local {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Local"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.foo(...).Local");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.foo(...).Local");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "Local - org.test.Outer.foo(...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(...).Local - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(...).Local");
	}

	public void testTypeParameterLabelType() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.*;\n");
		buf.append("public class TypeParams<Q extends ArrayList<? extends Number>, Element extends Map<String, Integer>, NoBound> {\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("TypeParams.java", content, false, null);

		IType typeParams= (IType)cu.getElementAt(content.indexOf("TypeParams"));
		ITypeParameter[] typeParameters= typeParams.getTypeParameters();
		ITypeParameter q= typeParameters[0];
		ITypeParameter element= typeParameters[1];
		ITypeParameter nobound= typeParameters[2];

		String lab= JavaElementLabels.getTextLabel(q, 0);
		assertEqualString(lab, "Q extends ArrayList<? extends Number>");
		lab= JavaElementLabels.getTextLabel(q, JavaElementLabels.ALL_POST_QUALIFIED);
		assertEqualString(lab, "Q extends ArrayList<? extends Number> - org.test.TypeParams");

		lab= JavaElementLabels.getTextLabel(element, 0);
		assertEqualString(lab, "Element extends Map<String, Integer>");
		lab= JavaElementLabels.getTextLabel(element, JavaElementLabels.DEFAULT_POST_QUALIFIED);
		assertEqualString(lab, "Element extends Map<String, Integer> - org.test.TypeParams");

		lab= JavaElementLabels.getTextLabel(nobound, 0);
		assertEqualString(lab, "NoBound");
		lab= JavaElementLabels.getTextLabel(nobound, JavaElementLabels.TP_POST_QUALIFIED);
		assertEqualString(lab, "NoBound - org.test.TypeParams");


		IType al= (IType)cu.codeSelect(content.indexOf("ArrayList"), 0)[0];
		ITypeParameter[] alTypeParameters= al.getTypeParameters();
		ITypeParameter e= alTypeParameters[0];

		lab= JavaElementLabels.getTextLabel(e, 0);
		assertEqualString(lab, "E"); // no " extends java.lang.Object"!
		lab= JavaElementLabels.getTextLabel(e, JavaElementLabels.ALL_POST_QUALIFIED);
		assertEqualString(lab, "E - java.util.ArrayList");
	}

	public void testTypeParameterLabelMethod() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.*;\n");
		buf.append("public class X {\n");
		buf.append("    <Q extends ArrayList<? extends Number>, Element extends Map<String, Integer>, NoBound> Q method(Element e, NoBound n) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", content, false, null);

		IMethod method= (IMethod)cu.getElementAt(content.indexOf("method"));
		ITypeParameter[] typeParameters= method.getTypeParameters();
		ITypeParameter q= typeParameters[0];
		ITypeParameter element= typeParameters[1];
		ITypeParameter nobound= typeParameters[2];

		String lab= JavaElementLabels.getTextLabel(q, 0);
		assertEqualString(lab, "Q extends ArrayList<? extends Number>");
		lab= JavaElementLabels.getTextLabel(q, JavaElementLabels.ALL_POST_QUALIFIED);
		assertEqualString(lab, "Q extends ArrayList<? extends Number> - org.test.X.method(Element, NoBound)");

		lab= JavaElementLabels.getTextLabel(element, 0);
		assertEqualString(lab, "Element extends Map<String, Integer>");
		lab= JavaElementLabels.getTextLabel(element, JavaElementLabels.DEFAULT_POST_QUALIFIED);
		assertEqualString(lab, "Element extends Map<String, Integer> - org.test.X.method(Element, NoBound)");

		lab= JavaElementLabels.getTextLabel(nobound, 0);
		assertEqualString(lab, "NoBound");
		lab= JavaElementLabels.getTextLabel(nobound, JavaElementLabels.TP_POST_QUALIFIED);
		assertEqualString(lab, "NoBound - org.test.X.method(Element, NoBound)");
	}

	public void testTypeLabelAnonymous() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class Outer {\n");
		buf.append("    public void foo(Vector vec) {\n");
		buf.append("        new Object() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Object"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.foo(...).new Object() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.foo(...).new Object() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "new Object() {...} - org.test.Outer.foo(...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(...).new Object() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(...).new Object() {...}");
	}

	public void testTypeLabelAnonymousInAnonymous() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class Outer {\n");
		buf.append("    public void foo(Vector vec) {\n");
		buf.append("        new Object() {\n");
		buf.append("            public void xoo() {\n");
		buf.append("                new Serializable() {\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Serializable"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.foo(...).new Object() {...}.xoo().new Serializable() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.foo(...).new Object() {...}.xoo().new Serializable() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "new Serializable() {...} - org.test.Outer.foo(...).new Object() {...}.xoo()");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(...).new Object() {...}.xoo().new Serializable() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(...).new Object() {...}.xoo().new Serializable() {...}");
	}

	public void testTypeLabelAnonymousInFieldInitializer() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class Outer {\n");
		buf.append("    Object o= new Thread() {\n");
		buf.append("    };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Thread"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.o.new Thread() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.o.new Thread() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "new Thread() {...} - org.test.Outer.o");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.o.new Thread() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.o.new Thread() {...}");
	}

	public void testTypeLabelAnonymousInInitializer() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class Outer {\n");
		buf.append("    static {\n");
		buf.append("        new Object() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Outer.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("Object"));
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.{...}.new Object() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.{...}.new Object() {...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "new Object() {...} - org.test.Outer.{...}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.{...}.new Object() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.{...}.new Object() {...}");
	}

	public void testTypeLabelWildcards() throws Exception {

			IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

			IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package org.test;\n");
			buf.append("public class Wildcards<T> {\n");
			buf.append("	Wildcards<? extends Number> upper;\n");
			buf.append("	Wildcards<? super Number> lower;\n");
			buf.append("	Wildcards<?> wild;\n");
			buf.append("}\n");
			String content= buf.toString();
			ICompilationUnit cu= pack1.createCompilationUnit("Wildcards.java", content, false, null);

			IJavaElement elem= cu.getElementAt(content.indexOf("upper"));
			String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
			assertEqualString(lab, "Wildcards<? extends Number> upper");

			elem= cu.getElementAt(content.indexOf("lower"));
			lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
			assertEqualString(lab, "Wildcards<? super Number> lower");

			elem= cu.getElementAt(content.indexOf("wild"));
			lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
			assertEqualString(lab, "Wildcards<?> wild");

		}

	public void testPackageLabels() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment packDefault= sourceFolder.getPackageFragment("");
		IPackageFragment packOrg= sourceFolder.createPackageFragment("org", false, null);
		IPackageFragment packOrgTest= sourceFolder.createPackageFragment("org.test", false, null);
		IPackageFragment packOrgTestLongname= sourceFolder.createPackageFragment("org.test.longname", false, null);

		assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.ALL_DEFAULT);
		assertExpectedLabel(packOrg, "org", JavaElementLabels.ALL_DEFAULT);
		assertExpectedLabel(packOrgTestLongname, "org.test.longname", JavaElementLabels.ALL_DEFAULT);

		assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
		assertExpectedLabel(packOrg, "org", JavaElementLabels.P_COMPRESSED);
		assertExpectedLabel(packOrgTestLongname, "org.test.longname", JavaElementLabels.P_COMPRESSED);

		assertExpectedLabel(packDefault, "(default package) - TestSetupProject/src", JavaElementLabels.P_POST_QUALIFIED);
		assertExpectedLabel(packOrg, "org - TestSetupProject/src", JavaElementLabels.P_POST_QUALIFIED);
		assertExpectedLabel(packOrgTestLongname, "org.test.longname - TestSetupProject/src", JavaElementLabels.P_POST_QUALIFIED);

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, true);

		try {
			store.setValue(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, "0");

			assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrg, "org", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTest, "test", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTestLongname, "longname", JavaElementLabels.P_COMPRESSED);

			store.setValue(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, ".");

			assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrg, "org", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTest, ".test", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTestLongname, "..longname", JavaElementLabels.P_COMPRESSED);

			store.setValue(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, "1~.");

			assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrg, "org", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTest, "o~.test", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTestLongname, "o~.t~.longname", JavaElementLabels.P_COMPRESSED);

			store.setValue(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW, "2*.");

			assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrg, "org", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTest, "org.test", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTestLongname, "org.te*.longname", JavaElementLabels.P_COMPRESSED);

		} finally {
			store.setToDefault(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW);
			store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
		}
	}

	private void assertExpectedLabel(IJavaElement element, String expectedLabel, long flags) {
		String lab= JavaElementLabels.getTextLabel(element, flags);
		assertEqualString(lab, expectedLabel);
	}

}
