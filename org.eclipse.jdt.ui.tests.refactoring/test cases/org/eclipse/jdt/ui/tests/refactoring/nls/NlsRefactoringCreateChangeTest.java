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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;

public class NlsRefactoringCreateChangeTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private NlsRefactoringTestHelper fHelper;
	private IJavaProject fJavaProject;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fJavaProject= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fHelper= new NlsRefactoringTestHelper(fJavaProject);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJavaProject, pts.getDefaultClasspath());
	}

	@Test
	public void withoutPreviousNlsing() throws Exception {

		fHelper.createPackageFragment("p2", "/TestSetupProject/src2"); //$NON-NLS-1$//$NON-NLS-2$

		// class to NLS
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\r\n");
		buf.append("class Test {\n");
		buf.append("	String hello=\"helloworld\";\n");
		buf.append("}");
		ICompilationUnit cu= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$//$NON-NLS-2$
				buf.toString());

		NLSRefactoring nls= createDefaultNls(cu);
		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src2/p2")); //$NON-NLS-1$

		performChange(nls);

		buf= new StringBuilder();
		buf.append("package p;\r\n\r\n");
		buf.append("import p2.Messages;\r\n\r\n");
		buf.append("class Test {\n");
		buf.append("	String hello=Messages.getString(\"test0\"); //$NON-NLS-1$\n");
		buf.append("}");
		checkContentOfCu("manipulated class", cu, buf.toString()); //$NON-NLS-1$

		buf= new StringBuilder();
		buf.append("test0=helloworld\n");
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), buf.toString()); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Test
	public void createChangeWithCollidingImport() throws Exception {
		//class to NLS
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import p.another.Messages;\n");
		buf.append("class Test {");
		buf.append("	String hello=\"helloworld\";\r\n");
		buf.append("}");
		ICompilationUnit cu= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", buf.toString()); //$NON-NLS-1$ //$NON-NLS-2$

		NLSRefactoring nls= createDefaultNls(cu);

		performChange(nls);

		buf=new StringBuilder();
		buf.append("package p;\n" );
		buf.append("import p.another.Messages;\n");
		buf.append("class Test {" );
		buf.append("	String hello=p.Messages.getString(\"test0\"); //$NON-NLS-1$\n");
		buf.append("}");
		checkContentOfCu("manipulated class", cu, buf.toString());

		buf= new StringBuilder();
		buf.append("test0=helloworld\n");
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), buf.toString()); //$NON-NLS-1$ //$NON-NLS-2$
	}


	// BUG 59156
	@Test
	public void createChangeWithExistingAccessorclassInDifferentPackage() throws Exception {
		//Accessor class
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("public class Accessor {\n");
		buf.append("		 private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n");
		buf.append("		 public static String getString(String s) {\n");
		buf.append("		 		 return \"\";\n");
		buf.append("		 }\n");
		buf.append("}\n");
		GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Accessor.java", buf.toString());

		//class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("class Test {\n");
		buf.append("  String hello=\"hello\";\n");
		buf.append("  String world=\"world\";\n");
		buf.append("}\n");
		fHelper.createPackageFragment("test", "/TestSetupProject/src1");
		ICompilationUnit testClass= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/test"), "AClass.java", buf.toString());

		NLSRefactoring nls= NLSRefactoring.create(testClass);

		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src1/p"));
		nls.setResourceBundlePackage(fHelper.getPackageFragment("/TestSetupProject/src2/p"));
		nls.setResourceBundleName("test.properties");
		nls.setAccessorClassName("Accessor");

		Properties properties= new Properties();
		NLSSubstitution[] substitutions= nls.getSubstitutions();
		nls.setPrefix("test");
		substitutions[0].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[0].generateKey(substitutions, properties);
		substitutions[1].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[1].generateKey(substitutions, properties);

		performChange(nls);

		//class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n\n");
		buf.append("import p.Accessor;\n\n");
		buf.append("class Test {\n");
		buf.append("  String hello=Accessor.getString(\"test0\"); //$NON-NLS-1$\n");
		buf.append("  String world=Accessor.getString(\"test1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("manipulated class", testClass, buf.toString());
	}

	// BUG 202566
	@Test
	public void createChangeWithExistingAccessorclassInDifferentPackage_1() throws Exception {
		//Accessor class
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("public class Accessor {\n");
		buf.append("		 private static final String BUNDLE_NAME = \"test.test\";//$NON-NLS-1$\n");
		buf.append("		 public static String getString(String s) {\n");
		buf.append("		 		 return \"\";\n");
		buf.append("		 }\n");
		buf.append("}\n");
		GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Accessor.java", buf.toString());

		//class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("class Test {\n");
		buf.append("  String hello=\"helloworld\";\n");
		buf.append("}\n");
		fHelper.createPackageFragment("test", "/TestSetupProject/src1");
		ICompilationUnit testClass= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/test"), "AClass.java", buf.toString());

		NLSRefactoring nls= NLSRefactoring.create(testClass);

		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src1/p"));
		nls.setResourceBundlePackage(fHelper.getPackageFragment("/TestSetupProject/src2/p"));
		nls.setResourceBundleName("test.properties");
		nls.setAccessorClassName("Accessor");

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		nls.setPrefix("test");
		substitutions[0].setState(NLSSubstitution.IGNORED);

		performChange(nls);

		//class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("class Test {\n");
		buf.append("  String hello=\"helloworld\"; //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("manipulated class", testClass, buf.toString());
	}
	@Test
	public void createChangeWithNonDefaultSubstitution() throws Exception {
		//class to NLS
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import p.another.Messages;\n");
		buf.append("class Test {\n");
		buf.append("	String hello=\"helloworld\";\n");
		buf.append("}");
		ICompilationUnit cu= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$ //$NON-NLS-2$
				buf.toString());
		NLSRefactoring nls= createDefaultNls(cu);

		String string= "nonDefault(" + NLSRefactoring.KEY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		nls.setSubstitutionPattern(string);

		performChange(nls);
		buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import p.another.Messages;\n");
		buf.append("class Test {\n");
		buf.append("	String hello=p.Messages.nonDefault(\"test0\"); //$NON-NLS-1$\n");
		buf.append("}");
		checkContentOfCu("manipulated class", //$NON-NLS-1$
				cu, buf.toString());

		buf=new StringBuilder();
		buf.append("test0=helloworld\n");
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), buf.toString()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createDefaultAccessor(IPackageFragment pack1) throws JavaModelException {
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\";//$NON-NLS-1$\n");
		buf.append("    public static String getString(String s) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);
	}

	@Test
	public void externalizedToIgnore() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("A.1=Hello1\n");
		buf.append("A.2=Hello2\n");
		buf.append("A.3=Hello3\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("A.2=Hello2\n");
		buf.append("A.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= \"Hello1\"; //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"A.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"A.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void insertToDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void renameToDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void renameDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void internalizeDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= \"Hello1\"; \n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void internalizeAndInsert() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= \"Hello1\"; \n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}


	@Test
	public void addMissing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void addMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello22\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("    String hello3= Accessor.getString(\"Test.3\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void noNewLineAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Test {\n");
		buf.append("    String hello1= Accessor.getString(\"Test.1\"); //$NON-NLS-1$\n");
		buf.append("    String hello2= Accessor.getString(\"Test.2\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		checkContentOfCu("nls file", cu, buf.toString());
	}

	@Test
	public void twoInsertsNoNewLineAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		StringBuilder buf= new StringBuilder();
		buf.append("Test.1=Hello1");
		IFile file= createPropertyFile(pack1, "Accessor.properties", buf.toString());

		// class to NLS
		buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("Test.1=Hello1\n");
		buf.append("Test.2=Hello2\n");
		buf.append("Test.3=Hello3\n");
		checkContentOfFile("property file", file, buf.toString());

		// class to NLS
		buf= new StringBuilder();
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
		GenericRefactoringTest.assertEqualLines(message, content, cu.getBuffer().getContents());
	}

	private void checkContentOfFile(String message, IFile file, String content) throws Exception {
		try (InputStream in= file.getContents()) {
			String realContent= copyToString(in);
			GenericRefactoringTest.assertEqualLines(message, content, realContent);
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
