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
 * @see #f
 * @see A#f
 * @see p.A#f
 * @see B#f
 */
class A{
	protected int f;
	void m(){
		f++;
	}
}
/**
 * @see #f
 */
class B{
	A a;
	protected int f;
	void m(){
		a.f= 0;
	}
}