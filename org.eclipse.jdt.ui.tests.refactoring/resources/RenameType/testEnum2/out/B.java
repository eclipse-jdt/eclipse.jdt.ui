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

enum B {
    ONE {
        String getKey() {
            return "eis";
        }
        boolean longerNameThan(B other) {
            return false;
        }
    },
    BIG {
        String getKey() {
            return "riesig";
        }
        boolean longerNameThan(B other) {
            return other != BIG;
        }
    };
    abstract boolean longerNameThan(B a);
}
