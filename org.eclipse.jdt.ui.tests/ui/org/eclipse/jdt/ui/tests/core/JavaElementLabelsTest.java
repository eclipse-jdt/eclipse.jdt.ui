/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

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
		assertEqualString(lab, "org.test.Outer.foo(..).Local");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.foo(..).Local");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "Local - org.test.Outer.foo(..)");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(..).Local - TestSetupProject/src");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(..).Local");
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
		assertEqualString(lab, "org.test.Outer.foo(..).new Object() {..}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.foo(..).new Object() {..}");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "new Object() {..} - org.test.Outer.foo(..)");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(..).new Object() {..} - TestSetupProject/src");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(..).new Object() {..}");
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
		assertEqualString(lab, "org.test.Outer.foo(..).new Object() {..}.xoo().new Serializable() {..}");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_CONTAINER_QUALIFIED);
		assertEqualString(lab, "Outer.foo(..).new Object() {..}.xoo().new Serializable() {..}");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "new Serializable() {..} - org.test.Outer.foo(..).new Object() {..}.xoo()");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.APPEND_ROOT_PATH);
		assertEqualString(lab, "org.test.Outer.foo(..).new Object() {..}.xoo().new Serializable() {..} - TestSetupProject/src");
		
		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.PREPEND_ROOT_PATH);
		assertEqualString(lab, "TestSetupProject/src - org.test.Outer.foo(..).new Object() {..}.xoo().new Serializable() {..}");
	}
			
	
		
}
