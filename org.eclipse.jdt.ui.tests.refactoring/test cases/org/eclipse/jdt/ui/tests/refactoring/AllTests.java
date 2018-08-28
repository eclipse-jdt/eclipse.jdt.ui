/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	private static final Class<AllTests> clazz= AllTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(AllRefactoringTests.suite());
		suite.addTest(AllChangeTests.suite());
		suite.addTest(UndoManagerTests.suite());
		suite.addTest(PathTransformationTests.suite());
		suite.addTest(RefactoringScannerTests.suite());
		suite.addTest(SurroundWithTests.suite());
		suite.addTest(SurroundWithTests17.suite());
		suite.addTest(SurroundWithTests18.suite());
		return suite;
	}
}

