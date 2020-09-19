/*******************************************************************************
 * Copyright (c) 2016, 2020 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ConvertAnonymousToNestedTests1d8 extends ConvertAnonymousToNestedTests {
	private static final String REFACTORING_PATH= "ConvertAnonymousToNested18/";

	public ConvertAnonymousToNestedTests1d8() {
		super(new Java1d8Setup());
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//--- TESTS

	@Test
	public void testEffectivelyFinal1() throws Exception{
		helper1(5, 15, 5, 17, true, "Inner", Modifier.PRIVATE);
	}
}
