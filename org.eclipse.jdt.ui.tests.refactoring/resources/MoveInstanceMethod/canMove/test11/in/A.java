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
package p1;

import p2.B;

public class A {
	public int fInt;
	public B fB;
	public String fString;
	public boolean fBool;
	
	/**
	 * This is a comment
	 * @param j
	 * @param foo
	 * @param bar
	 */
	public void mA1(float j, int foo, String bar) {
		fB.mB1();
		System.out.println(bar + j + this);
		String z= fString + fBool;
		fInt++;
	}
	
	public void mA2() {}
}