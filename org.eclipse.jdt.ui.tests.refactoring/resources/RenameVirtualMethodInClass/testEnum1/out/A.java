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

interface A {
    int getNameSize();
}

enum Enum implements A{
    RED, GREEN, BLUE;
    public int getNameSize() {
        return name().length();
    }
}

class Name implements A {
    Enum fRed= Enum.RED;
    
    public int getNameSize() {
        return fRed.getNameSize();
    }
}

interface IOther {
    int getNameLength();
}
interface IOther2 {
    int getNameSize();
}
