/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     John Kaplan, johnkaplantech@gmail.com - 108071 [code templates] template for body of newly created class
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.wizardapi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.wizards.NewAnnotationWizardPage;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.jdt.ui.wizards.NewEnumWizardPage;
import org.eclipse.jdt.ui.wizards.NewInterfaceWizardPage;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 *
 */
public class NewTypeWizardTest extends TestCase {
	private static final Class THIS= NewTypeWizardTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public NewTypeWizardTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, true);

		String newFileTemplate= "${filecomment}\n${package_declaration}\n\n${typecomment}\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/**\n * Type\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.FILECOMMENT_ID, "/**\n * File\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/**\n * Overridden\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CLASSBODY_ID, "/* class body */\n", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.INTERFACEBODY_ID, "/* interface body */\n", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.ENUMBODY_ID, "/* enum body */\n", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.ANNOTATIONBODY_ID, "/* annotation body */\n", null);


		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testCreateClass1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("    /* class body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public void testCreateClass2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("java.util.ArrayList<String>", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public class E extends ArrayList<String> {\n");
		buf.append("    /* class body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}


	public void testCreateClass3() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A<T> {\n");
		buf.append("    public abstract void foo(T t);\n");
		buf.append("}\n");
		pack0.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("pack.A<String>", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, true, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import pack.A;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public class E extends A<String> {\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * Overridden\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(String t) {\n");
		buf.append("    }\n");
		buf.append("    /* class body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

	}

	public void testCreateClass4() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A<T> {\n");
		buf.append("    public A(T t);\n");
		buf.append("}\n");
		pack0.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("pack.A<String>", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(true, true, true, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import pack.A;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public class E extends A<String> {\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * Constructor\n");
		buf.append("     */\n");
		buf.append("    public E(String t) {\n");
		buf.append("        super(t);\n");
		buf.append("    }\n");
		buf.append("    /* class body */\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * Method\n");
		buf.append("     */\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

	}


	public void testCreateInnerClass1() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A<T> {\n");
		buf.append("    public abstract void foo(T t);\n");
		buf.append("}\n");
		ICompilationUnit outer= pack0.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(true, true);
		wizardPage.setEnclosingType(outer.findPrimaryType(), true);
		wizardPage.setTypeName("E<S>", true);

		wizardPage.setSuperClass("java.util.ArrayList<S>", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, true, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class A<T> {\n");
		buf.append("    /**\n");
		buf.append("     * Type\n");
		buf.append("     */\n");
		buf.append("    public class E<S> extends ArrayList<S> {\n");
		buf.append("        /* class body */\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public abstract void foo(T t);\n");
		buf.append("}\n");

		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}



	public void testCreateClassExtraImports1() throws Exception {

		String newFileTemplate= "${filecomment}\n${package_declaration}\n\nimport java.util.Map;\n\n${typecomment}\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("", true);

		List interfaces= new ArrayList();
		interfaces.add("java.util.List<java.io.File>");
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public class E implements List<File> {\n");
		buf.append("    /* class body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);

	}

	public void testCreateClassExtraImports2() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A {\n");
		buf.append("    public static class Inner {\n");
		buf.append("    }\n");
		buf.append("    public abstract void foo(Inner inner);\n");
		buf.append("}\n");
		pack0.createCompilationUnit("A.java", buf.toString(), false, null);


		String newFileTemplate= "${filecomment}\n${package_declaration}\n\nimport java.util.Map;\n\n${typecomment}\n${type_declaration}";
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, newFileTemplate, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("pack.A", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, true, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import pack.A;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public class E extends A {\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * Overridden\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(Inner inner) {\n");
		buf.append("    }\n");
		buf.append("    /* class body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public void testCreateClassExtraImports3() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A {\n");
		buf.append("    public static class Inner {\n");
		buf.append("    }\n");
		buf.append("    public abstract void foo(Inner inner);\n");
		buf.append("}\n");
		pack0.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Map;\n"); // an unused import: should not be touched
		buf.append("\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		ICompilationUnit outer= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		NewClassWizardPage wizardPage= new NewClassWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setEnclosingTypeSelection(true, true);
		wizardPage.setEnclosingType(outer.findPrimaryType(), true);
		wizardPage.setTypeName("E", true);

		wizardPage.setSuperClass("pack.A", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setMethodStubSelection(false, false, true, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Map;\n");
		buf.append("\n");
		buf.append("import pack.A;\n");
		buf.append("\n");
		buf.append("public class B {\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * Type\n");
		buf.append("     */\n");
		buf.append("    public class E extends A {\n");
		buf.append("\n");
		buf.append("        /**\n");
		buf.append("         * Overridden\n");
		buf.append("         */\n");
		buf.append("        @Override\n");
		buf.append("        public void foo(Inner inner) {\n");
		buf.append("        }\n");
		buf.append("        /* class body */\n");
		buf.append("    }\n");
		buf.append("}\n");

		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}


	public void testCreateInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewInterfaceWizardPage wizardPage= new NewInterfaceWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setTypeName("E", true);

		List interfaces= new ArrayList();
		interfaces.add("java.util.List<String>");
		interfaces.add("java.lang.Runnable");
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public interface E extends List<String>, Runnable {\n");
		buf.append("    /* interface body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public void testCreateEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewEnumWizardPage wizardPage= new NewEnumWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setTypeName("E", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public enum E {\n");
		buf.append("    /* enum body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public void testCreateAnnotation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		NewAnnotationWizardPage wizardPage= new NewAnnotationWizardPage();
		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack1, true);
		wizardPage.setTypeName("E", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public @interface E {\n");
		buf.append("    /* annotation body */\n");
		buf.append("}\n");
		String expected= buf.toString();

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public void typeBodyTest( NewTypeWizardPage wizardPage, String templateID, String templateBody, String expectedBody,
	    String packageName, String typeName, String typeKeyword) throws Exception {
		StubUtility.setCodeTemplate(templateID, templateBody, null);

		IPackageFragment pack= fSourceFolder.createPackageFragment(packageName, false, null);

		wizardPage.setPackageFragmentRoot(fSourceFolder, true);
		wizardPage.setPackageFragment(pack, true);
		wizardPage.setEnclosingTypeSelection(false, true);
		wizardPage.setTypeName(typeName, true);

		wizardPage.setSuperClass("", true);

		List interfaces= new ArrayList();
		wizardPage.setSuperInterfaces(interfaces, true);

		//wizardPage.setMethodStubSelection(false, false, false, true);
		wizardPage.setAddComments(true, true);
		wizardPage.enableCommentControl(true);

		wizardPage.createType(null);

		String actual= wizardPage.getCreatedType().getCompilationUnit().getSource();

		StringBuffer buf= new StringBuffer();
		buf.append("/**\n");
		buf.append(" * File\n");
		buf.append(" */\n");
		buf.append("package ");
		buf.append(packageName);
		buf.append(";\n");
		buf.append("\n");
		buf.append("/**\n");
		buf.append(" * Type\n");
		buf.append(" */\n");
		buf.append("public ");
		buf.append(typeKeyword);
		buf.append( " ");
		buf.append(typeName);
		buf.append(" {\n");
		buf.append(expectedBody);
		buf.append("}\n");
		String expected= buf.toString();

		// one carriage return is the default for all body templates
		// ..resetting before any asserts are thrown
		StubUtility.setCodeTemplate(templateID, "\n", null);

		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public void testCreateClassWithBody() throws Exception
	{
		typeBodyTest( new NewClassWizardPage(),
			CodeTemplateContextType.CLASSBODY_ID,
			"    // test comment\n    String testMember = \"${type_name}\"\n",
			"    // test comment\n    String testMember = \"TestClassBodyType\"\n",
			"testclassbodypackage",
			"TestClassBodyType",
			"class" );
	}

	public void testCreateInterfaceWithBody() throws Exception
	{
		typeBodyTest( new NewInterfaceWizardPage(),
			CodeTemplateContextType.INTERFACEBODY_ID,
			"\n    // public methods for ${type_name}\n",
			"\n    // public methods for TestInterfaceBodyType\n",
			"testinterfacebodypackage",
			"TestInterfaceBodyType",
			"interface" );
	}

	public void testCreateEnumWithBody() throws Exception
	{
		typeBodyTest( new NewEnumWizardPage(),
			CodeTemplateContextType.ENUMBODY_ID,
			"\n    // enumeration constants\n    // public methods\n",
			"\n    // enumeration constants\n    // public methods\n",
			"enumbodypackage",
			"EnumBodyType",
			"enum" );
	}

	public void testCreateAnnotationWithBody() throws Exception
	{
		typeBodyTest( new NewAnnotationWizardPage(),
			CodeTemplateContextType.ANNOTATIONBODY_ID,
			"\n    @SomeOtherSpecialAnnotation ${package_name}_${type_name}\n",
			"\n    @SomeOtherSpecialAnnotation annotationbodypackage_AnnotationBodyType\n",
			"annotationbodypackage",
			"AnnotationBodyType",
			"@interface" );
	}
}
