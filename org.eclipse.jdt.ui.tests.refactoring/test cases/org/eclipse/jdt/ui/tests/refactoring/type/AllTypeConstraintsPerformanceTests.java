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
package org.eclipse.jdt.ui.tests.refactoring.type;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTypeConstraintsPerformanceTests {

	public static Test suite() {
		TestSuite suite= new TestSuite(AllTypeConstraintsPerformanceTests.class.getName());
		suite.addTest(ExtractInterfacePerfAcceptanceTests.suite());
		return suite;
	}
}
