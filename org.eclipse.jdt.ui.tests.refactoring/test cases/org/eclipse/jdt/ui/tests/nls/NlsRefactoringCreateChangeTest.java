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
package org.eclipse.jdt.ui.tests.nls;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.ltk.core.refactoring.Change;

public class NlsRefactoringCreateChangeTest extends TestCase {

	private NlsRefactoringTestHelper fHelper;
	private IJavaProject fJavaProject;

	public NlsRefactoringCreateChangeTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(NlsRefactoringCreateChangeTest.class));
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		fJavaProject= ProjectTestSetup.getProject();
		fHelper= new NlsRefactoringTestHelper(fJavaProject);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJavaProject, ProjectTestSetup.getDefaultClasspath());
	}

	public void testWithoutPreviousNlsing() throws Exception {

		fHelper.createPackageFragment("p2", "/TestSetupProject/src2"); //$NON-NLS-1$//$NON-NLS-2$

		ICompilationUnit cu= RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$//$NON-NLS-2$
				"package p;\r\nclass Test {String hello=\"helloworld\";}"); //$NON-NLS-1$

		NLSRefactoring nls= createDefaultNls(cu);
		nls.setSubstitutionPattern(nls.getDefaultSubstitutionPattern());
		nls.setAccessorPackage(fHelper.getPackageFragment("/TestSetupProject/src2/p2")); //$NON-NLS-1$

		performChange(nls);

		checkContentOfCu("manipulated class", cu, "package p;\r\n\r\nimport p2.Messages;\r\n\r\nclass Test {String hello=Messages.getString(\"test0\");} //$NON-NLS-1$"); //$NON-NLS-1$ //$NON-NLS-2$
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), "test0=helloworld\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	public void testCreateChangeWithCollidingImport() throws Exception {
		String testClass= "package p;" + "import p.another.Messages;" + "class Test {" + "String hello=\"helloworld\";\r\n" + "}";
		ICompilationUnit cu= RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$ //$NON-NLS-2$
				testClass); //$NON-NLS-1$
		NLSRefactoring nls= createDefaultNls(cu);
		nls.setSubstitutionPattern(NLSRefactoring.getDefaultSubstitutionPattern("Messages")); //$NON-NLS-1$

		performChange(nls);

		checkContentOfCu("manipulated class", cu, "package p;" + "import p.another.Messages;\r\n" + "class Test {" + "String hello=p.Messages.getString(\"test0\"); //$NON-NLS-1$\r\n" + "}");
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), "test0=helloworld\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	// BUG 59156
	public void testCreateChangeWithExistingAccessorclassInDifferentPackage() throws Exception {
		String accessorKlazz= "package test;\n" + "public class Accessor {\n" + "		 private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n" + "		 public static String getString(String s) {\n" + "		 		 return \"\";\n" + "		 }\n" + "}\n";
		RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Accessor.java", accessorKlazz);

		String nlsMe= "package test;\n" + "class Test {\n" + "  String hello=\"helloworld\";\n" + "}\n";
		fHelper.createPackageFragment("test", "/TestSetupProject/src1");
		ICompilationUnit testClass= RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/test"), "AClass.java", nlsMe);

		NLSRefactoring nls= NLSRefactoring.create(testClass);

		nls.setAccessorPackage(fHelper.getPackageFragment("/TestSetupProject/src1/p"));
		nls.setResourceBundlePackage(fHelper.getPackageFragment("/TestSetupProject/src2/p"));
		nls.setResourceBundleName("test.properties");
		nls.setAccessorClassName("Accessor");

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		NLSSubstitution.setPrefix("test");
		substitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[0].generateKey(substitutions);

		performChange(nls);

		checkContentOfCu("manipulated class", testClass, "package test;\n\n" + "import p.Accessor;\n\n" + "class Test {\n" + "  String hello=Messages.getString(\"test0\"); //$NON-NLS-1$\n" + "}\n");
	}

	public void testCreateChangeWithNonDefaultSubstitution() throws Exception {
		ICompilationUnit cu= RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$ //$NON-NLS-2$
				"package p;import p.another.Messages;class Test {String hello=\"helloworld\";}"); //$NON-NLS-1$
		NLSRefactoring nls= createDefaultNls(cu);

		String string= "nonDefault(" + NLSRefactoring.KEY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		nls.setSubstitutionPattern(string);

		performChange(nls);
		checkContentOfCu("manipulated class", //$NON-NLS-1$
				cu, "package p;import p.another.Messages;\r\nclass Test {String hello=nonDefault(\"test0\");} //$NON-NLS-1$"); //$NON-NLS-1$
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), "test0=helloworld\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void checkContentOfCu(String message, ICompilationUnit cu, String content) throws Exception {
		RefactoringTest.assertEqualLines(message, content, cu.getBuffer().getContents());
	}

	private void checkContentOfFile(String message, IFile file, String content) throws Exception {
		InputStream in= file.getContents();
		String realContent= copyToString(in);
		in.close();
		RefactoringTest.assertEqualLines(message, content, realContent);
	}

	private String copyToString(InputStream in) throws Exception {
		ByteArrayOutputStream out= new ByteArrayOutputStream();
		int read= in.read();
		while (read != -1) {
			out.write(read);
			read= in.read();
		}
		out.close();
		return out.toString();
	}

	private NLSRefactoring createDefaultNls(ICompilationUnit cu) {
		NLSRefactoring nls= NLSRefactoring.create(cu);

		nls.setAccessorPackage(fHelper.getPackageFragment("/TestSetupProject/src1/p")); //$NON-NLS-1$
		nls.setResourceBundlePackage(fHelper.getPackageFragment("/TestSetupProject/src2/p"));
		nls.setResourceBundleName("test.properties");
		nls.setPropertyFilePath(fHelper.getFile("/TestSetupProject/src2/p/test.properties").getFullPath()); //$NON-NLS-1$
		nls.setAccessorClassName("Messages"); //$NON-NLS-1$

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		NLSSubstitution.setPrefix("test");
		substitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[0].generateKey(substitutions);
		return nls;
	}

	private void performChange(NLSRefactoring nls) throws CoreException {
		nls.checkInitialConditions(fHelper.fNpm);
		nls.checkFinalConditions(fHelper.fNpm);
		Change c= nls.createChange(fHelper.fNpm);
		c.perform(fHelper.fNpm);
	}
}