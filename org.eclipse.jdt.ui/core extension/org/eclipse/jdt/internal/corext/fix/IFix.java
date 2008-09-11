/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;

/**
 * An <code>IFix</code> can calculate a <code>CompilationUnitChange</code>
 * which applied will fix one or several problems in a compilation unit.
 *
 * @since 3.2
 */
public interface IFix {

	/**
	 * A change which applied will fix problems in a compilation
	 * unit. This must not return <b>null</b> and must not return
	 * a null change.
	 *
	 * @return the change to apply
	 * @throws CoreException if something went wrong while calculating the change
	 */
	public CompilationUnitChange createChange() throws CoreException;

}
