/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchPart;

public class ActionChecker {

	private ActionChecker() {
	}

	public static ActionChecker create(ISelectionProvider provider) {
		return new ActionChecker();
	}
	
	public boolean elementsAreInstancesOf(IStructuredSelection selection, Class clazz, boolean ifEmpty) {
		if (selection.isEmpty())
			return ifEmpty;
		for (Iterator iter= ((IStructuredSelection)selection).iterator(); iter.hasNext();) {
			if (!clazz.isInstance(iter.next()))
				return false;
		}
		return true;
	}	
}
