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
 * @see #A_testEnum02_in(int, int)
 * @see A_testEnum02_in#A_testEnum02_in(int, int)
 */
enum A_testEnum02_in {
    A(1, 17), B(2, 17) { }
   	;
   	private A_testEnum02_in(int i, int a) { }
}
