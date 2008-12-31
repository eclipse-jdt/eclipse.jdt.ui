/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class TreeRoot {
    public static final Object EMPTY_ROOT = new Object();
    private MethodWrapper[] fRoots;

    public TreeRoot(MethodWrapper[] roots) {
        this.fRoots = roots;
    }

    MethodWrapper[] getRoots() {
        return fRoots;
    }
}
