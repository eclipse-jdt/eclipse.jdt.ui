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
		String str= """
			package p;\r
			class Test {
				String hello="helloworld";
			}""";
		ICompilationUnit cu= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$//$NON-NLS-2$
				str);

		NLSRefactoring nls= createDefaultNls(cu);
		nls.setAccessorClassPackage(fHelper.getPackageFragment("/TestSetupProject/src2/p2")); //$NON-NLS-1$

		performChange(nls);

		String str1= """
			package p;\r
			\r
			import p2.Messages;\r
			\r
			class Test {
				String hello=Messages.getString("test0"); //$NON-NLS-1$
			}""";
		checkContentOfCu("manipulated class", cu, str1); //$NON-NLS-1$

		String str2= """
			test0=helloworld
			""";
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), str2); //$NON-NLS-1$ //$NON-NLS-2$
	}


	@Test
	public void createChangeWithCollidingImport() throws Exception {
		//class to NLS
		String str= """
			package p;
			import p.another.Messages;
			class Test {\
				String hello="helloworld";\r
			}""";
		ICompilationUnit cu= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", str); //$NON-NLS-1$ //$NON-NLS-2$

		NLSRefactoring nls= createDefaultNls(cu);

		performChange(nls);

		String str1= """
			package p;
			import p.another.Messages;
			class Test {\
				String hello=p.Messages.getString("test0"); //$NON-NLS-1$
			}""";
		checkContentOfCu("manipulated class", cu, str1);

		String str2= """
			test0=helloworld
			""";
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), str2); //$NON-NLS-1$ //$NON-NLS-2$
	}


	// BUG 59156
	@Test
	public void createChangeWithExistingAccessorclassInDifferentPackage() throws Exception {
		//Accessor class
		String str= """
			package p;
			public class Accessor {
					 private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
					 public static String getString(String s) {
					 		 return "";
					 }
			}
			""";
		GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Accessor.java", str);

		//class to NLS
		String str1= """
			package test;
			class Test {
			  String hello="hello";
			  String world="world";
			}
			""";
		fHelper.createPackageFragment("test", "/TestSetupProject/src1");
		ICompilationUnit testClass= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/test"), "AClass.java", str1);

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
		String str2= """
			package test;
			
			import p.Accessor;
			
			class Test {
			  String hello=Accessor.getString("test0"); //$NON-NLS-1$
			  String world=Accessor.getString("test1"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("manipulated class", testClass, str2);
	}

	// BUG 202566
	@Test
	public void createChangeWithExistingAccessorclassInDifferentPackage_1() throws Exception {
		//Accessor class
		String str= """
			package p;
			public class Accessor {
					 private static final String BUNDLE_NAME = "test.test";//$NON-NLS-1$
					 public static String getString(String s) {
					 		 return "";
					 }
			}
			""";
		GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Accessor.java", str);

		//class to NLS
		String str1= """
			package test;
			class Test {
			  String hello="helloworld";
			}
			""";
		fHelper.createPackageFragment("test", "/TestSetupProject/src1");
		ICompilationUnit testClass= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/test"), "AClass.java", str1);

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
		String str2= """
			package test;
			class Test {
			  String hello="helloworld"; //$NON-NLS-1$
			}
			""";
		checkContentOfCu("manipulated class", testClass, str2);
	}
	@Test
	public void createChangeWithNonDefaultSubstitution() throws Exception {
		//class to NLS
		String str= """
			package p;
			import p.another.Messages;
			class Test {
				String hello="helloworld";
			}""";
		ICompilationUnit cu= GenericRefactoringTest.createCU(fHelper.getPackageFragment("/TestSetupProject/src1/p"), "Test.java", //$NON-NLS-1$ //$NON-NLS-2$
				str);
		NLSRefactoring nls= createDefaultNls(cu);

		String string= "nonDefault(" + NLSRefactoring.KEY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		nls.setSubstitutionPattern(string);

		performChange(nls);
		String str1= """
			package p;
			import p.another.Messages;
			class Test {
				String hello=p.Messages.nonDefault("test0"); //$NON-NLS-1$
			}""";
		checkContentOfCu("manipulated class", //$NON-NLS-1$
				cu, str1);

		String str2= """
			test0=helloworld
			""";
		checkContentOfFile("properties", fHelper.getFile("/TestSetupProject/src2/p/test.properties"), str2); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void createDefaultAccessor(IPackageFragment pack1) throws JavaModelException {
		String str= """
			package test;
			public class Accessor {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    public static String getString(String s) {
			        return null;
			    }
			}
			""";
		pack1.createCompilationUnit("Accessor.java", str, false, null);
	}

	@Test
	public void externalizedToIgnore() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			A.1=Hello1
			A.2=Hello2
			A.3=Hello3
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("A.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("A.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("A.3"); //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		substitutions[0].setState(NLSSubstitution.IGNORED);

		performChange(nls);

		String str2= """
			A.2=Hello2
			A.3=Hello3
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= "Hello1"; //$NON-NLS-1$
			    String hello2= Accessor.getString("A.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("A.3"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void insertToDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= "Hello1";
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];

		sub.setState(NLSSubstitution.EXTERNALIZED);
		sub.setKey("1");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.1"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void renameToDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			Test.3=Hello3
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setKey("Test.1");
		sub.setValue("Hello1");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.1"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void renameDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.1"); //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setKey("Test.3");
		sub.setValue("Hello3");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			Test.3=Hello3
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void internalizeDuplicate() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.1"); //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setState(NLSSubstitution.INTERNALIZED);

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= "Hello1";\s
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void internalizeAndInsert() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= "Hello1";
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[0];
		sub.setState(NLSSubstitution.INTERNALIZED);

		NLSSubstitution sub2= substitutions[2];
		sub2.setState(NLSSubstitution.EXTERNALIZED);
		sub2.setKey("1");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= "Hello1";\s
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.1"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}


	@Test
	public void addMissing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[2];
		sub.setValue("Hello3");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			Test.3=Hello3
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void addMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 3, substitutions.length);
		NLSSubstitution sub= substitutions[1];
		sub.setValue("Hello22");

		NLSSubstitution sub2= substitutions[2];
		sub2.setValue("Hello3");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello22
			Test.3=Hello3
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello3= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void noNewLineAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= "Hello2";
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

		NLSRefactoring nls= NLSRefactoring.create(cu);

		NLSSubstitution[] substitutions= nls.getSubstitutions();
		assertEquals("number of substitutions", 2, substitutions.length);
		NLSSubstitution sub= substitutions[1];
		sub.setState(NLSSubstitution.EXTERNALIZED);
		sub.setKey("2");

		performChange(nls);

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
	}

	@Test
	public void twoInsertsNoNewLineAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		createDefaultAccessor(pack1);

		// property file
		String str= """
			Test.1=Hello1""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str);

		// class to NLS
		String str1= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= "Hello2";
			    String hello2= "Hello3";
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("Test.java", str1, false, null);

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

		String str2= """
			Test.1=Hello1
			Test.2=Hello2
			Test.3=Hello3
			""";
		checkContentOfFile("property file", file, str2);

		// class to NLS
		String str3= """
			package test;
			public class Test {
			    String hello1= Accessor.getString("Test.1"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.2"); //$NON-NLS-1$
			    String hello2= Accessor.getString("Test.3"); //$NON-NLS-1$
			}
			""";
		checkContentOfCu("nls file", cu, str3);
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
			String realContent= new String(in.readAllBytes());
			GenericRefactoringTest.assertEqualLines(message, content, realContent);
		}
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
