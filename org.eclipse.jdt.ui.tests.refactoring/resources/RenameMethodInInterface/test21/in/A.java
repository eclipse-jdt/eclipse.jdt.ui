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
//renaming I.m to k
package p;
interface I {
	void m();
}
interface J{
	void m();
}
interface J2 extends J{
	void m();
}

class A{
	public void m(){};
}
class C extends A implements I, J{
	public void m(){};
}
class Test{
	void k(){
		I i= new C();
		i.m();
		I ii= new I(){
			public void m(){}
		};
		ii.m();
		J j= new C();
		j.m();
		J jj= new J(){
			public void m(){}
		};
		jj.m();
		A a= new C();
		((I)a).m();
		((J)a).m();
		((C)a).m();
		a.m();
	}
}