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

class A<T>{
	T tee;
	
	public T getTee() {
		return tee;
	}
	
	public void setTee(T t) {
		tee= t;
	}
}

class B {
	static {
		A a= new A();
		Object o= a.tee;
		
		A<Number> an= new A<Number>();
		an.setTee(new Double(1.3d));
		
		A<? extends Number> at= new A<Integer>();
		Number tee=at.getTee();
		at.setTee(null);
	}
}