/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstraction layer for deleting.
 */
public interface IDeleteSupport {
	boolean canDelete(Object o);
	
	// 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
	void delete(Object o, boolean deleteProjectContent, IProgressMonitor pm) throws JavaModelException, CoreException;
	/**
	 * @returns the segment count of the path of the underlying resource (or 0 if the
	 		path length can't be determined.
	 */
	int getPathLength(Object o);
	String getElementName(Object element);
}