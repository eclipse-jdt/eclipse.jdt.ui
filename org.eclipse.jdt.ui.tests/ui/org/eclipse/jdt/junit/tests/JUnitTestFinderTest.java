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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

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


@RunWith(Parameterized.class)
public class JUnitTestFinderTest {
	private static record TestScenario(String name, IPath containerEntry, Consumer<IJavaProject> setCompilerOptions, String testKindId) {

		@Override
		public String toString() {
			return name;
		}
	}

	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	@Parameters(name = "{0}")
	public static Collection<TestScenario> getTestScenarios() {
		return List.of(new TestScenario("JUnit4", JUnitCore.JUNIT4_CONTAINER_PATH, JavaProjectHelper::set15CompilerOptions, TestKindRegistry.JUNIT4_TEST_KIND_ID), //
				new TestScenario("JUnit5", JUnitCore.JUNIT5_CONTAINER_PATH, JavaProjectHelper::set18CompilerOptions, TestKindRegistry.JUNIT5_TEST_KIND_ID));
	}

	@Parameter
	public TestScenario fScenario;

	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(fScenario.containerEntry());
		JavaProjectHelper.addToClasspath(fProject, cpe);

		fScenario.setCompilerOptions().accept(fProject);

		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}

	/**
	 * Copy from {@link JUnit3TestFinderTest}: All tests must work in Junit 4 as well
	 * @throws Exception if it fails
	 */
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

		// Only private classes are invisible for JUnit5
		String innerClassVisibility= TestKindRegistry.JUNIT5_TEST_KIND_ID.equals(fScenario.testKindId()) ? "private" : "";
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("import junit.framework.TestCase;\n");
		buf.append("\n");
		buf.append("public class Outer2 {\n");
		buf.append("    public class NonStaticInnerTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    " + innerClassVisibility + " static class NonVisibleInnerTest extends TestCase {\n");
		buf.append("        public void testFoo() {\n");
		buf.append("            class LocalTest extends TestCase {\n");
		buf.append("                public void testFoo() {\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		IType[] invalidTests= p.createCompilationUnit("Outer2.java", buf.toString(), false, null).getAllTypes();
		for (IType invalidTest : invalidTests) {
			assertTestFound(invalidTest, new String[] {});
		}
		assertTestFound(invalidTests[0].getCompilationUnit(), new String[] {});


		String str4= """
			package p;
			import junit.framework.TestCase;
			
			public abstract class AbstractTest extends TestCase {
			        public void testFoo() {
			        }
			}
			""";
		IType invalidTest1= p.createCompilationUnit("AbstractTest.java", str4, false, null).findPrimaryType();

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		String str5= """
			package p;
			import java.util.Vector;
			
			public class NoTest extends Vector {
			        public void testFoo() {
			        }
			}
			""";
		IType invalidTest3= p.createCompilationUnit("NoTest.java", str5, false, null).findPrimaryType();

		assertTestFound(invalidTest3, new String[] {});
		assertTestFound(invalidTest3.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.MyTest", "p.MySuperTest", "p.InvisibleTest", "p.Outer.InnerTest" };

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	@Test
	public void testSuiteFinder() throws Exception {
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

		String[] validTests= { "p.SuiteClass" };

		assertTestFound(validTest1, validTests);
		assertTestFound(validTest1.getCompilationUnit(), validTests);
		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	@Test
	public void testRunWith() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			
			import org.junit.Test;
			
			public class Test1 {
			        @Test public void testFoo() {
			        }
			}
			""";
		p.createCompilationUnit("Test1.java", str, false, null);

		String str1= """
			package p;
			
			import org.junit.runner.RunWith;
			import org.junit.runners.Suite;
			import org.junit.runners.Suite.SuiteClasses;
			
			@RunWith(Suite.class)
			@SuiteClasses(Test1.class)
			public class Test2 {
			   \s
			}
			""";
		IType validTest1= p.createCompilationUnit("Test2.java", str1, false, null).getType("Test2");

		assertTestFound(validTest1, new String[] { "p.Test2" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test2" });

		String str2= """
			package p;
			
			public class Test3 extends Test2 {
			   \s
			}
			""";
		IType validTest2= p.createCompilationUnit("Test3.java", str2, false, null).getType("Test3");

		assertTestFound(validTest2, new String[] { "p.Test3" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.Test3" });

		String str3= """
			package p;
			
			import org.junit.runner.RunWith;
			import org.junit.runners.Suite;
			import org.junit.runners.Suite.SuiteClasses;
			
			@RunWith(Suite.class)
			@SuiteClasses(Test1.class)
			public interface Test4 {
			   \s
			}
			""";
		IType invalidTest1= p.createCompilationUnit("Test4.java", str3, false, null).getType("Test4");

		assertTestFound(invalidTest1, new String[] {});
		assertTestFound(invalidTest1.getCompilationUnit(), new String[] {});

		String str4= """
			package p;
			
			import org.junit.runner.RunWith;
			import org.junit.runners.Suite;
			import org.junit.runners.Suite.SuiteClasses;
			
			@RunWith(Suite.class)
			@SuiteClasses(Test1.class)
			class Test5 {
			   \s
			}
			""";
		IType validTest3= p.createCompilationUnit("Test5.java", str4, false, null).getType("Test5");

		assertTestFound(validTest3, new String[] { "p.Test5"});
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.Test5" });

		String str5= """
			package p;
			
			import org.junit.runner.RunWith;
			
			@SuiteClasses(Test1.class)
			public class Test6 {
			    RunWith aRunWith;
			}
			""";
		IType invalidTest2= p.createCompilationUnit("Test6.java", str5, false, null).getType("Test6");

		assertTestFound(invalidTest2, new String[] {});
		assertTestFound(invalidTest2.getCompilationUnit(), new String[] {});

		String str6= """
			import java.util.Arrays;
			import java.util.Collection;
			
			import org.junit.runners.Parameterized.Parameters;
			
			public class Test7 extends StackTest {
			
				public Test7(int num) {
					super(num);
				}
			\t
				@Parameters
				 public static Collection data() {
				   Object[][] data = new Object[][] { { 1 }, { 2 }, { 3 }, { 4 } };
				   return Arrays.asList(data);
				}
			}
			""";
		IType validTest4= fRoot.getPackageFragment("").createCompilationUnit("Test7.java", str6, false, null).getType("Test7");

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

	@Test
	public void testTestAnnotation() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			
			import org.junit.Test;
			
			public class Test1 {
			        @Test public void testFoo() {
			        }
			}
			""";
		IType validTest1= p.createCompilationUnit("Test1.java", str, false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { "p.Test1" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test1" });


		String str1= """
			package p;
			
			public class Test2 extends Test1 {
			        public void testBar() {
			        }
			}
			""";
		IType validTest2= p.createCompilationUnit("Test2.java", str1, false, null).getType("Test2");

		assertTestFound(validTest2, new String[] { "p.Test2" });
		assertTestFound(validTest2.getCompilationUnit(), new String[] { "p.Test2" });


		String str2= """
			package p;
			
			import org.junit.Test;
			
			public class Test3 {
			        @Test void testFoo() {
			        }
			}
			""";
		IType validTest3= p.createCompilationUnit("Test3.java", str2, false, null).getType("Test3");

		assertTestFound(validTest3, new String[] { "p.Test3" });
		assertTestFound(validTest3.getCompilationUnit(), new String[] { "p.Test3" });


		String str3= """
			package p;
			
			import org.junit.Test;
			
			public abstract class AbstractTest {
			        @Test public void testBar() {
			        }
			}
			""";
		IType invalidTest4= p.createCompilationUnit("AbstractTest.java", str3, false, null).getType("AbstractTest");

		assertTestFound(invalidTest4, new String[] {});
		assertTestFound(invalidTest4.getCompilationUnit(), new String[] {});

		String[] validTests= { "p.Test1", "p.Test2", "p.Test3"};

		assertTestFound(p, validTests);
		assertTestFound(fRoot, validTests);
		assertTestFound(fProject, validTests);
	}

	@Test
	public void testTestAnnotation_bug204682() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			
			import org.junit.Test;
			
			public class Test1 {
			        Test testFoo1() {
			            return null;
			        }
			        public void testFoo2() {
			            Test test;
			        }
			}
			""";
		IType validTest1= p.createCompilationUnit("Test1.java", str, false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { });
	}

	@Test
	public void testTestAnnotation2() throws Exception {

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String str= """
			package p;
			
			import org.junit.Test;
			
			@RunWith(Suite.class)
			@SuiteClasses(Test1.class)
			public class Test1 {
			        @Test Test testFoo1() {
			            return null;
			        }
			}
			""";
		IType validTest1= p.createCompilationUnit("Test1.java", str, false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { "p.Test1" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test1" });
	}

	@Test
	public void testInnerClassWithNestedAnnotationIsFound() throws Exception {

		Assume.assumeTrue("@Nested only works with JUnit5", fScenario.testKindId().equals(TestKindRegistry.JUNIT5_TEST_KIND_ID));

		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		String content= """
				package p;

				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.api.Nested;

				class Test1 {
					@Nested class NestedClass {
						@Test void myTest() {}
					}
				}
							""";

		IType validTest1= p.createCompilationUnit("Test1.java", content, false, null).getType("Test1");

		assertTestFound(validTest1, new String[] { "p.Test1" });
		assertTestFound(validTest1.getCompilationUnit(), new String[] { "p.Test1",  "p.Test1.NestedClass" });
	}

	private void assertTestFound(IJavaElement container, String[] expectedTypes) throws CoreException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(container);
		assertEquals(fScenario.testKindId(), testKind.getId());

		ITestFinder finder= testKind.getFinder();

		if (container instanceof IType) {
			IType type= (IType) container;
			boolean isTest= expectedTypes.length == 1 && type.getFullyQualifiedName('.').equals(expectedTypes[0]);
			assertEquals(type.getFullyQualifiedName(), isTest, finder.isTest(type));
		}

		HashSet<IType> set= new HashSet<>(Arrays.asList(JUnitCore.findTestTypes(container, null)));
		HashSet<String> namesFound= new HashSet<>();
		for (IType curr : set) {
			namesFound.add(curr.getFullyQualifiedName('.'));
		}
		String[] actuals= namesFound.toArray(new String[namesFound.size()]);
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expectedTypes);
	}


}
