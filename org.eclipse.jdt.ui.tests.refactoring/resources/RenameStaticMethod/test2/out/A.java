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
class A{
	static void k(){
	}
	void f(){
		k();
	}
	static int fred(){
		k();
		return 1;
	}
	{
		A.k();
		k();
		new A().k();
	}
	static {
		A.k();
		k();
		new A().k();
	}
}
class D{
	static void m(){
		A.k();
		new A().k();
		m();
	}
	static {
		A.k();
		new A().k();
		m();
	}
	{
		A.k();
		new A().k();
		m();
	}
}