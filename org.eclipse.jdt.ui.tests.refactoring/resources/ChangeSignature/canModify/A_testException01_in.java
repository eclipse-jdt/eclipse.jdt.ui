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

import java.io.IOException;

abstract class A {
	public abstract int m(long l) throws IOException;
}

class B extends A {
	public int m(long l) throws IOException {
		return 17;
	}
	public int m(int i) {
		return i;
	}
}

class C extends B {
}

class D extends A {
	public int m(long l) {
		return 0;
	}
}	