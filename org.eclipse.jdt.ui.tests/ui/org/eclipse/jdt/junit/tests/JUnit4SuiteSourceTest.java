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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;
import org.eclipse.jdt.internal.junit4.runner.JUnit4Identifier;
import org.eclipse.jdt.internal.junit4.runner.JUnit4TestLoader;

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
		ITestIdentifier root= tree(load(EmptySuite.class, null)).get(0);
		assertEquals("Empty suite", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(EmptySuite.class.getName(), root.getName());
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
		ITestIdentifier root= tree(load(AnnotatedEmptySuite.class, null)).get(0);
		assertEquals("Empty suite (java.lang.String)", root.getDisplayName()); //$NON-NLS-1$
		assertEquals(AnnotatedEmptySuite.class.getName(), root.getName());
	}

	@Test
	public void testSuiteMethodReturningSingleTestKeepsItsMethodIdentity() {
		ITestIdentifier root= tree(load(SingleTestSuite.class, null)).get(0);
		assertEquals("testFirst(" + SampleTest.class.getName() + ")", root.getName()); //$NON-NLS-1$ //$NON-NLS-2$
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

	private static ITestReference load(Class<?> testClass, String methodName) {
		return new JUnit4TestLoader().loadTests(new Class<?>[] { testClass }, methodName, null, null, null, null, null)[0];
	}

	private static List<ITestIdentifier> tree(ITestReference test) {
		List<ITestIdentifier> entries= new ArrayList<>();
		test.sendTree((identifier, hasChildren, count, dynamic, parentId) -> entries.add(identifier));
		return entries;
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
