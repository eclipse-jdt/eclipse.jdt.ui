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
	public static void method2(final int i) {
		final I x = new I() {//<-- refactor->convert local variable x to field
			public void methodI() {
				int y = 3;
			}
		};
		I y= x;
	}
}
interface I {
	void methodI();
}