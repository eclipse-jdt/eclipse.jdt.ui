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

import java.util.Collection;
import java.util.List;

class A<T extends Number & Cloneable> {
    T t;
    T transform(T t) {
        return t;
    }
    Collection<? super T> add(List<? extends T> t) {
        return null;
    }
    
    class Inner<I extends T> {
        T tee;
    }
}