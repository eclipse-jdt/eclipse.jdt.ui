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
package cast_out;

class Base5 {
}

class Derived5 extends Base5 {
}

public class TestOverloaded {
	public void foo(Derived5 d) {
	}
	public void foo(Base5 b) {
	}
	public Base5 goo() {
		return new Derived5();
	}
	public void main() {
		foo((Base5) new Derived5());
	}
}
