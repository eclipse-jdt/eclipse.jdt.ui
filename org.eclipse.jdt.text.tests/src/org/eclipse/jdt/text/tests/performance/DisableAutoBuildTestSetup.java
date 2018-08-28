/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;

import org.eclipse.jdt.internal.ui.util.CoreUtility;

import junit.extensions.TestSetup;
import junit.framework.Test;


/**
 * Disables automatic builds.
 *
 * @since 3.1
 */
public class DisableAutoBuildTestSetup extends TestSetup {

	private boolean fWasAutobuilding;

	public DisableAutoBuildTestSetup(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fWasAutobuilding= CoreUtility.setAutoBuilding(false);
	}

	@Override
	protected void tearDown() throws Exception {
		if (fWasAutobuilding) {
			ResourceTestHelper.fullBuild();
			CoreUtility.setAutoBuilding(true);
		}
		super.tearDown();
	}
}
