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
// 8, 17 -> 8, 22
package p;

class A {
	private static final int CONSTANT= 2 + 2;

	void foob() {
		int c= 2 + 2 * 4 + (28 + CONSTANT) + 1;

		int e= (CONSTANT) * 3 * 1;
	}
}