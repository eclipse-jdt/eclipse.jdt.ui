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
//renaming A.m to k
class B{
	private void m(){
		System.out.println("B.m");	
	}
	void fred(){
		m();
		B b= new B();
		b.m();	
		B b1= new A();
		b1.m();
		B bc= new C();
		bc.m();
		
		A ba= new A();
		ba.m();
		A ac= new C();
		ac.m();
		C c= new C();
		c.m();
		
	}
}
class A extends B{
	void m(){
		System.out.println("A.m");	
	}
}
class C extends A{
	void m(){
		System.out.println("C.m");	
	}
}

class test{
	public static void main(String[] args){
		new B().fred();
	}
}