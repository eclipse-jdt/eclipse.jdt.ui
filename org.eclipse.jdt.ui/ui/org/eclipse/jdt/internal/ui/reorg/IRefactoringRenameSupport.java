/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

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