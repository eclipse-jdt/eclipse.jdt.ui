/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstraction layer for renaming using refactoring.
 */
public interface IRefactoringRenameSupport {
	
	/**
	 * whether the rename action should be enabled
	 */
	public boolean canRename(Object element) throws JavaModelException;
	
	/**
	 * Do the rename
	 */
	public void rename(Object element) throws JavaModelException;
}