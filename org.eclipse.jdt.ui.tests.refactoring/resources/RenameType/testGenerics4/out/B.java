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

class B {
    Class<? extends B> class1= B.this.getClass();
    Class<? extends B> class2= B.class;
    Class<B> class3= (Class<B>) B.this.getClass();
    X<B> getX() {
        X<B> x= new X<B>();
        x.t= new ArrayList<B>().toArray(new B[0]);
        return x;
    }
}

class X<T extends B> {
    T[] t;
}
