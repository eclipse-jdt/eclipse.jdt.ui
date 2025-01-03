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

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
	org.eclipse.jdt.ui.tests.refactoring.actions.RefactoringActionsTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.nls.NLSTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.extensions.RefactoringExtensionPointTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.changes.RefactoringChangesTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.ccp.RefactoringCCPTestSuite.class,
	org.eclipse.jdt.ui.tests.refactoring.typeconstraints.RefactoringTypeContraintTestSuite.class
})
public class AllRefactoringTestsSuite {
}

