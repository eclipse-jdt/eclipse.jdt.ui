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

public class DifferentArgs2_in {
	private int fN;
	public DifferentArgs2_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		DifferentArgs2_in	da= /*[*/new DifferentArgs2_in(16)/*]*/;

		System.out.println("Value = " + da.get());
	}
	public void bar(String[] args) {
		int					size= 24;
		DifferentArgs2_in	da= new DifferentArgs2_in(size);

		System.out.println("Value = " + da.get());
	}
}
