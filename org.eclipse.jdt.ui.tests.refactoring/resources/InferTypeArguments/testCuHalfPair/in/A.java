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

class A<T, U> {
	// cannot infer type for U -> leave raw
    T t;
    U u;
    
    void addT(T arg) {
        t= arg;
    }
    
    static void m() {
        A p = new A();
        p.addT("Hello");
    }
}
