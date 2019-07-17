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
 *     Red Hat Inc. - modified to use internal class to perform logic
 *******************************************************************************/
package org.eclipse.jdt.ui.cleanup;

import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;


/**
 * Specifies the requirements of a clean up.
 *
 * @since 3.5
 */
public final class CleanUpRequirements {

	private CleanUpRequirementsCore requirementsCore;

	/**
	 * Create a new instance
	 *
	 * @param requiresAST <code>true</code> if an AST is required
	 * @param requiresFreshAST <code>true</code> if a fresh AST is required
	 * @param requiresChangedRegions <code>true</code> if changed regions are required
	 * @param compilerOptions map of compiler options or <code>null</code> if no requirements
	 */
	public CleanUpRequirements(boolean requiresAST, boolean requiresFreshAST, boolean requiresChangedRegions, Map<String, String> compilerOptions) {
		this.requirementsCore= new CleanUpRequirementsCore(requiresAST, requiresFreshAST, requiresChangedRegions, compilerOptions);
	}

	/**
	 * Create a new instance
	 *
	 * @param requirementsCore a CleanUpRequirementsCore instance to use
	 * @since 3.19
	 */
	public CleanUpRequirements(CleanUpRequirementsCore requirementsCore) {
		this.requirementsCore= requirementsCore;
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
		return requirementsCore.requiresAST();
	}

	/**
	 * Tells whether a fresh AST, containing all the changes from previous clean ups, will be
	 * needed.
	 *
	 * @return <code>true</code> if the caller needs an up to date AST
	 */
	public boolean requiresFreshAST() {
		return requirementsCore.requiresFreshAST();
	}

	/**
	 * Required compiler options.
	 *
	 * @return the compiler options map or <code>null</code> if none
	 * @see JavaCore
	 */
	public Map<String, String> getCompilerOptions() {
		return requirementsCore.getCompilerOptions();
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
		return requirementsCore.requiresChangedRegions();
	}

}