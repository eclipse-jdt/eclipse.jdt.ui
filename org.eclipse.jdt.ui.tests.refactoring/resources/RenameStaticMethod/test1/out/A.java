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
		return 0;
	}
}
class B{
	static void m(){
		A.k();
		new A().k();
	}
}