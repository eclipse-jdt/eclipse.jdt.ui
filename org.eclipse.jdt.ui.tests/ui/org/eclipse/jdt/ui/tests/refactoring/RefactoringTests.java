/*******************************************************************************
 * Copyright (c) 2017 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RefactoringTests {

	public static Test suite() {
		TestSuite suite= new TestSuite(RefactoringTests.class.getName());
		suite.addTestSuite(IsCompletelySelectedTest.class);
		suite.addTestSuite(ParentCheckerTest.class);
		return suite;
	}
}
