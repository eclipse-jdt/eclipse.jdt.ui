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

class Woo4 {
}

public class TestNotCastableOverloaded {
	public void foo(int i) {
	}
	public void foo(Woo4 w) {
	}
	public int goo() {
		return 'a';
	}
	public void main() {
		foo('a');
	}
}
