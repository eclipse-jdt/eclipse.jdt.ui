/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - modified to use internal class to supply logic
 *******************************************************************************/
package org.eclipse.jdt.ui.cleanup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;


/**
 * The context that contains all information required by a clean up to create a fix.
 *
 * @since 3.5
 */
public class CleanUpContext extends CleanUpContextCore {

	/**
	 * Creates a new clean up context.
	 *
	 * @param unit the compilation unit
	 * @param ast the AST, can be <code>null</code> if {@link CleanUpRequirements#requiresAST()}
	 *            returns <code>false</code>. The AST is guaranteed to contain changes made by
	 *            previous clean ups only if {@link CleanUpRequirements#requiresFreshAST()} returns
	 *            <code>true</code>.
	 */
	public CleanUpContext(ICompilationUnit unit, CompilationUnit ast) {
		super(unit, ast);
	}

	@Override
	public ICompilationUnit getCompilationUnit() {
		return super.getCompilationUnit();
	}

	@Override
	public CompilationUnit getAST() {
		return super.getAST();
	}

}