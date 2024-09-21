/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 *
 * Tests for the actions in the source menu
 */
@Suite
@SelectClasses({
AddUnimplementedMethodsTest.class,
GenerateGettersSettersTest.class,
GenerateGettersSettersTest16.class,
GenerateDelegateMethodsTest.class,
AddUnimplementedConstructorsTest.class,
GenerateConstructorUsingFieldsTest.class,
GenerateHashCodeEqualsTest.class,
GenerateToStringTest.class
})
public class SourceActionTests {
}
