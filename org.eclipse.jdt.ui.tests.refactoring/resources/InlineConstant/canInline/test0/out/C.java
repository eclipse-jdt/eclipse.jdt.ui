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
// 5, 30 -> 5, 36  replaceAll == true, removeDeclaration == false
package p;

class C {
	private static final int FOOBAR= 0;
	
	private static void jb() {
		System.out.println("Ceci, ce n'est pas une pipe: " + 0 + " ;");
	}	
}