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

public class SelectConstructor_in {
	public /*[*/SelectConstructor_in/*]*/() {
	}
	public void test(String msg) {
	}
	public static void main(String[] args) {
		SelectConstructor_in sc= createSelectConstructor_in();

		sc.test("hello");
	}
	public static SelectConstructor_in createSelectConstructor_in() {
		return new SelectConstructor_in();
	}
}
