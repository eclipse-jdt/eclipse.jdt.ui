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
	public B fB;

	/**
	 * This is a comment
	 * @param j a float
	 * @param foo a foo
	 * @param bar a bar
	 */
	public void mA1(float j, int foo, String bar) {
		fB.mA1(j, foo, bar);
	}
	
	public void mA2() {}
}