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

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
public class InlineConstantTests1d7 extends InlineConstantTests {
	private static final Class<InlineConstantTests1d7> clazz = InlineConstantTests1d7.class;

	public InlineConstantTests1d7(String name) {
		super(name);
	}

	@Override
	protected String successPath() {
		return toSucceed ? "/canInline17/" : "/cannotInline17/";
	}

	public static Test suite() {
		return new Java1d7Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java1d7Setup(someTest);
	}


	//--- TESTS

	@Override
	public void test0() throws Exception {
		helper1("p.C", 5, 28, 5, 33, true, false);
	}
}
