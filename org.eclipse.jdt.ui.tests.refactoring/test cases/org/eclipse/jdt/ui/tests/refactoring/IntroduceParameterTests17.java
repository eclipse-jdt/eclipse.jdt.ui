/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

public class IntroduceParameterTests17 extends IntroduceParameterTests {

	private static final Class clazz= IntroduceParameterTests17.class;

	public IntroduceParameterTests17(String name) {
		super(name);
	}

	public static Test setUpTest(Test test) {
		return new Java17Setup(test);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

// ---

	public void testSimple17_Catch1() throws Exception {
		performOK();
	}

	public void testSimple17_Catch2() throws Exception {
		performOK();
	}
	
	public void testSimple17_NewInstance2() throws Exception {
		performOK();
	}
	
	public void testSimple17_NewInstance3() throws Exception {
		performOK();
	}
	
	public void testSimple17_NewInstance4() throws Exception {
		performOK();
	}
	
}
