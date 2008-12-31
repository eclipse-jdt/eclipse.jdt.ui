/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RenameFieldTests {

	private static final Class clazz= RenameFieldTests.class;

	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(RenameNonPrivateFieldTests.suite());
		suite.addTest(RenamePrivateFieldTests.suite());
		return suite;
	}
}
