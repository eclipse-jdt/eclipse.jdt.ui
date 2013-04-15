/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
 *
 * Tests for the actions in the source menu
 *
 */
public class SourceActionTests extends TestCase {

	public static Test suite() {

		TestSuite suite= new TestSuite(SourceActionTests.class.getName());
		suite.addTest(AddUnimplementedMethodsTest.suite());
		suite.addTest(GenerateGettersSettersTest.suite());
		suite.addTest(GenerateDelegateMethodsTest.suite());
		suite.addTest(AddUnimplementedConstructorsTest.suite());
		suite.addTest(GenerateConstructorUsingFieldsTest.suite());
		suite.addTest(GenerateHashCodeEqualsTest.suite());
		suite.addTest(GenerateToStringTest.suite());

		return new ProjectTestSetup(suite);
	}

	public SourceActionTests(String name) {
		super(name);
	}

}
