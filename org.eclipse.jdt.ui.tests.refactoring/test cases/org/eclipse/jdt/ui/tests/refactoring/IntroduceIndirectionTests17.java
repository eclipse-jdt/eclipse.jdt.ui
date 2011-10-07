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

public class IntroduceIndirectionTests17 extends IntroduceIndirectionTests{
	private static final Class clazz= IntroduceIndirectionTests17.class;

	public IntroduceIndirectionTests17(String name) {
		super(name);
	}

	public static Test setUpTest(Test test) {
		return new Java17Setup(test);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

// ---
	
	public void test17_32() throws Exception {
		// test for bug 349405
		helperPass(new String[] { "p.Foo" }, "foo", "p.Foo", 10, 17, 10, 20);
	}
	
	public void test17_33() throws Exception {
		// test for bug 349405
		helperPass(new String[] { "p.Foo" }, "getX", "p.Foo", 14, 17, 14, 21);
	}
	
	public void test17_34() throws Exception {
		// test for bug 
		helperFail(new String[] { "p.Foo" }, "m2", "p.Foo", 7, 18, 7, 18);
	}

}
