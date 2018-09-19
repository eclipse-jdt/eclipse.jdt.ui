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
	 * @return Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 */
	public static Test suite() {
		return new UIInteractiveSuite();
	}

	public UIInteractiveSuite() {
		super(UIInteractiveSuite.class.getName());
		addTest(PreferencesTest.suite());
		addTest(WizardsTest.suite());
		addTest(DialogsTest.suite());
	}

}
