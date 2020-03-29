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
	RefactoringScannerTests.class,
	RenamingNameSuggestorTests.class,

	RenameVirtualMethodInClassTests.class,
	RenameMethodInInterfaceTests.class,
	RenamePrivateMethodTests.class,
	RenameStaticMethodTests.class,
	RenameParametersTests.class,
	RenameTypeTests.class,
	RenamePackageTests.class,
	RenamePrivateFieldTests.class,
	RenameTypeParameterTests.class,
	RenameNonPrivateFieldTests.class,
	RenameJavaProjectTests.class,
	RenameTests18.class

	//XXX: NOT part of AllRefactoringTests. Also add suites there!
})
public class RenameTests {
}

