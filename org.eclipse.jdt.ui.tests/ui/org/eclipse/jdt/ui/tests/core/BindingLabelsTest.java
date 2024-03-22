/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
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
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.util.ASTHelper;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLinks;

/**
 * This test suite is copied and adjusted from {@link JavaElementLabelsTest}
 */
public class BindingLabelsTest extends AbstractBindingLabelsTest {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

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
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "Outer - {{org.test}}");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer");
 */
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
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.Outer.Inner");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.Inner");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "Inner - {{org.test.Outer}}");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.Inner - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.Inner");
 */
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
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.Outer}}.{{org.test.Outer|foo}}(...).Local");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertLinkMatch(lab, "{{org.test|Outer}}.{{org.test.Outer|foo}}(...).Local");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "Local - {{org.test.Outer}}.{{foo}}(...)");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(...).Local - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(...).Local");
 */
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

		String lab= getBindingLabel(q, 0);
		assertLinkMatch(lab, "Q extends {{java.util|ArrayList}}<? extends {{java.lang|Number}}>");
		lab= getBindingLabel(q, JavaElementLabels.ALL_POST_QUALIFIED);
		assertLinkMatch(lab, "Q extends {{java.util|ArrayList}}<? extends {{java.lang|Number}}> - {{org.test.TypeParams}}");

		lab= getBindingLabel(element, 0);
		assertLinkMatch(lab, "Element extends {{java.util|Map}}<{{java.lang|String}}, {{java.lang|Integer}}> & {{java.io|Serializable}}");
		lab= getBindingLabel(element, JavaElementLabels.DEFAULT_POST_QUALIFIED);
		assertLinkMatch(lab, "Element extends {{java.util|Map}}<{{java.lang|String}}, {{java.lang|Integer}}> & {{java.io|Serializable}} - {{org.test.TypeParams}}");

		lab= getBindingLabel(nobound, 0);
		assertEqualString(lab, "NoBound");
		lab= getBindingLabel(nobound, JavaElementLabels.TP_POST_QUALIFIED);
		assertLinkMatch(lab, "NoBound - {{org.test.TypeParams}}");


/* cannot select 'E' from cu.
		IType al= (IType)cu.codeSelect(content.indexOf("ArrayList"), 0)[0];
		ITypeParameter[] alTypeParameters= al.getTypeParameters();
		ITypeParameter e= alTypeParameters[0];

		lab= JavaElementLabels.getTextLabel(e, 0);
		assertEqualString(lab, "E"); // no " extends java.lang.Object"!
 */

/*
		lab= JavaElementLabels.getTextLabel(e, JavaElementLabels.ALL_POST_QUALIFIED);
		assertEqualString(lab, "E - java.util.ArrayList");
 */


		lab= getBindingLabel(typeParams, 0);
		assertEqualString(lab, "TypeParams");
		lab= getBindingLabel(typeParams, JavaElementLabels.ALL_DEFAULT);
		assertLinkMatch(lab, "TypeParams<{{org.test.TypeParams|Q}} extends {{java.util|ArrayList}}<? extends {{java.lang|Number}}>, {{org.test.TypeParams|Element}} extends {{java.util|Map}}<{{java.lang|String}}, {{java.lang|Integer}}> & {{java.io|Serializable}}, {{org.test.TypeParams|NoBound}}>");
/*
		lab= JavaElementLabels.getTextLabel(typeParams, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED);
		assertEqualString(lab, "TypeParams<Q extends ArrayList<? extends Number>, Element extends Map<String, Integer> & Serializable, NoBound> - org.test");
 */
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

		String lab= getBindingLabel(q, 0);
		assertLinkMatch(lab, "Q extends {{java.util|ArrayList}}<? extends {{java.lang|Number}}>");
		lab= getBindingLabel(q, JavaElementLabels.ALL_POST_QUALIFIED);
		assertLinkMatch(lab, "Q extends {{java.util|ArrayList}}<? extends {{java.lang|Number}}> - {{org.test.X}}.{{org.test.X|method}}({{org.test.X.method(...)|Element}}, {{org.test.X.method(...)|NoBound}})");

		lab= getBindingLabel(element, 0);
		assertLinkMatch(lab, "Element extends {{java.util|Map}}<{{java.lang|String}}, {{java.lang|Integer}}>");
		lab= getBindingLabel(element, JavaElementLabels.DEFAULT_POST_QUALIFIED);
		assertLinkMatch(lab, "Element extends {{java.util|Map}}<{{java.lang|String}}, {{java.lang|Integer}}> - {{org.test.X}}.{{org.test.X|method}}(Element, {{org.test.X.method(...)|NoBound}})");

		lab= getBindingLabel(nobound, 0);
		assertEqualString(lab, "NoBound");
		lab= getBindingLabel(nobound, JavaElementLabels.TP_POST_QUALIFIED);
		assertLinkMatch(lab, "NoBound - {{org.test.X}}.{{org.test.X|method}}({{org.test.X.method(...)|Element}}, NoBound)");
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
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.Outer}}.{{org.test.Outer|foo}}(...).new Object() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertLinkMatch(lab, "{{org.test|Outer}}.{{org.test.Outer|foo}}(...).new Object() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "new Object() {...} - {{org.test.Outer}}.{{org.test.Outer|foo}}(...)");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(...).new Object() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(...).new Object() {...}");
 */
	}

	@Test
	public void testTypeLabelAnonymousInAnonymous() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.Vector;
			import java.io.Serializable;
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

		IJavaElement elem= cu.getElementAt(content.indexOf("Serializable()"));
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertLinkMatch(lab, """
			{{org.test.Outer}}.{{org.test.Outer|foo}}(...).{{org.test.Outer.foo(...)|new Object() {...}}}.\
			{{org.test.Outer.foo(...).new Object() {...}|xoo}}()\
			.new Serializable() {...}""");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertLinkMatch(lab, "{{org.test|Outer}}.{{org.test.Outer|foo}}(...).{{org.test.Outer.foo(...)|new Object() {...}}}." +
								"{{org.test.Outer.foo(...).new Object() {...}|xoo}}().new Serializable() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, """
			new Serializable() {...} - \
			{{org.test.Outer}}.{{org.test.Outer|foo}}(...).{{org.test.Outer.foo(...)|new Object() {...}}}.\
			{{org.test.Outer.foo(...).new Object() {...}|xoo}}()""");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(...).new Object() {...}.xoo().new Serializable() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(...).new Object() {...}.xoo().new Serializable() {...}");
 */
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
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.Outer}}.{{org.test.Outer|o}}.new Thread() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertLinkMatch(lab, "{{org.test|Outer}}.{{org.test.Outer|o}}.new Thread() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "new Thread() {...} - {{org.test.Outer}}.{{o}}");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.o.new Thread() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.o.new Thread() {...}");
 */
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
		String lab= getBindingLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.Outer}}.{...}.new Object() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertLinkMatch(lab, "{{org.test|Outer}}.{...}.new Object() {...}");

		lab= getBindingLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "new Object() {...} - {{org.test.Outer}}.{...}");

/* *_ROOT_PATH is not relevant for javadoc hover/view
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.{...}.new Object() {...} - TestSetupProject/src");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.{...}.new Object() {...}");
 */
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
			String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
			assertLinkMatch(lab, "{{org.test|Wildcards}}<? extends {{java.lang|Number}}> upper");

			elem= cu.getElementAt(content.indexOf("lower"));
			lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
			assertLinkMatch(lab, "{{org.test|Wildcards}}<? super {{java.lang|Number}}> lower");

			elem= cu.getElementAt(content.indexOf("wild"));
			lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
			assertLinkMatch(lab, "{{org.test|Wildcards}}<?> wild");

		}

	@Test
	public void testPackageLabels() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment packDefault= sourceFolder.getPackageFragment("");
		IPackageFragment packOrg= sourceFolder.createPackageFragment("org", false, null);
//		IPackageFragment packOrgTest= sourceFolder.createPackageFragment("org.test", false, null);
		IPackageFragment packOrgTestLongname= sourceFolder.createPackageFragment("org.test.longname", false, null);

		// to obtain an IPackageBinding go via imported types to their #package:
		packOrg.createCompilationUnit("T1.java", "package org;\npublic class T1 {}\n", false, null);
		packOrgTestLongname.createCompilationUnit("T2.java", "package org.test.longname;\npublic class T2 {}\n", false, null);

		String content= """
			import org.T1;
			import org.test.longname.T2;
			public class Main {
			}
			""";
		ICompilationUnit cu= packDefault.createCompilationUnit("Main.java", content, false, null);

		IJavaElement main= cu.getElementAt(content.indexOf("Main"));
		IJavaElement t1= cu.getElementAt(content.indexOf("T1"));
		IJavaElement t2= cu.getElementAt(content.indexOf("T2"));

		ASTParser parser= ASTParser.newParser(ASTHelper.JLS8);
		parser.setResolveBindings(true);
		parser.setProject(fJProject1);
		IBinding[] bindings= parser.createBindings(new IJavaElement[]{main, t1, t2}, null);

		String lab= JavaElementLinks.getBindingLabel(((ITypeBinding)bindings[0]).getPackage(), main.getAncestor(IJavaElement.PACKAGE_FRAGMENT), JavaElementLabels.ALL_DEFAULT, true);
		assertEqualString(lab, "(default package)");
		lab= JavaElementLinks.getBindingLabel(((ITypeBinding)bindings[1]).getPackage(), t1.getAncestor(IJavaElement.PACKAGE_FRAGMENT), JavaElementLabels.ALL_DEFAULT, true);
		assertLink(lab, "org");
		lab= JavaElementLinks.getBindingLabel(((ITypeBinding)bindings[2]).getPackage(), t2.getAncestor(IJavaElement.PACKAGE_FRAGMENT), JavaElementLabels.ALL_DEFAULT, true);
		assertLink(lab, "org.test.longname");

/* P_COMPRESSED is not relevant for hovers / javadoc view:
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
 */
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

		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertLinkMatch(lab, "foo(int, {{java.lang|String}}...)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "foo(i, varargs)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "foo(int, {{java.lang|String}}...)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "foo(int i, {{java.lang|String}}... varargs)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "foo(int, {{java.lang|String}}...)");
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

		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertLinkMatch(lab, "asList({{java.util.Arrays.asList(...)|T}}...) <{{java.util.Arrays.asList(...)|T}}>");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "asList(arg0)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "asList({{java.util.Arrays.asList(...)|T}}...)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "asList({{java.util.Arrays.asList(...)|T}}... arg0)");

		lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "asList({{java.lang|Object}}...) <{{java.lang|Object}}>");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "asList({{java.lang|Object}}...)");
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

		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertLinkMatch(lab, "asList({{java.util.Arrays.asList(...)|T}}...) <{{java.util.Arrays.asList(...)|T}}>");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "asList(arg0)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "asList({{java.util.Arrays.asList(...)|T}}...)");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "asList({{java.util.Arrays.asList(...)|T}}... arg0)");

		lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "asList({{java.lang|Integer}}...) <{{java.lang|Integer}}>");

		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "asList({{java.lang|Integer}}...)");
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
		String lab= getBindingLabel(foo, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_ANNOTATIONS);
		assertLinkMatch(lab, "{{org.test.Annotations}}.foo(@{{Outer}}({{a}}=@{{Ann}}({{value}}=\"Hello world\\r\\n\\t\\\"<'#@%^&\")) {{String}})");

		IJavaElement foo2= cu.getElementAt(content.indexOf("foo2"));
		lab= getBindingLabel(foo2, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_ANNOTATIONS);
		assertLinkMatch(lab, "{{org.test.Annotations}}.foo2(@{{Ann}}({{value}}=\"\", {{cl}}={{Annotations}}.class, {{ints}}={1, 2, -19}, {{ch}}='\\u0000', {{sh}}=32767, {{r}}=@{{Retention}}({{value}}={{RetentionPolicy}}.{{SOURCE}})) {{String}})");
	}

	// disabled, because we cannot retrieve a binding for the selected element: local variable inside instance initializer
	@Test
	public void testLocalClassInInitializer() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			public class LambdaTests {
			    {
			        class Local implements Function<Integer, String> {
			            @Override
			            public String apply(Integer t) {
			                return t.toString();
			            }
			        }
			
			        Local toStringL = new Local();
			        System.out.println(toStringL.apply(123));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("LambdaTests.java", content, false, null);

		IJavaElement foo= cu.getElementAt(content.indexOf("Local implements"));
		String lab= getBindingLabel(foo, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.LambdaTests}}.{...}.Local");

		IJavaElement foo2= cu.codeSelect(content.indexOf("toStringL"), 9)[0];
		lab= getBindingLabel(foo2, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
		assertLinkMatch(lab, "{{org.test.LambdaTests.{...}|Local}} toStringL - {{org}}.{{test}}.{{LambdaTests}}.{...}");

// can't select the constructor, only the type (label computation works fine once we find the binding)
//		IJavaElement ctor= cu.codeSelect(content.indexOf("Local()"), 0)[0];
//		lab= getBindingLabel(ctor, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
//		assertLinkMatch(lab, "{{org.test.LambdaTests}}.{...}.{{org.test.LambdaTests.{...}.|Local}}.Local()");
	}

	@Test
	public void testLocalClassInStaticInitializer() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			public class LambdaTests {
			    static {
			        class Local implements Function<Integer, String> {
			            @Override
			            public String apply(Integer t) {
			                return t.toString();
			            }
			        }
			
			        Local toStringL = new Local();
			        System.out.println(toStringL.apply(123));
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("LambdaTests.java", content, false, null);

		IJavaElement foo= cu.getElementAt(content.indexOf("Local implements"));
		String lab= getBindingLabel(foo, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.LambdaTests}}.{...}.Local");

		IJavaElement foo2= cu.codeSelect(content.indexOf("toStringL"), 9)[0];
		lab= getBindingLabel(foo2, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
		assertLinkMatch(lab, "{{org.test.LambdaTests.{...}|Local}} toStringL - {{org}}.{{test}}.{{LambdaTests}}.{...}");
	}

	@Test
	public void testRecursiveType() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			public interface TypeTest {
			    public <V extends Comparable<? super V>> boolean compare(V t);
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("TypeTest.java", content, false, null);

		IJavaElement compare= cu.getElementAt(content.indexOf("compare"));
		String lab= getBindingLabel(compare, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.TypeTest}}.compare({{org.test.TypeTest.compare(...)|V}}) <{{org.test.TypeTest.compare(...)|V}} extends {{java.lang|Comparable}}<? super {{org.test.TypeTest.compare(...)|V}}>>");

		IJavaElement v= cu.codeSelect(content.indexOf("V t"), 0)[0];
		lab= getBindingLabel(v, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "V extends {{java.lang|Comparable}}<? super V>");

		lab= getBindingLabel(v, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED);
		assertLinkMatch(lab, "V extends {{java.lang|Comparable}}<? super V> - {{org.test.TypeTest}}.{{org.test.TypeTest|compare}}(V)");
	}

	@Test
	public void testMultipleTypeVariables() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			class Two<B extends Number, A extends B> { }
			public class Three<E extends Number> {
				<F extends E> void foo() { }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Three.java", content, false, null);

		IJavaElement two= cu.getElementAt(content.indexOf("Two"));
		String lab= getBindingLabel(two, JavaElementLabels.ALL_DEFAULT |JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "org.test.Two<{{org.test.Two|B}} extends {{java.lang|Number}}, {{org.test.Two|A}} extends {{org.test.Two|B}}>");

		IJavaElement foo= cu.getElementAt(content.indexOf("foo"));
		lab= getBindingLabel(foo, JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PRE_TYPE_PARAMETERS);
		assertLinkMatch(lab, "<{{org.test.Three.foo(...)|F}} extends {{org.test.Three|E}}> {{org.test.Three}}.foo()");
	}
}
