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

enum A {
    RED, GREEN, BLUE, YELLOW;
    A buddy;
    public A getBuddy() {
        return buddy;
    }
    public void setBuddy(A b) {
        buddy= b;
    }
}

class User {
    void m() {
        A.RED.setBuddy(A.GREEN);
        if (A.RED.getBuddy() == A.GREEN) {
            A.GREEN.buddy= null;
        }
    }
}