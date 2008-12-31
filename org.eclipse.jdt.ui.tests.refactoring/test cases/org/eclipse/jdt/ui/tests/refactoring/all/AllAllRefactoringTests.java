/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.all;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.TestOptionsSetup;

public class AllAllRefactoringTests {
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All All Refactoring Tests");
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.actions.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.nls.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.extensions.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.changes.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.ccp.AllTests.suite());
		suite.addTest(org.eclipse.jdt.ui.tests.refactoring.typeconstraints.AllTests.suite());
	    return new TestOptionsSetup(suite);
	}
}

