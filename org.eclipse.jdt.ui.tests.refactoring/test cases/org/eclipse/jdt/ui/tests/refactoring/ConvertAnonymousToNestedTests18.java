/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
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

import org.eclipse.jdt.core.dom.Modifier;

import junit.framework.Test;

public class ConvertAnonymousToNestedTests18 extends ConvertAnonymousToNestedTests {

	private static final Class<ConvertAnonymousToNestedTests18> clazz= ConvertAnonymousToNestedTests18.class;
	private static final String REFACTORING_PATH= "ConvertAnonymousToNested18/";

	public ConvertAnonymousToNestedTests18(String name) {
		super(name);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public static Test suite() {
		return new Java18Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}


	//--- TESTS

	public void testEffectivelyFinal1() throws Exception{
		helper1(5, 15, 5, 17, true, "Inner", Modifier.PRIVATE);
	}
}
