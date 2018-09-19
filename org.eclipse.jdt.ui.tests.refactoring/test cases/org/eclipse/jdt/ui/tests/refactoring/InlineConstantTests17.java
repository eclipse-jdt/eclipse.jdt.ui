/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import junit.framework.Test;

public class InlineConstantTests17 extends InlineConstantTests {
	private static final Class<InlineConstantTests17> clazz = InlineConstantTests17.class;

	public InlineConstantTests17(String name) {
		super(name);
	}

	@Override
	protected String successPath() {
		return toSucceed ? "/canInline17/" : "/cannotInline17/";
	}

	public static Test suite() {
		return new Java17Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java17Setup(someTest);
	}


	//--- TESTS

	@Override
	public void test0() throws Exception {
		helper1("p.C", 5, 28, 5, 33, true, false);
	}
}
