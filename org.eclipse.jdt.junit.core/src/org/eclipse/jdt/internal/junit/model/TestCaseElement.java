/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Xavier Coulon <xcoulon@redhat.com> - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512 - [JUnit] test method name cut off before (
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.junit.model.ITestCaseElement;

import org.eclipse.core.runtime.Assert;


public class TestCaseElement extends TestElement implements ITestCaseElement {

	private boolean fIgnored;
	private boolean fIsDynamicTest;
	private boolean fIsParameterizedTest;
	private String fParameterSourceType; // e.g., "EnumSource", "ValueSource", "MethodSource", etc.
	private String fParameterEnumType; // For @EnumSource, the enum class name

	public TestCaseElement(TestSuiteElement parent, String id, String testName, String displayName, boolean isDynamicTest, String[] parameterTypes, String uniqueId) {
		super(parent, id, testName, displayName, parameterTypes, uniqueId);
		Assert.isNotNull(parent);
		fIsDynamicTest= isDynamicTest;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jdt.junit.model.ITestCaseElement#getTestMethodName()
	 * @see org.eclipse.jdt.internal.junit.runner.MessageIds#TEST_IDENTIFIER_MESSAGE_FORMAT
	 * @see org.eclipse.jdt.internal.junit.runner.MessageIds#IGNORED_TEST_PREFIX
	 */
	@Override
	public String getTestMethodName() {
		String testName= getTestName();
		int index= testName.lastIndexOf('(');
		if (index > 0)
			return testName.substring(0, index);
		index= testName.indexOf('@');
		if (index > 0)
			return testName.substring(0, index);
		return testName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.eclipse.jdt.junit.model.ITestCaseElement#getTestClassName()
	 */
	@Override
	public String getTestClassName() {
		return getClassName();
	}

	/*
	 * @see org.eclipse.jdt.internal.junit.model.TestElement#getTestResult(boolean)
	 * @since 3.6
	 */
	@Override
	public Result getTestResult(boolean includeChildren) {
		if (fIgnored)
			return Result.IGNORED;
		else
			return super.getTestResult(includeChildren);
	}

	public void setIgnored(boolean ignored) {
		fIgnored= ignored;
	}

	public boolean isIgnored() {
		return fIgnored;
	}

	@Override
	public String toString() {
		return "TestCase: " + getTestClassName() + "." + getTestMethodName() + " : " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public boolean isDynamicTest() {
		return fIsDynamicTest;
	}

	/**
	 * Returns whether this test is a parameterized test.
	 * 
	 * @return <code>true</code> if this is a parameterized test, <code>false</code> otherwise
	 * @since 3.15
	 */
	public boolean isParameterizedTest() {
		return fIsParameterizedTest;
	}

	/**
	 * Sets whether this test is a parameterized test.
	 * 
	 * @param isParameterized <code>true</code> if this is a parameterized test
	 * @since 3.15
	 */
	public void setParameterizedTest(boolean isParameterized) {
		fIsParameterizedTest= isParameterized;
	}

	/**
	 * Returns the parameter source type for parameterized tests.
	 * 
	 * @return the parameter source type (e.g., "EnumSource", "ValueSource"), or <code>null</code> if not a parameterized test
	 * @since 3.15
	 */
	public String getParameterSourceType() {
		return fParameterSourceType;
	}

	/**
	 * Sets the parameter source type for parameterized tests.
	 * 
	 * @param sourceType the parameter source type (e.g., "EnumSource", "ValueSource")
	 * @since 3.15
	 */
	public void setParameterSourceType(String sourceType) {
		fParameterSourceType= sourceType;
	}

	/**
	 * Returns the enum type for @EnumSource parameterized tests.
	 * 
	 * @return the fully qualified enum class name, or <code>null</code> if not an EnumSource test
	 * @since 3.15
	 */
	public String getParameterEnumType() {
		return fParameterEnumType;
	}

	/**
	 * Sets the enum type for @EnumSource parameterized tests.
	 * 
	 * @param enumType the fully qualified enum class name
	 * @since 3.15
	 */
	public void setParameterEnumType(String enumType) {
		fParameterEnumType= enumType;
	}
}
