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
	private static final class Inner implements I {
		public void bar(){
		}
	}
	interface I	{
		void bar();
	}
	public static void foo(){
		I foo= new I() {
			public void bar() {
				I foo = new Inner();
			}
		};
	}
}
