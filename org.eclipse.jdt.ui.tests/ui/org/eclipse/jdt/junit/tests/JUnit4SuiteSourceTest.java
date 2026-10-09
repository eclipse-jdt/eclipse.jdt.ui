/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.junit.runners.BlockJUnit4ClassRunner;

import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;
import org.eclipse.jdt.internal.junit4.runner.JUnit4Identifier;
import org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader;

import junit.extensions.TestSetup;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit4SuiteSourceTest {

	@Test
	public void testUnnamedSuiteRetainsItsDisplayNameAndIdentifiesItsClass() {
		ITestReference test= load(UnnamedSuite.class, null);
		ITestIdentifier root= tree(test).get(0);
		assertTrue(root.getDisplayName().startsWith("TestSuite with 2 tests")); //$NON-NLS-1$
		assertEquals(UnnamedSuite.class.getName(), root.getName());
		assertEquals(root, test.getIdentifier());
	}

	@Test
	public void testNamedSuiteRetainsItsDisplayNameAndIdentifiesItsClass() {
		ITestIdentifier root= tree(load(NamedSuite.class, null)).get(0);
		assertEquals("Readable suite (with punctuation)", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(NamedSuite.class.getName(), root.getName());
	}

	@Test
	public void testEmptySuiteIdentifiesItsClass() {
		ITestReference test= load(EmptySuite.class, null);
		ITestIdentifier root= tree(test).get(0);
		assertEquals("Empty suite", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(EmptySuite.class.getName(), root.getName());
		assertEmptySuite(test);
	}

	@Test
	public void testMethodLikeEmptySuiteNameDoesNotBecomeAMethod() {
		ITestReference test= load(MethodLikeEmptySuite.class, null);
		assertEquals(MethodLikeEmptySuite.class.getName(), test.getIdentifier().getName());
		assertEquals("runBare(junit.framework.TestCase)", test.getIdentifier().getDisplayName()); //$NON-NLS-1$
		assertEmptySuite(test);
	}

	@Test
	public void testDecoratedEmptySuiteKeepsItsSuiteKind() {
		assertEmptySuite(load(DecoratedEmptySuite.class, null));
	}

	@Test
	public void testNestedEmptySuiteKeepsItsSuiteKind() {
		ITestReference test= load(NestedEmptySuite.class, null);
		List<TreeEntry> entries= treeEntries(test);
		assertEquals(0, test.countTestCases());
		assertEquals(2, entries.size());
		assertTrue(entries.get(0).suite());
		assertEquals(1, entries.get(0).count());
		assertTrue(entries.get(1).suite());
		assertEquals(0, entries.get(1).count());
		assertEquals("Empty suite", entries.get(1).identifier().getName()); //$NON-NLS-1$
	}

	@Test
	public void testClassNameInsideSuiteDisplayNameIsNotItsSource() {
		ITestIdentifier root= tree(load(MisleadingNameSuite.class, null)).get(0);
		assertEquals("Readable suite (java.lang.String)", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(MisleadingNameSuite.class.getName(), root.getName());
	}

	@Test
	public void testClassNameInsideEmptySuiteDisplayNameIsNotItsSource() {
		ITestIdentifier root= tree(load(EmptyMisleadingNameSuite.class, null)).get(0);
		assertEquals("Empty suite (java.lang.String)", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(EmptyMisleadingNameSuite.class.getName(), root.getName());
	}

	@Test
	public void testExplicitAllTestsRunnerKeepsTheEmptySuitesSource() {
		ITestReference test= load(AnnotatedEmptySuite.class, null);
		ITestIdentifier root= tree(test).get(0);
		assertEquals("Empty suite (java.lang.String)", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(AnnotatedEmptySuite.class.getName(), root.getName());
		assertEmptySuite(test);
	}

	@Test
	public void testSuiteMethodReturningSingleTestKeepsItsMethodIdentity() {
		ITestReference test= load(SingleTestSuite.class, null);
		ITestIdentifier root= tree(test).get(0);
		assertEquals("testFirst(" + SampleTest.class.getName() + ")", root.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(treeEntries(test).get(0).suite());
		assertEquals(1, treeEntries(test).get(0).count());
		assertEquals(1, test.countTestCases());
	}

	@Test
	public void testNestedGroupDoesNotInheritTheLaunchedClass() {
		List<ITestIdentifier> entries= tree(load(UnnamedSuite.class, null));
		assertEquals("Unmapped group", entries.get(1).getName()); //$NON-NLS-1$
		assertEquals("Unmapped group", entries.get(1).getDisplayName()); //$NON-NLS-1$
		assertEquals("testFirst(" + SampleTest.class.getName() + ")", entries.get(2).getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSuiteIdentifiersStillMatchJUnitEvents() {
		ITestReference test= load(NamedSuite.class, null);
		Description description= Request.aClass(NamedSuite.class).getRunner().getDescription();
		ITestIdentifier original= new JUnit4Identifier(description);
		assertEquals(original, test.getIdentifier());
		assertEquals(original.hashCode(), test.getIdentifier().hashCode());

		List<ITestIdentifier> entries= tree(test);
		List<ITestIdentifier> started= new ArrayList<>();
		List<ITestIdentifier> ended= new ArrayList<>();
		test.run(new TestExecution(new IListensToTestExecutions() {
			@Override
			public void notifyTestStarted(ITestIdentifier identifier) {
				started.add(identifier);
			}

			@Override
			public void notifyTestEnded(ITestIdentifier identifier) {
				ended.add(identifier);
			}

			@Override
			public void notifyTestFailed(TestReferenceFailure failure) {
				fail(failure.getTrace());
			}
		}, null));
		assertEquals(entries.subList(2, 4), started);
		assertEquals(started, ended);
	}

	@Test
	public void testSuiteCanBeLoadedAgainByItsTechnicalName() throws Exception {
		ITestIdentifier root= tree(load(NamedSuite.class, null)).get(0);
		assertEquals(NamedSuite.class.getName(), root.getName());
		ITestReference rerun= load(Class.forName(root.getName()), null);
		assertEquals(2, rerun.countTestCases());
		assertEquals("Readable suite (with punctuation)", rerun.getIdentifier().getDisplayName()); //$NON-NLS-1$
	}

	@Test
	public void testFilteredMethodKeepsItsMethodIdentity() {
		List<ITestIdentifier> entries= tree(load(SampleTest.class, "testSecond")); //$NON-NLS-1$
		assertEquals(1, entries.size());
		assertEquals("testSecond(" + SampleTest.class.getName() + ")", entries.get(0).getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSuiteMethodIsOnlyInvokedOnce() {
		CountingSuite.calls= 0;
		ITestReference test= load(CountingSuite.class, null);
		tree(test);
		assertEquals(1, test.countTestCases());
		assertEquals(1, CountingSuite.calls);
	}

	@Test
	public void testIgnoredSuiteMethodIsNotInvoked() {
		CountingSuite.calls= 0;
		load(IgnoredSuite.class, null);
		assertEquals(0, CountingSuite.calls);
	}

	@Test
	public void testExplicitRunnerTakesPrecedenceOverSuiteMethod() {
		CountingSuite.calls= 0;
		ITestReference test= load(ExplicitRunnerSuite.class, null);
		assertEquals(1, test.countTestCases());
		assertEquals(0, CountingSuite.calls);
		assertEquals("actualTest(" + ExplicitRunnerSuite.class.getName() + ")", tree(test).get(1).getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSuiteInitializationFailureIsReported() {
		ITestReference test= load(FailingSuite.class, null);
		assertEquals(1, test.countTestCases());
		assertEquals("initializationError(" + FailingSuite.class.getName() + ")", tree(test).get(1).getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMissingRunnerClassIsReportedAsInitializationFailure() throws Exception {
		String testClassName= ExplicitRunnerSuite.class.getName();
		byte[] classBytes;
		try (InputStream input= ExplicitRunnerSuite.class.getResourceAsStream("JUnit4SuiteSourceTest$ExplicitRunnerSuite.class")) { //$NON-NLS-1$
			classBytes= input.readAllBytes();
		}
		Class<?> testClass= new ClassLoader(getClass().getClassLoader()) {
			Class<?> defineTestClass() {
				return defineClass(testClassName, classBytes, 0, classBytes.length);
			}

			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if (name.equals(BlockJUnit4ClassRunner.class.getName())) {
					throw new ClassNotFoundException(name);
				}
				return super.loadClass(name, resolve);
			}
		}.defineTestClass();
		ITestReference test= load(testClass, null);
		assertEquals(1, test.countTestCases());
		assertEquals("initializationError(" + testClassName + ")", tree(test).get(1).getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static ITestReference load(Class<?> testClass, String methodName) {
		return new JUnit4TestLoader().loadTests(new Class<?>[] { testClass }, methodName, null, null, null, null, null)[0];
	}

	private static List<ITestIdentifier> tree(ITestReference test) {
		return treeEntries(test).stream().map(TreeEntry::identifier).toList();
	}

	private record TreeEntry(ITestIdentifier identifier, boolean suite, int count) {
	}

	private static List<TreeEntry> treeEntries(ITestReference test) {
		List<TreeEntry> entries= new ArrayList<>();
		test.sendTree((identifier, suite, count, dynamic, parentId) -> entries.add(new TreeEntry(identifier, suite, count)));
		return entries;
	}

	private static void assertEmptySuite(ITestReference test) {
		List<TreeEntry> entries= treeEntries(test);
		assertEquals(1, entries.size());
		assertTrue("An empty suite must not be emitted as a test case", entries.get(0).suite()); //$NON-NLS-1$
		assertEquals(0, entries.get(0).count());
		assertEquals(0, test.countTestCases());
	}

	public static class UnnamedSuite {
		public static junit.framework.Test suite() {
			TestSuite suite= new TestSuite();
			TestSuite group= new TestSuite("Unmapped group"); //$NON-NLS-1$
			group.addTest(new SampleTest("testFirst")); //$NON-NLS-1$
			group.addTest(new SampleTest("testSecond")); //$NON-NLS-1$
			suite.addTest(group);
			return suite;
		}
	}

	public static class NamedSuite {
		public static junit.framework.Test suite() {
			TestSuite suite= (TestSuite) UnnamedSuite.suite();
			suite.setName("Readable suite (with punctuation)"); //$NON-NLS-1$
			return suite;
		}
	}

	public static class EmptySuite {
		public static junit.framework.Test suite() {
			return new TestSuite("Empty suite"); //$NON-NLS-1$
		}
	}

	public static class MethodLikeEmptySuite {
		public static junit.framework.Test suite() {
			return new TestSuite("runBare(junit.framework.TestCase)"); //$NON-NLS-1$
		}
	}

	public static class DecoratedEmptySuite {
		public static junit.framework.Test suite() {
			return new TestSetup(EmptySuite.suite());
		}
	}

	public static class NestedEmptySuite {
		public static junit.framework.Test suite() {
			TestSuite suite= new TestSuite("Outer suite"); //$NON-NLS-1$
			suite.addTest(EmptySuite.suite());
			return suite;
		}
	}

	public static class MisleadingNameSuite {
		public static junit.framework.Test suite() {
			TestSuite suite= (TestSuite) UnnamedSuite.suite();
			suite.setName("Readable suite (java.lang.String)"); //$NON-NLS-1$
			return suite;
		}
	}

	public static class EmptyMisleadingNameSuite {
		public static junit.framework.Test suite() {
			return new TestSuite("Empty suite (java.lang.String)"); //$NON-NLS-1$
		}
	}

	public static class SingleTestSuite {
		public static junit.framework.Test suite() {
			return new SampleTest("testFirst"); //$NON-NLS-1$
		}
	}

	public static class CountingSuite {
		static int calls;

		public static junit.framework.Test suite() {
			calls++;
			return new SampleTest("testFirst"); //$NON-NLS-1$
		}
	}

	@Ignore
	public static class IgnoredSuite extends CountingSuite {
	}

	@RunWith(BlockJUnit4ClassRunner.class)
	public static class ExplicitRunnerSuite extends CountingSuite {
		@Test
		public void actualTest() {
		}
	}

	public static class FailingSuite {
		public static junit.framework.Test suite() {
			throw new IllegalStateException("suite failed"); //$NON-NLS-1$
		}
	}

	@RunWith(AllTests.class)
	public static class AnnotatedEmptySuite extends EmptyMisleadingNameSuite {
	}

	public static class SampleTest extends TestCase {
		public SampleTest(String name) {
			super(name);
		}

		public void testFirst() {
		}

		public void testSecond() {
		}
	}
}
