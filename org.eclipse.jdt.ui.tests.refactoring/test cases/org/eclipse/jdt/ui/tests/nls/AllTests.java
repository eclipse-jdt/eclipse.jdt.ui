/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.nls;


import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {
	
	public static Test suite ( ) {
        TestSuite suite = new TestSuite();
        // old
        suite.addTest(NLSElementTester.suite());
		suite.addTest(NLSScannerTester.suite());
		suite.addTest(CellEditorTester.suite());
		suite.addTest(OrderedMapTester.suite());

		// new
        suite.addTest(NlsRefactoringCheckInitialConditionsTest.allTests());
        suite.addTest(NlsRefactoringCheckFinalConditionsTest.allTests());
        suite.addTest(NlsRefactoringCreateChangeTest.allTests());
        suite.addTest(NLSSourceModifierTest.allTests());
        suite.addTest(NLSHintTest.allTests());
        suite.addTest(PropertyFileDocumentModellTest.suite());
        suite.addTest(SimpleLineReaderTest.suite());
        suite.addTest(NLSHolderTest.suite());
        suite.addTest(NLSSubstitutionTest.suite());

        return suite;
	}
}


