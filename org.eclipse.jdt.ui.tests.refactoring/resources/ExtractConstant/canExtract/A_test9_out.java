/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// 6, 24 -> 6, 29
package p;

class A {
	private static final int CONSTANT= 3 + 1;

	void foob() {
		int a= 2 * 3 + CONSTANT;

		int b= (CONSTANT) + 1*1;
	}
}