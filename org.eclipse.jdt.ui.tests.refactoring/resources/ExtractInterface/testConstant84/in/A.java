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
class A {
	public static final int Y= 0;
	public static final int X= 0;
}
class Test{
	void f(){
		A a= null;
		int i= a.X;
		u(a);

		A a1= null;
		int i1= a.X;
		u1(a1);
	}
	void u(A a){
		int u= a.X;
	}
	void u1(A a){
		int u= a.Y;
	}
}