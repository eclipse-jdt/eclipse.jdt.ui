/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.buildpath;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @since 3.5
 */
public class BuildpathTestSuite {

	public static Test suite() {
		TestSuite suite= new TestSuite(BuildpathTestSuite.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(BuildpathModifierActionEnablementTest.suite());
		suite.addTest(BuildpathModifierActionTest.suite());
		suite.addTest(CPUserLibraryTest.suite());
		suite.addTest(BuildpathProblemQuickFixTest.suite());
		//$JUnit-END$
		return suite;
	}

}
