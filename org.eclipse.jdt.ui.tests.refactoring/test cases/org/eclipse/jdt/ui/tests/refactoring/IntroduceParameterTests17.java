/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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
}
