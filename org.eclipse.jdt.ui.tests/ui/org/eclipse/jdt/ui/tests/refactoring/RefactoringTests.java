/*******************************************************************************
 * Copyright (c) 2017, 2020 Simeon Andreev and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	IsCompletelySelectedTest.class,
	ParentCheckerTest.class
})
public class RefactoringTests {
}
