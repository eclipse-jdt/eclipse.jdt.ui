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

public class A {
	int fMagic;

	public static void main(String[] args) {
		Second s= new Second("Bingo");
		A a= new A();
		a.fMagic= 17;
		a.print(s);
	}

	/**
	 * Print
	 * @param s
	 */
	public void print(Second s) {
		s.print(this);
	}
}

class Second {
	String fName;
	public Second(String name) {
		fName= name;
	}
	public String toString() {
		return fName;
	}
	/**
	 * Print
	 * @param a TODO
	 */
	public void print(A a) {
		System.out.println(this + ": " + a.fMagic);
	}
}
