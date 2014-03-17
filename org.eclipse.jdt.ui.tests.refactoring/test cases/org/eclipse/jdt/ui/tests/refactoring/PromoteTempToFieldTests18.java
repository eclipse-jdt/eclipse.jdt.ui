/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;

public class PromoteTempToFieldTests18 extends PromoteTempToFieldTests {
	private static final Class clazz= PromoteTempToFieldTests18.class;

	public PromoteTempToFieldTests18(String name) {
		super(name);
	}

	public static Test setUpTest(Test test) {
		return new Java18Setup(test);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

	public void testFailInterfaceMethods1() throws Exception {
		failHelper(6, 13, 6, 14, "i", false, false, PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD, Modifier.PRIVATE, RefactoringStatus.FATAL);
	}

	public void testFailInterfaceMethods2() throws Exception {
		failHelper(6, 13, 6, 14, "i", false, false, PromoteTempToFieldRefactoring.INITIALIZE_IN_METHOD, Modifier.PRIVATE, RefactoringStatus.FATAL);
	}

}
