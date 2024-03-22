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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedConstructorsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;

public class AddUnimplementedConstructorsTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IType fClassA, fClassB, fClassC;

	private IJavaProject fJavaProject;

	private IPackageFragment fPackage;

	private CodeGenerationSettings fSettings;

	private void checkDefaultConstructorWithCommentWithSuper(String con) throws IOException {
		String constructor= """
			/** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			""";
		compareSource(constructor, con);
	}

	private void checkMethods(String[] expected, IMethod[] methods) {
		int nMethods= methods.length;
		int nExpected= expected.length;
		assertEquals("" + nExpected + " methods expected, is " + nMethods, nExpected, nMethods);
		for (int i= 0; i < nExpected; i++) {
			String methName= expected[i];
			assertTrue("method " + methName + " expected", nameContained(methName, methods));
		}
	}

	private void compareSource(String expected, String actual) throws IOException {
		assertEqualStringIgnoreDelim(actual, expected);
	}

	private AddUnimplementedConstructorsOperation createOperation(IType type) throws CoreException {
		return createOperation(type, -1);
	}

	private AddUnimplementedConstructorsOperation createOperation(IType type, int insertPos) throws CoreException {
		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);
		AbstractTypeDeclaration declaration= ASTNodes.getParent(NodeFinder.perform(unit, type.getNameRange()), AbstractTypeDeclaration.class);
		assertNotNull("Could not find type declararation node", declaration);
		ITypeBinding binding= declaration.resolveBinding();
		assertNotNull("Binding for type declaration could not be resolved", binding);

		return new AddUnimplementedConstructorsOperation(unit, binding, null, insertPos, true, true, true, FormatterProfileManager.getProjectSettings(type.getJavaProject()));
	}

	private void initCodeTemplates() {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		JavaCore.setOptions(options);

		String str= """
			/** Constructor Comment
			 * ${tags}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, str, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "${body_statement}\n// TODO", null);
		fSettings= JavaPreferencesSettings.getCodeGenerationSettings((IJavaProject)null);
		fSettings.createComments= true;
	}

	private boolean nameContained(String methName, IJavaElement[] methods) {
		for (IJavaElement method : methods) {
			if (method.getElementName().equals(methName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a new test Java project.
	 */
	@Before
	public void setUp() {
		initCodeTemplates();
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
		fPackage= null;
		fClassA= null;
	}

	/*
	 * basic test: test with default constructor only
	 */
	@Test
	public void testDefaultConstructorToOverride() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);

		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);

		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1"}, createdMethods); //$NON-NLS-1$

		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());

		String str= """
			public class Test1 extends A {
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test with 8 constructors to override
	 */
	@Test
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

		CodeGenerationSettings settings= new CodeGenerationSettings();
		settings.createComments= false;
		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1", "Test1", "Test1", "Test1", "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());

		String str= """
			public class Test1 extends A {
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     */
			    public Test1(int a) {
			        super(a);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     */
			    public Test1(int a, boolean boo, String fooString) {
			        super(a, boo, fooString);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {
			        super(a, boo, fooString, buf);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2) {
			        super(a, boo, fooString, buf, a2);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c) {
			        super(a, boo, fooString, buf, a2, c);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d) {
			        super(a, boo, fooString, buf, a2, c, d);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     * @param secondString
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d, String secondString) {
			        super(a, boo, fooString, buf, a2, c, d, secondString);
			        // TODO
			    }
			}""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test with 5 constructors to override. Class C extends B extends Class
	 * A.
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1", "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		String str= """
			public class Test1 extends C {
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2) {
			        super(a, boo, fooString, buf, a2);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c) {
			        super(a, boo, fooString, buf, a2, c);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d) {
			        super(a, boo, fooString, buf, a2, c, d);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     * @param secondString
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d, String secondString) {
			        super(a, boo, fooString, buf, a2, c, d, secondString);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     * @param secondString
			     * @param xxx
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d, String secondString, int xxx) {
			        super(a, boo, fooString, buf, a2, c, d, secondString, xxx);
			        // TODO
			    }
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test with 4 constructors to override. Class B extends Class A.
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		String str= """
			public class Test1 extends B {
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2) {
			        super(a, boo, fooString, buf, a2);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c) {
			        super(a, boo, fooString, buf, a2, c);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d) {
			        super(a, boo, fooString, buf, a2, c, d);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     * @param a2
			     * @param c
			     * @param d
			     * @param secondString
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf, char a2, double c, float d, String secondString) {
			        super(a, boo, fooString, buf, a2, c, d, secondString);
			        // TODO
			    }
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * found 4 with 5 constructors, 1 already overridden
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1", "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		String str= """
			public class Test1 extends A {
			
			    public Test1(int a, boolean boo, String fooString) {super();}
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     */
			    public Test1(int a) {
			        super(a);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param bologna
			     */
			    public Test1(int a, boolean boo, String fooString, int bologna) {
			        super(a, boo, fooString, bologna);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {
			        super(a, boo, fooString, buf);
			        // TODO
			    }
			}""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test with nothing to override
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] existingMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1"}, existingMethods); //$NON-NLS-1$

		String str= """
			public class Test1 extends A {
			
			    public Test1(){}
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test an Interface to make sure no exception is thrown
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] existingMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1"}, existingMethods); //$NON-NLS-1$

		String str= """
			public class Test1 implements A {
			
			    public Test1(){}
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * nothing found with default constructor
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1"}, createdMethods); //$NON-NLS-1$

		String str= """
			public class Test1 extends A {
			
			    public Test1() {
			    super();}
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * nothing found with 3 constructors
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(true);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] existingMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1"}, existingMethods); //$NON-NLS-1$ //$NON-NLS-2$

		String str= """
			public class Test1 extends A {
			
			    public Test1() {
			    super();}
			
			    public Test1(int a) {super();}
			
			    public Test1(int a, boolean boo, String fooString) {super();}
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test with one constructor
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1"}, createdMethods); //$NON-NLS-1$

		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());

		String str= """
			public class Test1 extends A {
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * found one with constructor which isn't default or the same as existing
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();

		checkMethods(new String[] { "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		String str= """
			public class Test1 extends A {
			
			    public Test1(int a, boolean boo, String fooString) {super();}
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param buf
			     */
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {
			        super(a, boo, fooString, buf);
			        // TODO
			    }
			}""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * found one with constructor needs import statement
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		String fullSource= testClass.getCompilationUnit().getSource();

		checkMethods(new String[] { "Test1"}, createdMethods); //$NON-NLS-1$

		String str= """
			package ibm.util.bogus;
			
			import java.util.Vector;
			
			public class Test1 extends A {
			
			    /** Constructor Comment
			     * @param v
			     * @param a
			     */
			    public Test1(Vector v, int a) {
			        super(v, a);
			        // TODO
			    }
			}
			
			""";
		compareSource(str, fullSource);
	}

	/*
	 * basic test: test with 3 constructors to override
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());

		String str= """
			public class Test1 extends A {
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     */
			    public Test1(int a) {
			        super(a);
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     */
			    public Test1(int a, boolean boo) {
			        super(a, boo);
			        // TODO
			    }
			}""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * basic test: test with 2 constructors to override
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		checkDefaultConstructorWithCommentWithSuper(createdMethods[0].getSource());

		String str= """
			public class Test1 extends A {
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     */
			    public Test1(int a) {
			        super(a);
			        // TODO
			    }
			}
			""";
		compareSource(str, testClass.getSource());
	}

	/*
	 * found 2 with 5 constructors, 3 already overridden
	 */
	@Test
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

		AddUnimplementedConstructorsOperation op= createOperation(testClass);

		op.setOmitSuper(false);
		op.setVisibility(Modifier.PUBLIC);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(testClass.getCompilationUnit());

		IMethod[] createdMethods= testClass.getMethods();
		checkMethods(new String[] { "Test1", "Test1", "Test1", "Test1", "Test1"}, createdMethods); //$NON-NLS-1$ //$NON-NLS-2$

		checkDefaultConstructorWithCommentWithSuper(createdMethods[3].getSource());

		String str= """
			public class Test1 extends A {
			
			    public Test1(int a, boolean boo, String fooString, StringBuffer buf) {
			    super();}
			
			    public Test1(int a) {super();}
			
			    public Test1(int a, boolean boo, String fooString) {super();}
			
			    /** Constructor Comment
			     *\s
			     */
			    public Test1() {
			        super();
			        // TODO
			    }
			
			    /** Constructor Comment
			     * @param a
			     * @param boo
			     * @param fooString
			     * @param bologna
			     */
			    public Test1(int a, boolean boo, String fooString, int bologna) {
			        super(a, boo, fooString, bologna);
			        // TODO
			    }
			}
			""";
		compareSource(str, testClass.getSource());
	}

	@Test
	public void testInsertAt() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("p", true, null);

		String str= """
			package p;
			
			public class B  {
				public B(int x) {
				}
			}""";
		fPackage.createCompilationUnit("B.java", str, true, null);


		String originalContent= """
			package p;
			
			public class A extends B {
			    int x;
			
			    A() {
			        super(1);
			    }
			
			    void foo() {
			    }
			
			    {
			    }
			
			    static {
			    }
			
			    class Inner {
			    }
			}""";

		final int NUM_MEMBERS= 6;

		String expectedConstructor= """
			public A(int x) {
			        super(x);
			        // TODO
			    }""";

		// try to insert the new constructor after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackage.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				assertEquals(NUM_MEMBERS, children.length);

				int insertIndex= i < NUM_MEMBERS ? ((IMember) children[i]).getSourceRange().getOffset() : -1;

				AddUnimplementedConstructorsOperation op= createOperation(type, insertIndex);
				op.setCreateComments(false);
				op.setOmitSuper(false);
				op.setVisibility(Modifier.PUBLIC);
				op.run(new NullProgressMonitor());
				JavaModelUtil.reconcile(type.getCompilationUnit());

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
