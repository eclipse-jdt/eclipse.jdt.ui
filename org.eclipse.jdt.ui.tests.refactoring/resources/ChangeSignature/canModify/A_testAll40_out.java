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
//to public
class A{
	public int m(boolean jj, int[] x, int i){
		return m(false, x, m(jj, x, i));
	}
}
class B extends A{
	public int m(boolean jj, int[] x, int i){
		return m(false, x, m(jj, x, i));
	}
}