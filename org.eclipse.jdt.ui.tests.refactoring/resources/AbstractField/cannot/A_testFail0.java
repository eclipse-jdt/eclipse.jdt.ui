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
//abstract and make private, only getter
package p;
class A{
	public int f;
	void m(){
		f= f;
	}
}
class B{
	int m(){
		A a= new A();
		a.f= a.f;
		return a.f;
	}
}