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

public class MultipleCallers_in {
	private int fN;
	public static MultipleCallers_in createMultipleCallers_in(int N) {
		return new MultipleCallers_in(N);
	}
	public MultipleCallers_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		MultipleCallers_in	mc= createMultipleCallers_in(16);

		System.out.println("Value = " + mc.get());
	}
	public void bar(String[] args) {
		MultipleCallers_in	mc= createMultipleCallers_in(16);

		System.out.println("Value = " + mc.get());
	}
}
