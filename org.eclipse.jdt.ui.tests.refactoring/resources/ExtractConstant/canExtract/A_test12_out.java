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
// 9, 19 -> 9, 24
package p;

class A {
	private static final int CONSTANT= 3*  1;

	void foob() {
		
		int e= (2 + 2) * CONSTANT;
		
		int f= 5 *CONSTANT *(1 + 1);
	}
}