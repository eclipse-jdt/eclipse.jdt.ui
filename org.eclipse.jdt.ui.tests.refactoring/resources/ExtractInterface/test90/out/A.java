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
class A implements I {
	int x;
}
class ST{
}
class T extends ST{
	void add(A c){
		gm()[0]= c;
		
		gm1()[0]= c;
		gm1()[0].x= 0;
	}
	I[] gm() {
		return null;
	}
	A[] gm1() {
		return null;
	}

}