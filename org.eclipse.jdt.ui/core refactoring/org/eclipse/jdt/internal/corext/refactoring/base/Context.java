/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A <code>Context<code> can be used to annotate a <code>RefactoringStatusEntry</code>with 
 * additional information presentable in the UI.
 */
public class Context {

	/** A singleton for the null context */
	public static final Context NULL_CONTEXT= new Context();

	public IAdaptable getCorrespondingElement() {
		return null;
	}
}
