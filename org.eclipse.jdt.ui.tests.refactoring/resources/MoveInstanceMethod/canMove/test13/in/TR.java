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

public class TR {

	/**
	 * Run the test case
	 * @param test
	 */
	protected void run(final TC test) {
		startTest(test);
		P p= new P() {
			public void protect() throws Throwable {
				test.runBare();
			}
		};
		runProtected(test, p);
			// Some comment
		endTest(test);
	}

	private void runProtected(TC test, P p) {
	}

	private void endTest(TC test) {
	}

	private void startTest(TC test) {
	}

}