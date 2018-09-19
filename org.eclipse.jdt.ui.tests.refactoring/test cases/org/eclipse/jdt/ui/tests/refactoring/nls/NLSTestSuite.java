/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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


import junit.framework.Test;
import junit.framework.TestSuite;


public class NLSTestSuite {

	public static Test suite ( ) {
        TestSuite suite = new TestSuite(NLSTestSuite.class.getName());
        // old
        suite.addTest(NLSElementTester.suite());
		suite.addTest(NLSScannerTester.suite());
		suite.addTest(CellEditorTester.suite());

		// new
		suite.addTest(NlsRefactoringCheckInitialConditionsTest.suite());
		suite.addTest(NlsRefactoringCheckFinalConditionsTest.suite());
		suite.addTest(NlsRefactoringCreateChangeTest.suite());
		suite.addTest(NLSSourceModifierTest.suite());
		suite.addTest(NLSHintTest.suite());
        suite.addTest(NLSHintHelperTest.suite());
        suite.addTest(PropertyFileDocumentModellTest.suite());
        suite.addTest(SimpleLineReaderTest.suite());
        suite.addTest(NLSHolderTest.suite());
        suite.addTest(NLSSubstitutionTest.suite());

        return suite;
	}
}


