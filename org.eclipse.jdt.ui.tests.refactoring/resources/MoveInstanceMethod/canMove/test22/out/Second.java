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
class Second {
	public void foo(Second s) {
		s.bar();
	}

	public void bar() {
	}
	
	public void go(int i, int j) {
	}

	public void print() {
		foo(this);
		bar();
		go(17, 18);
	}
}
