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
//selection: 10, 21, 11, 40
//name: is -> ints
package simple;

public class Formatting1 {
	public void method1() {
		method2();
	}
	public void method2() {
		doSomething(new int[]{1, 2, //newline
				3/*important comment*/});
	}
	private void doSomething(int[] is) {
	}
}