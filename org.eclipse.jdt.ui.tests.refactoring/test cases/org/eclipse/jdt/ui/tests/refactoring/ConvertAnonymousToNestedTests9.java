/*******************************************************************************
 * Copyright (c) 2021 Till Brychcy and others.
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
 *     Red Hat Inc. - modified to test on Java 9
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java9Setup;

/**
 * Those tests are made to run on Java 9.
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ConvertAnonymousToNestedTests9 extends ConvertAnonymousToNestedTests {
	private static final String REFACTORING_PATH= "ConvertAnonymousToNested9/";

	public ConvertAnonymousToNestedTests9() {
		super(new Java9Setup());
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//--- TESTS

	@Test
	public void testTypeParameters1() throws Exception{
		helper1(8, 30, 8, 38, true, "CallableImplementation", Modifier.PRIVATE);
	}
}
