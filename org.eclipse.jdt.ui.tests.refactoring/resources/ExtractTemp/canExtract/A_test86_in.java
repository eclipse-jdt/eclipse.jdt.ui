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
public class A<T> {
    enum TEST {
        FIRST, SECOND
    }
    void foo() {
        B<T> b= getB();
    }
    B<T> getB() {
        A.TEST test= TEST.FIRST;
        return null;
    }
    void bar() {
        A<String> s= new A<String>();
        A<T> a= new A<T>();
    }
}