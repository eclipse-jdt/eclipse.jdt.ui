package junit.extensions;

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
 * A Decorator for Tests. Use TestDecorator as the base class for defining new
 * test decorators. Test decorator subclasses can be introduced to add behaviour
 * before or after a test is run.
 *
 */
public class TestDecorator extends Assert implements Test {
	protected Test fTest;

	public TestDecorator(Test test) {
		fTest = test;
	}

	/**
	 * The basic run behaviour.
	 */
	public void basicRun(TestResult result) {
		fTest.run(result);
	}

	public int countTestCases() {
		return fTest.countTestCases();
	}

	public void run(TestResult result) {
		basicRun(result);
	}

	public String toString() {
		return fTest.toString();
	}

	public Test getTest() {
		return fTest;
	}
}
