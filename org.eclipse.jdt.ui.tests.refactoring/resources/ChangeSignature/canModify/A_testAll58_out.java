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
	 * @param bbb bee
	 * @param abb an a
	 * @return the number
	 * @see #m(String[][][], int)
	 */
	public abstract int m(String[] bbb[][], int abb);
}
class B extends A {
	public int m(String[] bbb[][], int number) {
		return number + 0;
	}
}
class C extends B {
	/**
	 * @param bbb bee
	 * @param abb an a
	 */
	public int m(String[] strings[][], int abb) {
		return abb + 17;
	}
}
