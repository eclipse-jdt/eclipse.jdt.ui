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
/**
 * @see p.A
 * @see #m()
 * @see A#m()
 * @see p.A#m()
 * @see #f
 * @see A#f
 * @see p.A#f
 */
public class A{
	public A() {
		m();
		f= A.f;
	}
	
	/**
	 * @see A
	 * @see p.A
	 * @see #m()
	 * @see A#m()
	 * @see p.A#m()
	 */
	public static void m(){
		A.m();
		m();
		f= A.f;
	}
	
	/**
	 * @see #f
	 * @see A#f
	 * @see p.A#f
	 */
	public static int f;
}