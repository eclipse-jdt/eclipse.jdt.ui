/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

public class MoveMembersTests18 extends MoveMembersTests {

	private static final Class clazz= MoveMembersTests18.class;

	public MoveMembersTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	public void test63() throws Exception {
		// move private static method from class to interface, update visibility
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void test64() throws Exception {
		// move static method from interface to another interface
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void test65() throws Exception {
		// move static method from interface to class, add 'public'
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}
}
