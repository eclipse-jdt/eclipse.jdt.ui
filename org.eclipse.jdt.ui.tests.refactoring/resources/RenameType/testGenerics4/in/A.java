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

import java.util.ArrayList;

class A {
    Class<? extends A> class1= A.this.getClass();
    Class<? extends A> class2= A.class;
    Class<A> class3= (Class<A>) A.this.getClass();
    X<A> getX() {
        X<A> x= new X<A>();
        x.t= new ArrayList<A>().toArray(new A[0]);
        return x;
    }
}

class X<T extends A> {
    T[] t;
}
