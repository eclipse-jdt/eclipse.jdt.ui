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
//7, 36 -> 7, 36  replaceAll == true, removeDeclaration == false
package p;

@Annot("John")
class Test {
	@interface Annot {
		public static final String DEFAULT= "John";
		String value();
	}
	@Annot("John")
	int a;
	@Annot(value="John")
	int b;
}

@Test.Annot(value="John")
enum Test2 {}