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
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.jarexport;

import junit.framework.Test;
import junit.framework.TestSuite;

public class JarExportTests {

	public static Test suite() {
		TestSuite suite= new TestSuite(JarExportTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTest(PlainJarExportTests.suite());
		suite.addTest(FatJarExportTests.suite());
		//$JUnit-END$
		return suite;
	}
}
