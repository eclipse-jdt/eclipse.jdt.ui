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
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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


public class JUnit3TestFinderTest {
	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT3_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fProject, cpe);

		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}

	@Test
	public void testTestCase() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			import junit.framework.TestCase;
			
			public class MyTest extends TestCase {
			        public void testFoo() {
			        }
			}
			""";
		IType validTest1= p.createCompilationUnit("MyTest.java", str, false, null).findPrimaryType();

		assertTestFound(validTest1, new String[] { "p.MyTest" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.MyTest" });

		String str1= """
			package p;
			import junit.framework.TestCase;
			
			public class MySuperTest extends MyTest {
			        public void testFoo() {
			        }
			}
			""";
		IType validTest2= p.createCompilationUnit("MySuperTest.java", str1, false, null).findPrimaryType();

		assertTestFound(validTest2, new String[] { "p.MySuperTest" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.MySuperTest" });

		String str2= """
			package p;
			import junit.framework.TestCase;
			
			class InvisibleTest extends TestCase {
			        public void testFoo() {
			        }
			}
			""";
		IType validTest3= p.createCompilationUnit("InvisibleTest.java", str2, false, null).findPrimaryType();

		// accept invisible top level types
		assertTestFound(validTest3, new String[] { "p.InvisibleTest" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.InvisibleTest" });

		String str3= """
			package p;
			import junit.framework.TestCase;
			
			public class Outer {
			    public static class InnerTest extends TestCase {
			        public void testFoo() {
			        }
			    }
			}
			""";
		IType validTest4= p.createCompilationUnit("Outer.java", str3, false, null).getType("Outer").getType("InnerTest");

		assertTestFound(validTest4, new String[] { "p.Outer.InnerTest" });
		assertTestFound(validTest4.getCompilationUnit(), new String[] { "p.Outer.InnerTest" });

		String str4= """
			package p;
			import junit.framework.TestCase;
			
			public class Outer2 {
			    public class NonStaticInnerTest extends TestCase {
			        public void testFoo() {
			        }
			    }
			    static class NonVisibleInnerTest extends TestCase {
			        public void testFoo() {
			            class LocalTest extends TestCase {
			                public void testFoo() {
			                }
			            }
			        }
			    }
			}
			""";
		IType[] invalidTests= p.createCompilationUnit("Outer2.java", str4, false, null).getAllTypes();
		for (IType invalidTest : invalidTests) {
			assertTestFound(invalidTest, new String[] {});
		}
		assertTestFound(invalidTests[0].getCompilationUnit(), new String[] {});


		String str5= """
			package p;
			import junit.framework.TestCase;
			
			public abstract class AbstractTest extends TestCase {
			        public void testFoo() {
			        }
			}
			""";
		IType invalidTest1= p.createCompilationUnit("AbstractTest.java", str5, false, null).findPrimaryType();

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		String str6= """
			package p;
			import java.util.Vector;
			
			public class NoTest extends Vector {
			        public void testFoo() {
			        }
			}
			""";
		IType invalidTest3= p.createCompilationUnit("NoTest.java", str6, false, null).findPrimaryType();

		assertTestFound(invalidTest3, new String[] {});
		assertTestFound(invalidTest3.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.MyTest", "p.MySuperTest", "p.InvisibleTest", "p.Outer.InnerTest" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	@Test
	public void testSuite() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			import junit.framework.Test;
			
			public class SuiteClass {
			    public static Test suite() {
			        return null;
			    }
			}
			""";
		IType validTest1= p.createCompilationUnit("SuiteClass.java", str, false, null).getType("SuiteClass");

		assertTestFound(validTest1, new String[] { "p.SuiteClass" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.SuiteClass" });

		String str1= """
			package p;
			import junit.framework.Test;
			
			public abstract class AbstractSuiteClass {
			    public static Test suite() {
			        return null;
			    }
			}
			""";
		IType validTest2= p.createCompilationUnit("AbstractSuiteClass.java", str1, false, null).getType("AbstractSuiteClass");

		assertTestFound(validTest2, new String[] { "p.AbstractSuiteClass" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.AbstractSuiteClass" });

		String str2= """
			package p;
			import junit.framework.Test;
			
			class InvisibleSuiteClass {
			    public static Test suite() {
			        return null;
			    }
			}
			""";
		IType validTest3= p.createCompilationUnit("InvisibleSuiteClass.java", str2, false, null).getType("InvisibleSuiteClass");

		assertTestFound(validTest3, new String[] { "p.InvisibleSuiteClass" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.InvisibleSuiteClass" });

		String str3= """
			package p;
			import junit.framework.Test;
			
			public class SuiteOuter {
			    public static class InnerSuite {
			        public static Test suite() {
			            return null;
			        }
			    }
			}
			""";
		IType validTest4= p.createCompilationUnit("SuiteOuter.java", str3, false, null).getType("SuiteOuter").getType("InnerSuite");

		assertTestFound(validTest4, new String[] { "p.SuiteOuter.InnerSuite" });
		assertTestFound(validTest4.getCompilationUnit(), new String[] { "p.SuiteOuter.InnerSuite" });

		String str4= """
			package p;
			import junit.framework.TestCase;
			
			public class Outer2 {
			    public class NonStaticInnerSuiteTest extends TestCase {
			        public static Test suite() {
			            return null;
			        }
			    }
			    static class NonVisibleInnerTest extends TestCase {
			        public static Test suite() {
			            class LocalTest extends TestCase {
			                public static Test suite() {
			                }
			            }
			        }
			    }
			}
			""";
		IType[] invalidTests= p.createCompilationUnit("Outer2.java", str4, false, null).getAllTypes();
		for (IType invalidTest : invalidTests) {
			assertTestFound(invalidTest, new String[] {});
		}
		assertTestFound(invalidTests[0].getCompilationUnit(), new String[] {});

		String str5= """
			package p;
			import junit.framework.Test;
			
			public class NonStaticSuite {
			    public Test suite() {
			        return null;
			    }
			}
			""";
		IType invalidTest1= p.createCompilationUnit("NonStaticSuite.java", str5, false, null).getType("NonStaticSuite");

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		String str6= """
			package p;
			import junit.framework.Test;
			
			public class NonVisibleSuite {
			    private static Test suite() {
			        return null;
			    }
			}
			""";
		IType invalidTest2= p.createCompilationUnit("NonVisibleSuite.java", str6, false, null).getType("NonVisibleSuite");

		assertTestFound(invalidTest2, new String[] {});
		assertTestFound(invalidTest2.getCompilationUnit(), new String[] {});

		String str7= """
			package p;
			import junit.framework.Test;
			
			public class ParameterSuite {
			    public static Test suite(int i) {
			        return null;
			    }
			}
			""";
		IType invalidTest3= p.createCompilationUnit("ParameterSuite.java", str7, false, null).getType("ParameterSuite");

		assertTestFound(invalidTest3, new String[] {});
		assertTestFound(invalidTest3.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.SuiteClass", "p.AbstractSuiteClass", "p.InvisibleSuiteClass", "p.SuiteOuter.InnerSuite" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	@Test
	public void testTestInterface() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			import junit.framework.Test;
			import junit.framework.TestResult;
			
			public class MyITest implements Test {
			    public int countTestCases() {
			        return 1;
			    }
			    public void run(TestResult result) {
			    }
			}
			""";
		IType validTest1= p.createCompilationUnit("MyITest.java", str, false, null).findPrimaryType();

		assertTestFound(validTest1, new String[] { "p.MyITest" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.MyITest" });

		String str1= """
			package p;
			
			public class MySuperITest extends MyITest {
			        public void testFoo() {
			        }
			}
			""";
		IType validTest2= p.createCompilationUnit("MySuperITest.java", str1, false, null).findPrimaryType();

		assertTestFound(validTest2, new String[] { "p.MySuperITest" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.MySuperITest" });

		String str2= """
			package p;
			import junit.framework.Test;
			
			public interface MyITestSuperInterface extends Test {
			}
			""";
		IType invalidTest1= p.createCompilationUnit("MyITestSuperInterface.java", str2, false, null).findPrimaryType();

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		String str3= """
			package p;
			import junit.framework.TestResult;
			
			public class MyITestSuperInterfaceImpl implements MyITestSuperInterface {
			    public int countTestCases() {
			        return 1;
			    }
			    public void run(TestResult result) {
			    }
			}
			""";
		IType validTest3= p.createCompilationUnit("MyITestSuperInterfaceImpl.java", str3, false, null).findPrimaryType();

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

		HashSet<IType> set= new HashSet<>();
		finder.findTestsInContainer(container, set, null);

		HashSet<String> namesFound= new HashSet<>();
		for (IType iType : set) {
			namesFound.add(iType.getFullyQualifiedName('.'));
		}
		String[] actuals= namesFound.toArray(new String[namesFound.size()]);
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expectedValidTests);
	}


}
