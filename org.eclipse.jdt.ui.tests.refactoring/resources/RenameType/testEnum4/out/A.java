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
class Outer {
    enum B {
       ONE, TWO, THREE
    }
}

class User {
    /**
     * Uses {@link p.Outer.B#ONE}.
     */
    Outer.B a= p.Outer.B.ONE;
}
