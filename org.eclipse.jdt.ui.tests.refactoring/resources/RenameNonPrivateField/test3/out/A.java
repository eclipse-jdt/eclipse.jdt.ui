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
 * @see #g
 * @see A#g
 * @see p.A#g
 * @see B#f
 */
class A{
	protected int g;
	void m(){
		g++;
	}
}
/**
 * @see #f
 */
class B{
	A a;
	protected int f;
	void m(){
		a.g= 0;
	}
}