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

class A<E extends Number> {
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int c) {
        count= c;
    }
    
    void test() {
        Integer i= getCount();
        setCount(i);
        new A<Double>().setCount(1);
        i= new A<Number>().getCount();
    }
}
