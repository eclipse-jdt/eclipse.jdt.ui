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
package p;

abstract class A {
	/**
	 * @param a an a
	 * @param b bee
	 * @return the number
	 * @see #m(int, String[][][])
	 */
	public abstract int m(int a, String[] b[][]);
}
class B extends A {
	public int m(int number, String[] b[][]) {
		return number + 0;
	}
}
class C extends B {
	/**
	 * @param a an a
	 * @param b bee
	 */
	public int m(int a, String[] strings[][]) {
		return a + 17;
	}
}
