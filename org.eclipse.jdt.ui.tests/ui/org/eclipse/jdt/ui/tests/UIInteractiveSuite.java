/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.tests.dialogs.DialogsTest;
import org.eclipse.jdt.ui.tests.dialogs.PreferencesTest;
import org.eclipse.jdt.ui.tests.dialogs.WizardsTest;

/**
 * Test all areas of the UI.
 */
public class UIInteractiveSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 */
	public static Test suite() {
		return new UIInteractiveSuite();
	}

	/**
	 * Construct the test suite.
	 */
	public UIInteractiveSuite() {
		addTest(PreferencesTest.suite());
		addTest(WizardsTest.suite());
		addTest(DialogsTest.suite());
	}
	
}