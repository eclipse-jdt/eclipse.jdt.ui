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
class B<T> {
    public B() {}
    public <T>B(T t) {}
    public <X>B(T t, X x) {}

    void m(B a) {
        new B<T>();
        new B<T>(null);
        new <String>B<T>(null, "y");
    };
}

class X {
    void x(B a) {
        new B<Integer>();
        new B<Integer>(null);
        new <String>B<Integer>(new Integer(1), "x");
    };
}
