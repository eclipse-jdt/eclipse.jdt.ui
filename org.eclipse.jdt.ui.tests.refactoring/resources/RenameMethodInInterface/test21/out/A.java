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
	void k();
}
interface J{
	void k();
}
interface J2 extends J{
	void k();
}

class A{
	public void k(){};
}
class C extends A implements I, J{
	public void k(){};
}
class Test{
	void k(){
		I i= new C();
		i.k();
		I ii= new I(){
			public void k(){}
		};
		ii.k();
		J j= new C();
		j.k();
		J jj= new J(){
			public void k(){}
		};
		jj.k();
		A a= new C();
		((I)a).k();
		((J)a).k();
		((C)a).k();
		a.k();
	}
}