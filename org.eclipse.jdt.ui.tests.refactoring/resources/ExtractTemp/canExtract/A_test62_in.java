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
package p;//10, 17 - 10, 28


class A  {
	void f() {
		String x;
		boolean test= false, test2= false, test3= false;
		if (test) {
		} else if (test2) {
			x = "ExtractMe";
		} else if (test3) {
			x = "ExtractMe";
		}
	}
}