/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

public class InlineConstantTests17 extends InlineConstantTests {
	private static final Class clazz = InlineConstantTests17.class;

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

	public void test0() throws Exception {
		helper1("p.C", 5, 28, 5, 33, true, false);
	}
}
