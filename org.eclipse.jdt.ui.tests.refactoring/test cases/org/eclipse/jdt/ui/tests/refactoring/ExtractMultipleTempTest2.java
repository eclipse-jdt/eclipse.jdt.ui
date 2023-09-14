/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Taiming Wang <3120205503@bit.edu.cn> - [extract local] Extract Similar Expression in All Methods If End-Users Want. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/785
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

public class ExtractMultipleTempTest2 {
	public int getLength1(int a, int b, int c) {
		int z = a + b + c;
		return z;
	}

	public int getLength2() {
		int a = 1;
		int b = 2;
		int c = 3;
		int z = a + b + c;
		return z;
	}

}
