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
class A{
	private final class Inner extends A {
		int k;
		private final int u;
		private Inner(int x, int u) {
			super(x);
			this.u= u;
			k= u;
		}
	}
	A(int x){
	}
	void f(){
		final int u= 9;
		int s= 2;
		new Inner(s, u);
	}
}