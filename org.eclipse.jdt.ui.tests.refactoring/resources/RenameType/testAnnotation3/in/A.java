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

import java.util.ArrayList;

@Test.A
class Test {
    @interface A {
        String value() default "NULL";
    }
    
    @A("A and p.Test.A and p.A and q.Test.A")
    void test () {
        ArrayList<String> list= new ArrayList<String>() {
            void sort() {
                @A
                int current= 0;
                current++;
            }
        };
    }
}
