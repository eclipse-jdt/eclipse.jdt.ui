/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.buildpath.JUnitContainerInitializer;
import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

public class JUnit4TestFinderTest extends TestCase {

	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	public static Test setUpTest(Test test) {
		return test;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT4_PATH);
		JavaProjectHelper.addToClasspath(fProject, cpe);
		JavaProjectHelper.set15CompilerOptions(fProject);

		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
		super.tearDown();
	}

	/**
	 * Copy from {@link JUnit3TestFinderTest}: All tests must work in Junit 4 as well
	 * @throws Exception if it fails
	 */
	public void testTestCase() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public class MyTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("MyTest.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(validTest1, new String[] { "p.MyTest" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.MyTest" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public class MySuperTest extends MyTest {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest2= p.createCompilationUnit("MySuperTest.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(validTest2, new String[] { "p.MySuperTest" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.MySuperTest" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("class InvisibleTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest3= p.createCompilationUnit("InvisibleTest.java", buf.toString(), false, null).findPrimaryType();

		// accept invisible top level types
		assertTestFound(validTest3, new String[] { "p.InvisibleTest" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.InvisibleTest" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public class Outer {\n");
		buf.append("    public static class InnerTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest4= p.createCompilationUnit("Outer.java", buf.toString(), false, null).getType("Outer").getType("InnerTest");

		assertTestFound(validTest4, new String[] { "p.Outer.InnerTest" });
		assertTestFound(validTest4.getCompilationUnit(), new String[] { "p.Outer.InnerTest" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public class Outer2 {\n");
		buf.append("    public class NonStaticInnerTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    static class NonVisibleInnerTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("            class LocalTest extends TestCase {\n");
		buf.append("                public void testFoo() {\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType[] invalidTests= p.createCompilationUnit("Outer2.java", buf.toString(), false, null).getAllTypes();
		for (int i= 0; i < invalidTests.length; i++) {
			assertTestFound(invalidTests[i], new String[] {});
		}
		assertTestFound(invalidTests[0].getCompilationUnit(), new String[] {});


		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public abstract class AbstractTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType invalidTest1= p.createCompilationUnit("AbstractTest.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class NoTest extends Vector {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType invalidTest3= p.createCompilationUnit("NoTest.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(invalidTest3, new String[] {});
		assertTestFound(invalidTest3.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.MyTest", "p.MySuperTest", "p.InvisibleTest", "p.Outer.InnerTest" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	public void testSuiteFinder() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public class SuiteClass {\n");
		buf.append("    public static Test suite() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("SuiteClass.java", buf.toString(), false, null).getType("SuiteClass");

		String[] validTests= { "p.SuiteClass" };

		assertTestFound(validTest1, validTests);
		assertTestFound(validTest1.getCompilationUnit(), validTests);
		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	public void testRunWith() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.Test;\n");
		buf.append("\n");
		buf.append("public class Test1 {\n");
		buf.append("        @Test public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		p.createCompilationUnit("Test1.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.runner.RunWith;\n");
		buf.append("import org.junit.runners.Suite;\n");
		buf.append("import org.junit.runners.Suite.SuiteClasses;\n");
		buf.append("\n");
		buf.append("@RunWith(Suite.class)\n");
		buf.append("@SuiteClasses(Test1.class)\n");
		buf.append("public class Test2 {\n");
		buf.append("    \n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("Test2.java", buf.toString(), false, null).getType("Test2");

		assertTestFound(validTest1, new String[] { "p.Test2" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test2" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class Test3 extends Test2 {\n");
		buf.append("    \n");
		buf.append("}\n");
		IType validTest2= p.createCompilationUnit("Test3.java", buf.toString(), false, null).getType("Test3");

		assertTestFound(validTest2, new String[] { "p.Test3" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.Test3" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.runner.RunWith;\n");
		buf.append("import org.junit.runners.Suite;\n");
		buf.append("import org.junit.runners.Suite.SuiteClasses;\n");
		buf.append("\n");
		buf.append("@RunWith(Suite.class)\n");
		buf.append("@SuiteClasses(Test1.class)\n");
		buf.append("public interface Test4 {\n");
		buf.append("    \n");
		buf.append("}\n");
		IType invalidTest1= p.createCompilationUnit("Test4.java", buf.toString(), false, null).getType("Test4");

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.runner.RunWith;\n");
		buf.append("import org.junit.runners.Suite;\n");
		buf.append("import org.junit.runners.Suite.SuiteClasses;\n");
		buf.append("\n");
		buf.append("@RunWith(Suite.class)\n");
		buf.append("@SuiteClasses(Test1.class)\n");
		buf.append("class Test5 {\n");
		buf.append("    \n");
		buf.append("}\n");
		IType validTest3= p.createCompilationUnit("Test5.java", buf.toString(), false, null).getType("Test5");

		assertTestFound(validTest3, new String[] { "p.Test5"});
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.Test5" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.runner.RunWith;\n");
		buf.append("\n");
		buf.append("@SuiteClasses(Test1.class)\n");
		buf.append("public class Test6 {\n");
		buf.append("    RunWith aRunWith;\n");
		buf.append("}\n");
		IType invalidTest2= p.createCompilationUnit("Test6.java", buf.toString(), false, null).getType("Test6");

		assertTestFound(invalidTest2, new String[] {});
		assertTestFound(invalidTest2.getCompilationUnit(), new String[] {});

		buf= new StringBuffer();
		buf.append("import java.util.Arrays;\n");
		buf.append("import java.util.Collection;\n");
		buf.append("\n");
		buf.append("import org.junit.runners.Parameterized.Parameters;\n");
		buf.append("\n");
		buf.append("public class Test7 extends StackTest {\n");
		buf.append("\n");
		buf.append("	public Test7(int num) {\n");
		buf.append("		super(num);\n");
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	@Parameters\n");
		buf.append("	 public static Collection data() {\n");
		buf.append("	   Object[][] data = new Object[][] { { 1 }, { 2 }, { 3 }, { 4 } };\n");
		buf.append("	   return Arrays.asList(data);\n");
		buf.append("	}\n");
		buf.append("}\n");
		IType validTest4= fRoot.getPackageFragment("").createCompilationUnit("Test7.java", buf.toString(), false, null).getType("Test7");

		File lib= JavaTestPlugin.getDefault().getFileInPlugin(new Path("testresources/stacktest.jar"));
		JavaProjectHelper.addLibrary(fProject, Path.fromOSString(lib.getPath()));
		
		assertTestFound(validTest4, new String[] { "Test7"});
		assertTestFound(validTest4.getCompilationUnit(), new String[] { "Test7" });
		
		String[] validTestsP= { "p.Test1", "p.Test2", "p.Test3", "p.Test5"};
		assertTestFound(p, validTestsP);
		
		String[] validTests= new String[validTestsP.length + 1];
		System.arraycopy(validTestsP, 0, validTests, 0, validTestsP.length);
		validTests[validTestsP.length]= "Test7";
		
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	public void testTestAnnotation() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.Test;\n");
		buf.append("\n");
		buf.append("public class Test1 {\n");
		buf.append("        @Test public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("Test1.java", buf.toString(), false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { "p.Test1" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test1" });


		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class Test2 extends Test1 {\n");
		buf.append("        public void testBar() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest2= p.createCompilationUnit("Test2.java", buf.toString(), false, null).getType("Test2");

		assertTestFound(validTest2, new String[] { "p.Test2" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.Test2" });


		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.Test;\n");
		buf.append("\n");
		buf.append("public class Test3 {\n");
		buf.append("        @Test void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest3= p.createCompilationUnit("Test3.java", buf.toString(), false, null).getType("Test3");

		assertTestFound(validTest3, new String[] { "p.Test3" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.Test3" });


		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.Test;\n");
		buf.append("\n");
		buf.append("public abstract class AbstractTest {\n");
		buf.append("        @Test public void testBar() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType invalidTest4= p.createCompilationUnit("AbstractTest.java", buf.toString(), false, null).getType("AbstractTest");

		assertTestFound(invalidTest4, new String[] {});
		assertTestFound(invalidTest4.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.Test1", "p.Test2", "p.Test3"};

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	public void testTestAnnotation_bug204682() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.Test;\n");
		buf.append("\n");
		buf.append("public class Test1 {\n");
		buf.append("        Test testFoo1() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("        public void testFoo2() {\n");
		buf.append("            Test test;\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("Test1.java", buf.toString(), false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { });
	}

	public void testTestAnnotation2() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("import org.junit.Test;\n");
		buf.append("\n");
		buf.append("@RunWith(Suite.class)\n");
		buf.append("@SuiteClasses(Test1.class)\n");
		buf.append("public class Test1 {\n");
		buf.append("        @Test Test testFoo1() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("Test1.java", buf.toString(), false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { "p.Test1" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test1" });
	}


	private void assertTestFound(IJavaElement container, String[] expectedTypes) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(container);
		assertEquals(TestKindRegistry.JUNIT4_TEST_KIND_ID, testKind.getId());

		ITestFinder finder= testKind.getFinder();

		if (container instanceof IType) {
			IType type= (IType) container;
			boolean isTest= expectedTypes.length == 1 && type.getFullyQualifiedName('.').equals(expectedTypes[0]);
			assertEquals(type.getFullyQualifiedName(), isTest, finder.isTest(type));
		}

		HashSet set= new HashSet();
//		finder.findTestsInContainer(container, set, null);
		set.addAll(Arrays.asList(JUnitCore.findTestTypes(container, null)));

		HashSet namesFound= new HashSet();
		for (Iterator iterator= set.iterator(); iterator.hasNext();) {
			IType curr= (IType) iterator.next();
			namesFound.add(curr.getFullyQualifiedName('.'));
		}
		String[] actuals= (String[]) namesFound.toArray(new String[namesFound.size()]);
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expectedTypes);
	}


}
