/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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
	/*
	 * Implementation creates a few unnecessary test objects.
	 * Alternative would have been to copy most of the implementation of TestSuite.
	 */
	
	public NoSuperTestsSuite(Class<? extends Test> theClass) {
		super(theClass);
	}
	
	@Override
	public void addTest(Test test) {
		if (test instanceof TestCase) {
			TestCase testCase= (TestCase) test;
			Class<? extends TestCase> testClass= testCase.getClass();
			try {
				testClass.getDeclaredMethod(testCase.getName());
			} catch (NoSuchMethodException e) {
				if (testClass != warning(null).getClass())
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
