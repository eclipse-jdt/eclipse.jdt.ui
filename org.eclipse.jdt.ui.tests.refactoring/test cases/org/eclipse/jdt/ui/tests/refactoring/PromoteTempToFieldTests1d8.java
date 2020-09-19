/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class PromoteTempToFieldTests1d8 extends PromoteTempToFieldTests {
	public PromoteTempToFieldTests1d8() {
		super(new Java1d8Setup());
	}

	@Test
	public void testFailInterfaceMethods1() throws Exception {
		failHelper(6, 13, 6, 14, "i", false, false, PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD, Modifier.PRIVATE, RefactoringStatus.FATAL);
	}

	@Test
	public void testFailInterfaceMethods2() throws Exception {
		failHelper(6, 13, 6, 14, "i", false, false, PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD, Modifier.PRIVATE, RefactoringStatus.FATAL);
	}
}
