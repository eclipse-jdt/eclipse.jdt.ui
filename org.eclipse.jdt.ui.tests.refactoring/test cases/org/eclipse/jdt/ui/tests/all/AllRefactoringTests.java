/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.all;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.TestOptionsSetup;

public class AllRefactoringTests {
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All Refactoring Tests");
		suite.addTest(org.eclipse.jdt.ui.tests.actions.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.nls.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.extensions.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.changes.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.reorg.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.typeconstraints.AllTests.suite());
	    return new TestOptionsSetup(suite);
	}
}

