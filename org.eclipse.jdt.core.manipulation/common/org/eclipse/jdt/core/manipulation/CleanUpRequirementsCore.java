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
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation;

import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.JavaCore;


/**
 * Specifies the requirements of a clean up.
 *
 * @since 1.12
 */
final public class CleanUpRequirementsCore {

	protected final boolean fRequiresAST;

	protected final Map<String, String> fCompilerOptions;

	protected final boolean fRequiresFreshAST;

	protected final boolean fRequiresChangedRegions;


	/**
	 * Create a new instance
	 *
	 * @param requiresAST <code>true</code> if an AST is required
	 * @param requiresFreshAST <code>true</code> if a fresh AST is required
	 * @param requiresChangedRegions <code>true</code> if changed regions are required
	 * @param compilerOptions map of compiler options or <code>null</code> if no requirements
	 */
	public CleanUpRequirementsCore(boolean requiresAST, boolean requiresFreshAST, boolean requiresChangedRegions, Map<String, String> compilerOptions) {
		Assert.isLegal(!requiresFreshAST || requiresAST, "Must not request fresh AST if no AST is required"); //$NON-NLS-1$
		Assert.isLegal(compilerOptions == null || requiresAST, "Must not provide options if no AST is required"); //$NON-NLS-1$
		fRequiresAST= requiresAST;
		fRequiresFreshAST= requiresFreshAST;
		fRequiresChangedRegions= requiresChangedRegions;

		fCompilerOptions= compilerOptions;
		// Make sure that compile warnings are not suppressed since some clean ups work on reported warnings
		if (fCompilerOptions != null)
			fCompilerOptions.put(JavaCore.COMPILER_PB_SUPPRESS_WARNINGS, JavaCore.DISABLED);
	}

	/**
	 * Tells whether the clean up requires an AST.
	 * <p>
	 * <strong>Note:</strong> This should return <code>false</code> whenever possible because
	 * creating an AST is expensive.
	 * </p>
	 *
	 * @return <code>true</code> if the CleanUpContext context must provide an AST
	 */
	public boolean requiresAST() {
		return fRequiresAST;
	}

	/**
	 * Tells whether a fresh AST, containing all the changes from previous clean ups, will be
	 * needed.
	 *
	 * @return <code>true</code> if the caller needs an up to date AST
	 */
	public boolean requiresFreshAST() {
		return fRequiresFreshAST;
	}

	/**
	 * Required compiler options.
	 *
	 * @return the compiler options map or <code>null</code> if none
	 * @see JavaCore
	 */
	public Map<String, String> getCompilerOptions() {
		return fCompilerOptions;
	}

	/**
	 * Tells whether this clean up requires to be informed about changed regions. The changed regions are the
	 * regions which have been changed between the last save state of the compilation unit and its
	 * current state.
	 * <p>
	 * Has only an effect if the clean up is used as save action.
	 * </p>
	 * <p>
	 * <strong>Note:</strong>: This should return <code>false</code> whenever possible because
	 * calculating the changed regions is expensive.
	 * </p>
	 *
	 * @return <code>true</code> if the CleanUpContext context must provide changed
	 *         regions
	 */
	public boolean requiresChangedRegions() {
		return fRequiresChangedRegions;
	}

}