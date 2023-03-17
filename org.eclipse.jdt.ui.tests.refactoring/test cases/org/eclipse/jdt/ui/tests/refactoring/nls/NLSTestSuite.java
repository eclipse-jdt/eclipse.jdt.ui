/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
// old
		NLSElementTester.class,
		NLSScannerTester.class,
		CellEditorTester.class,

// new
		NlsRefactoringCheckInitialConditionsTest.class,
		NlsRefactoringCheckFinalConditionsTest.class,
		NlsRefactoringCreateChangeTest.class,
		NLSSourceModifierTest.class,
		NLSSourceModifierTest1d8.class,
		NLSHintTest.class,
		NLSHintHelperTest.class,
		PropertyFileDocumentModellTest.class,
		SimpleLineReaderTest.class,
		NLSHolderTest.class,
		NLSSubstitutionTest.class
})
public class NLSTestSuite {
}
