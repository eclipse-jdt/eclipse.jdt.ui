/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	/**
	 * Adds the new roots to the list.
	 *
	 * @param roots the roots to add
	 * @since 3.7
	 */
    void addRoots(MethodWrapper[] roots){
		List<MethodWrapper>newRoots= new ArrayList<>(Arrays.asList(fRoots));
		newRoots.addAll(Arrays.asList(roots));

		fRoots= newRoots.toArray(new MethodWrapper[newRoots.size()]);
    }


}
