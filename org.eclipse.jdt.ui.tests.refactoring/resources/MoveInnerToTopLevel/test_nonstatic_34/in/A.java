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
class A {

	public class Inner {

		public Inner() {
			super();
			System.out.println(getName());
		}

		public String getName() {
			return getTopName() + ".Inner";
		}
	}

	public A() {
		new Object(){};
		System.out.println(new Inner().getName());
	}

	public String getTopName() {
		return "Top";
	}

	static public void main(String[] argv) {
		new A();
	}
}
