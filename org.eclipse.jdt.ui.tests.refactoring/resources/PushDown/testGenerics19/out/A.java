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
abstract class A<T>{

	public abstract void f();

	public abstract T m(T t);
}
abstract class B extends A<String>{

	public String m(String t) {
		String s= t;
		return null;
	}

	public abstract void f();
}
class C extends A<Object>{
	public void f(){}

	public Object m(Object t) {
		Object s= t;
		return null;
	}
}