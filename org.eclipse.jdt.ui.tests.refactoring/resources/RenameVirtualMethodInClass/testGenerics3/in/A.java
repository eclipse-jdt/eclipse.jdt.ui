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

/**
 * @see #add(T)
 * @see Sub#add(T)
 * @see Sub#add(E)
 * @see Unrelated1#add(E)
 * @see Unrelated1#add(Object)
 * @see Unrelated1#add(Number)
 * @see Unrelated1#add(Integer)
 * @see Unrelated2#add(T)
 */
class A<T>{
    public boolean add(T t) {
        return true;
    }
}

class Sub<E extends Number> extends A<E> {
    public boolean add(E e) {
        if (e.doubleValue() > 0)
            return false;
        return super.add(e);
    }
}

class Unrelated1<E extends Number> {
    public boolean add(E e) {
        return false;
    }
}

interface Unrelated2<E> {
    boolean add(E e);
}

interface Unrelated3<T> {
    boolean add(T t);
}