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
	static void m(){
	}
	void f(){
		m();
	}
	static int fred(){
		m();
		return 1;
	}
	{
		A.m();
		m();
		new A().m();
	}
	static {
		A.m();
		m();
		new A().m();
	}
}
class D{
	static void m(){
		A.m();
		new A().m();
		m();
	}
	static {
		A.m();
		new A().m();
		m();
	}
	{
		A.m();
		new A().m();
		m();
	}
}