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
// 6, 37 -> 6, 43
package p;

class A {
	void foob() {
		int temp= 2+2* 4;
		int e= (2 + 2) * (27 + 2 * (temp+1*2));
		
		int c= 3 * (2 + 1) + temp + 28;
	}
}