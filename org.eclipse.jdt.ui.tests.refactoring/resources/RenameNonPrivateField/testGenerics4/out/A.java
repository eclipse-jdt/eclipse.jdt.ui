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
    private int number;

    public int getNumber() {
        return number;
    }

    public void setNumber(int c) {
        number= c;
    }
    
    void test() {
        Integer i= getNumber();
        setNumber(i);
        new A<Double>().setNumber(1);
        i= new A<Number>().getNumber();
    }
}
