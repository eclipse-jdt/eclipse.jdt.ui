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

public class UnmovableArg1_in {
	private int fN;
	public UnmovableArg1_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		int				size;
		UnmovableArg1_in	ua= /*[*/new UnmovableArg1_in(size=16)/*]*/;

		System.out.println("Value = " + ua.get());
	}
}
