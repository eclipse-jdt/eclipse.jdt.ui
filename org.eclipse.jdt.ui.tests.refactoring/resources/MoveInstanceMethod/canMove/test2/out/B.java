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
	 * mA1
	 * @param a TODO
	 */
	public void mA1(A a) {
		mB1();
		a.mA2();
		mB2();
		System.out.println(a);
	}
}