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
//nor ref update
package p;
//renaming I.m to k
interface I {
void k();
}
class C1 implements I{
	public void m(){};
}
class D{
	void h(){
		I a= new C1();
		a.m();
	}
}
class NotRel{
	public void m(){};
	void f(){
		m();
	}
}