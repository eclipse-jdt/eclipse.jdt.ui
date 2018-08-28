/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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


/**
 * The context that contains all information required by a clean up to create a fix.
 * 
 * @since 3.5
 */
public class CleanUpContext {
	
	// Use internal class to supply logic
	private final org.eclipse.jdt.internal.corext.fix.CleanUpContext fContext;

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
		fContext= new org.eclipse.jdt.internal.corext.fix.CleanUpContext(unit, ast);
	}

	/**
	 * The compilation unit to clean up.
	 * 
	 * @return the compilation unit to clean up
	 */
	public ICompilationUnit getCompilationUnit() {
		return fContext.getCompilationUnit();
	}

	/**
	 * An AST built from the compilation unit to fix.
	 * <p>
	 * Can be <code>null</code> if {@link CleanUpRequirements#requiresAST()} returns
	 * <code>false</code>. The AST is guaranteed to contain changes made by previous clean ups only
	 * if {@link CleanUpRequirements#requiresFreshAST()} returns <code>true</code>.
	 * </p>
	 * <p>Clients should check the AST API level and do nothing if they are given an AST
	 * they can't handle (see {@link org.eclipse.jdt.core.dom.AST#apiLevel()}).
	 * 
	 * @return an AST or <code>null</code> if none required
	 */
	public CompilationUnit getAST() {
		return fContext.getAST();
	}
}