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

class Base2 {
	public void foo(int i) {
	}
}

class Derived2 extends Base2 {
	public void foo(char c) {
	}
}

public class TestHierarchyOverloadedPrimitives {
	public int goo() {
		return 'a';
	}
	public void main(Derived2 d) {
		d.foo(/*]*/goo()/*[*/);
	}
}
