/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package p1;
public enum TR implements P {
	PASSED, FAILED;
	/**
	 * Runs the test
	 * @param test the test to run
	 * @deprecated Use {@link p1.TC#run(p1.TR)} instead
	 */
	protected void run(final TC test) {
		test.run(this);
	}

	void handleRun(TC test) {
	}

	void runProtected(TC test, P p) {
	}

	void endTest(TC test) {
	}

	void startTest(TC test) {
	}

}