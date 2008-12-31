/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All Reorg Tests");
		suite.addTest(DeleteTest.suite());
		suite.addTest(CopyToClipboardActionTest.suite());
		suite.addTest(PasteActionTest.suite());
		suite.addTest(CopyTest.suite());
		suite.addTest(MoveTest.suite());
		suite.addTest(MultiMoveTest.suite());

		//------old reorg tests
		suite.addTest(CopyResourcesToClipboardActionTest.suite());

		return suite;
	}
}
