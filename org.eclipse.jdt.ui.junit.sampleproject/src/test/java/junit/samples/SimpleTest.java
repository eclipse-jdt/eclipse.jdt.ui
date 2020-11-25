package junit.samples;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import junit.framework.*;

/**
 * Some simple tests.
 *
 */
public class SimpleTest extends TestCase {
	protected int fValue1;
	protected int fValue2;

	protected void setUp() {
		fValue1 = 2;
		fValue2 = 3;
	}

	public static Test suite() {

		/*
		 * the type safe way
		 *
		 * TestSuite suite= new TestSuite(); suite.addTest( new SimpleTest("add") {
		 * protected void runTest() { testAdd(); } } );
		 * 
		 * suite.addTest( new SimpleTest("testDivideByZero") { protected void runTest()
		 * { testDivideByZero(); } } ); return suite;
		 */

		/*
		 * the dynamic way
		 */
		return new TestSuite(SimpleTest.class);
	}

	public void testAdd() {
		double result = fValue1 + fValue2;
		// forced failure result == 5
		assertTrue(result == 5);
	}

	public void _testDivideByZero() {
		int zero = 0;
		int result = 8 / zero;
	}

	public void testEquals() {
		assertEquals(12, 12);
		assertEquals(12L, 12L);
		assertEquals(new Long(12), new Long(12));

//		assertEquals("Size", 12, 13);
//		assertEquals("Capacity", 12.0, 11.99, 0.0);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
