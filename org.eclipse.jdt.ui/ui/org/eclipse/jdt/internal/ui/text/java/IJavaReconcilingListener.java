/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.dom.CompilationUnit;


/**
 * Interface of an object listening to Java reconciling.
 *
 * @since 3.0
 */
public interface IJavaReconcilingListener {

	/**
	 * Called before reconciling is started.
	 */
	void aboutToBeReconciled();

	/**
	 * Called after reconciling has been finished.
	 * @param ast				the compilation unit AST or <code>null</code> if
 * 								the working copy was consistent or reconciliation has been cancelled
	 * @param forced			<code>true</code> iff this reconciliation was forced
	 * @param progressMonitor	the progress monitor
	 */
	void reconciled(CompilationUnit ast, boolean forced, IProgressMonitor progressMonitor);
}
