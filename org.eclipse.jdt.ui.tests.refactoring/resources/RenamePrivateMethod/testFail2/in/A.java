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
//can't rename A.m to k
package p;

class X {
	void k(){
	}
}
class A {
	private void m(){
		System.out.println("a");
	}
	class B extends X{
		void f(){
			m();
		}
		public void foo() {
			
		}
	}
}