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
// Here, an import is added for a type needed only after a qualification is added.
// 7, 37 -> 7, 43  replaceAll == true, removeDeclaration == false
package p2;

class InlineSite {
	static {
		System.out.println(p1.Declarer.CONSTANT);	
	}
}