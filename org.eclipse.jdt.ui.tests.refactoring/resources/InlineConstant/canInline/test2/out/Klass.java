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
// 10, 22 -> 10, 30  replaceAll == false

package p;

class Klass {
	static final Klass KONSTANT=           new   Klass()  ;
	
	
	static void f() {
		Klass klass= new   Klass();	
	}
	
	Klass klass=KONSTANT;
}