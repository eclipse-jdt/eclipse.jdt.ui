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
	public void m() {
	}
}
class C2 extends A{
}
class C1 extends C2{
}
class B extends C1 {
	public void m() {
	}
}
class B1 extends C1{
	public void foo() {
		m();//if you move B.m() to C2 this will result in a different call
	}
}

