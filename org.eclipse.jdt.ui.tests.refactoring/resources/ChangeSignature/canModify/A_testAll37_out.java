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
//to protected
class A{
	protected int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}
class B extends A{
	protected int m(int iii, boolean j){
		return m(m(iii, j), false);
	}
}