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
 * @see #takeANumber(Number)
 * @see Sub#takeANumber(Number)
 * @see Sub#takeANumber(Number)
 * @see Unrelated1#takeANumber(Number)
 * @see Unrelated1#takeANumber(Object)
 * @see Unrelated1#takeANumber(Number)
 * @see Unrelated1#takeANumber(Integer)
 * @see Unrelated2#takeANumber(Number)
 */
class A<T>{
    public boolean takeANumber(Number n) {
        return true;
    }
}

class Sub<E extends Number> extends A<E> {
    public boolean takeANumber(Number n) {
        if (n.doubleValue() > 0)
            return false;
        return super.takeANumber(n);
    }
}

class Unrelated1<E extends Number> {
    public boolean takeANumber(Number n) {
        return false;
    }
}

interface Unrelated2<E> {
    boolean takeANumber(Number n);
}

interface Unrelated3<T> {
    boolean takeANumber(Number n);
}