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

public class A {
    public String runThese(Runnable[] runnables) {
        return "A";
    }
    
    public static void main(String[] args) {
        Runnable r1 = null, r2 = null;
        System.out.println(new A().runThese(new Runnable[] { r1, r2 }));
        System.out.println(new Sub().runThese(new Runnable[] { r1, r2 }));
        System.out.println(new Sub().runThese(r1, r2));
        System.out.println(new Sub2().runThese(new Runnable[] { r1, r2 }));
    }
}

class Sub extends A {
    public String runThese(Runnable... runnables) {
        return "Sub, " + super.runThese(runnables);
    }
}

class Sub2 extends Sub {
    public String runThese(Runnable[] runnables) {
        return "Sub2, " + super.runThese(runnables);
    }
}
