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

/**
 * @see #A
 * @see #A()
 * @see A#A
 * @see A#A()
 */
enum A {
    A(1), B(2) { }, C, D(), E { }, F() { }
   	;
   	A() {}
    A(int i) { }
}
