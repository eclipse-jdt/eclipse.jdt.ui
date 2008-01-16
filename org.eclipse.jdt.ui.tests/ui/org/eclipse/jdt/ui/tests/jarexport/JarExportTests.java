/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		TestSuite suite= new TestSuite("Test for org.eclipse.jdt.ui.tests.jarexport");
		//$JUnit-BEGIN$
		suite.addTest(PlainJarExportTests.suite());
		suite.addTest(FatJarExportTests.suite());
		//$JUnit-END$
		return suite;
	}
}
