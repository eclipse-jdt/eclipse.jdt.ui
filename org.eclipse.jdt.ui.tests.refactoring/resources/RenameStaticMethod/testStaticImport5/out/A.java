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
//static import imports two methods
import static p.A.k;
import static p.A.m;

public class A {
    public static void k() { }
    public static void m(int arg) { }
}

class B {
    void use() {
        k();
        m(1);
    }
}