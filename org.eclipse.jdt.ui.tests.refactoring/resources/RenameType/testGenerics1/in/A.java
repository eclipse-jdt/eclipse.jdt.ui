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
class A<T> {
    public A() {}
    public <T>A(T t) {}
    public <X>A(T t, X x) {}

    void m(A a) {
        new A<T>();
        new A<T>(null);
        new <String>A<T>(null, "y");
    };
}

class X {
    void x(A a) {
        new A<Integer>();
        new A<Integer>(null);
        new <String>A<Integer>(new Integer(1), "x");
    };
}
