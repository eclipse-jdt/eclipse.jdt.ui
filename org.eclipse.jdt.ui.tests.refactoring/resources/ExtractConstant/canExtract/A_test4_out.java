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
//5, 23 -> 5, 34   AllowLoadtime == false
package p;
class A {
	private static final int CONSTANT= 2 * b() * 5;
	static void f() {
		int i= 2*(1 + CONSTANT);
		System.out.println(i);
		System.out.println(CONSTANT  +1);
	}
	static int b() {
		return 4;	
	}
}