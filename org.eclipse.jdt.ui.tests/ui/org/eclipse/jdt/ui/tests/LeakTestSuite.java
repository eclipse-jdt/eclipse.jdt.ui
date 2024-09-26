/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests;



/**
 * Test for leaks
 */
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import org.eclipse.jdt.ui.tests.leaks.JavaLeakTest;
import org.eclipse.jdt.ui.tests.leaks.TextViewerUndoManagerLeakTest;
import org.eclipse.jdt.ui.tests.leaks.UndoManagerLeakTest;
import org.eclipse.jdt.ui.tests.search.SearchLeakTestWrapper;

@Suite
@SelectClasses({
	JavaLeakTest.class,
	SearchLeakTestWrapper.class,
	UndoManagerLeakTest.class,
	TextViewerUndoManagerLeakTest.class,
})
public class LeakTestSuite {
}

