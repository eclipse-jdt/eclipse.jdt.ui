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
class Second {
	public String str;
	public void foo(Second s) {
	}
	public void print(A a) {
		foo(this);
		int s= 17;
		s= 18;
		foo(a.s2);
		a.s2.foo(this);
		System.out.println(str);
		a.getClass();
	}
}
