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
package org.eclipse.jdt.ui.tests.refactoring;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	AllRefactoringTests.class,
	AllChangeTests.class,
	UndoManagerTests.class,
	PathTransformationTests.class,
	RefactoringScannerTests.class,
	SurroundWithTests.class,
	SurroundWithTests1d7.class,
	SurroundWithTests1d8.class,
})
public class AllTests {
}