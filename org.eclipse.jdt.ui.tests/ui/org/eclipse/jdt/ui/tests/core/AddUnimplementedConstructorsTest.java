/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.io.IOException;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;


public class AddUnimplementedConstructorsTest extends CoreTests {

	private static final Class THIS= AddUnimplementedConstructorsTest.class;
	
	private IJavaProject fJavaProject;
	private IPackageFragment fPackage;
	private IType fClassA, fClassB, fClassC;
	private boolean fOldFormatter;

	private CodeGenerationSettings fSettings;

	public AddUnimplementedConstructorsTest(String name) {
		super(name);
		fOldFormatter= false;
	}
	
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new AddUnimplementedConstructorsTest("testOneConstructorWithImportStatement"));
			return new ProjectTestSetup(suite);
		}	
	}

	/**
	 * Creates a new test Java project.
	 */	
	protected void setUp() throws Exception {
		initCodeTemplates();		
	}	
	
	private void initCodeTemplates() {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		JavaCore.setOptions(options);		

		StringBuffer comment= new StringBuffer();
		comment.append("/** Constructor Comment\n");
		comment.append(" * ${tags}\n");
		comment.append(" */");
		
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT).setPattern(comment.toString());
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.CONSTRUCTORSTUB).setPattern("${body_statement}\n// TODO");	
		fSettings= JavaPreferencesSettings.getCodeGenerationSettings(null);
		fSettings.createComments= true;
	}

	/**
	 * Removes the test java project.
	 */
	protected void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
		fPackage= null;
		fClassA= null;
	}
	
	/*
	 * basic test: test with one constructor
	 */
	public void testOneConstructorToOverride() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.createCompilationUnit("A.java", "package ibm.util;\n\n", true, null);
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java"); //$NON-NLS-1$
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null); //$NON-NLS-1$
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A" }, constructorMethods); //$NON-NLS-1$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("        super();\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");
		compareSource(buf.toString(), testClass.getSource());
	}
	
	/*
	 * basic test: test with 2 constructors to override
	 */
	public void testTwoConstructorsToOverride() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A", "A" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("        super();\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a) {\n");
		buf.append("        super(a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}
	
	/*
	 * basic test: test with 3 constructors to override
	 */
	public void testThreeConstructorsToOverride() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo) {super();}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A", "A", "A" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		checkDefaultConstructorWithCommentNoSuper(createdMethods[0].getSource());

		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a) {\n");
		buf.append("        super(a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo) {\n");
		buf.append("        super(a, boo);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}
	
	/*
	 * basic test: test with default constructor only
	 */
	public void testDefaultConstructorToOverride() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);	
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "Object" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
				
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("        super();\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}	
	
	/*
	 * basic test: test with nothing to override
	 */
	public void testNoConstructorsToOverrideAvailable() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1(){}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] existingMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1"}, existingMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("public Test1(){}\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}	
	
	/*
	 * basic test: test an Interface to make sure no exception is thrown
	 */
	public void testNoConstructorsToOverrideWithInterface() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public interface A {\n}\n", null, true, null);		
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 implements A {\n}\n", null, true, null);
		testClass.createMethod("public Test1(){}\n", null, true, null);
		
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] existingMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1"}, existingMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 implements A {\n");
		buf.append("public Test1(){}\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}	
	
	/*
	 * basic test: test with 8 constructors to override
	 */
	public void testEightConstructorsToOverride() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf, char a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {super();}\n", null, true, null);
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A", "A", "A", "A", "A", "A", "A", "A" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		fSettings.createComments= false;
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1", "Test1", "Test1", "Test1", "Test1", "Test1", "Test1", "Test1"}, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		checkDefaultConstructorNoCommentNoSuper(createdMethods[0].getSource());	
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("    public Test1() {\n");
		buf.append("\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a) {\n");
		buf.append("        super(a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a, boolean boo, String fooString) {\n");
		buf.append("        super(a, boo, fooString);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {\n");
		buf.append("        super(a, boo, fooString, buf);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a) {\n");
		buf.append("        super(a, boo, fooString, buf, a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d, secondString);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}
	
	/*
	 * basic test: test with 4 constructors to override. Class B extends Class A.
	 */
	public void testFourConstructorsToOverride() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf) {super();}\n", null, true, null);

		ICompilationUnit cuB= fPackage.getCompilationUnit("B.java");
		fClassB= cuB.createType("public class B extends A{\n}\n", null, true, null);		
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a) {super();}\n", null, true, null);
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {super();}\n", null, true, null);
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {super();}\n", null, true, null);
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {super();}\n", null, true, null);

		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends B {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "B", "B", "B", "B" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1", "Test1", "Test1", "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends B {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a) {\n");
		buf.append("        super(a, boo, fooString, buf, a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     * @param d\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     * @param d\n");
		buf.append("     * @param secondString\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d, secondString);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");
		compareSource(buf.toString(), testClass.getSource());
	}
	
	/*
	 * basic test: test with 5 constructors to override. Class C extends B extends Class A.
	 */
	public void testFiveConstructorsToOverrideWithTwoLevelsOfInheritance() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf) {super();}\n", null, true, null);

		ICompilationUnit cuB= fPackage.getCompilationUnit("B.java");
		fClassB= cuB.createType("public class B extends A{\n}\n", null, true, null);		
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a) {super();}\n", null, true, null);
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {super();}\n", null, true, null);
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {super();}\n", null, true, null);
		fClassB.createMethod("public B(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {super();}\n", null, true, null);

		ICompilationUnit cuC= fPackage.getCompilationUnit("C.java");
		fClassC= cuC.createType("public class C extends B{\n}\n", null, true, null);		
		fClassC.createMethod("public C(int a, boolean boo, String fooString, StringBuffer buf, char a) {super();}\n", null, true, null);
		fClassC.createMethod("public C(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {super();}\n", null, true, null);
		fClassC.createMethod("public C(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {super();}\n", null, true, null);
		fClassC.createMethod("public C(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {super();}\n", null, true, null);
		fClassC.createMethod("public C(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString, int xxx) {super();}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends C {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "C", "C", "C", "C", "C" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1", "Test1", "Test1", "Test1", "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends C {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a) {\n");
		buf.append("        super(a, boo, fooString, buf, a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     * @param d\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     * @param d\n");
		buf.append("     * @param secondString\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d, secondString);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     * @param a\n");
		buf.append("     * @param c\n");
		buf.append("     * @param d\n");
		buf.append("     * @param secondString\n");
		buf.append("     * @param xxx\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a, double c, float d, String secondString, int xxx) {\n");
		buf.append("        super(a, boo, fooString, buf, a, c, d, secondString, xxx);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}

	/*
	 * nothing found with default constructor
	 */
	public void testNoConstructorsToOverrideWithOneExistingConstructors() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
	
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1() {\nsuper();}\n", null, true, null);

		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1"}, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("public Test1() {\n");
		buf.append("super();}\n");
		buf.append("}\n");

		
		compareSource(buf.toString(), testClass.getSource());
	}


	/*
	 * nothing found with 3 constructors
	 */
	public void testNoConstructorsToOverrideWithThreeExistingConstructors() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1() {\nsuper();}\n", null, true, null);
		testClass.createMethod("public Test1(int a) {super();}\n", null, true, null);
		testClass.createMethod("public Test1(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] existingMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1", "Test1", "Test1"}, existingMethods);	 //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("public Test1() {\n");
		buf.append("super();}\n");
		buf.append("public Test1(int a) {super();}\n");
		buf.append("public Test1(int a, boolean boo, String fooString) {super();}\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}

	/*
	 * found 2 with 5 constructors, 3 already overriden
	 */
	public void testTwoConstructorsToOverrideWithThreeExistingConstructors() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, int bologna) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf) {super();}\n", null, true, null);

		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1(int a, boolean boo, String fooString, StringBuffer buf) {\nsuper();}\n", null, true, null);
		testClass.createMethod("public Test1(int a) {super();}\n", null, true, null);
		testClass.createMethod("public Test1(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {"A", "A"}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		fSettings.createComments= false;
		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1", "Test1", "Test1", "Test1", "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$

		checkDefaultConstructorNoCommentWithSuper(createdMethods[3].getSource());
		
		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("public Test1(int a, boolean boo, String fooString, StringBuffer buf) {\n");
		buf.append("super();}\n");
		buf.append("public Test1(int a) {super();}\n");
		buf.append("public Test1(int a, boolean boo, String fooString) {super();}\n");
		buf.append("    public Test1() {\n");
		buf.append("        super();\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, int bologna) {\n");
		buf.append("        super(a, boo, fooString, bologna);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");



		compareSource(buf.toString(), testClass.getSource());
	}

	/*
	 * found 4 with 5 constructors, 1 already overriden
	 */
	public void testFourConstructorsToOverrideWithOneExistingConstructor() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, int bologna) {super();}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf) {super();}\n", null, true, null);

		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {"A", "A", "A", "A"}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] {"Test1", "Test1", "Test1", "Test1", "Test1" }, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("public Test1(int a, boolean boo, String fooString) {super();}\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a) {\n");
		buf.append("        super(a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param bologna\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, int bologna) {\n");
		buf.append("        super(a, boo, fooString, bologna);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {\n");
		buf.append("        super(a, boo, fooString, buf);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");

		compareSource(buf.toString(), testClass.getSource());
	}

	/*
	 * found one with constructor which isn't default or the same as existing
	 */
	public void testOneConstructorToOverrideNotDefault() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A(int a, boolean boo, String fooString, StringBuffer buf) {super();}\n", null, true, null);

		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1(int a, boolean boo, String fooString) {super();}\n", null, true, null);
		
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {"A"}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		
		checkMethods(new String[] {"Test1", "Test1"}, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer buf= new StringBuffer();
		buf.append("public class Test1 extends A {\n");
		buf.append("public Test1(int a, boolean boo, String fooString) {super();}\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param a\n");
		buf.append("     * @param boo\n");
		buf.append("     * @param fooString\n");
		buf.append("     * @param buf\n");
		buf.append("     */\n");
		buf.append("    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {\n");
		buf.append("        super(a, boo, fooString, buf);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");


		compareSource(buf.toString(), testClass.getSource());
	}

	/*
	 * found one with constructor needs import statement
	 */
	public void testOneConstructorWithImportStatement() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util.bogus", true, null);
		
		ICompilationUnit cuA= fPackage.getCompilationUnit("A.java");
		fClassA= cuA.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A(java.util.Vector v, int a) {super();}\n", null, true, null);

		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {"A"}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
		
		AddUnimplementedConstructorsOperation op= new AddUnimplementedConstructorsOperation(testClass, fSettings, constructorMethods, true, null);
		
		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		
		IMethod[] createdMethods= testClass.getMethods();
		String fullSource= testClass.getCompilationUnit().getSource();
		
		checkMethods(new String[] {"Test1"}, createdMethods);	 //$NON-NLS-1$ //$NON-NLS-2$

		StringBuffer buf= new StringBuffer();
		buf.append("package ibm.util.bogus;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class Test1 extends A {\n");
		buf.append("    /** Constructor Comment\n");
		buf.append("     * @param v\n");
		buf.append("     * @param a\n");
		buf.append("     */\n");
		buf.append("    public Test1(Vector v, int a) {\n");
		buf.append("        super(v, a);\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		if (fOldFormatter) buf.append("\n");
		buf.append("}\n");


		compareSource(buf.toString(), fullSource);
	}
	
	private void checkMethods(String[] expected, IMethod[] methods) {
		int nMethods= methods.length;
		int nExpected= expected.length;
		assertTrue("" + nExpected + " methods expected, is " + nMethods, nMethods == nExpected);
		for (int i= 0; i < nExpected; i++) {
			String methName= expected[i];
			assertTrue("method " + methName + " expected", nameContained(methName, methods));
		}
	}			
		
	private boolean nameContained(String methName, IJavaElement[] methods) {
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].getElementName().equals(methName)) {
				return true;
			}
		}
		return false;
	}			
	
	private void checkDefaultConstructorNoCommentNoSuper(String con) throws IOException {
		StringBuffer buf= new StringBuffer();
		buf.append("public Test1() {\n");
		buf.append("\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		compareSource(buf.toString(), con);
	}
	
	private void checkDefaultConstructorWithCommentNoSuper(String con) throws IOException {
		StringBuffer buf= new StringBuffer();
		buf.append("/** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		compareSource(buf.toString(), con);
	}
	
	private void checkDefaultConstructorNoCommentWithSuper(String con) throws IOException {
		StringBuffer buf= new StringBuffer();
		buf.append("public Test1() {\n");
		buf.append("        super();\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		String constructor= buf.toString();
		compareSource(constructor, con);
	}
	
	private void checkDefaultConstructorWithCommentWithSuper(String con) throws IOException {
		StringBuffer buf= new StringBuffer();
		buf.append("/** Constructor Comment\n");
		buf.append("     * \n");
		buf.append("     */\n");
		buf.append("    public Test1() {\n");
		buf.append("        super();\n");
		buf.append("        // TODO\n");
		buf.append("    }\n");
		String constructor= buf.toString();
		compareSource(constructor, con);
	}

	private void compareSource(String expected, String actual) throws IOException {
		assertEqualStringIgnoreDelim(actual, expected);
	}
	/*
	private String writeSource(String str) {
		StringBuffer buf= new StringBuffer();
		buf.append("\t\tStringBuffer buf= new StringBuffer();\n");
		
		String[] lines= Strings.convertIntoLines(str);
		for (int i= 0; i < lines.length; i++) {
			buf.append("\t\tbuf.append(\"").append(lines[i]).append("\\n\");\n");
		}
		return buf.toString();
	}*/
	

}
