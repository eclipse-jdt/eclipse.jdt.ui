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

class SuperClass2<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}

public class TestSubClass2<A, B> extends SuperClass2<B, A> {
	void bar() {
		B t= null;
		A e= null;
	}
}

