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
package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All Action Tests");
		suite.addTest(DeleteSourceReferenceEditTests.suite());

//      FIX ME - randomly fails on linux
//		suite.addTest(PasteSourceReferenceActionTests.suite());

		suite.addTest(DeleteSourceReferenceActionTests.suite());
		suite.addTest(StructureSelectionActionTests.suite());
		suite.addTest(CopyResourcesToClipboardActionTest.suite());
		suite.addTest(PasteResourcesFromClipboardActionTest.suite());
		suite.addTest(GoToNextPreviousMemberActionTests.suite());
		return suite;
	}

}


