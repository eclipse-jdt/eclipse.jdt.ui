/*******************************************************************************
 * Copyright (c) 2017 Simeon Andreev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		return suite;
	}
}
