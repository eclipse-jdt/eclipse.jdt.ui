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

public class B {
	private int count;

	public void f() {
		Inner s= new Inner();
		print(s);
	}

	/**
	 * Bla bla
	 * @param s
	 */
	public void print(Inner s) {
		count++;
		System.out.println(s);
	}	

	public class Inner {
	}
}
