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
//abstract and make private
package p;
class A{
	private int f;
	void m(){
		setF(getF());
	}
	public int getF(){
		return f;
	}
	public void setF(int f){
		this.f= f;
	}
}
class B{
	int m(){
		A a= new A();
		a.setF(a.getF());
		return a.getF();
	}
}