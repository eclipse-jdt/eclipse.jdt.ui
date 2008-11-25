/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 83258 [jar exporter] Deploy java application as executable jar
 *******************************************************************************/
package org.eclipse.jdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.junit.tests.JUnitJUnitTests;
import org.eclipse.jdt.testplugin.TestOptionsSetup;

import org.eclipse.jdt.ui.tests.browsing.PackagesViewContentProviderTests;
import org.eclipse.jdt.ui.tests.browsing.PackagesViewContentProviderTests2;
import org.eclipse.jdt.ui.tests.browsing.PackagesViewDeltaTests;
import org.eclipse.jdt.ui.tests.buildpath.BuildpathModifierActionTest;
import org.eclipse.jdt.ui.tests.callhierarchy.CallHierarchyContentProviderTest;
import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.jarexport.JarExportTests;
import org.eclipse.jdt.ui.tests.model.ContentProviderTests;
import org.eclipse.jdt.ui.tests.packageview.PackageExplorerTests;
import org.eclipse.jdt.ui.tests.quickfix.QuickFixTest;
import org.eclipse.jdt.ui.tests.search.SearchTest;
import org.eclipse.jdt.ui.tests.wizardapi.NewJavaProjectWizardTest;
import org.eclipse.jdt.ui.tests.wizardapi.NewTypeWizardTest;


/**
 * Test all areas of the UI.
 */
public class AutomatedSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 *
	 * @return the test
	 */
	public static Test suite() {
		return new TestOptionsSetup(new AutomatedSuite());
	}

	/**
	 * Construct the test suite.
	 */
	public AutomatedSuite() {
		addTest(CoreTests.suite());
		addTest(QuickFixTest.suite());

		addTest(NewJavaProjectWizardTest.suite());
		addTest(NewTypeWizardTest.suite());

		addTest(PackageExplorerTests.suite());

		addTest(PackagesViewContentProviderTests.suite());
		addTest(PackagesViewContentProviderTests2.suite());
		addTest(PackagesViewDeltaTests.suite());

		addTest(ContentProviderTests.suite());

		addTest(CallHierarchyContentProviderTest.suite());

		addTest(SearchTest.suite());
		addTest(JUnitJUnitTests.suite());

		addTest(BuildpathModifierActionTest.suite());

		addTest(JarExportTests.suite());
	}
}

