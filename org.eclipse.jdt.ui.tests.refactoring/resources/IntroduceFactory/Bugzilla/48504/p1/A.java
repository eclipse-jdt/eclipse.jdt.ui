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
    {
        field= 10;
    }
    
    public /*[*/A/*]*/() {
        
    }
    
    private int field;
    
    static class XX extends A {
        public void foo() {
            bar();
        }
        public void bar() {
        }
    }
    public void foo(int y) {
        Runnable runnable= new Runnable() {
            private int field;
            public void run() {
                {
                    A a= null;
                }
            }
        };
    }
    
    public String foo(String ss) {
        A a= new A();
        return ss;
    }
}
