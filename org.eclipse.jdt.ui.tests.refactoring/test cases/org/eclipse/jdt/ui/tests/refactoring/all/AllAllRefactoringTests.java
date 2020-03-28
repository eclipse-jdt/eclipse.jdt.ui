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
package org.eclipse.jdt.ui.tests.refactoring.all;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	org.eclipse.jdt.ui.tests.refactoring.actions.AllTests.class,
	org.eclipse.jdt.ui.tests.refactoring.nls.NLSTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.AllTests.class,
	org.eclipse.jdt.ui.tests.refactoring.extensions.AllTests.class,
	org.eclipse.jdt.ui.tests.refactoring.changes.AllTests.class,
	org.eclipse.jdt.ui.tests.refactoring.ccp.AllTests.class,
	org.eclipse.jdt.ui.tests.refactoring.typeconstraints.AllTests.class
})
public class AllAllRefactoringTests {
}

