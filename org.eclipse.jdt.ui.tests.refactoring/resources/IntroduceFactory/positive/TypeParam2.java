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

public class TypeParam2_in {
    public void foo() {
        Cell<String> cs1= Factory.createCell("");
        Cell<Integer> cs2= Factory.createCell(3);
        Cell<Float> cs3= Factory.createCell(3.14F);
    }
}
class Cell<T> {
    T fData;
    /*[*/Cell/*]*/(T t) {
        fData= t;
    }
}
class Factory {

	public static <T> Cell<T> createCell(T t) {
		return new Cell<T>(t);
	}
}
