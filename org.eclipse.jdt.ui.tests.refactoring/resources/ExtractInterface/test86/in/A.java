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
	int x;
}
class Y{
	A[] cs;
	void add(A c){
		cs[0]= c;
	}
	void f(){
		cs[0].x= 0;
	}
	void foo(){
		A[] tab= null;
		add(tab[0]);
	}
}