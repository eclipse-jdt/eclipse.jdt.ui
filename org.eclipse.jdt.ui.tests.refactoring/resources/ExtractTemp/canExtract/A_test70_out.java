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

class A {
	void f() {
		StringBuffer buf= new StringBuffer();
		String[] strings= new String[] {"A", "B", "C", "D"};
		final int temp= strings.length;
		for (int i= 0; i < temp; i++) {
			buf.append(strings[i]);
		}
	}
}
