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
 * @see A
 * @see p.A
 * @see p.B#m()
 * @see p.B#f
 */
class B{

	/**
	 * @see #f
	 * @see B#f
	 * @see p.B#f
	 */
	public static int f;

	/**
	 * @see A
	 * @see p.A
	 * @see #m()
	 * @see B#m()
	 * @see p.B#m()
	 */
	public static void m(){
		B.m();
		m();
		f= B.f;
	}
}