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
package generic_out;

public class TestClassInstance {
	void bar() {
		X<String, Number> x= new X<String, Number>();
		String t= null;
		Number e= null;
	}
}

class X<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}