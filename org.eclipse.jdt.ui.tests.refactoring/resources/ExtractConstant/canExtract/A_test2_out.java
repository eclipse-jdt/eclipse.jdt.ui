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
//8, 16 -> 8, 27   AllowLoadtime == false
package p;
class A {
	static final int foo= 1;
	static final int bar= 2;
	private static final int CONSTANT= 1 + 3 * bar;
	static final int baz= 3;
	void f() {
		int i= CONSTANT;
	}
}