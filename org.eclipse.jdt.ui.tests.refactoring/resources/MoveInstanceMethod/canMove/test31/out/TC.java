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

import static java.lang.Math.cos;
import java.util.List;

public class TC {
	public void runBare() {
	}

	/**
	 * Runs the test
	 * @param tr TODO
	 */
	protected void run(final TR tr) {
		List<Integer> integers= null;
		tr.startTest(this);
		P p= new P() {
			public void protect() throws Throwable {
				runBare();
				tr.handleRun(TC.this);
				double d= cos(0);
			}
		};
		tr.runProtected(this, p);
	
		tr.endTest(this);
	}
}