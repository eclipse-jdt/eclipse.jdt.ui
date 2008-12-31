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
package org.eclipse.jdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.tests.leaks.JavaLeakTest;
import org.eclipse.jdt.ui.tests.leaks.TextViewerUndoManagerLeakTest;
import org.eclipse.jdt.ui.tests.leaks.UndoManagerLeakTest;
import org.eclipse.jdt.ui.tests.search.SearchLeakTestWrapper;


/**
 * Test for leaks
 */
public class LeakTestSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 *
	 * @return the test suite.
	 */
	public static Test suite() {
		return new LeakTestSuite();
	}

	/**
	 * Construct the test suite.
	 */
	public LeakTestSuite() {
		addTest(JavaLeakTest.suite());
		addTest(SearchLeakTestWrapper.suite());
		addTest(UndoManagerLeakTest.suite());
		addTest(TextViewerUndoManagerLeakTest.suite());
	}

}

