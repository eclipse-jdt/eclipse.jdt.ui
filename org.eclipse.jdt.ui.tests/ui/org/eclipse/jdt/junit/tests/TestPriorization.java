/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.jdt.internal.junit.runner.FailuresFirstPrioritizer;

import junit.extensions.TestDecorator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestPriorization extends TestCase {

	public void testReorderSimple() {
		TestSuite suite= createSuiteDEF();
		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test prioritized= prioritize(suite, priority);
		List<String> order= new ArrayList<>();
		collectOrder(prioritized, order);
		String[] expected= {
				"testF", "testD", "testE"
		};
		checkOrder(expected, order);
	}

	public void testReorderCustomSuite() {
		// custom suite
		//		D
		//		E
		//		F
		TestSuite suite= new TestSuite() {};
		suite.addTest(new TestPriorizationSuite2("testD"));
		suite.addTest(new TestPriorizationSuite2("testE"));
		suite.addTest(new TestPriorizationSuite2("testF"));

		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test prioritized= prioritize(suite, priority);

		List<String> order= new ArrayList<>();
		collectOrder(prioritized, order);
		String[] expected= {
				"testF", "testD", "testE"
		};
		//printOrder(order);
		checkOrder(expected, order);
	}

	public void testReorderSimpleWithDecorator() {
		// suite
		//		decorator
		//			D
		//			E
		//			F

		Test suite= new TestSetup(createSuiteDEF()) {
			@Override
			protected void setUp() throws Exception {}
		};

		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);
		String[] expected= {
				"testF", "testD", "testE"
		};
		checkOrder(expected, order);
	}

	public void testReorderWithPropagation() {
		// suite
		//		suite1
		//			A
		//			B
		//			C
		//		suite2
		//			D
		//			E
		//			F
		TestSuite suite= new TestSuite();
		suite.addTest(createSuiteABC());
		suite.addTest(createSuiteDEF());

		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC",
		};
		checkOrder(expected, order);
	}

	public void testReorderWithPropagationWithDecorator() {
		// suite
		//		suite1
		//			A
		//			B
		//			C
		//		decorator
		//			suite2
		//				D
		//				E
		//				F
		TestSuite suite= new TestSuite();
		suite.addTest(createSuiteABC());
		TestSuite suite2= createSuiteDEF();
		suite.addTest(new TestSetup(suite2) {
			@Override
			protected void setUp() throws Exception {
			}
		});

		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC",
		};
		checkOrder(expected, order);
	}

	public void testReorderWithPropagation2() {
		// suite4
		//		suite3
		//			X
		//			Y
		//			Z
		//		suite
		//	 		suite1
		//				A
		//				B
		//				C
		//			suite2
		//				D
		//				E
		//				F*
		TestSuite suite= new TestSuite();
		suite.addTest(createSuiteABC());
		suite.addTest(createSuiteDEF());
		TestSuite suite4= new TestSuite();
		suite4.addTest(createSuiteXYZ());
		suite4.addTest(suite);

		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite4, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC", "testX", "testY", "testZ",
		};
		checkOrder(expected, order);
	}

	public void testReorderWithPropagationBug() {
		// suite4
		//		suite3
		//			X
		//			Y
		//			Z
		//		suite
		//	 		suite1
		//				A
		//				B
		//				C
		//			suite2
		//				D
		//				E*
		//				F*
		TestSuite suite= new TestSuite();
		suite.addTest(createSuiteABC());
		suite.addTest(createSuiteDEF());
		TestSuite suite4= new TestSuite();
		suite4.addTest(createSuiteXYZ());
		suite4.addTest(suite);

		String[] priority= {
				"testE(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)", "testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite4, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testE", "testD", "testA", "testB", "testC", "testX", "testY", "testZ",
		};
		//printOrder(order);
		checkOrder(expected, order);
	}

	public void testReorderWithPropagation3() {
		// suite4
		//		suite3
		//			X
		//			Y
		//			Z*
		//		suite
		//	 		suite1
		//				A
		//				B
		//				C
		//			suite2
		//				D
		//				E
		//				F*
		TestSuite suite= new TestSuite();
		suite.addTest(createSuiteABC());
		suite.addTest(createSuiteDEF());
		TestSuite suite4= new TestSuite();
		suite4.addTest(createSuiteXYZ());
		suite4.addTest(suite);

		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)", "testZ(org.eclipse.jdt.junit.tests.TestPriorizationSuite)"
		};
		Test reordered= prioritize(suite4, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC", "testZ", "testX", "testY",
		};
		checkOrder(expected, order);
	}

	public void testReorder() {
		// suite
		//		suite1
		//			X
		//			Y
		//			Z*
		//		suite2
		//			suite3
		//				A
		//				B
		//				C
		//			suite4
		//				D
		//				E
		//				F*
		TestSuite suite= new TestSuite();
		suite.addTestSuite(TestPriorizationSuite.class);
		TestSuite suite2= new TestSuite();
		suite2.addTestSuite(TestPriorizationSuite1.class);
		suite2.addTestSuite(TestPriorizationSuite2.class);
		suite.addTest(suite2);
		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)", "testZ(org.eclipse.jdt.junit.tests.TestPriorizationSuite)"
		};
		Test reordered= prioritize(suite, priority);
		List<String> order= new ArrayList<>();
		collectOrder(reordered, order);

		// can't check for exact order, since order of Class.getDeclaredMethods() is unspecified (bug 144503)
		List<String> suiteTests= new ArrayList<>(Arrays.asList("testX", "testY", "testZ"));
		List<String> suite1Tests= new ArrayList<>(Arrays.asList("testA", "testB", "testC"));
		List<String> suite2Tests= new ArrayList<>(Arrays.asList("testD", "testE", "testF"));

		assertEquals("testF", order.get(0));
		assertEquals("testZ", order.get(6));
		for (int i= 0; i < 3; i++) {
			String test= order.get(i);
			assertTrue(test, suite2Tests.remove(test));
		}
		for (int i= 3; i < 6; i++) {
			String test= order.get(i);
			assertTrue(test, suite1Tests.remove(test));
		}
		for (int i= 6; i < 9; i++) {
			String test= order.get(i);
			assertTrue(test, suiteTests.remove(test));
		}
	}

	private TestSuite createSuiteDEF() {
		// suite
		//		D
		//		E
		//		F
		TestSuite suite= new TestSuite();
		suite.addTest(new TestPriorizationSuite2("testD"));
		suite.addTest(new TestPriorizationSuite2("testE"));
		suite.addTest(new TestPriorizationSuite2("testF"));
		return suite;
	}

	private TestSuite createSuiteABC() {
		// suite
		//		A
		//		B
		//		C
		TestSuite suite= new TestSuite();
		suite.addTest(new TestPriorizationSuite1("testA"));
		suite.addTest(new TestPriorizationSuite1("testB"));
		suite.addTest(new TestPriorizationSuite1("testC"));
		return suite;
	}

	private TestSuite createSuiteXYZ() {
		// suite
		//		X
		//		Y
		//		Z
		TestSuite suite= new TestSuite();
		suite.addTest(new TestPriorizationSuite("testX"));
		suite.addTest(new TestPriorizationSuite("testY"));
		suite.addTest(new TestPriorizationSuite("testZ"));
		return suite;
	}


	private void checkOrder(String[] expected, List<String> order) {
//		assertEquals(Arrays.asList(expected), order);

		assertEquals(enumerate(Arrays.asList(expected)), enumerate(order));
	}

	private static String enumerate(List<String> list) {
		StringBuilder buf= new StringBuilder();
		for (String s : list) {
			buf.append(s).append('\n');
		}
		return buf.toString();
	}

	/*
	private void printOrder(List order) {
		for (int i= 0; i < order.size(); i++) {
			String s= (String)order.get(i);
			System.out.println(s);
		}
	}
	*/

	private void collectOrder(Test suite, List<String> order) {
		if (suite instanceof TestCase) {
			String s= suite.toString();
			s= s.substring(0, s.indexOf('('));
			order.add(s);
		} else if (suite instanceof TestSuite) {
			TestSuite aSuite= (TestSuite)suite;
			for (Enumeration<Test> e= aSuite.tests(); e.hasMoreElements();) {
				Test test= e.nextElement();
				collectOrder(test, order);
			}
		} else if (suite instanceof TestDecorator) {
			TestDecorator aDecorator= (TestDecorator)suite;
			collectOrder(aDecorator.getTest(), order);
		}
	}

	private Test prioritize(Test suite, String[] priority) {
		FailuresFirstPrioritizer prioritizer= new FailuresFirstPrioritizer(priority);
		return prioritizer.prioritize(suite);
	}
}
