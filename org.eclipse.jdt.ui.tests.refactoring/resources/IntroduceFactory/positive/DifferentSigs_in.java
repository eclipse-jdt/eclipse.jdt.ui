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

public class DifferentSigs_in {
	private int fN;
	public DifferentSigs_in() {
		this(10);
	}
	public DifferentSigs_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		DifferentSigs_in	ds= /*[*/new DifferentSigs_in(16)/*]*/;

		System.out.println("Value = " + ds.get());
	}
	public void bar(String[] args) {
		DifferentSigs_in	ds= new DifferentSigs_in();

		System.out.println("Value = " + ds.get());
	}
}
