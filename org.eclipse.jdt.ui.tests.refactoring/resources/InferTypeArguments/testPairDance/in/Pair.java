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

public class Pair<A, B> {
    private A a;
    private B b;
    
    public A getA() {
        return a;
    }
    public void setA(A a) {
        this.a= a;
    }
    public B getB() {
        return b;
    }
    public void setB(B bee) {
        b= bee;
    }
    public String toString() {
        return super.toString() + ", a=" + a + ", b=" + b;
    }
}
