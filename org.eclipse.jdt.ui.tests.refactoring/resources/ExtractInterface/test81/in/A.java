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
	private int x;
	private A[] v2= null; 
	void f(A[] v){
		A[] a= v;
		a[0].x= 0;
		A[] v1= null;
		a= v1;
		a= v2;
	}
}