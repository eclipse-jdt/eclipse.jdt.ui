/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;
import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.INVALID_SELECTION;
import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.VALID_SELECTION;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractMethodTests1d8 extends ExtractMethodTests {

	@Rule
	public ExtractMethodTestSetup1d8 fgTestSetup1d8= new ExtractMethodTestSetup1d8();

	protected void defaultMethodsTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup1d8.getDefaultMethodsPackage(), "A", COMPARE_WITH_OUTPUT, "defaultMethods18_out", null, null, destination, visibility);
	}

	protected void staticMethodsTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup1d8.getStaticMethodsPackage(), "A", COMPARE_WITH_OUTPUT, "staticMethods18_out", null, null, destination, visibility);
	}

	protected void destinationTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup1d8.getDestinationPackage(), "A", COMPARE_WITH_OUTPUT, "destination18_out", null, null, destination, visibility);
	}

	protected void lambdaExpressionTest(int destination, int visibility) throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", COMPARE_WITH_OUTPUT, "lambdaExpression18_out", null, null, destination, visibility);
	}

	//====================================================================================
	// Testing Default Methods
	//====================================================================================

	@Override
	@Test
	public void test1() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test1a() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test2() throws Exception {
		defaultMethodsTest(1, Modifier.PROTECTED);
	}

	@Test
	public void test2a() throws Exception {
		defaultMethodsTest(1, Modifier.PROTECTED);
	}

	@Override
	@Test
	public void test3() throws Exception {
		defaultMethodsTest(1, Modifier.PUBLIC);
	}

	@Test
	public void test3a() throws Exception {
		defaultMethodsTest(1, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test4() throws Exception {
		defaultMethodsTest(1, Modifier.PUBLIC);
	}

	@Test
	public void test5() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test6() throws Exception {
		defaultMethodsTest(0, Modifier.PUBLIC);
	}

	//====================================================================================
	// Testing Static Methods
	//====================================================================================

	@Override
	@Test
	public void test101() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test102() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test103() throws Exception {
		staticMethodsTest(0, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test104() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test105() throws Exception {
		staticMethodsTest(2, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test106() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test107() throws Exception {
		staticMethodsTest(1, Modifier.PUBLIC);
	}

	//====================================================================================
	// Testing Destination Types
	//====================================================================================

	@Override
	@Test
	public void test201() throws Exception {
		destinationTest(0, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test202() throws Exception {
		destinationTest(0, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test203() throws Exception {
		destinationTest(1, Modifier.PROTECTED);
	}

	@Test
	public void test204() throws Exception {
		destinationTest(1, Modifier.PROTECTED);
	}

	//====================================================================================
	// Testing Lambda Expressions
	//====================================================================================

	@Override
	@Test
	public void test301() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test302() throws Exception {
		lambdaExpressionTest(1, Modifier.PRIVATE);
	}

	@Test
	public void test303() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test304() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test305() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test306() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Override
	@Test
	public void test307() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test308() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test309() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test310() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Override
	@Test
	public void test311() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test312() throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	@Test
	public void test313() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test314() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test315() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test316() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test317() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test318() throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	@Test
	public void test319() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test320() throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	@Test
	public void test321() throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	@Test
	public void test322() throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", INVALID_SELECTION, null);
	}

	@Test
	public void test323() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test324() throws Exception {
		performTest(fgTestSetup1d8.getLambdaExpressionPackage(), "A", VALID_SELECTION, null);
	}

	@Test
	public void test325() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test326() throws Exception {
		lambdaExpressionTest(0, Modifier.PUBLIC);
	}

	@Test
	public void test327() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test328() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

	@Test
	public void test329() throws Exception {
		lambdaExpressionTest(0, Modifier.PRIVATE);
	}

}
