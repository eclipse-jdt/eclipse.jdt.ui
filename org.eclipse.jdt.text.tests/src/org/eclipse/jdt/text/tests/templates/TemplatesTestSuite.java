/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.templates;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Template test suite.
 *
 * @since 3.4
 */
public class TemplatesTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(TemplatesTestSuite.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(TemplateContributionTest.suite());
		//$JUnit-END$
		return suite;
	}
}
