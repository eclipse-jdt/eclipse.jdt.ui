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
//rename A#m() to k() -> must rename all m()
package p;
abstract class Abstract {
	public abstract void m();
	void caller(Abstract abstr, A a, Interface inter, Impl2 impl2) {
		abstr.m();
		a.m();
		inter.m();
		impl2.m();
	}
}

class A extends Abstract {
	public void m() { // from Abstract
	}
}

interface Interface { //independent of Abstract
	void m();
}

class Impl2 extends Abstract implements Interface {
	public void m() { // from Abstract AND from Interface
	}
}
