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
package org.eclipse.jdt.internal.ui.refactoring.reorg;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Abstraction layer for renaming using refactoring.
 */
public interface IRefactoringRenameSupport {
	
	/**
	 * Returns the refactoring used by this rename support.
	 */
	public IRenameRefactoring getRefactoring();
	
	/**
	 * whether the rename action should be enabled
	 */
	public boolean canRename(Object element) throws JavaModelException;
	
	/**
	 * Does a light precondition check. Clients can assume that performing this
	 * check if fast.
	 */
	public RefactoringStatus lightCheck() throws JavaModelException;
	
	/**
	 * Do the rename
	 */
	public void rename(Shell parent, Object element) throws JavaModelException;
}
