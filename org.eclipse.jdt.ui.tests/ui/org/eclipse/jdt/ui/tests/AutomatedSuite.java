/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.eclipse.jdt.junit.tests.JUnitJUnitTests;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.browsing.PackagesViewContentProviderTests;
import org.eclipse.jdt.ui.tests.browsing.PackagesViewContentProviderTests2;
import org.eclipse.jdt.ui.tests.browsing.PackagesViewDeltaTests;
import org.eclipse.jdt.ui.tests.buildpath.BuildpathTestSuite;
import org.eclipse.jdt.ui.tests.callhierarchy.CallHierarchyContentProviderTest;
import org.eclipse.jdt.ui.tests.core.CoreTestSuite;
import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.hover.JavadocHoverTests;
import org.eclipse.jdt.ui.tests.hover.PackageJavadocTests;
import org.eclipse.jdt.ui.tests.jarexport.JarExportTests;
import org.eclipse.jdt.ui.tests.model.ContentProviderTests;
import org.eclipse.jdt.ui.tests.packageview.PackageExplorerTests;
import org.eclipse.jdt.ui.tests.quickfix.QuickFixTestSuite;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTests;
import org.eclipse.jdt.ui.tests.search.SearchTest;
import org.eclipse.jdt.ui.tests.wizardapi.ImporterTest;
import org.eclipse.jdt.ui.tests.wizardapi.NewJavaProjectWizardTest;
import org.eclipse.jdt.ui.tests.wizardapi.NewTypeWizardTest;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Test all areas of JDT UI.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CoreTests.class,
	CoreTestSuite.class,
	QuickFixTestSuite.class,

	NewJavaProjectWizardTest.class,
	NewTypeWizardTest.class,
	ImporterTest.class,

	PackageExplorerTests.class,

	PackagesViewContentProviderTests.class,
	PackagesViewContentProviderTests2.class,
	PackagesViewDeltaTests.class,

	ContentProviderTests.class,

	CallHierarchyContentProviderTest.class,

	RefactoringTests.class,

	SearchTest.class,
	JUnitJUnitTests.class,

	BuildpathTestSuite.class,

	JarExportTests.class,
	PackageJavadocTests.class,
	JavadocHoverTests.class
})
public class AutomatedSuite {
	@Before
	protected void setUp() throws Exception {
		JavaCore.setOptions(TestOptions.getDefaultOptions());
		TestOptions.initializeCodeGenerationOptions();
		// Use load since restore doesn't really restore the defaults.
		JavaPlugin.getDefault().getCodeTemplateStore().load();
	}
}
