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

class A<S extends Number & Cloneable> {
    S t;
    S transform(S t) {
        return t;
    }
    Collection<? super S> add(List<? extends S> t) {
        return null;
    }
    
    class Inner<I extends S> {
        S tee;
    }
}