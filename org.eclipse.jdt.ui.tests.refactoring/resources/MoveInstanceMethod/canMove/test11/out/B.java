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
package p2;

import p1.A;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	/**
	 * This is a comment
	 * @param a TODO
	 * @param j
	 * @param foo
	 * @param bar
	 */
	public void mA1(A a, float j, int foo, String bar) {
		mB1();
		System.out.println(bar + j + a);
		String z= a.fString + a.fBool;
		a.fInt++;
	}
}