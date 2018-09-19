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
import org.eclipse.jdt.ui.tests.buildpath.BuildpathTestSuite;
import org.eclipse.jdt.ui.tests.callhierarchy.CallHierarchyContentProviderTest;
import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.jarexport.JarExportTests;
import org.eclipse.jdt.ui.tests.model.ContentProviderTests;
import org.eclipse.jdt.ui.tests.packageHover.PackageJavadocTests;
import org.eclipse.jdt.ui.tests.packageview.PackageExplorerTests;
import org.eclipse.jdt.ui.tests.quickfix.QuickFixTest;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTests;
import org.eclipse.jdt.ui.tests.search.SearchTest;
import org.eclipse.jdt.ui.tests.wizardapi.ImporterTest;
import org.eclipse.jdt.ui.tests.wizardapi.NewJavaProjectWizardTest;
import org.eclipse.jdt.ui.tests.wizardapi.NewTypeWizardTest;


/**
 * Test all areas of JDT UI.
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

	public AutomatedSuite() {
		super(AutomatedSuite.class.getName());
		
		addTest(CoreTests.suite());
		addTest(QuickFixTest.suite());

		addTest(NewJavaProjectWizardTest.suite());
		addTest(NewTypeWizardTest.suite());
		addTest(ImporterTest.suite());
		
		addTest(PackageExplorerTests.suite());

		addTest(PackagesViewContentProviderTests.suite());
		addTest(PackagesViewContentProviderTests2.suite());
		addTest(PackagesViewDeltaTests.suite());

		addTest(ContentProviderTests.suite());

		addTest(CallHierarchyContentProviderTest.suite());

		addTest(RefactoringTests.suite());

		addTest(SearchTest.suite());
		addTest(JUnitJUnitTests.suite());

		addTest(BuildpathTestSuite.suite());

		addTest(JarExportTests.suite());
		addTest(PackageJavadocTests.suite());
	}
}

