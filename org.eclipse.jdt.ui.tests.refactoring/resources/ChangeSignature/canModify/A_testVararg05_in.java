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
	 * @see #use(Object, String[])
	 */
	public void use(Object first, String... args) {
		System.out.println(first);
	}
	
	public void call() {
		use(null);
		use(null, "one");
		use(null, "one", "two");
		use(null, new String[] {"one", "two"});
		use(null, null);
		use(null, (String[]) null);
		use(null, (String) null);
	}
}