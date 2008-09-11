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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

public class NlsRefactoringCreateChangeTest extends TestCase {

	private NlsRefactoringTestHelper fHelper;
	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fSourceFolder;

	public NlsRefactoringCreateChangeTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return setUpTest(new TestSuite(NlsRefactoringCreateChangeTest.class));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		fJavaProject= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
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
		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src2/p2")); //$NON-NLS-1$

		performChange(nls);

		checkContentOfCu("manipulated class", cu, "package p;\r\n\r\nimport p2.Messages;\r\n\r\nclass Test {String hello=Messages.getString(\"test0\");} //$NON-NLS-1$"); //$NON-NLS-1$ //$NON-NLS-2$
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), "test0=helloworld\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}


	public void testCreateChangeWithCollidingImport() throws Exception {
		String testClass= "package p;\n" + "import p.another.Messages;\n" + "class Test {" + "String hello=\"helloworld\";\r\n" + "}";
		ICompilationUnit cu= RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", testClass); //$NON-NLS-1$ //$NON-NLS-2$

		NLSRefactoring nls= createDefaultNls(cu);

		performChange(nls);

		checkContentOfCu("manipulated class", cu, "package p;\n" + "import p.another.Messages;\n" + "class Test {" + "String hello=p.Messages.getString(\"test0\"); //$NON-NLS-1$\n" + "}");
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

		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src1/p"));
		nls.setResourceBundlePackage(fHelper.getPackageFragment("/TestSetupProject/src2/p"));
		nls.setResourceBundleName("test.properties");
		nls.setAccessorClassName("Accessor");

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		nls.setPrefix("test");
		substitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[0].generateKey(substitutions, new Properties());

		performChange(nls);

		checkContentOfCu("manipulated class", testClass, "package test;\n\n" + "import p.Accessor;\n\n" + "class Test {\n" + "  String hello=Accessor.getString(\"test0\"); //$NON-NLS-1$\n" + "}\n");
	}

	public void testCreateChangeWithNonDefaultSubstitution() throws Exception {
		ICompilationUnit cu= RefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$ //$NON-NLS-2$
				"package p;\nimport p.another.Messages;\nclass Test {String hello=\"helloworld\";}"); //$NON-NLS-1$
		NLSRefactoring nls= createDefaultNls(cu);

		String string= "nonDefault(" + NLSRefactoring.KEY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		nls.setSubstitutionPattern(string);

		performChange(nls);
		checkContentOfCu("manipulated class", //$NON-NLS-1$
				cu, "package p;\nimport p.another.Messages;\nclass Test {String hello=p.Messages.nonDefault(\"test0\");} //$NON-NLS-1$"); //$NON-NLS-1$
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), "test0=helloworld\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private void createDefaultAccessor(IPackageFragment pack1) throws JavaModelException {
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\";//$NON-NLS-1$\n");
		buf.append("    public static String getString(String s) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);
	}

	public void testExternalizedToIgnore() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("A.1=Hello1\n");
		buf.append("A.2=Hello2\n");
		buf.append("A.3=Hello3\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"A.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"A.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"A.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		substitutions[0].setState(NLSSubstitution.IGNORED);

		performChange(nls);

		buf= new StringBuffer();
		buf.append("A.2=Hello2\n");
		buf.append("A.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= \"Hello1\"; //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"A.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"A.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testInsertToDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= \"Hello1\";\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];

		sub.setState(NLSSubstitution.EXTERNALIZED);
		sub.setKey("1");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testRenameToDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setKey("Test.1");
		sub.setValue("Hello1");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testRenameDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setKey("Test.3");
		sub.setValue("Hello3");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testInternalizeDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setState(NLSSubstitution.INTERNALIZED);

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= \"Hello1\"; \n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testInternalizeAndInsert() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= \"Hello1\";\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[0];
		sub.setState(NLSSubstitution.INTERNALIZED);

		NLSSubstitution sub2= substitutions[2];
		sub2.setState(NLSSubstitution.EXTERNALIZED);
		sub2.setKey("1");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= \"Hello1\"; \n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}


	public void testAddMissing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setValue("Hello3");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testAddMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[1];
		sub.setValue("Hello22");

		NLSSubstitution sub2= substitutions[2];
		sub2.setValue("Hello3");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello22\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testNoNewLineAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= \"Hello2\";\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 2, substitutions.length);
		NLSSubstitution sub= substitutions[1];
		sub.setState(NLSSubstitution.EXTERNALIZED);
		sub.setKey("2");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	public void testTwoInsertsNoNewLineAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuffer buf= new StringBuffer();
		buf.append("Test.1=Hello1");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= \"Hello2\";\n");
		buf.append("    String hello2= \"Hello3\";\n");
		buf.append("}\n");
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", buf.toString(), false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[1];
		sub.setState(NLSSubstitution.EXTERNALIZED);
		sub.setKey("2");

		sub= substitutions[2];
		sub.setState(NLSSubstitution.EXTERNALIZED);
		sub.setKey("3");

		performChange(nls);

		buf= new StringBuffer();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	private IFile createPropertyFile(IPackageFragment pack, String name, String content) throws UnsupportedEncodingException, CoreException {
		ByteArrayInputStream is= new ByteArrayInputStream(content.getBytes("8859_1"));
		IFile file= ((IFolder) pack.getResource()).getFile(name);
		file.create(is, false, null);
		return file;
	}

	private void checkContentOfCu(String message, ICompilationUnit cu, String content) throws Exception {
		RefactoringTest.assertEqualLines(message, content, cu.getBuffer().getContents());
	}

	private void checkContentOfFile(String message, IFile file, String content) throws Exception {
		InputStream in= file.getContents();
		try {
			String realContent= copyToString(in);
			RefactoringTest.assertEqualLines(message, content, realContent);
		} finally {
			in.close();
		}
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

		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src1/p")); //$NON-NLS-1$
		nls.setResourceBundlePackage(fHelper.getPackageFragment("/TestSetupProject/src2/p"));
		nls.setResourceBundleName("test.properties");
		//nls.setPropertyFilePath(fHelper.getFile("/TestSetupProject/src2/p/test.properties").getFullPath()); //$NON-NLS-1$
		nls.setAccessorClassName("Messages"); //$NON-NLS-1$

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		nls.setPrefix("test");
		substitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[0].generateKey(substitutions, new Properties());
		return nls;
	}

	private void performChange(NLSRefactoring nls) throws CoreException {
		nls.checkInitialConditions(fHelper.fNpm);
		nls.checkFinalConditions(fHelper.fNpm);
		Change c= nls.createChange(fHelper.fNpm);
		c.initializeValidationData(fHelper.fNpm);
		try {
			c.perform(fHelper.fNpm);
		} finally {
			c.dispose();
		}
	}
}
