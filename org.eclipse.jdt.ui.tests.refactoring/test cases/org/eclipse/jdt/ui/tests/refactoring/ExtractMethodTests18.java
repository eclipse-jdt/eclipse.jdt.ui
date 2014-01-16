/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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

import org.eclipse.jdt.core.dom.Modifier;

public class ExtractMethodTests18 extends ExtractMethodTests {
	private static ExtractMethodTestSetup18 fgTestSetup;

	public ExtractMethodTests18(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup18(new NoSuperTestsSuite(ExtractMethodTests18.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup18(test);
		return fgTestSetup;
	}

	protected void defaultMethodsTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup.getDefaultMethodsPackage(), "A", COMPARE_WITH_OUTPUT, "defaultMethods18_out", null, null, destination, visibility);
	}

	protected void staticMethodsTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup.getStaticMethodsPackage(), "A", COMPARE_WITH_OUTPUT, "staticMethods18_out", null, null, destination, visibility);
	}

	protected void destinationTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup.getDestinationPackage(), "A", COMPARE_WITH_OUTPUT, "destination18_out", null, null, destination, visibility);
	}

	//====================================================================================
	// Testing Default Methods
	//====================================================================================

	public void test1() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	public void test1a() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	public void test2() throws Exception {
		defaultMethodsTest(1, Modifier.PROTECTED);
	}

	public void test2a() throws Exception {
		defaultMethodsTest(1, Modifier.PROTECTED);
	}

	public void test3() throws Exception {
		defaultMethodsTest(1, Modifier.PUBLIC);
	}

	public void test3a() throws Exception {
		defaultMethodsTest(1, Modifier.PUBLIC);
	}

	public void test4() throws Exception {
		defaultMethodsTest(1, Modifier.PUBLIC);
	}

	public void test5() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	//====================================================================================
	// Testing Static Methods
	//====================================================================================

	public void test101() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	public void test102() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	public void test103() throws Exception {
		staticMethodsTest(0, Modifier.PUBLIC);
	}

	public void test104() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	public void test105() throws Exception {
		staticMethodsTest(2, Modifier.PUBLIC);
	}

	public void test106() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	public void test107() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	//====================================================================================
	// Testing Destination Types
	//====================================================================================

	public void test201() throws Exception {
		destinationTest(0, Modifier.PUBLIC);
	}

	public void test202() throws Exception {
		destinationTest(0, Modifier.PUBLIC);
	}

	public void test203() throws Exception {
		destinationTest(1, Modifier.PROTECTED);
	}

	public void test204() throws Exception {
		destinationTest(1, Modifier.PROTECTED);
	}
}
