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
//8, 16 -> 8, 22   AllowLoadtime == true
package p;

class S {
	static int s;
	private static final int CONSTANT= 23 * s;

	int f() {
		return CONSTANT;
	}
}