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
//10, 28 -> 10, 44   AllowLoadtime == true
package p;

class S {
	public static S instance= new S();

	private static final int CONSTANT= S.instance.s + 1;

	int s;

	int f() {
		System.out.println(CONSTANT);
		return 1;
	}
}