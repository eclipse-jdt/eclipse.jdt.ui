/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstraction layer for renaming using refactoring.
 */
public interface IRefactoringRenameSupport {
	
	/**
	 * whether the rename action should be enabled
	 */
	public boolean canRename(Object element);
	
	/**
	 * Do the rename
	 */
	public void rename(Object element);
}