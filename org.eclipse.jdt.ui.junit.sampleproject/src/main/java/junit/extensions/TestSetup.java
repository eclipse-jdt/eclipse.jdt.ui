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
 * A Decorator to set up and tear down additional fixture state. Subclass
 * TestSetup and insert it into your tests when you want to set up additional
 * state once before the tests are run.
 */
public class TestSetup extends TestDecorator {

	public TestSetup(Test test) {
		super(test);
	}

	public void run(final TestResult result) {
		Protectable p = new Protectable() {
			public void protect() throws Exception {
				setUp();
				basicRun(result);
				tearDown();
			}
		};
		result.runProtected(this, p);
	}

	/**
	 * Sets up the fixture. Override to set up additional fixture state.
	 */
	protected void setUp() throws Exception {
	}

	/**
	 * Tears down the fixture. Override to tear down the additional fixture state.
	 */
	protected void tearDown() throws Exception {
	}
}
