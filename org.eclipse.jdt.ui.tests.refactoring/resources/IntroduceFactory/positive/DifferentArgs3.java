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

public class DifferentArgs3_in {
	private int fN;
	public static DifferentArgs3_in createDifferentArgs3_in(int N) {
		return new DifferentArgs3_in(N);
	}
	public DifferentArgs3_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		int				size;
		DifferentArgs3_in	da= createDifferentArgs3_in(size=16);

		System.out.println("Value = " + da.get());
	}
	public void bar(String[] args) {
		DifferentArgs3_in	da= createDifferentArgs3_in(24);

		System.out.println("Value = " + da.get());
	}
}
