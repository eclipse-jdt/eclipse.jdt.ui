/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.ltk.core.refactoring.TextFileChange;

import org.eclipse.jdt.core.ICompilationUnit;

/**
 * A {@link TextFileChange} that operates on an {@link ICompilationUnit}.
 * 
 * @deprecated will be removed before 3.5
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CompilationUnitChange extends org.eclipse.jdt.core.refactoring.CompilationUnitChange {

	/**
	 * Creates a new <code>CompilationUnitChange</code>.
	 *
	 * @param name the change's name, mainly used to render the change in the UI
	 * @param cunit the compilation unit this change works on
	 */
	public CompilationUnitChange(String name, ICompilationUnit cunit) {
		super(name, cunit);
	}
}

