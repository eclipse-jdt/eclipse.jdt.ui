/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import org.junit.Test;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

public class IntroduceFactoryTests16 extends IntroduceFactoryTestsBase {
	private static final String REFACTORING_PATH= "IntroduceFactory/";

	public IntroduceFactoryTests16() {
		rts= new Java16Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//--- TESTS
	@Test
	public void test568987() throws Exception {
		singleUnitBugHelper("RecCanConst", false);
	}

	@Test
	public void test566943() throws Exception {
		singleUnitBugHelper("RecCompConst", false);
	}
}
