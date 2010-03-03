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

import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.ITestFinder;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;


public class JUnit3TestFinderTest extends TestCase {
	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	public static Test setUpTest(Test test) {
		return test;
	}

	protected void setUp() throws Exception {
		super.setUp();
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT3_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fProject, cpe);

		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
		super.tearDown();
	}

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

	public void testSuite() throws Exception {

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

		assertTestFound(validTest1, new String[] { "p.SuiteClass" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.SuiteClass" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public abstract class AbstractSuiteClass {\n");
		buf.append("    public static Test suite() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest2= p.createCompilationUnit("AbstractSuiteClass.java", buf.toString(), false, null).getType("AbstractSuiteClass");

		assertTestFound(validTest2, new String[] { "p.AbstractSuiteClass" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.AbstractSuiteClass" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("class InvisibleSuiteClass {\n");
		buf.append("    public static Test suite() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest3= p.createCompilationUnit("InvisibleSuiteClass.java", buf.toString(), false, null).getType("InvisibleSuiteClass");

		assertTestFound(validTest3, new String[] { "p.InvisibleSuiteClass" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.InvisibleSuiteClass" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public class SuiteOuter {\n");
		buf.append("    public static class InnerSuite {\n");
		buf.append("        public static Test suite() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest4= p.createCompilationUnit("SuiteOuter.java", buf.toString(), false, null).getType("SuiteOuter").getType("InnerSuite");

		assertTestFound(validTest4, new String[] { "p.SuiteOuter.InnerSuite" });
		assertTestFound(validTest4.getCompilationUnit(), new String[] { "p.SuiteOuter.InnerSuite" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public class Outer2 {\n");
		buf.append("    public class NonStaticInnerSuiteTest extends TestCase {\n");
		buf.append("        public static Test suite() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    static class NonVisibleInnerTest extends TestCase {\n");
		buf.append("        public static Test suite() {\n");
		buf.append("            class LocalTest extends TestCase {\n");
		buf.append("                public static Test suite() {\n");
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
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public class NonStaticSuite {\n");
		buf.append("    public Test suite() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType invalidTest1= p.createCompilationUnit("NonStaticSuite.java", buf.toString(), false, null).getType("NonStaticSuite");

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public class NonVisibleSuite {\n");
		buf.append("    private static Test suite() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType invalidTest2= p.createCompilationUnit("NonVisibleSuite.java", buf.toString(), false, null).getType("NonVisibleSuite");

		assertTestFound(invalidTest2, new String[] {});
		assertTestFound(invalidTest2.getCompilationUnit(), new String[] {});

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public class ParameterSuite {\n");
		buf.append("    public static Test suite(int i) {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType invalidTest3= p.createCompilationUnit("ParameterSuite.java", buf.toString(), false, null).getType("ParameterSuite");

		assertTestFound(invalidTest3, new String[] {});
		assertTestFound(invalidTest3.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.SuiteClass", "p.AbstractSuiteClass", "p.InvisibleSuiteClass", "p.SuiteOuter.InnerSuite" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	public void testTestInterface() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("import junit.framework.TestResult;\n");
		buf.append("\n");
		buf.append("public class MyITest implements Test {\n");
		buf.append("    public int countTestCases() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("    public void run(TestResult result) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest1= p.createCompilationUnit("MyITest.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(validTest1, new String[] { "p.MyITest" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.MyITest" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class MySuperITest extends MyITest {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("}\n");
		IType validTest2= p.createCompilationUnit("MySuperITest.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(validTest2, new String[] { "p.MySuperITest" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.MySuperITest" });

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.Test;\n");
		buf.append("\n");
		buf.append("public interface MyITestSuperInterface extends Test {\n");
		buf.append("}\n");
		IType invalidTest1= p.createCompilationUnit("MyITestSuperInterface.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestResult;\n");
		buf.append("\n");
		buf.append("public class MyITestSuperInterfaceImpl implements MyITestSuperInterface {\n");
		buf.append("    public int countTestCases() {\n");
		buf.append("        return 1;\n");
		buf.append("    }\n");
		buf.append("    public void run(TestResult result) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType validTest3= p.createCompilationUnit("MyITestSuperInterfaceImpl.java", buf.toString(), false, null).findPrimaryType();

		assertTestFound(validTest3, new String[] { "p.MyITestSuperInterfaceImpl" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.MyITestSuperInterfaceImpl" });

		String[] validTests= { "p.MyITest", "p.MySuperITest", "p.MyITestSuperInterfaceImpl" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}



	private void assertTestFound(IJavaElement container, String[] expectedValidTests) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(container);
		assertEquals(TestKindRegistry.JUNIT3_TEST_KIND_ID, testKind.getId());

		ITestFinder finder= testKind.getFinder();

		if (container instanceof IType) {
			IType type= (IType) container;
			boolean isValidTest= expectedValidTests.length == 1 && type.getFullyQualifiedName('.').equals(expectedValidTests[0]);
			assertEquals(type.getFullyQualifiedName('.'), isValidTest, finder.isTest(type));
		}

		HashSet set= new HashSet();
		finder.findTestsInContainer(container, set, null);

		HashSet namesFound= new HashSet();
		for (Iterator iterator= set.iterator(); iterator.hasNext();) {
			namesFound.add(((IType) iterator.next()).getFullyQualifiedName('.'));
		}
		String[] actuals= (String[]) namesFound.toArray(new String[namesFound.size()]);
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expectedValidTests);
	}


}
