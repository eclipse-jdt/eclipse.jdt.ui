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

public class TypeParam_in {
    public void foo() {
        Cell<String> cs1= new Cell<String>("");
        Cell<Integer> cs2= new Cell<Integer>(3);
        Cell<Float> cs3= new Cell<Float>(3.14F);
    }
}
class Cell<T> {
    T fData;
    public /*[*/Cell/*]*/(T t) {
        fData= t;
    }
}
