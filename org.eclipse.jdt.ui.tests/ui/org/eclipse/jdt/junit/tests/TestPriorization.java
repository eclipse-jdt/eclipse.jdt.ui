/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import junit.extensions.TestDecorator;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.jdt.internal.junit.runner.FailuresFirstPrioritizer;

public class TestPriorization extends TestCase {

	public void testReorderSimple() {
		TestSuite suite= createSuiteDEF();
		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test prioritized= prioritize(suite, priority);
		List order= new ArrayList();
		collectOrder(prioritized, order);
		String[] expected= {
				"testF", "testD", "testE"
		};
		assertTrue(checkOrder(expected, order));
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
		
		List order= new ArrayList();
		collectOrder(prioritized, order);
		String[] expected= {
				"testF", "testD", "testE"
		};
		//printOrder(order);
		assertTrue(checkOrder(expected, order));
	}

	public void testReorderSimpleWithDecorator() {
		// suite
		//		decorator
		//			D
		//			E
		//			F
		
		Test suite= new TestSetup(createSuiteDEF()) {
			protected void setUp() throws Exception {}
		};
			
		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite, priority);
		List order= new ArrayList();
		collectOrder(reordered, order);
		String[] expected= {
				"testF", "testD", "testE"
		};
		assertTrue(checkOrder(expected, order));
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
		List order= new ArrayList();
		collectOrder(reordered, order);
	
		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC", 
		};
		assertTrue(checkOrder(expected, order));
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
			protected void setUp() throws Exception {
			}
		});
		
		String[] priority= {
				"testF(org.eclipse.jdt.junit.tests.TestPriorizationSuite2)"
		};
		Test reordered= prioritize(suite, priority);
		List order= new ArrayList();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC", 
		};
		assertTrue(checkOrder(expected, order));
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
		List order= new ArrayList();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC", "testX", "testY", "testZ",
		};
		assertTrue(checkOrder(expected, order));
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
		List order= new ArrayList();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testE", "testD", "testA", "testB", "testC", "testX", "testY", "testZ",
		};
		//printOrder(order);
		assertTrue(checkOrder(expected, order));
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
		List order= new ArrayList();
		collectOrder(reordered, order);

		String[] expected= {
				"testF", "testD", "testE", "testA", "testB", "testC", "testZ", "testX", "testY",
		};
		assertTrue(checkOrder(expected, order));
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
		List order= new ArrayList();
		collectOrder(reordered, order);

		String[] expected2= {
				"testF", "testD", "testE", "testA", "testB", "testC", "testZ", "testX", "testY"
		};
		assertTrue(checkOrder(expected2, order));
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


	private boolean checkOrder(String[] expected, List order) {
		for (int i= 0; i < expected.length; i++) {
			String s= (String)order.get(i);
			s= s.substring(0, s.indexOf('('));
			if (!s.equals(expected[i]))
				return false;
		}
		return true;
	}

	private void printOrder(List order) {
		for (int i= 0; i < order.size(); i++) {
			String s= (String)order.get(i);
			System.out.println(s);
		}
	}

	private void collectOrder(Test suite, List order) {
		if (suite instanceof TestCase) {
			order.add(suite.toString());
		} else if (suite instanceof TestSuite) {
			TestSuite aSuite= (TestSuite)suite;
			for (Enumeration e= aSuite.tests(); e.hasMoreElements();) {
				Test test= (Test)e.nextElement();
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
