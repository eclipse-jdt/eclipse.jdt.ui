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
	public void use(String... args) { }
	
	public void call() {
		use();
		use("one");
		use("one", "two");
		use(new String[] {"one", "two"});
		use(null);
		use((String[]) null);
		use((String) null);
	}
}