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
public class A {
	public static class I {
		public I(I other) {
			a= new A();
		}
		public static class J {
			static int bla;
			int x() {return 1;}
		}
	}
	public static A a;
	public A.I i;
	{
		i= new A.I(i);
		int blub= I.J.bla + new I.J().x();
	}
}
