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
	static void foo() {
		final int x= 10; 
		Runnable runnable= new Runnable() {
			private int field;
			public void run() {
				I i= new I() {
					public void method() {
						field= x;
					}
				};
			}
		};
	}
}

interface I {
	void method();
}