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
	/**
	 * This is {@link #m(String[])}.
	 * @param args TODO
	 */
	public void m(String... args) {
		if (12 > 12)
			m(args);
	}
	void x() {
		m("Hello", new String());
	}
}

class B {
	public void b() {
		new A().m("Hello", new String());
	}	
}
