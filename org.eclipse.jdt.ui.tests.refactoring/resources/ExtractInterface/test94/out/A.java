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
	 A[] gm() {
		return null;
	}
}
class T extends ST{
	void add(A c){
		super.gm()[0]= c;
		super.gm()[0].x= 0;
	}
}