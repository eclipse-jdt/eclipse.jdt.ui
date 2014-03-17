/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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

	protected void lambdaExpressionTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", COMPARE_WITH_OUTPUT, "lambdaExpression18_out", null, null, destination, visibility);
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

	//====================================================================================
	// Testing Lambda Expressions
	//====================================================================================

	public void test301() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test302() throws Exception {
		lambdaExpressionTest(1, Modifier.PRIVATE);
	}

	public void test303() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test304() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test305() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	public void test306() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	public void test307() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test308() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test309() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test310() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test311() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test312() throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	public void test313() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test314() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test315() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	public void test316() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	public void test317() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	public void test318() throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	public void test319() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	public void test320() throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	public void test321() throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}
	
	public void test322() throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	public void test323() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	public void test324() throws Exception {
		performTest(fgTestSetup.getLambdaExpressionPackage(), "A", VALID_SELECTION, null);
	}

	public void test325() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}
}
