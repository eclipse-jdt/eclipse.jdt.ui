/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstraction layer for renaming.
 */
public interface IRenameSupport {
	/**
	 * whether the rename action should be enabled
	 */
	boolean canRename(Object element);
	/**
	 * Do the rename
	 */
	Object rename(Object element, String newName, IProgressMonitor pm) throws JavaModelException, CoreException;
}