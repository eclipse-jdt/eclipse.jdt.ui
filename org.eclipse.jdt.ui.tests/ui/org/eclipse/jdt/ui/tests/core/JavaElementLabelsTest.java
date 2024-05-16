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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class JavaElementLabelsTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	private static void assertExpectedLabel(IJavaElement element, String expectedLabel, long flags) {
		String lab= JavaElementLabels.getTextLabel(element, flags);
		assertEqualString(lab, expectedLabel);
	}

	@Test
	public void testTypeLabelOuter() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			public class Outer {
			}
			""";
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

	@Test
	public void testTypeLabelInner() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			public class Outer {
			    public void foo(Vector vec) {
			    }
			    public class Inner {
			        public int inner(Vector vec) {
			        }
			    }
			}
			""";
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

	@Test
	public void testTypeLabelLocal() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			public class Outer {
			    public void foo(Vector vec) {
			        public class Local {
			        }
			    }
			}
			""";
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

	@Test
	public void testTypeParameterLabelType() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.*;
			import java.io.Serializable;
			public class TypeParams<Q extends ArrayList<? extends Number>, Element extends Map<String, Integer> & Serializable, NoBound> {
			}
			""";
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
		assertEqualString(lab, "Element extends Map<String, Integer> & Serializable");
		lab= JavaElementLabels.getTextLabel(element, JavaElementLabels.DEFAULT_POST_QUALIFIED);
		assertEqualString(lab, "Element extends Map<String, Integer> & Serializable - org.test.TypeParams");

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


		lab= JavaElementLabels.getTextLabel(typeParams, 0);
		assertEqualString(lab, "TypeParams");
		lab= JavaElementLabels.getTextLabel(typeParams, JavaElementLabels.ALL_DEFAULT);
		assertEqualString(lab, "TypeParams<Q extends ArrayList<? extends Number>, Element extends Map<String, Integer> & Serializable, NoBound>");
		lab= JavaElementLabels.getTextLabel(typeParams, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED);
		assertEqualString(lab, "TypeParams<Q extends ArrayList<? extends Number>, Element extends Map<String, Integer> & Serializable, NoBound> - org.test");
	}

	@Test
	public void testTypeParameterLabelMethod() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.*;
			public class X {
			    <Q extends ArrayList<? extends Number>, Element extends Map<String, Integer>, NoBound> Q method(Element e, NoBound n) {
			        return null;
			    }
			}
			""";
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

	@Test
	public void testTypeLabelAnonymous() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			public class Outer {
			    public void foo(Vector vec) {
			        new Object() {
			        };
			    }
			}
			""";
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

	@Test
	public void testTypeLabelAnonymousInAnonymous() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			public class Outer {
			    public void foo(Vector vec) {
			        new Object() {
			            public void xoo() {
			                new Serializable() {
			                };
			            }
			        };
			    }
			}
			""";
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

	@Test
	public void testTypeLabelAnonymousInFieldInitializer() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			public class Outer {
			    Object o= new Thread() {
			    };
			}
			""";
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

	@Test
	public void testTypeLabelAnonymousInInitializer() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			public class Outer {
			    static {
			        new Object() {
			        };
			    }
			}
			""";
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

	@Test
	public void testTypeLabelWildcards() throws Exception {

			IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

			IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
			String content= """
				package org.test;
				public class Wildcards<T> {
					Wildcards<? extends Number> upper;
					Wildcards<? super Number> lower;
					Wildcards<?> wild;
				}
				""";
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

	@Test
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


			store.setValue(PreferenceConstants.APPEARANCE_ABBREVIATE_PACKAGE_NAMES, true);

			assertExpectedLabel(packOrgTestLongname, "org.te*.longname", JavaElementLabels.P_COMPRESSED);

			store.setValue(PreferenceConstants.APPEARANCE_PKG_NAME_ABBREVIATION_PATTERN_FOR_PKG_VIEW, "#com=@C\norg=@O");

			assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrg, "@O", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTest, "@O.test", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTestLongname, "@O.te*.longname", JavaElementLabels.P_COMPRESSED);

			store.setValue(PreferenceConstants.APPEARANCE_PKG_NAME_ABBREVIATION_PATTERN_FOR_PKG_VIEW, "org=@O\n\norg.test=@OT\n");

			assertExpectedLabel(packDefault, "(default package)", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrg, "@O", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTest, "@OT", JavaElementLabels.P_COMPRESSED);
			assertExpectedLabel(packOrgTestLongname, "@OT.longname", JavaElementLabels.P_COMPRESSED);

		} finally {
			store.setToDefault(PreferenceConstants.APPEARANCE_PKG_NAME_PATTERN_FOR_PKG_VIEW);
			store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
			store.setToDefault(PreferenceConstants.APPEARANCE_PKG_NAME_ABBREVIATION_PATTERN_FOR_PKG_VIEW);
			store.setValue(PreferenceConstants.APPEARANCE_ABBREVIATE_PACKAGE_NAMES, false);
		}
	}

	@Test
	public void testMethodLabelVarargsDeclaration() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			public class Varargs {
			    public void foo(int i, String... varargs) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Varargs.java", content, false, null);

		IJavaElement elem= cu.getElementAt(content.indexOf("foo"));

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertEqualString(lab, "foo(int, String...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "foo(i, varargs)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "foo(int, String...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "foo(int i, String... varargs)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "foo(int, String...)");
	}

	@Test
	public void testMethodLabelVarargsReference0() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Arrays;
			public class Varargs {
			    void foo() {
			        Arrays.asList();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Varargs.java", content, false, null);

		IJavaElement elem= cu.codeSelect(content.indexOf("asList"), 0)[0];

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertEqualString(lab, "asList(T...) <T>");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "asList(arg0)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "asList(T...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "asList(T... arg0)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "asList(Object...) <Object>");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "asList(Object...)");
	}

	@Test
	public void testMethodLabelVarargsReference1() throws Exception {
		assertMethodLabelVarargsReference("1");
	}

	@Test
	public void testMethodLabelVarargsReference2() throws Exception {
		assertMethodLabelVarargsReference("1, 2");
	}

	@Test
	public void testMethodLabelVarargsReference3() throws Exception {
		assertMethodLabelVarargsReference("1, 2, Integer.valueOf(3)");
	}

	private void assertMethodLabelVarargsReference(String args) throws CoreException, JavaModelException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package org.test;\n");
		buf.append("import java.util.Arrays;\n");
		buf.append("public class Varargs {\n");
		buf.append("    void foo() {\n");
		buf.append("        Arrays.asList(" + args + ");\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Varargs.java", content, false, null);

		IJavaElement elem= cu.codeSelect(content.indexOf("asList"), 0)[0];

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertEqualString(lab, "asList(T...) <T>");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "asList(arg0)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "asList(T...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "asList(T... arg0)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "asList(Integer...) <Integer>");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "asList(Integer...)");
	}

	@Test
	public void testMethodLabelAnnotatedParameters() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			
			import java.lang.annotation.Retention;
			import java.lang.annotation.RetentionPolicy;
			
			public class Annotations {
			    void foo(@Outer(a=@Ann("Hello world\\r\\n\\t\\"<'#@%^&")) String param) { }
			   \s
			    void foo2(@Ann(value="", cl=Annotations.class, ints={1, 2, -19},
			            ch='\\0', sh= 0x7FFF, r= @Retention(RetentionPolicy.SOURCE)) String param) { }
			}
			@interface Ann {
			    String value();
			    Class<?> cl() default Ann.class;
			    int[] ints() default {1, 2};
			    char ch() default 'a';
			    short sh() default 1;
			    Retention r() default @Retention(RetentionPolicy.CLASS);
			}
			@interface Outer {
			    Ann a();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Annotations.java", content, false, null);

		IJavaElement foo= cu.getElementAt(content.indexOf("foo"));
		String lab= JavaElementLabels.getTextLabel(foo, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_ANNOTATIONS);
		assertEqualString(lab, "org.test.Annotations.foo(@Outer(a=@Ann(value=\"Hello world\\r\\n\\t\\\"<'#@%^&\")) String)");

		IJavaElement foo2= cu.getElementAt(content.indexOf("foo2"));
		lab= JavaElementLabels.getTextLabel(foo2, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_ANNOTATIONS);
		assertEqualString(lab, "org.test.Annotations.foo2(@Ann(value=\"\", cl=Annotations.class, ints={1, 2, -19}, ch='\\u0000', sh=32767, r=@Retention(value=RetentionPolicy.SOURCE)) String)");
	}
}
