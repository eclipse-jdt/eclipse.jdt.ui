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
package cast_in;

class Woo1 {
}

class Zoo1 extends Woo1 {
}

class Base1 {
	public void foo(Woo1 w) {
	}
}

class Derived1 extends Base1 {
	public void foo(Zoo1 z) {
	}
}

public class TestHierarchyOverloaded {
	public Woo1 goo() {
		return new Zoo1();
	}
	public void main(Derived1 d) {
		d.foo(/*]*/goo()/*[*/);
	}
}
