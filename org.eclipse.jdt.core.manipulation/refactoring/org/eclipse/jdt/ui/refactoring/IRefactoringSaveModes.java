/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - moved constants from org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper
 *******************************************************************************/
package org.eclipse.jdt.ui.refactoring;

/**
 * Constants moved from RefactoringSaveHelper for the purposes of making them
 * available to non-UI bundles.
 *
 * @since 1.20
 */
public interface IRefactoringSaveModes {

	/**
	 * Save mode to save all dirty editors (always ask).
	 */
	public static final int SAVE_ALL_ALWAYS_ASK= 1;

	/**
	 * Save mode to save all dirty editors.
	 */
	public static final int SAVE_ALL= 2;

	/**
	 * Save mode to not save any editors.
	 */
	public static final int SAVE_NOTHING= 3;

	/**
	 * Save mode to save all editors that are known to cause trouble for Java refactorings, e.g.
	 * editors on compilation units that are not in working copy mode.
	 */
	public static final int SAVE_REFACTORING= 4;

}
