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
public class A implements I{
	public void m(String m){
		System.out.println("A");
	}
}
class B extends A{
	public void k(Object m){
		System.out.println("B");
	}
}
interface I{
void m(String k);
}