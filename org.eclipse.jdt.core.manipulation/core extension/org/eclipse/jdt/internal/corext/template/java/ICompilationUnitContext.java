/*******************************************************************************
 * Copyright (c) 2019 Microsoft Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;

public interface ICompilationUnitContext {
	/**
	 * Returns the compilation unit if one is associated with this context,
	 * <code>null</code> otherwise.
	 * @param elementType the type of the element
	 *
	 * @return the compilation unit of this context or <code>null</code>
	 */
	IJavaElement findEnclosingElement(int elementType);

	/**
	 * Returns the compilation unit if one is associated with this context,
	 * <code>null</code> otherwise.
	 *
	 * @return the compilation unit of this context or <code>null</code>
	 */
	ICompilationUnit getCompilationUnit();
}
