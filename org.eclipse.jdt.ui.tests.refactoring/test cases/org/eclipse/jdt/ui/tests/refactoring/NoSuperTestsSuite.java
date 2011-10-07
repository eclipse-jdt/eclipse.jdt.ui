/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite that only includes tests from the given test class, but not
 * tests from super classes.
 * 
 * @since 3.7
 */
public class NoSuperTestsSuite extends TestSuite {
	/**
	 * This implementation creates unnecessary test objects for tests from super classes
	 * (in {@link junit.framework.TestSuite#addTestMethod}).
	 * Alternative would have been to copy most of the implementation of TestSuite.
	 */
	
	private static final Class<? extends Test> WARNING_TEST_CLASS= warning(null).getClass();

	public NoSuperTestsSuite(Class<? extends Test> theClass) {
		super(theClass);
	}
	
	/**
	 * Adds the given test to this suite, but only if the test was declared in
	 * the test object's class (and not in a superclass).
	 */
	@Override
	public void addTest(Test test) {
		if (test instanceof TestCase) {
			TestCase testCase= (TestCase) test;
			Class<? extends TestCase> testClass= testCase.getClass();
			try {
				testClass.getDeclaredMethod(testCase.getName());
			} catch (NoSuchMethodException e) {
				if (testClass != WARNING_TEST_CLASS)
					return;
			}
			super.addTest(test);
		}
	}
	
	@Override
	public void addTestSuite(Class<? extends TestCase> testClass) {
		super.addTest(new NoSuperTestsSuite(testClass));
	}
}
