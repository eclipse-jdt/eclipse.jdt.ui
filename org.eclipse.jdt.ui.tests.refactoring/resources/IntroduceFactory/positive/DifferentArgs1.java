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

public class DifferentArgs1_in {
	private int fN;
	public static DifferentArgs1_in createDifferentArgs1_in(int N) {
		return new DifferentArgs1_in(N);
	}
	public DifferentArgs1_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		DifferentArgs1_in	da= createDifferentArgs1_in(16);

		System.out.println("Value = " + da.get());
	}
	public void bar(String[] args) {
		DifferentArgs1_in	da= createDifferentArgs1_in(24);

		System.out.println("Value = " + da.get());
	}
}
