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
//selection: 17, 21, 17, 40
//name: is -> ints
package simple;

/**
 * @see Javadoc1#doSomething(int[])
 * @see #doSomething(int[] is)
 * 
 * @see Javadoc1#go(float)
 * @see #go(float ship)
 */
public class Javadoc1 {
	public void run() {
		go(3.0f);
	}
	public void go(float ship) {
		doSomething(new int[] {1, 2, 3});
	}
	static void doSomething(int[] is) {
	}
}